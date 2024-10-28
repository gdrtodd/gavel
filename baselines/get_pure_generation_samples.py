import argparse
import json
import os
import random

from datasets import load_dataset
import numpy as np
from transformers import AutoModelForCausalLM, AutoTokenizer, GenerationConfig, set_seed
from tqdm import tqdm

from mutators import LLMMutator
from utils import _extract_parentheticals

parser = argparse.ArgumentParser()
parser.add_argument("--temperature", type=float, default=1)
parser.add_argument("--seed", type=int, default=0)

args = parser.parse_args()

set_seed(args.seed)

print(f"Running with seed {args.seed} and temperature {args.temperature}")

MODEL_NAME = "LudiiLMs/code-llama-13b-fitm-mask-heldout-1-epoch"
NUM_SAMPLES = 4500

run_args = argparse.Namespace() 
run_args.__dict__ = json.load(open(os.path.join("./exp_outputs", "main_experiment_seed_1_05_18_24", "run_args.json"), "r"))
dataset_val = load_dataset(run_args.model_name + '-base-data', split='val', token=os.environ['HF_TOKEN'])

# We exclude Amazons because in the initial run it was not included in the archive
# on account of occupying the same cell as another game (and having lower fitness)
base_games = [d['base_game'] for d in dataset_val if d['name'] != "Amazons"]

model = AutoModelForCausalLM.from_pretrained(run_args.model_name)
tokenizer = AutoTokenizer.from_pretrained(run_args.model_name)

config = GenerationConfig(
    temperature=args.temperature,
    num_beams=run_args.mutation_beam_size,
    num_beam_groups=run_args.mutation_beam_size,
    diversity_penalty=run_args.mutation_diversity_penalty,

    do_sample=True,
    num_return_sequences=run_args.num_mutations,
    renormalize_logits=True,
)

mutator = LLMMutator(model, tokenizer, config)

all_new_samples = []
for _ in tqdm(range(NUM_SAMPLES // run_args.num_mutations), desc="Generating samples", total=NUM_SAMPLES // run_args.num_mutations):
    game = random.choice(base_games)
    parentheticals = _extract_parentheticals(game)
    prefix, middle, suffix, depth = random.choice(parentheticals)

    new_games = mutator._standard_mutate(prefix, suffix)
    all_new_samples.extend(new_games)

# Save the samples
temp_str = str(args.temperature).replace(".", "dot")
with open(f"./baselines/pure_generation_outputs/samples_temp={temp_str}_seed={args.seed}.npy", "wb") as f:
    np.save(f, all_new_samples)