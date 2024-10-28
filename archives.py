from collections import defaultdict
import pickle
import os
import random
import typing

from datasets import load_dataset
import numpy as np
from sklearn.decomposition import PCA
from tqdm import tqdm

from config import ArchiveGame, EliteSelectionStrategy
from java_api import Concepts
from java_helpers import CONCEPT_NAMES, CONCEPT_DTYPES
from fitness_helpers import UNCOMPILABLE_FITNESS

class MAPElitesArchive():
    '''
    A MAP-Elites archive using a subset of Ludii concepts as binary behavioral characteristics
    '''
    
    def __init__(self, entries_per_cell: int):
        self.entries_per_cell = entries_per_cell
        self.archive = defaultdict(list)

        # Necerssary for UCB selection
        self.samples_per_cell = defaultdict(int)
        self.successful_mutations_per_cell = defaultdict(int)
        self.num_samples = 0

    def __len__(self):
        return len(self.archive)

    def _get_cell(self, game: ArchiveGame):
        raise NotImplementedError
    
    @property
    def max_fitness(self):
        return max([elite.fitness_score for cell in self.archive.values() for elite in cell])

    @property
    def avg_fitness(self):
        return np.mean([elite.fitness_score for cell in self.archive.values() for elite in cell])
    
    @property
    def size(self):
        return sum([len(cell) for cell in self.archive.values()])
    
    @property
    def _ucb_stats(self):
        samples_per_cell = {str(cell): n for cell, n in self.samples_per_cell.items()}
        successful_mutations_per_cell = {str(cell): n for cell, n in self.successful_mutations_per_cell.items()}
        return {
            "samples_per_cell": samples_per_cell,
            "successful_mutations_per_cell": successful_mutations_per_cell,
            "num_samples": self.num_samples
        }
    
    def _ucb_value(self, cell: typing.Union[int, tuple]):
        '''
        Compute the UCB value for the specified key
        '''

        if self.samples_per_cell[cell] == 0:
            return float("inf")
        
        exploitation = self.successful_mutations_per_cell[cell] / self.samples_per_cell[cell]
        exploration = (2 * np.log(self.num_samples) / self.samples_per_cell[cell]) ** 0.5

        return exploitation + exploration
    
    def select_elites(self, num_elites: int, strategy: EliteSelectionStrategy):
        '''
        Selects one or more elites from the archive using the specified selection strategy
        '''
        
        if strategy == EliteSelectionStrategy.RANDOM:
            all_elites = [elite for cell in self.archive.values() for elite in cell]
            return random.sample(all_elites, num_elites)

        elif strategy == EliteSelectionStrategy.UCB:
            selected_cells = []
            selected_elites = []

            cells_and_values = [(cell, self._ucb_value(cell)) for cell in self.archive.keys()]

            for _ in range(num_elites):
                unselected_cells_and_values = [(cell, value) for cell, value in cells_and_values if cell not in selected_cells]
                best_cell = max(unselected_cells_and_values, key=lambda cell_and_value: cell_and_value[1])[0]

                selected_cells.append(best_cell)
                selected_elites.append(random.choice(self.archive[best_cell]))
            
            return selected_elites
        
    def add_entry(self, game: ArchiveGame):
        '''
        Adds an entry to the archive if either:
        - the archive cell has fewer than entries_per_cell entries
        - the game beats an existing entry in the archive cell in a fitness comparison

        Returns whether the game was added to the archive
        '''

        # Uncompilable games should never be added to the archive (since they don't have valid cells)
        if game.fitness_score == UNCOMPILABLE_FITNESS or np.isnan(game.fitness_score):
            return False

        cell = self._get_cell(game)
        
        if len(self.archive[cell]) < self.entries_per_cell:
            self.archive[cell].append(game)
            return True

        else:
            elite_fitnesses = sorted([elite.fitness_score for elite in self.archive[cell]], reverse=True)

            for idx, fitness in enumerate(elite_fitnesses):
                if game.fitness_score > fitness:
                    new_elites = self.archive[cell][:idx] + [game] + self.archive[cell][idx:-1] # ensures elites stay in sorted order
                    self.archive[cell] = new_elites
                    return True
                
        return False
    
    def update_ucb_stats(self, game: ArchiveGame, success: bool):
        '''
        Updates the UCB statistics for the specified game
        '''
        cell = self._get_cell(game)
        self.samples_per_cell[cell] += 1
        self.successful_mutations_per_cell[cell] += int(success)

        self.num_samples += 1

class SelectedConceptArchive(MAPElitesArchive):
    def __init__(self,
                 concept_list: list,
                 entries_per_cell: int):
        
        super().__init__(entries_per_cell)
        
        self.concept_idxs = [CONCEPT_NAMES.index(concept) for concept in concept_list]
        self.concept_generator = Concepts()
    
    @property
    def max_size(self):
        return 2**len(self.concept_idxs)
    
    @property
    def num_bcs_active(self):
        '''
        Returns the number of BCs for which at least one elite has it set to True
        '''

        active_keys = np.array(list(self.archive.keys()))
        num_active = np.any(active_keys, axis=0).sum()
        return num_active

    def _get_cell(self, game: ArchiveGame):
        '''
        Returns the cell of the archive that the game belongs to
        '''

        all_game_concepts = self.concept_generator.compile(game.game_str)
        concept_values = tuple([all_game_concepts[idx] for idx in self.concept_idxs])

        return concept_values
    
class ConceptPCAArchive(MAPElitesArchive):
    def __init__(self,
                 only_boolean: bool,
                 pca_dims: int,
                 cells_per_dim: int,
                 dim_extents: list,
                 entries_per_cell: int,
                 cache_dir: str = "./caches"):
        
        super().__init__(entries_per_cell)
        
        self.concept_generator = Concepts()

        self.pca_type = "boolean" if only_boolean else "all"
        self.pca_model = pickle.loads(open(os.path.join(cache_dir, f"{self.pca_type}_concepts_pca_{pca_dims}.pkl"), "rb").read())

        self.cells_per_dim = cells_per_dim
        if isinstance(self.cells_per_dim, list):
            assert len(self.cells_per_dim) == pca_dims, "Number of specified cells per dimension must match the number of PCA dimensions"
            self.cell_boundaries = [np.linspace(dim_extents[0], dim_extents[1], cells + 1) for cells in cells_per_dim]

        else:
            self.cell_boundaries = np.linspace(dim_extents[0], dim_extents[1], cells_per_dim + 1)
    
    @property
    def max_size(self):
        if isinstance(self.cells_per_dim, list):
            return np.prod([cells for cells in self.cells_per_dim])
        else:
            return self.cells_per_dim ** self.pca_model.n_components

    def _get_cell(self, game: ArchiveGame, concepts=None):
        if concepts is None:
            concepts = self.concept_generator.compile(game.game_str)

            if concepts == []:
                return "none"

        if self.pca_type == "boolean":
            concepts = [concept for idx, concept in enumerate(concepts) if CONCEPT_DTYPES[idx] == bool]

        pca_concepts = self.pca_model.transform(np.array(concepts).reshape(1, -1))[0]

        if isinstance(self.cells_per_dim, list):
            cell = tuple([np.digitize(pca_concept, boundaries) for pca_concept, boundaries in zip(pca_concepts, self.cell_boundaries)])

        else:
            cell = tuple([np.digitize(pca_concept, self.cell_boundaries) for pca_concept in pca_concepts])

        return cell
    
class ConceptsAndLengthArchive(MAPElitesArchive):
    '''
    An archive based on both the PCA reduction of Ludii concepts and the length of the game description

    NOTE: unlike other archives, this one expects to receive the dataset so it can fit the PCA model on the fly
    '''
    def __init__(self,
                 dataset_name: str,
                 pca_dims: int,
                 cells_per_pca_dim: int,
                 num_length_cells: int,
                 dim_extents: typing.Optional[list] = None,
                 boundary_type: str = "percentile",
                 entries_per_cell: int = 1,
                 seed: int = 42):
        
        super().__init__(entries_per_cell)

        self.concept_generator = Concepts()
        self.pca_dims = pca_dims
        self.cells_per_pca_dim = cells_per_pca_dim
        self.num_length_cells = num_length_cells

        dataset_train = load_dataset(dataset_name + '-base-data', split='train', token=os.environ['HF_TOKEN'])
        dataset_val = load_dataset(dataset_name + '-base-data', split='val', token=os.environ['HF_TOKEN'])

        concepts_per_train_game = []
        for train_game in tqdm(dataset_train['base_game'], desc='Extracting concepts from training games', leave=False):
            concepts = self.concept_generator.compile(train_game)
            concepts = [concept for idx, concept in enumerate(concepts) if CONCEPT_DTYPES[idx] == bool]
            concepts_per_train_game.append(concepts)

        concepts_per_val_game = []
        for val_game in tqdm(dataset_val['base_game'], desc='Extracting concepts from validation games', leave=False):
            concepts = self.concept_generator.compile(val_game)
            concepts = [concept for idx, concept in enumerate(concepts) if CONCEPT_DTYPES[idx] == bool]
            concepts_per_val_game.append(concepts)

        concepts_per_game = concepts_per_train_game + concepts_per_val_game

        # Fit a PCA model to the data, which is used to construct the archive boundaries
        self.pca_model = PCA(n_components=pca_dims, random_state=seed)
        self.pca_model.fit(concepts_per_game)
        concepts_pca = self.pca_model.transform(concepts_per_game)

        self.boundaries_per_dim = []

        for i in range(self.pca_dims):
            if dim_extents is None:
                min_extent, max_extent = concepts_pca[:, i].min(), concepts_pca[:, i].max()
            else:
                min_extent, max_extent = dim_extents

            if boundary_type == "uniform":
                boundaries = np.linspace(min_extent, max_extent, cells_per_pca_dim + 1) 
            elif boundary_type == "percentile":
                boundaries = np.percentile(concepts_pca[:, i], np.linspace(0, 100, cells_per_pca_dim + 1))
            else:
                raise ValueError(f"Boundary type {boundary_type} not recognized")
            
            self.boundaries_per_dim.append(boundaries)

        game_lengths = [len(game) for game in dataset_train['base_game'] + dataset_val['base_game']]
        self.length_boundaries = np.linspace(0, max(game_lengths), num_length_cells + 1)

    @property
    def max_size(self):
        return (self.cells_per_dim ** self.pca_model.n_components) * self.num_length_cells

    def _get_cell(self, game: ArchiveGame):
        concepts = self.concept_generator.compile(game.game_str)
        concepts = [concept for idx, concept in enumerate(concepts) if CONCEPT_DTYPES[idx] == bool]
        pca_concepts = self.pca_model.transform(np.array(concepts).reshape(1, -1))[0]

        pca_dimension_indices = [np.digitize(c, boundaries) for c, boundaries in zip(pca_concepts, self.boundaries_per_dim)]
        length_index = [np.digitize(len(game.game_str), self.length_boundaries)]

        cell = tuple(pca_dimension_indices + length_index)

        return cell