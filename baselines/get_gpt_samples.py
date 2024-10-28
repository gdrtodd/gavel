import argparse
import json
import os
import random

from datasets import load_dataset
import numpy as np
import outlines
from openai import OpenAI
import tiktoken
from tqdm import tqdm

from utils import pretty_format_single_line_game

def get_model_response(client: OpenAI,
                       model_name: str,
                       messages: list,
                       temperature: float = 1.0,
                       max_tokens: int = 1000,
                       seed: int = 0) -> str:

    '''
    Obtain a response from the OpenAI API, given the client /
    model name / context / hyperparameters
    '''

    completion = client.chat.completions.create(
        model=model_name,
        messages=messages,
        temperature=temperature,
        max_tokens=max_tokens,
        seed=seed
    )

    return completion.choices[0].message.content

def format_prompt(reference_games, test_game):
    '''
    Format the prompt for the model
    '''

    prompt = "Your task is to mutate a game written in the Ludii game description language to produce a new game. Use the following games as reference for proper Ludii syntax and game structure:\n\n"

    for i, game in enumerate(reference_games):
        prompt += f"=====Game {i + 1}======:\n{game}\n==========\n\n"

    prompt += "Now, create a modification of the following game. Make sure to obey the constraints of the Ludii grammar to create a syntactically-valid games. In addition, make sure to modify at least part of the game so that it becomes a new game. Do not simply copy an existing game.\n\n"

    prompt += f"=====Game to modify=====\n{test_game}\n==========\n\n"

    prompt += "=====Modified game=====\n"

    return prompt


MODEL_NAME = "gpt-4o"
USE_TRAIN = False
TRAIN_GAMES = 100

NUM_SAMPLES = 4500
RUN_NAME_SUFFIX = "_9000-13500"
SEED_OFFSET = 9000

SAVE_FREQ = 100

# Load the val dataset from the main experiment
run_args = argparse.Namespace() 
run_args.__dict__ = json.load(open(os.path.join("./exp_outputs", "main_experiment_seed_1_05_18_24", "run_args.json"), "r"))
dataset_train = load_dataset(run_args.model_name + '-base-data', split='train', token=os.environ['HF_TOKEN'])
dataset_val = load_dataset(run_args.model_name + '-base-data', split='val', token=os.environ['HF_TOKEN'])

train_games = [d['base_game'] for d in dataset_train]
# train_formatted_games = [pretty_format_single_line_game(game) for game in train_games]

# We exclude Amazons because in the initial run it was not included in the archive
# on account of occupying the same cell as another game (and having lower fitness)
names = [d['name'] for d in dataset_val if d['name'] != "Amazons"]
base_games = [d['base_game'] for d in dataset_val if d['name'] != "Amazons"]
formatted_games = [pretty_format_single_line_game(game) for game in base_games]

CLIENT = OpenAI(api_key=os.environ["OPENAI_API_KEY"])
ENCODER = tiktoken.encoding_for_model(MODEL_NAME)

system_prompt = "You are an expert programming agent in the Ludii game description language. You output syntactically correct Ludii game descriptions and no other text of any kind."

if USE_TRAIN:
    SAVE_DIR = f"./baselines/gpt_outputs/{MODEL_NAME}_including_train_outputs{RUN_NAME_SUFFIX}.npy"

else:
    SAVE_DIR = f"./baselines/gpt_outputs/{MODEL_NAME}_outputs{RUN_NAME_SUFFIX}.npy"

if os.path.exists(SAVE_DIR):
    print(f"Loading existing samples from '{SAVE_DIR}'")
    samples = np.load(open(SAVE_DIR, "rb"), allow_pickle=True).tolist()
    num_initial_samples = len(samples)
else:
    samples = []
    num_initial_samples = 0


for i in tqdm(range(NUM_SAMPLES), desc=f"Generating samples with model '{MODEL_NAME}'", total=NUM_SAMPLES):

    if i < num_initial_samples:
        continue

    if USE_TRAIN:
        game = random.choice(base_games)
        remaining_games = train_games.copy()
        random.shuffle(remaining_games)
        remaining_games = remaining_games[:TRAIN_GAMES]

    else:
        game = random.choice(base_games)
        remaining_games = base_games.copy()
        remaining_games.remove(game)

    prompt = format_prompt(remaining_games, game)

    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": prompt}
    ]

    response = get_model_response(CLIENT, MODEL_NAME, messages, seed=i+SEED_OFFSET)
    samples.append(response)

    if i % SAVE_FREQ == 0:
        with open(SAVE_DIR, "wb") as f:
            np.save(f, samples)


# Final save
with open(SAVE_DIR, "wb") as f:
    np.save(f, samples)