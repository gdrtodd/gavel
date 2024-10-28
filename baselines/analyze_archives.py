import json
import os
import sys

from datasets import load_dataset
import matplotlib.pyplot as plt
import numpy as np
import pickle
from ribs.archives import CVTArchive, SlidingBoundariesArchive
import seaborn as sns
from sklearn.decomposition import PCA
import sklearn.feature_extraction.text as sk_text
from sklearn.manifold import TSNE
from tqdm import tqdm
import tabulate

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
from archives import ConceptPCAArchive, ConceptsAndLengthArchive
from config import ArchiveGame
from ludii_datasets import _collect_games
from java_api import Concepts
from java_helpers import CONCEPT_DTYPES

DATASET_NAME = "LudiiLMs/code-llama-13b-fitm-mask-heldout-1-epoch"
dataset_train = load_dataset(DATASET_NAME + '-base-data', split='train', token=os.environ['HF_TOKEN'])
dataset_val = load_dataset(DATASET_NAME + '-base-data', split='val', token=os.environ['HF_TOKEN'])

concept_extractor = Concepts()
concepts_per_train_game = []
for train_game in tqdm(dataset_train['base_game'], desc='Extracting concepts from training games'):
    concepts = concept_extractor.compile(train_game)
    concepts = [concept for idx, concept in enumerate(concepts) if CONCEPT_DTYPES[idx] == bool]
    concepts_per_train_game.append(concepts)

concepts_per_val_game = []
for val_game in tqdm(dataset_val['base_game'], desc='Extracting concepts from validation games'):
    concepts = concept_extractor.compile(val_game)
    concepts = [concept for idx, concept in enumerate(concepts) if CONCEPT_DTYPES[idx] == bool]
    concepts_per_val_game.append(concepts)

concepts_per_game = concepts_per_train_game + concepts_per_val_game

def get_unique_cells(transformed_concepts, boundaries_per_dim):
    unique_cells = set()
    for entry in transformed_concepts:
        cell = tuple([np.digitize(c, boundaries) for c, boundaries in zip(entry, boundaries_per_dim)])
        unique_cells.add(cell)
    return unique_cells

DIMENSIONS = [2, 3, 4, 5, 10]
CELLS = [100, 500, 1000, 1500, 2500, 5000, 10000, 50000, 100000]
TEST_CVT = True

results_all = []
results_val = []
for dim in DIMENSIONS:
    if TEST_CVT:
        continue

    dim_results_all = [f"{dim}D"]
    dim_results_val = [f"{dim}D"]
    for num_cells in CELLS:
        cells_per_dim = int(num_cells ** (1 / dim))
        archive_size = cells_per_dim ** dim

        if cells_per_dim ** dim < num_cells:
            initial_cells = int(num_cells / (cells_per_dim ** (dim-1)))
            cells_per_dim = [initial_cells] + [int(num_cells ** (1 / dim))] * (dim-1)
            archive_size = np.prod(cells_per_dim)

        print(f"\nD={dim}, N={num_cells}, C={cells_per_dim}, S={archive_size}")
        archive = ConceptPCAArchive(only_boolean=True, pca_dims=dim, cells_per_dim=cells_per_dim, dim_extents=[-5, 5], entries_per_cell=1)
        train_cells = set([archive._get_cell(None, concepts) for concepts in concepts_per_train_game])
        val_cells = set([archive._get_cell(None, concepts) for concepts in concepts_per_val_game])

        dim_results_all.append(len(train_cells))
        dim_results_val.append(len(val_cells))

    results_all.append(dim_results_all)
    results_val.append(dim_results_val)

if TEST_CVT:
    no_pca_results_all = ["Full"]
    no_pca_results_val = ["Full"]
    for num_cells in CELLS:
        cvt_archive = CVTArchive(solution_dim=510, cells=num_cells, ranges=[[0, 1]] * 510)
        train_cells = set([cvt_archive.index_of_single(x) for x in tqdm(concepts_per_train_game, desc=f"Train cells (N={num_cells})", total=len(concepts_per_train_game))])
        val_cells = set([cvt_archive.index_of_single(x) for x in tqdm(concepts_per_val_game, desc=f"Val cells (N={num_cells})", total=len(concepts_per_val_game))])

        no_pca_results_all.append(len(train_cells))
        no_pca_results_val.append(len(val_cells))

    results_all.append(no_pca_results_all)
    results_val.append(no_pca_results_val)



print("\nAll:")
print(tabulate.tabulate(results_all, headers=[""] + [f"{n} cells" for n in CELLS], tablefmt="github"))

print("\nValidation:")
print(tabulate.tabulate(results_val, headers=[""] + [f"{n} cells" for n in CELLS], tablefmt="github"))

for row in results_all:
    print(" & ".join(map(str, row)) + " \\\\")

for row in results_val:
    print(" & ".join(map(str, row)) + " \\\\")