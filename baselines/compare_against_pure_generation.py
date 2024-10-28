import argparse
from collections import defaultdict
import os
import json
import pickle
import sys

from datasets import load_dataset
import numpy as np
from scipy import stats
from tqdm import tqdm

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
from evolution import ArchiveGame, SelectedConceptArchive, ConceptPCAArchive
from java_helpers import SEMANTIC_BEHAVIORAL_CHARACTERISTICS, SYNTACTIC_BEHAVIORAL_CHARACTERISTICS

def initialize_archive(run_args):
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
        
    return dummy_archive

def get_qd_score(archive, min_fitness=0):
    qd_score = 0
    for key, games in archive.items():
        for game in games:
            qd_score += (game.fitness_score - min_fitness)

    return qd_score

EXP_OUTPUT_PATH = "./exp_outputs"

MAIN_EXP_PATHS = [
    "main_experiment_seed_1_05_18_24",
    "main_experiment_seed_2_05_18_24",
    "main_experiment_seed_3_05_18_24",
]

FITNESS_THRESHOLDS = [-2, 0, 0.5, 0.9]
GENERATION = 500
RESULTS = defaultdict(list)

# Load the base archive from the main experiments
run_args = argparse.Namespace() 
run_args.__dict__ = json.load(open(os.path.join(EXP_OUTPUT_PATH, MAIN_EXP_PATHS[0], "run_args.json"), "r"))

dataset_train = load_dataset(run_args.model_name + '-base-data', split='train', token=os.environ['HF_TOKEN'])
dataset_val = load_dataset(run_args.model_name + '-base-data', split='val', token=os.environ['HF_TOKEN'])
dummy_archive = initialize_archive(run_args)

train_cells = set([dummy_archive._get_cell(ArchiveGame(game['base_game'], 0, {}, 0, [], 0, game['name'])) for game in tqdm(dataset_train, desc="Determinings cells for training games")])
val_cells = set([dummy_archive._get_cell(ArchiveGame(game['base_game'], 0, {}, 0, [], 0, game['name'])) for game in tqdm(dataset_val, desc="Determinings cells for validation games")])

occupied_cells = {
    'train': train_cells,
    'val': val_cells,
    'any': train_cells.union(val_cells)
}

PURE_GENERATION_FILENAMES = [
    "temp=1dot0_seed=1_{0}_section={1}",
    "temp=1dot0_seed=2_{0}_section={1}",
    "temp=1dot0_seed=1_{0}_section={1}"
]

NUM_SECTIONS = 5

qd_scores = []
occupied_cells_by_threshold = defaultdict(list)
novel_occupied_cells_by_threshold = defaultdict(list)

overall_path = "./baselines/pure_generation_outputs/{0}.npy"
for path in tqdm(PURE_GENERATION_FILENAMES, desc="Pure generaiton", total=3):
    archive = {}
    for section in range(1, NUM_SECTIONS + 1):
        fitnesses_path = path.format("fitnesses", section)
        evaluations_path = path.format("evaluations", section)
        
        fitnesses = np.load(overall_path.format(fitnesses_path), allow_pickle=True)
        evaluations = np.load(overall_path.format(evaluations_path), allow_pickle=True)

        for fitness, evaluation in tqdm(zip(fitnesses, evaluations),
                                        total=len(fitnesses),
                                        desc=f"Processing section {section}",
                                        leave=False):
            
            if fitness == -3 or np.isnan(fitness):
                continue

            cell = dummy_archive._get_cell(ArchiveGame(evaluation['game_str'], 0, {}, 0, [], 0, ""))
            if cell not in archive or fitness > archive[cell]:
                archive[cell] = fitness

    qd_score = 0
    for key, fitness in archive.items():
        qd_score += (fitness - -2)
    qd_scores.append(qd_score)

    for threshold in FITNESS_THRESHOLDS:
        occupied = len([1 for fitness in archive.values() if fitness >= threshold])
        novel_occupied = len([1 for cell, fitness in archive.items() if fitness >= threshold and cell not in occupied_cells['any']])

        occupied_cells_by_threshold[threshold].append(occupied)
        novel_occupied_cells_by_threshold[threshold].append(novel_occupied)

print(f"\n=====Run group: PURE GENERATION=====")
print(f"QD score: {np.mean(qd_scores):.3f} ± {np.std(qd_scores):.3f}")
for threshold in FITNESS_THRESHOLDS:
    print(f"Occupied cells with f >= {threshold}: {np.mean(occupied_cells_by_threshold[threshold]):.3f} ± {np.std(occupied_cells_by_threshold[threshold]):.3f}")

for threshold in FITNESS_THRESHOLDS:
    print(f"Novel occupied cells with f >= {threshold}: {np.mean(novel_occupied_cells_by_threshold[threshold]):.3f} ± {np.std(novel_occupied_cells_by_threshold[threshold]):.3f}")

RESULTS["qd_scores"].append(qd_scores)
for threshold in FITNESS_THRESHOLDS:
    RESULTS[f"occupied_cells_{threshold}"].append(occupied_cells_by_threshold[threshold])
    RESULTS[f"novel_occupied_cells_{threshold}"].append(novel_occupied_cells_by_threshold[threshold])

# Compare against main experiment
saved_archives = [pickle.load(open(os.path.join(EXP_OUTPUT_PATH, run_dir, f'archive_{GENERATION}.pkl'), 'rb')) for run_dir in MAIN_EXP_PATHS]

qd_scores = []
occupied_cells_by_threshold = defaultdict(list)
novel_occupied_cells_by_threshold = defaultdict(list)

for archive in saved_archives:
    qd_scores.append(get_qd_score(archive, min_fitness=-2))

    fitness_scores = [game.fitness_score for games in archive.values() for game in games]
    novel_fitness_scores = [game.fitness_score for cell, games in archive.items() for game in games if cell not in occupied_cells['any']]

    for threshold in FITNESS_THRESHOLDS:
        occupied_cells_by_threshold[threshold].append(len([game for games in archive.values() for game in games if game.fitness_score >= threshold]))
        novel_occupied_cells_by_threshold[threshold].append(len([game for cell, games in archive.items() for game in games if game.fitness_score >= threshold and cell not in occupied_cells['any']]))

print(f"\n=====Run group: MAIN EXPERIMENT=====")
print(f"QD score: {np.mean(qd_scores):.3f} ± {np.std(qd_scores):.3f}")
for threshold in FITNESS_THRESHOLDS:
    print(f"Occupied cells with f >= {threshold}: {np.mean(occupied_cells_by_threshold[threshold]):.3f} ± {np.std(occupied_cells_by_threshold[threshold]):.3f}")

for threshold in FITNESS_THRESHOLDS:
    print(f"Novel occupied cells with f >= {threshold}: {np.mean(novel_occupied_cells_by_threshold[threshold]):.3f} ± {np.std(novel_occupied_cells_by_threshold[threshold]):.3f}")

RESULTS["qd_scores"].append(qd_scores)
for threshold in FITNESS_THRESHOLDS:
    RESULTS[f"occupied_cells_{threshold}"].append(occupied_cells_by_threshold[threshold])
    RESULTS[f"novel_occupied_cells_{threshold}"].append(novel_occupied_cells_by_threshold[threshold])

# Welch's t-test
print(f"\n\n=====Statistical tests=====")
for test, [p1, p2] in RESULTS.items():
    t, p = stats.ttest_ind(p1, p2, equal_var=False)
    print(f"{test}: t={t:.3f}, p={p:.3f}")