import argparse
from collections import defaultdict
from functools import partial
import glob
import itertools
import json
import multiprocessing as mp
import os
import pickle
import typing

from datasets import load_dataset
import numpy as np
import pandas as pd
from scipy import stats
from thefuzz import fuzz
from tqdm import tqdm

from evolution import ArchiveGame, SelectedConceptArchive, ConceptPCAArchive
from fitness_helpers import _get_fast_evaluation, _close_fast_evaluation, _evaluate_fitness
from java_api import StandardEvaluation, FastTrace, Novelty
from java_helpers import SEMANTIC_BEHAVIORAL_CHARACTERISTICS, SYNTACTIC_BEHAVIORAL_CHARACTERISTICS
from utils import pretty_format_single_line_game, _remove_extraneous_code

import difflib
import sys

# 1. select a run
# 2. report the number of games in the archive that descend from each initial game
# 3. for each game...
#     - how long are the lineages?
#     - how are the fitnesses distributed?
#     - how many cells away are the children?
#     - how are the novelty scores distributed?
#     - what about edit distance to the ancestor?

parser = argparse.ArgumentParser(description='Select a run to evaluate.')
parser.add_argument('--data_dir', type=str, default="./exp_outputs", help='Directory containing the run outputs')
parser.add_argument('--data_dir_idx', type=int)
args = parser.parse_args()

folders = list(sorted([folder for folder in os.listdir(args.data_dir) if os.path.isdir(os.path.join(args.data_dir, folder))]))

if args.data_dir_idx is None:    
    prompt = "\n".join([f"[{i}]: {folder}" for i, folder in enumerate(folders)])
    selection = int(input(f"Select a folder to evaluate:\n{prompt}\n"))
else:
    selection = args.data_dir_idx

selected_path = os.path.join(args.data_dir, folders[selection])

print(f"Loading run from {selected_path}...")

run_args = argparse.Namespace() 
run_args.__dict__ = json.load(open(os.path.join(selected_path, "run_args.json"), "r"))

# Load run stats JSON into dataframe
run_stats = pd.read_json(os.path.join(selected_path, 'run_stats.json'))

# Load the final archive state
last_epoch = -1
for path in os.listdir(selected_path):
    if path.startswith('archive'):
        epoch = int(path.split('_')[1].split('.')[0])
        if epoch > last_epoch:
            last_epoch = epoch

print(f"Loading archive from epoch {last_epoch}...")
with open(os.path.join(selected_path, f'archive_{last_epoch}.pkl'), 'rb') as f:
    archive = pickle.load(f)

print(f"Loading dataset used during training...")
dataset_train = load_dataset(run_args.model_name + '-base-data', split='train', token=os.environ['HF_TOKEN'])
dataset_val = load_dataset(run_args.model_name + '-base-data', split='val', token=os.environ['HF_TOKEN'])
# dataset_test = load_dataset(run_args.model_name + '-base-data', split='test', token=os.environ['HF_TOKEN'])

name_to_game = {}
# for game in list(dataset_train) + list(dataset_val) + list(dataset_test):
for game in list(dataset_train) + list(dataset_val):
    name_to_game[game['name']] = game['base_game']

print(f"Initializing dummy archive with correct behavioral characteristics...")
# Create the archive
if run_args.archive_type == "selected_concept":

    # Determine the behavioral characteristics to use
    if run_args.bc_type == "syntactic":
        bc_concepts = SYNTACTIC_BEHAVIORAL_CHARACTERISTICS
    elif run_args.bc_type == "semantic":
        bc_concepts = SEMANTIC_BEHAVIORAL_CHARACTERISTICS
    elif run_args.bc_type == "combined":
        bc_concepts = SYNTACTIC_BEHAVIORAL_CHARACTERISTICS + SEMANTIC_BEHAVIORAL_CHARACTERISTICS
    else:
        raise ValueError(f"Behavioral characteristic type {run_args.bc_type} not recognized")
    
    dummy_archive = SelectedConceptArchive(bc_concepts, run_args.entries_per_cell)

elif run_args.archive_type == "pca":
    dummy_archive = ConceptPCAArchive(only_boolean=not run_args.use_all_concepts,
                                      pca_dims=run_args.pca_dims,
                                      cells_per_dim=run_args.cells_per_dim,
                                      dim_extents=run_args.dim_extents,
                                      entries_per_cell=run_args.entries_per_cell)

train_cells = set([dummy_archive._get_cell(ArchiveGame(game['base_game'], 0, {}, 0, [], 0, game['name'])) for game in tqdm(dataset_train, desc="Determinings cells for training games")])
val_cells = set([dummy_archive._get_cell(ArchiveGame(game['base_game'], 0, {}, 0, [], 0, game['name'])) for game in tqdm(dataset_val, desc="Determinings cells for validation games")])
# test_cells = set([dummy_archive._get_cell(ArchiveGame(game['base_game'], 0, {}, 0, [], 0, game['name'])) for game in tqdm(dataset_test, desc="Determinings cells for testing games")])

occupied_cells = {
    'train': train_cells,
    'val': val_cells,
    # 'test': test_cells,
    # 'any': train_cells.union(val_cells).union(test_cells)
    'any': train_cells.union(val_cells)
}

# print("Loading novelty evaluator...")
# novelty = Novelty()
# novelty.load_game_library(dataset_train['base_game'] + dataset_val['base_game'] + dataset_test['base_game'])
# novelty.load_game_library(dataset_train['base_game'] + dataset_val['base_game'])


# Start analysis
games_by_ancestor = defaultdict(list)
all_games = []
games_in_unique_cells = []
for cell, games in archive.items():
    for game in games:
        games_by_ancestor[game.original_game_name].append(game)
        if cell not in occupied_cells['any']:
            games_in_unique_cells.append(game)

        all_games.append(game)

exemplars = []
for ancestor in sorted(games_by_ancestor, key=lambda x: len(games_by_ancestor[x]), reverse=True):
    print("\n" + "=" * 100)
    print(f"ANCESTOR: {ancestor}")

    games = games_by_ancestor[ancestor]

    original_game = name_to_game[ancestor]
    dummy_game = ArchiveGame(original_game, 0, {}, 0, [], 0, ancestor)
    original_game_cell = dummy_archive._get_cell(dummy_game)

    fitness_scores = [game.fitness_score for game in games]
    # novelty_scores = [novelty.evaluate(game.game_str) for game in games]
    cell_deltas = [np.array(dummy_archive._get_cell(game)) - np.array(original_game_cell) for game in games]
    cell_distances = [np.linalg.norm(delta, ord=1) for delta in cell_deltas]
    edit_distances = [1 - (fuzz.ratio(game.game_str, original_game) / 100) for game in games]
    global_edit_distances = [np.min([1 - (fuzz.ratio(game.game_str, comp_game) / 100) for comp_game in name_to_game.values()]) for game in games]
    lineage_lengths = [len(game.lineage) for game in games]
    length_ratios = [min((len(game.game_str) / len(original_game)), (len(original_game) / len(game.game_str))) for game in games]

    print(f"- Number of games: {len(games)}")
    print(f"- Fitnesses: max={np.max(fitness_scores):.2f}, min={np.min(fitness_scores):.2f}, avg={np.mean(fitness_scores):.2f} ± {np.std(fitness_scores):.2f}")
    # print(f"- Novelty: max={np.max(novelty_scores):.2f}, min={np.min(novelty_scores):.2f}, avg={np.mean(novelty_scores):.2f} ± {np.std(novelty_scores):.2f}")
    print(f"- Cell distances: max={np.max(cell_distances):.2f}, min={np.min(cell_distances):.2f}, avg={np.mean(cell_distances):.2f} ± {np.std(cell_distances):.2f}")
    print(f"- Edit distances (w.r.t. ancestor): max={np.max(edit_distances):.2f}, min={np.min(edit_distances):.2f}, avg={np.mean(edit_distances):.2f} ± {np.std(edit_distances):.2f}")
    print(f"- Edit distances (w.r.t. all base games): max={np.max(global_edit_distances):.2f}, min={np.min(global_edit_distances):.2f}, avg={np.mean(global_edit_distances):.2f} ± {np.std(global_edit_distances):.2f}")
    print(f"- Lineage lengths: max={np.max(lineage_lengths):.2f}, min={np.min(lineage_lengths):.2f}, avg={np.mean(lineage_lengths):.2f} ± {np.std(lineage_lengths):.2f}")
    print(f"- Length ratios: max={np.max(length_ratios):.2f}, min={np.min(length_ratios):.2f}, avg={np.mean(length_ratios):.2f} ± {np.std(length_ratios):.2f}")

    # Determine the 'best' game for each ancestor, which has the highest product of fitness and edit distance
    # metrics = [fitness * dist for fitness, dist in zip(fitness_scores, global_edit_distances)]

    # Best game is the most fit that's at least 0.15 in edit distance from its ancestor
    metrics = [fitness if (dist > 0.15 and ratio > 0.5) else 0 for fitness, dist, ratio in zip(fitness_scores, global_edit_distances, length_ratios)]

    if max(metrics) == 0:
        print("No suitable games found for this ancestor.")
        continue

    best_idx = np.argmax(metrics)
    best_game = games[best_idx]

    formatted_best_game = pretty_format_single_line_game(best_game.game_str)
    formatted_original_game = pretty_format_single_line_game(original_game)

    info = "\n".join([
        f"// Ancestor: {ancestor}",
        f"// Selection metric: {metrics[best_idx]:.2f}",
        f"// Fitness: {fitness_scores[best_idx]:.2f}",
        # f"// Novelty: {novelty_scores[best_idx]:.2f}",
        f"// Cell distance: {cell_distances[best_idx]:.2f}",
        f"// Edit Distance: {edit_distances[best_idx]:.2f}",
        f"// Global Edit Distance: {global_edit_distances[best_idx]:.2f}",
        f"// Lineage length: {lineage_lengths[best_idx]:.2f}",
        f"// Length ratio: {length_ratios[best_idx]:.2f}",
    ])

    output_text = f"{formatted_best_game}\n\n{info}"
    diff = difflib.HtmlDiff().make_file(formatted_original_game.splitlines(), formatted_best_game.splitlines(), f"Original ({ancestor})", "New Game")
    exemplars.append((metrics[best_idx], output_text, diff))

    os.makedirs(os.path.join(selected_path, f"{ancestor.lower()}_samples"), exist_ok=True)
    for i, idx in enumerate(np.argsort(metrics)[::-1]):
        metric = metrics[idx]
        if metric == 0:
            break

        game = games[idx]
        formatted_game = pretty_format_single_line_game(game.game_str)
        info = "\n".join([
            f"// Ancestor: {ancestor}",
            f"// Selection metric: {metrics[idx]:.2f}",
            f"// Fitness: {fitness_scores[idx]:.2f}",
            f"// Cell distance: {cell_distances[idx]:.2f}",
            f"// Edit Distance: {edit_distances[idx]:.2f}",
            f"// Global Edit Distance: {global_edit_distances[idx]:.2f}",
            f"// Lineage length: {lineage_lengths[idx]:.2f}",
            f"// Length ratio: {length_ratios[idx]:.2f}",
        ])

        output_text = f"{formatted_game}\n\n{info}"
        diff = difflib.HtmlDiff().make_file(formatted_original_game.splitlines(), formatted_game.splitlines(), f"Original ({ancestor})", "New Game")
        with open(os.path.join(selected_path, f"{ancestor.lower()}_samples", f"game_{i}.lud"), 'w') as f:
            f.write(output_text)
        
        with open(os.path.join(selected_path, f"{ancestor.lower()}_samples", f"game_{i}_diff.html"), 'w') as f:
            f.write(diff)

exemplars.sort(key=lambda x: x[0], reverse=True)
os.makedirs(os.path.join(selected_path, "best_games"), exist_ok=True)

for idx, (metric, output_text, diff) in enumerate(exemplars):
    with open(os.path.join(selected_path, "best_games", f"game_{idx}.lud"), 'w') as f:
        f.write(output_text)
    
    with open(os.path.join(selected_path, "best_games", f"game_{idx}_diff.html"), 'w') as f:
        f.write(diff)


print("\n\n==========GENERAL RUN STATISTICS==========")
print(f"Number of cells occupied in archive: {len(all_games)}")
print(f"Number of cells occupied by games with f>0: {len([game for game in all_games if game.fitness_score > 0])}")
print(f"Number of cells occupied by games with f>0.5: {len([game for game in all_games if game.fitness_score > 0.5])}")
print(f"Number of games in unique cells: {len(games_in_unique_cells)}")
print(f"Number of games in unique cells with f>0: {len([game for game in games_in_unique_cells if game.fitness_score > 0])}")
print(f"Number of games in unique cells with f>0.5: {len([game for game in games_in_unique_cells if game.fitness_score > 0.5])}")