import argparse
from collections import defaultdict
from dataclasses import dataclass
from enum import Enum
import json
import os
import re
import typing

from peft import PeftModel
from pynvml import *
import torch
from transformers import AutoTokenizer, AutoConfig, AutoModelForCausalLM, BitsAndBytesConfig
from tqdm import tqdm

# This seems to be necessary for loading some of the older archives from their pickled state (?)
from config import ArchiveGame
from java_api import Autocomplete, Compile, StandardEvaluation

'''
GPU utilization helper functions from https://huggingface.co/docs/transformers/perf_train_gpu_one#load-model
'''

GREEK_ALPHABET = ["alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda", "mu",
                  "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"]

def gpu_utilization():
    nvmlInit()
    handle = nvmlDeviceGetHandleByIndex(0)
    info = nvmlDeviceGetMemoryInfo(handle)
    return info.used//1024**2

def spin_gpu():
    while True:
        t1 = torch.randn(1000, 1000).cuda()
        t2 = torch.randn(1000, 1000).cuda()

        mult = torch.matmul(t1, t2)
    

MODEL_MAPPING = {"gpt2": "gpt2",
                 "gpt2-large": "gpt2-large",
                 "gpt2-xl": "gpt2-xl",
                 "gpt-j": "EleutherAI/gpt-j-6B",
                 "opt-350m": "facebook/opt-350m",
                 "opt-1.3b": "facebook/opt-1.3b",
                 "opt-2.7b": "facebook/opt-2.7b",
                 "opt-6.7b": "facebook/opt-6.7b",
                 "opt-13b": "facebook/opt-13b",
                 "opt-66b": "facebook/opt-66b",
                 "stablelm-3b": "StabilityAI/stablelm-base-alpha-3b",
                 "stablelm-7b": "StabilityAI/stablelm-base-alpha-7b",
                 "replit": "replit/replit-code-v1-3b",
                 "starcoder": "bigcode/starcoder",
                 "llama-7b": "decapoda-research/llama-7b-hf",
                 "llama-2-7b": "meta-llama/Llama-2-7b-hf",
                 "llama-2-13b": "meta-llama/Llama-2-13b-hf",
                 "code-llama-7b": "codellama/CodeLlama-7b-hf",
                 "code-llama-7b-instruct": "codellama/CodeLlama-7b-Instruct-hf",
                 "code-llama-13b": "codellama/CodeLlama-13b-hf",
                 "code-llama-13b-instruct": "codellama/CodeLlama-13b-Instruct-hf",
                 "code-llama-34b": "codellama/CodeLlama-34b-hf",
                 "code-llama-34b-instruct": "codellama/CodeLlama-34b-Instruct-hf",
                 "phi-2": "microsoft/phi-2",}

def load_model_and_tokenizer(load_dir: str, only_tokenizer: bool = False):
    
    run_args = argparse.Namespace() 
    run_args.__dict__ = json.load(open(os.path.join(load_dir, "run_args.json"), "r"))

    model_name = MODEL_MAPPING.get(run_args.model, run_args.model)

    if only_tokenizer:
        tokenizer = AutoTokenizer.from_pretrained(model_name, use_fast=True)
        return tokenizer

    # Determine the latest checkpoint
    if os.path.exists(os.path.join(load_dir, "final_model")):
        load_dir = os.path.join(load_dir, "final_model")
    else:
        checkpoint_dirs = [os.path.join(load_dir, d) for d in os.listdir(load_dir) if 
                            d.startswith("checkpoint") and os.path.isdir(os.path.join(load_dir, d))]
        
        checkpoint_dirs.sort(key=lambda x: int(x.split("-")[-1]))
        load_dir = checkpoint_dirs[-1]

    # Check to see if a merged version of the model exists -- if not, make one
    merge_dir = load_dir + "_merged"
    if run_args.lora and not os.path.exists(merge_dir):
        print(f"No merged model found, loading initial model from {load_dir}...")
        model = AutoModelForCausalLM.from_pretrained(
            model_name,
            low_cpu_mem_usage=True,
            torch_dtype=torch.float16,
            device_map="cpu",
        )

        tokenizer = AutoTokenizer.from_pretrained(model_name, use_fast=True)

        model = PeftModel.from_pretrained(model, load_dir)

        print("Merging model...")
        model = model.merge_and_unload()

        os.makedirs(merge_dir, exist_ok=True)
        model.save_pretrained(merge_dir, safe_serialization=True)
        tokenizer.save_pretrained(merge_dir)

        del model
        del tokenizer

        final_load_dir = merge_dir

    elif os.path.exists(merge_dir):
        final_load_dir = merge_dir

    else:
        final_load_dir = load_dir

    if run_args.bits is not None:
        quantization_config = BitsAndBytesConfig(
            load_in_8bit=run_args.bits == 8,
            load_in_4bit=run_args.bits == 4,
            bnb_4bit_compute_dtype=torch.float16, # silences UserWarning about default 4Bit dtype being float32
        )
    else:
        quantization_config = None

    print(f"Loading model and tokenizer from {final_load_dir}...")
    model = AutoModelForCausalLM.from_pretrained(
        final_load_dir,
        quantization_config=quantization_config,
        low_cpu_mem_usage=True,
        torch_dtype=torch.float16,
        device_map="auto",
    )

    tokenizer = AutoTokenizer.from_pretrained(model_name, use_fast=True)

    return model, tokenizer

# If a ludeme starts with this token, then everything until the closing parenthesis will be on the same line
ENFORCE_SAME_LINE_TOKENS = ["if", "then", "from", "to", "is", "count", "all", "who", ":", "=", "+", "-"]

# ...unless it's another ludeme that starts with one of these tokens
ENFORCE_NEW_LINE_TOKENS = ["move"]

# After we've formatted / indented the game, we apply an additional newline to these ludemes for readability
ADDITIONAL_SPACING_TOKENS = ["equipment", "rules", "phase", "end"]

def get_current_parenthetical(game: str):
    '''
    Returns the final open parenthetical in the game string, or the empty string
    if no such parenthetical exists
    '''
    for i in range(1, len(game) + 1):
        sub = game[-i:]
        if sub.count("(") > sub.count(")"):
            return sub
    
    return ""

def pretty_format_single_line_game(game: str):
    new_game = "("
    inside_quotes = False

    for i in range(1, len(game)):
        c = game[i]

        if c == '"':
            inside_quotes = not inside_quotes

        if not inside_quotes:            
            if c == '(':
                cur_parenthetical = get_current_parenthetical(new_game)
                enforce_same_line = any([cur_parenthetical[1:].startswith(token) for token in ENFORCE_SAME_LINE_TOKENS])
                enforce_new_line = any([game[i+1:].split(" ")[0] == token for token in ENFORCE_NEW_LINE_TOKENS])
                if enforce_new_line or not enforce_same_line:
                    new_game += "\n"

            new_game += c

            if c == ')':
                cur_parenthetical = get_current_parenthetical(new_game)
                enforce_same_line = any([cur_parenthetical[1:].startswith(token) for token in ENFORCE_SAME_LINE_TOKENS])
                enforce_new_line = any([cur_parenthetical[1:].startswith(token) for token in ENFORCE_NEW_LINE_TOKENS])
                if enforce_new_line or not enforce_same_line:
                    new_game += "\n"

        else:
            new_game += c

    new_game = indent_game(new_game)

    return new_game

def format_single_line_game(game: str):
    '''
    Like the above function, except that every ludeme is on its own line except for leaf nodes
    '''
    new_game = "("
    inside_quotes = False

    for i in range(1, len(game)):
        c = game[i]

        if c == '"':
            inside_quotes = not inside_quotes

        if not inside_quotes:
            # Each ludeme goes on a new line            
            if c == '(':
                new_game += "\n"

            # Closing parentheses go on a new line unless there's only one open parenthetical
            current_parenthical = get_current_parenthetical(new_game)
            if c == ')' and current_parenthical.count('(') > 1:
                new_game += "\n"

            new_game += c

            if c == ')':
                new_game += "\n"

        else:
            new_game += c

    return indent_game(new_game)

def format_multi_line_game(game: str):
    '''
    Invert the above function, taking a game with newlines and spacing and compressing it
    into a single line
    '''

    game = game.replace("\n", " ").strip()

    game = re.sub(r"\s+", " ", game)                    # shorten all whitespace to a single character
    game = re.sub(r"\s(?=[\)}])", "", game)             # remove whitespace before closing brackets
    game = re.sub(r"(?<=[\({])\s", "", game)            # remove whitespace after opening brackets
    game = re.sub(r"(?<=:)\s", "", game)                # remove whitespace after colons

    return game

def indent_game(game: str):

    lines = [line for line in game.split("\n") if line.strip()!= ""]

    # Left justify all lines
    for i in range(len(lines)):
        line = lines[i]
        count = 0
        while count < len(line) and (line[count] == ' ' or line[count] == '\t'):
            line = line[1:]
        lines[i] = line

    indent_lines(lines)

    # Apply additional spacing
    for i in range(len(lines)):
        line = lines[i]
        if any([line.strip()[1:].startswith(token) for token in ADDITIONAL_SPACING_TOKENS]):
            lines[i] = "\n" + line

    return "\n".join(lines)

def remove_double_empty_lines(lines):
    n = 1
    while n < len(lines):
        if lines[n] == "" and lines[n - 1] == "":
            lines.pop(n)
        else:
            n += 1

def indent_lines(lines):
    indent = 0
    for i in range(len(lines)):
        line = lines[i]
        num_open = line.count('(') + line.count('{')
        num_close = line.count(')') + line.count('}')
        difference = num_open - num_close

        if difference < 0:
            indent += difference
            if indent < 0:
                indent = 0

        for _ in range(indent):
            line = "    " + line

        lines[i] = line

        if difference > 0:
            indent += difference

def _extract_parentheticals(game: str):
    '''
    Given a game description, extract all substrings within parentheses. For each substring,
    we return the prefix, actual substring, suffix, and depth
    '''

    parenthetical_idxs_by_depth = defaultdict(list)
    depth = 0

    for idx, char in enumerate(game):
        
        # Add an open parenthetical at the current depth and increase the depth by 1
        if char == "(":
            parenthetical_idxs_by_depth[depth].append([idx, None, depth])
            depth += 1

        # Close the last parenthetical at the current depth and decrease the depth by 1
        elif char == ")":
            depth -= 1
            parenthetical_idxs_by_depth[depth][-1][1] = idx

    parentheticals = []
    for start, end, depth in sum(parenthetical_idxs_by_depth.values(), []):
        prefix = game[:start]
        parenthetical = game[start:end+1]
        suffix = game[end+1:]

        if depth != 0:
            parentheticals.append((prefix, parenthetical, suffix, depth))

    return parentheticals

def _remove_extraneous_code(game: str):
    '''
    Removes all possible parentheticals from the code that don't affect
    its ability to compile
    '''
    compiler = Compile()
    eval = StandardEvaluation()

    done = False
    current_game = game
    while True:
        shortened = False
        parentheticals = _extract_parentheticals(current_game)

        for prefix, parenthetical, suffix, depth in tqdm(parentheticals, desc=f"Shortening game of length {len(current_game)}", leave=False):
            test_game = prefix.strip() + ' ' + suffix.strip()

            evaluation = eval.evaluate(test_game, num_games=5)
            if evaluation['compilable'] and evaluation['playable'] and evaluation['completion'] > 0:
                current_game = test_game
                shortened = True
                break

        if not shortened:
            break

    return current_game