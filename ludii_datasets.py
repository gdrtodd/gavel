import argparse
from collections import defaultdict
import glob
import hashlib
import itertools
import os
import json
import re
import typing
os.environ["BITSANDBYTES_NOWELCOME"] = "true"

import inflect
from datasets import Dataset
import numpy as np
from tqdm import tqdm
from transformers import AutoTokenizer

from config import VALIDATION_GAMES
from utils import GREEK_ALPHABET, load_model_and_tokenizer, _extract_parentheticals

class LudiiDataset():
    '''
    Stable wrapper around train / val / test splits for different kinds of the Ludii dataset
    '''
    def __init__(self,
                 tokenizer: AutoTokenizer,
                 max_length: int,
                 dataset_type: str,
                 mask_names: bool = True,
                 categories: typing.Optional[typing.Union[str, typing.List[str]]] = None,
                 sub_categories: typing.Optional[typing.Union[str, typing.List[str]]] = None,
                 data_dir: str = "./ludii_data/games",
                 cache_dir: str = "./caches",
                 train_prop: float = 0.8,
                 val_prop: float = 0.2,
                 val_set: typing.Optional[typing.List[str]] = None,
                 seed: int = 42):
        
        assert dataset_type in ["base", "realized", "expanded", "instruct", "fitm"], "Invalid dataset type"
        assert train_prop + val_prop <= 1 or (val_set is not None), "Train and val proportions cannot sum to more than 1"

        self.tokenizer = tokenizer
        
        args_string = f"{type(tokenizer)}_{max_length}_{dataset_type}_{mask_names}_{categories}_{sub_categories}_{train_prop}_{val_prop}_{val_set}_{seed}"
        stable_hash = hashlib.md5(args_string.encode()).hexdigest()

        train_filename = os.path.join(cache_dir, f"ludii_train_{stable_hash}.json")
        val_filename = os.path.join(cache_dir, f"ludii_val_{stable_hash}.json")

        if os.path.exists(train_filename) and os.path.exists(val_filename):
            print(f"Loading Ludii dataset from cache at {train_filename}...")

            with open(train_filename, "r") as file:
                self.train_info = json.load(file)

            with open(val_filename, "r") as file:
                self.val_info = json.load(file)

        else:
            if dataset_type == "base":
                self.base_dir = os.path.join(data_dir, "official")
            elif dataset_type == "realized":
                self.base_dir = os.path.join(data_dir, "realised_options")
            elif dataset_type == "expanded":
                self.base_dir = os.path.join(data_dir, "expanded")
            elif dataset_type == "instruct":
                self.base_dir = os.path.join(data_dir, "instructions")
            elif dataset_type == "fitm":
                self.base_dir = os.path.join(data_dir, "expanded")

            # Collect relevant games
            game_files = _collect_games(self.base_dir, categories, sub_categories)

            engine = inflect.engine()
            final_game_list = []
            game_names = []
            base_games = []

            for game_path in tqdm(game_files, desc="Generating Ludii dataset"):

                game_name = os.path.basename(game_path).split(".")[0]

                with open(game_path, "r") as file:
                    if dataset_type == "instruct":
                        instructions, game = file.read().strip().split("\n")
                    else:
                        game = file.read()

                # If specified, mask out piece / game names
                if mask_names:
                    game = _mask_names(game, engine)

                # Add insutrctions if necessary
                if dataset_type == "instruct":
                    formatted = _format_instruct_with_output(instructions, game)

                elif dataset_type == "fitm":
                    parentheticals = _extract_parentheticals(game)
                    formatted = [_format_fitm(p) for p in parentheticals]

                else:
                    formatted = game

                if not isinstance(formatted, list):
                    formatted = [formatted]

                # Skip games that are too long
                if len(self.tokenizer(formatted[0]).input_ids) > max_length:
                    continue

                final_game_list.append(formatted)
                game_names.append(game_name)
                base_games.append(game)

            # If a specific set of validation games is provided, then use those
            if val_set is not None:
                train_game_names, val_game_names = [], []
                train_base_games, val_base_games = [], []
                train_games, val_games = [], []
                
                for game_name, base_game, formatted in zip(game_names, base_games, final_game_list):
                    if game_name in val_set:
                        val_games = sum([val_games, formatted], [])
                        val_game_names.append(game_name)
                        val_base_games.append(base_game)

                    else:
                        train_games = sum([train_games, formatted], [])
                        train_game_names.append(game_name)
                        train_base_games.append(base_game)

            # Otherwise, shuffle the games and split them into train and val
            else:
                # Shuffle the games and the names in the same way
                rng = np.random.default_rng(seed)
                rng.shuffle(final_game_list)

                rng = np.random.default_rng(seed)
                rng.shuffle(game_names)

                rng = np.random.default_rng(seed)
                rng.shuffle(base_games)

                # Split games into train, val, and test (notably different than splitting on individual
                # training examples in the FITM case where each game produces many examples)
                train_size = int(train_prop * len(final_game_list))
                val_size = int(val_prop * len(final_game_list))

                train_games = sum(final_game_list[:train_size], [])
                train_game_names = game_names[:train_size]
                train_base_games = base_games[:train_size]

                val_games = sum(final_game_list[train_size:train_size+val_size], [])
                val_game_names = game_names[train_size:train_size+val_size]
                val_base_games = base_games[train_size:train_size+val_size]

            self.train_info = {
                "names": train_game_names,
                "data": train_games,
                "base_games": train_base_games
            }
            
            self.val_info = {
                "names": val_game_names,
                "data": val_games,
                "base_games": val_base_games
            }
            
            # Save to cache
            with open(train_filename, "w") as file:
                json.dump(self.train_info, file)

            with open(val_filename, "w") as file:
                json.dump(self.val_info, file)

        # Load into datasets
        self.train = Dataset.from_dict({"text": self.train_info["data"]})
        self.val = Dataset.from_dict({"text": self.val_info["data"]})

    @classmethod
    def _load_from_dir(cls, load_dir: str):
        run_args = argparse.Namespace() 
        run_args.__dict__ = json.load(open(os.path.join(load_dir, "run_args.json"), "r"))

        tokenizer = load_model_and_tokenizer(load_dir, only_tokenizer=True)

        val_set = VALIDATION_GAMES if run_args.use_val_set else None
        dataset = cls(
            tokenizer=tokenizer,
            max_length=run_args.block_size,
            dataset_type=run_args.dataset_type,
            mask_names=run_args.mask_names,
            categories=run_args.data_categories,
            sub_categories=run_args.data_subcategories,
            train_prop=run_args.train_prop,
            val_prop=run_args.val_prop,
            val_set=val_set,
            seed=run_args.seed
        )
        
        return dataset

def _collect_games(base_dir: str,
                   categories: typing.Optional[typing.Union[str, typing.List[str]]] = None,
                   sub_categories: typing.Optional[typing.Union[str, typing.List[str]]] = None):
    '''
    Collect raw games from all categories and sub-categories
    '''
    if categories is None:
        categories = [""]
    elif isinstance(categories, str):
        categories = [categories]

    if sub_categories is None:
        sub_categories = [""]
    elif isinstance(sub_categories, str):
        sub_categories = [sub_categories]

    games = []

    for category, sub_category in itertools.product(categories, sub_categories):
        games.extend(glob.glob(os.path.join(base_dir, category, sub_category, "**", "*.lud"), recursive=True))

    return games

def _mask_names(game: str, engine: inflect.engine):
    '''
    Masks out the names of the game and pieces, replacing them with generic identifiers
    '''
    
    # Mask the game's overall name
    game = re.sub(r"(?<=\(game\s\").*?(?=\")", "GAME_NAME", game)

    # NOTE: it's possible that a string immediately following '(piece' could be a macro / define, at least
    # in principle, but it's unlikely in practice. Still something to keep an eye on

    # Extract all pieces that begin with '(piece', and ignore any trailing numbers
    pieces = re.findall(r"(?<=\(piece\s\").*?(?=[0-9]*\")", game)

    # Dedupe pieces, but keep order
    pieces = list(dict.fromkeys(pieces))

    for idx, piece in enumerate(pieces):
        if idx >= len(GREEK_ALPHABET):
            suffix = engine.number_to_words(idx // len(GREEK_ALPHABET) + 1).upper().replace("-", "_")
            new_name = GREEK_ALPHABET[idx % len(GREEK_ALPHABET)].upper() + "_" + suffix
        else:
            new_name = GREEK_ALPHABET[idx].upper()

        # Exact matches for the piece name
        expr = re.compile(r"(?<=\"){0}(?=\")".format(piece))
        game = re.sub(expr, f"PIECE_{new_name}", game)

        # Matches for the piece name followed by a (player) number
        expr = re.compile(r"{0}(?=[0-9])".format(piece))
        game = re.sub(expr, f"PIECE_{new_name}", game)

    return game

def _format_instruct_with_output(instructions, ludii_code):
    system_prompt = "Provide answers in the Ludii game description language"
    system_instruction = "You are a system that translates natural language descriptions of board games into code in the Ludii game description language. If the natural language description is underspecified, then do your best to generate a valid game which implements all of the specified rules and introduces only the minimal additions in order to make the game playable. Translate the following description into Ludii code:\n\n"

    return f"<s>[INST] <<SYS>>\\n{system_prompt}\\n<</SYS>>\\n\\n{system_instruction}{instructions} [/INST] {ludii_code} </s>"

def _format_instruct_without_output(instructions):
    system_prompt = "Provide answers in the Ludii game description language"
    system_instruction = "You are a system that translates natural language descriptions of board games into code in the Ludii game description language. If the natural language description is underspecified, then do your best to generate a valid game which implements all of the specified rules and introduces only the minimal additions in order to make the game playable. Translate the following description into Ludii code:\n\n"

    return f"<s>[INST] <<SYS>>\\n{system_prompt}\\n<</SYS>>\\n\\n{system_instruction}{instructions} [/INST]"

def _format_fitm(extracted_parenthetical):
    prefix, parenthetical, suffix, _ = extracted_parenthetical

    return f"<s><PRE> {prefix} <SUF>{suffix} <MID> {parenthetical} </s>"

def convert_and_upload_dataset(load_dir: str):
    '''
    Construct the dataset corresponding to the provided run directory, reformat
    it to include the raw text of the games as well, and upload it to the HuggingFace
    hub
    '''
    dataset = LudiiDataset._load_from_dir(load_dir)

    run_name = os.path.basename(load_dir)
    token = os.environ.get("HF_TOKEN", None)
    assert token is not None, "Must set HUGGINGFACE_WRITE_TOKEN environment variable to upload to the hub"

    split_names = ["train", "val"]
    for split, (game_dataset, info) in zip(split_names, [(dataset.train, dataset.train_info), (dataset.val, dataset.val_info)]):
        # Contains the actual data used to train the model
        game_dataset.push_to_hub(f"LudiiLMs/{run_name}-dataset", split=split, token=token, private=True)

        # Contains the names / base games used to construct the above dataset
        base_data = Dataset.from_dict({"name": info['names'], 'base_game': info['base_games']})
        base_data.push_to_hub(f"LudiiLMs/{run_name}-base-data", split=split, token=token, private=True)

    print(f"Successfully uploaded datasets for run {run_name}!")