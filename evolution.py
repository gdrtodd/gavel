import argparse
from datetime import datetime
from functools import partial
import glob
import itertools
import json
import multiprocessing as mp
import os
import pickle
import psutil
import re
import shutil
import threading
import typing

from datasets import load_dataset
import numpy as np
import scipy.stats as stats
from tqdm import tqdm
from transformers import AutoModelForCausalLM, AutoTokenizer, GenerationConfig, set_seed

from archives import MAPElitesArchive, SelectedConceptArchive, ConceptPCAArchive, ConceptsAndLengthArchive
from config import ArchiveGame, MutationSelectionStrategy, EliteSelectionStrategy, MutationStrategy, FitnessEvaluationStrategy, VALIDATION_GAMES
from fitness_helpers import _get_fast_evaluation, _close_fast_evaluation, _evaluate_fitness, _compute_balance, _compute_drawishness, FITNESS_METRIC_KEYS, UNCOMPILABLE_FITNESS
from java_helpers import SYNTACTIC_BEHAVIORAL_CHARACTERISTICS, SEMANTIC_BEHAVIORAL_CHARACTERISTICS
from mutators import LLMMutator
from utils import gpu_utilization, spin_gpu

os.environ["TOKENIZERS_PARALLELISM"] = "false"

ORGANIZATION = "LudiiLMs/"
class MAPElitesSearch():
    def __init__(self,
                 model: AutoModelForCausalLM,
                 tokenizer: AutoTokenizer,
                 config: GenerationConfig,
                 archive: MAPElitesArchive,
                 num_selections: int,
                 elite_selection_strategy: EliteSelectionStrategy,
                 mutation_selection_strategy: MutationSelectionStrategy,
                 mutation_strategy: MutationStrategy,
                 thinking_time: int,
                 games_per_eval: int,
                 max_turns: int,
                 num_fitness_evals: int,
                 fitness_evaluation_strategy: FitnessEvaluationStrategy,
                 fitness_aggregator: typing.Callable,
                 fitness_eval_timeout: int,
                 num_threads: int = 16,
                 save_dir: str = "./exp_outputs/map-elites-test",
                 verbose: bool = False):
        
        self.num_threads = num_threads
        self.save_dir = save_dir
        self.verbose = verbose
        self.spin_gpu = spin_gpu

        # Initialize mutator
        if self.verbose: print("Initializing LLM mutator...")
        self.mutator = LLMMutator(model, tokenizer, config)
        self.mutation_selection_strategy = mutation_selection_strategy
        self.mutation_strategy = mutation_strategy

        # Initialize fitness evaluation function
        self.num_fitness_evals = num_fitness_evals
        self.fitness_aggregator = fitness_aggregator
        self.fitness_eval_timeout = fitness_eval_timeout

        if fitness_evaluation_strategy == FitnessEvaluationStrategy.RANDOM:
            self.evaluation_fn = partial(self._random_eval, timeout_duration=self.fitness_eval_timeout)
        
        elif fitness_evaluation_strategy == FitnessEvaluationStrategy.UCT:
            self.evaluation_fn = partial(self._uct_eval, thinking_time=thinking_time, num_games=games_per_eval,
                                         max_turns=max_turns, timeout_duration=self.fitness_eval_timeout)

        elif fitness_evaluation_strategy == FitnessEvaluationStrategy.ONE_PLY:
            self.evaluation_fn = partial(self._one_ply_eval, thinking_time=thinking_time, num_games=games_per_eval,
                                         max_turns=max_turns, timeout_duration=self.fitness_eval_timeout)

        elif fitness_evaluation_strategy == FitnessEvaluationStrategy.COMBINED:
            self.evaluation_fn = partial(self._combined_eval, thinking_time=thinking_time, num_games=games_per_eval,
                                         max_turns=max_turns, timeout_duration=self.fitness_eval_timeout)

        self.archive = archive
        self.num_selections = num_selections
        self.elite_selection_strategy = elite_selection_strategy

        # Store a record of every evaluated game (in case duplicates are produced)
        self.evaluation_cache = {}

        self.epoch = 0
        self.run_stats = []
        self.ucb_stats = []
        self.error_logs = []

    def _clean(self, game_str: str):
        '''
        Return a 'clean' version of specified game, standardizing the formatting so that
        games can be compared for equivalence
        '''
        game_str = re.sub(r"\s+", " ", game_str)                    # remove extra whitespace
        game_str = re.sub(r"\s(?=[\)}])", "", game_str)             # remove whitespace before closing brackets
        game_str = re.sub(r"(?<=[\({])\s", "", game_str)            # remove whitespace after opening brackets
        game_str = re.sub(r"(?<=:)\s", "", game_str)                # remove whitespace after colons

        return game_str
    
    @staticmethod
    def _random_eval(game_str: str, timeout_duration: int):
        '''
        Wrapper function for evaluating the fitness of a game using a random agent
        '''
        return _get_fast_evaluation(game_str, ai_name="Random", timeout_duration=timeout_duration)
    
    @staticmethod
    def _uct_eval(game_str: str, thinking_time:int, num_games: int, max_turns: int, timeout_duration: int):
        '''
        Wrapper function for evaluating the fitness of a game using the UCT agent
        '''
        return _get_fast_evaluation(game_str, ai_name="UCT", thinking_time=thinking_time, num_games=num_games, 
                                    max_turns=max_turns, timeout_duration=timeout_duration)
    
    @staticmethod
    def _one_ply_eval(game_str: str, thinking_time:int, num_games: int, max_turns: int, timeout_duration: int):
        '''
        Wrapper function for evaluating the fitnes of a game using a one-step lookahead agent
        '''
    
        return _get_fast_evaluation(game_str, ai_name="One-Ply (No Heuristic)", thinking_time=thinking_time, num_games=num_games, 
                                    max_turns=max_turns, timeout_duration=timeout_duration)
    
    @staticmethod
    def _combined_eval(game_str: str, thinking_time:int, num_games: int, max_turns: int, timeout_duration: int):
        '''
        Evaluate the fitness of a game using both the UCT and one-ply agents, returning the average of the two
        '''
        timeout_duration = -1 if timeout_duration == -1 else timeout_duration // 2

        uct_result = _get_fast_evaluation(game_str, ai_name="UCT", thinking_time=thinking_time, num_games=num_games, 
                                          max_turns=max_turns, timeout_duration=timeout_duration)
        
        one_ply_result = _get_fast_evaluation(game_str, ai_name="One-Ply (No Heuristic)", thinking_time=thinking_time, num_games=num_games, 
                                              max_turns=max_turns, timeout_duration=timeout_duration)

        combined_result = {key: (uct_result[key] + one_ply_result[key]) / 2 for key in FITNESS_METRIC_KEYS}

        combined_result["playable"] = uct_result["playable"] and one_ply_result["playable"]
        combined_result["compilable"] = uct_result["compilable"] and one_ply_result["compilable"]
        combined_result["game_str"] = game_str
        combined_result["wins"] = uct_result["wins"] + one_ply_result["wins"]

        return combined_result

    def _get_fitnesses(self, game_strs: typing.List[str], num_evals: int):
        '''
        Performs k evaluations of each of n game strings, returning a list of lists of fitness values.
        Optionally performs evaluations in parallel across multiple threads.
        '''

        # Repeat the input game strings k times
        repeated_game_strs = game_strs * num_evals

        if self.num_threads > 1:

            batched_evaluations = []
            with mp.Pool(self.num_threads) as pool:
                with tqdm(total=len(repeated_game_strs), desc="Evaluating fitnesses (MP)", leave=False) as pbar:
                    for evaluation in pool.imap(self.evaluation_fn, repeated_game_strs):
                        batched_evaluations.append(evaluation)
                        pbar.update(1)
                        pbar.set_postfix({"cpu": np.mean(psutil.cpu_percent(percpu=True)), "mem": psutil.virtual_memory().percent})

                    pool.map(_close_fast_evaluation, repeated_game_strs)

        else:
            batched_evaluations = [self.evaluation_fn(game_str) for game_str in tqdm(repeated_game_strs, desc="Evaluating fitnesses", leave=False)]
            _close_fast_evaluation(repeated_game_strs)

        # Group evaluations by game string and convert to fitnesses
        batched_evaluations = sorted(batched_evaluations, key=lambda evaluation: evaluation["game_str"])

        fitness_scores, evaluations = [], []
        for key, eval_group in itertools.groupby(batched_evaluations, key=lambda evaluation: evaluation["game_str"]):
            eval_group = list(eval_group)

            average_evaluation = {}

            # Compilability and playability shouldn't be difference across different evaluations, so
            # we can just take the first
            average_evaluation["compilable"] = eval_group[0]["compilable"]
            average_evaluation["playable"] = eval_group[0]["playable"]

            # Compute the average evaluation metrics across the group]
            for key in FITNESS_METRIC_KEYS:
                average_evaluation[key] = np.mean([evaluation[key] for evaluation in eval_group])

            # For balance and drawishness, in particular, we manually compute the average from the raw
            # win counts because the micro-average of balance and drawishness is not meaningful
            collected_wins = sum([evaluation["wins"] for evaluation in eval_group], [])
            average_evaluation["balance"] = _compute_balance(collected_wins)
            average_evaluation["drawishness"] = _compute_drawishness(collected_wins)

            fitness_score = _evaluate_fitness(average_evaluation, self.fitness_aggregator)

            fitness_scores.append(fitness_score)
            evaluations.append(average_evaluation)

        return fitness_scores, evaluations
    
    def _save(self, epoch: typing.Optional[int] = None):
        '''
        Save the current archive and stats to the save directory
        '''
        if epoch is None:
            epoch = self.epoch

        archive_filename = os.path.join(self.save_dir, f"archive_{epoch}.pkl")
        stats_filename = os.path.join(self.save_dir, f"run_stats.json")
        errors_filename = os.path.join(self.save_dir, f"errors.json")

        pickle.dump(self.archive.archive, open(archive_filename, "wb"), protocol=pickle.HIGHEST_PROTOCOL)
        json.dump(self.run_stats, open(stats_filename, "w"))
        json.dump(self.error_logs, open(errors_filename, "w"))

        # Dump the UCB stats for the mutator and archive
        joined_ucb_stats = {"epoch": epoch, "archive": self.archive._ucb_stats, "mutator": self.mutator._ucb_stats}
        self.ucb_stats.append(joined_ucb_stats)
        json.dump(self.ucb_stats, open(os.path.join(self.save_dir, f"ucb_stats.json"), "w"))


    def initialize_archive(self, game_strs: typing.List[str], game_names: typing.List[str]):
        '''
        Seed the MAP-Elites archive with the provided set of games and names, evaluating them
        and attempting to add them to the archive
        '''

        # Sort both game strings and names by the game strings
        game_strs, game_names = zip(*sorted(zip(game_strs, game_names)))

        fitness_scores, evaluations = self._get_fitnesses(game_strs, self.num_fitness_evals)
        for game_str, game_name, fitness_score, evaluation in zip(game_strs, game_names, fitness_scores, evaluations):
            game = ArchiveGame(
                game_str=game_str,
                fitness_score=fitness_score,
                evaluation=evaluation,
                lineage=[],
                generation=0,
                original_game_name=game_name,
                epoch=0
            )

            self.archive.add_entry(game)

        if self.verbose:
            cells_filled = len(self.archive)
            prop_filled = 100 * cells_filled / self.archive.max_size

            print(f"\nInitialized archive with {cells_filled} cells ({'%.2f' % prop_filled}%) from {len(game_strs)} initial games")
            print(f"...of the {len(self.archive.concept_idxs)} BCs, {self.archive.num_bcs_active} are active")


    def step(self):
        '''
        Perform a single MAp-Elites step by selecting one or more elites from the archive, mutating them,
        and adding the mutated games back to the archive
        '''

        selected_elites = self.archive.select_elites(self.num_selections, self.elite_selection_strategy)
        
        novel_mutated_game_strs = []
        corresponding_elites = {}
        corresponding_mutations = {}

        for elite in selected_elites:
            mutated_game_strs, selected_mutation = self.mutator.mutate(elite, self.mutation_selection_strategy, self.mutation_strategy)
            
            # Ensure that we don't waste time evaluating duplicate game strings
            for game_str in mutated_game_strs:
                is_duplicate = (self._clean(game_str) == self._clean(elite.game_str))
                
                if game_str not in novel_mutated_game_strs and not is_duplicate:
                    novel_mutated_game_strs.append(game_str)
                    corresponding_elites[game_str] = elite
                    corresponding_mutations[game_str] = selected_mutation
 
                # Record mutations that duplicate the existing game as "failures" for UCB selection
                else:
                    self.archive.update_ucb_stats(elite, False)
                    self.mutator.update_ucb_stats(selected_mutation, False)
   
        # Evaluate novel mutations
        fitness_scores, evaluations = self._get_fitnesses(novel_mutated_game_strs, self.num_fitness_evals)

        # Fitness scores and evaluations arrive sorted by game string, so we need to sort
        # the mutated game strings to match
        novel_mutated_game_strs = list(sorted(novel_mutated_game_strs))

        num_successful_mutations = 0
        for fitness_score, evaluation, game_str in zip(fitness_scores, evaluations, novel_mutated_game_strs):

            elite = corresponding_elites[game_str]
            mutation = corresponding_mutations[game_str]

            mutated_game = ArchiveGame(
                game_str=game_str,
                fitness_score=fitness_score, 
                evaluation=evaluation,
                lineage=elite.lineage + [elite],
                generation=elite.generation + 1,
                original_game_name=elite.original_game_name,
                epoch=self.epoch
            )

            success = self.archive.add_entry(mutated_game)
            num_successful_mutations += int(success)

            # Record the UCB statistics for both the elite section and mutation selection
            self.archive.update_ucb_stats(elite, success)
            self.mutator.update_ucb_stats(mutation, success)

        # Record some run statistics
        prop_novel_mutations = len(novel_mutated_game_strs) / (self.num_selections * self.mutator.config.num_return_sequences)

        # Catch case where no novel mutations were produced
        if len(novel_mutated_game_strs) > 0:
            prop_nan_evals = len([score for score in fitness_scores if np.isnan(score)]) / len(novel_mutated_game_strs)
            prop_invalid_mutations = len([score for score in fitness_scores if score == UNCOMPILABLE_FITNESS]) / len(novel_mutated_game_strs)
            prop_successful_mutations = num_successful_mutations / len(novel_mutated_game_strs)
        else:
            prop_nan_evals = 0
            prop_invalid_mutations = 0
            prop_successful_mutations = 0

        stats = {"epoch": self.epoch, "invalid_mutations": prop_invalid_mutations,  "novel_mutations": prop_novel_mutations, 
                 "successful_mutations": prop_successful_mutations, "archive_size": self.archive.size, "num_cells": len(self.archive),
                 "max_fitness": self.archive.max_fitness, "nan_evals": prop_nan_evals, "avg_fitness": self.archive.avg_fitness}
        
        self.run_stats.append(stats)
        self.epoch += 1

    def search(self, num_epochs: int, save_freq: int):
        '''
        Perform the specified number of steps and save at the specified interval
        '''

        self._save(epoch=0)

        with tqdm(range(num_epochs), desc="Running MAP-Elites search") as pbar:
            for _ in pbar:
                self.step()

                if self.epoch % save_freq == 0:
                    self._save()

                pbar.set_postfix({"Num cells": len(self.archive),
                                  "Archive size": self.archive.size,
                                  "Max fitness": self.archive.max_fitness,
                                  "Avg fitness": self.archive.avg_fitness})

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--model_name', type=str, default="LudiiLMs/code-llama-13b-fitm-mask", help="Name of the model to load")
    parser.add_argument('--verbose', action='store_true', help="Whether to print verbose output")
    parser.add_argument('--spin_gpu', action='store_true', help="Whether to spin the GPU during evaluation")

    # Search arguments
    parser.add_argument('--initial_archive_splits', type=str, nargs="+", choices=["train", "val", "test", "all"], default="val", help="Which split(s) of the dataset to use for the initial archive")
    parser.add_argument('--seed_with_heldout_set', action='store_true', help="Whether to seed the archive with the held-out games in the validation set")
    parser.add_argument('--num_epochs', type=int, default=1000, help="Number of MAPElites steps to perform")
    parser.add_argument('--num_selections', type=int, default=5, help="Number of games to select from the archive per epoch")

    # Archive arguments
    parser.add_argument('--archive_type', default="selected_concept", choices=["selected_concept", "pca", "pca_and_length"], help="Type of archive to use")
    parser.add_argument('--elite_selection_strategy', type=str, default="random", choices=["random", "ucb"], help="Strategy for selecting elites from the archive")
    parser.add_argument('--entries_per_cell', type=int, default=1, help="Number of games to store in each archive cell")
    parser.add_argument('--bc_type', type=str, default="semantic", choices=["syntactic", "semantic", "combined"], help="Type of behavioral characteristic to use for selected-concept archive")
    parser.add_argument('--use_all_concepts', action='store_true', help="Whether to use all concepts (i.e. including non-boolean concepts) for PCA archive")
    parser.add_argument('--pca_dims', type=int, default=2, help="Number of PCA dimensions to use for PCA archive")
    parser.add_argument('--cells_per_dim', type=int, default=40, help="Number of cells to use per PCA dimension")
    parser.add_argument('--dim_extents', type=float, nargs=2, default=[-5, 5], help="Extents of the PCA dimensions")
    parser.add_argument('--num_length_cells', type=int, default=5, help="Number of length cells to use for PCA and length archive")
    
    # Fitness arguments
    parser.add_argument('--fitness_aggregator', type=str, default="hmean", choices=["avg", "hmean", "min"], help="Aggregation function to use when combining evaluation metrics into a fitness score")
    parser.add_argument('--fitness_evaluation_strategy', type=str, default="random", choices=["random", "uct", "one_ply", "combined"], help="Strategy for evaluating fitnesses")
    parser.add_argument('--games_per_eval', type=int, default=5, help="Number of games to simulate for each evaluation")
    parser.add_argument('--thinking_time', type=float, default=0.1, help="Thinking time to use per move when evaluating fitnesses with search agents")
    parser.add_argument('--max_turns', type=int, default=250, help="Maximum number of turns to allow for each game during evaluation")
    parser.add_argument('--num_fitness_evals', type=int, default=5, help="Number of fitness evaluations to average over for each individual")
    parser.add_argument('--num_threads', type=int, default=16, help="Number of threads to use when evaluating fitnesses")
    parser.add_argument('--fitness_eval_timeout', type=int, default=-1, help="Maximum time allowed for fitness evaluation")
    
    # Mutation arguments
    parser.add_argument('--num_mutations', type=int, default=5, help="Number of mutations to perform per selected game")
    parser.add_argument('--mutation_temperature', type=float, default=1.0, help="Temperature to use when performing mutations")
    parser.add_argument('--mutation_beam_size', type=int, default=1, help="Beam size to use when performing mutations")
    parser.add_argument('--mutation_diversity_penalty', type=float, default=0.0, help="Diversity penalty to use when performing mutations across multiple beams")
    parser.add_argument('--mutation_selection_strategy', type=str, default="random", choices=["random", "ucb_depth", "ucb_ludeme"], help="Strategy for selecting where to perform a mutation in a game")
    parser.add_argument('--mutation_strategy', type=str, default="standard", choices=["standard", "grammar_enforced"], help="Strategy for performing a mutation in a game")

    # Save arguments
    parser.add_argument('--overwrite', action='store_true', help="Whether to overwrite the save directory if it already exists")
    parser.add_argument('--save_dir', type=str, default='./exp_outputs/map_elites_test', help="Directory to save the archive")
    parser.add_argument('--add_current_date', action='store_true', help="Whether to add the current date to the save directory")
    parser.add_argument('--save_freq', type=int, default=20, help="How often to save the archive during training")
    parser.add_argument('--seed', type=int, default=42, help="Random seed to use")

    args = parser.parse_args()

    # Add the current date as MM_DD_YY to the save directory, if specified
    if args.add_current_date:
        date_str = datetime.now().strftime("%m_%d_%y")
        args.save_dir = f"{args.save_dir}_{date_str}"

    # Check to see if the save directory already exists
    if os.path.exists(args.save_dir):
        if not args.overwrite:
            response = input(f"Save directory {args.save_dir} already exists. Overwrite? (y/n) ")
            if response.lower() != "y":
                exit("Exiting without overwriting save directory...")

        print("Overwriting save directory...")
        shutil.rmtree(args.save_dir)

    # Save run arguments
    os.makedirs(args.save_dir, exist_ok=True)
    json.dump(args.__dict__, open(os.path.join(args.save_dir, "run_args.json"), "w"), indent=4)

    if args.verbose:
        print(f"Running MAP-Elites search with the following arguments:")
        print(json.dumps(args.__dict__, indent=4))

    # Set the random seed
    set_seed(args.seed)

    # Load the model and tokenizer
    model = AutoModelForCausalLM.from_pretrained(args.model_name)
    tokenizer = AutoTokenizer.from_pretrained(args.model_name)

    config = GenerationConfig(
        temperature=args.mutation_temperature,
        num_beams=args.mutation_beam_size,
        num_beam_groups=args.mutation_beam_size,
        diversity_penalty=args.mutation_diversity_penalty,

        do_sample=True,
        num_return_sequences=args.num_mutations,
        renormalize_logits=True,
    )

    # Create the archive
    if args.archive_type == "selected_concept":

        # Determine the behavioral characteristics to use
        if args.bc_type == "syntactic":
            bc_concepts = SYNTACTIC_BEHAVIORAL_CHARACTERISTICS
        elif args.bc_type == "semantic":
            bc_concepts = SEMANTIC_BEHAVIORAL_CHARACTERISTICS
        elif args.bc_type == "combined":
            bc_concepts = SYNTACTIC_BEHAVIORAL_CHARACTERISTICS + SEMANTIC_BEHAVIORAL_CHARACTERISTICS
        else:
            raise ValueError(f"Behavioral characteristic type {args.bc_type} not recognized")
        
        archive = SelectedConceptArchive(bc_concepts, args.entries_per_cell)

    elif args.archive_type == "pca":
        archive = ConceptPCAArchive(only_boolean=not args.use_all_concepts,
                                    pca_dims=args.pca_dims,
                                    cells_per_dim=args.cells_per_dim,
                                    dim_extents=args.dim_extents,
                                    entries_per_cell=args.entries_per_cell)
        
    elif args.archive_type == "pca_and_length":
        archive = ConceptsAndLengthArchive(
            dataset_name=args.model_name,
            pca_dims=args.pca_dims,
            cells_per_pca_dim=args.cells_per_dim,
            num_length_cells=args.num_length_cells,
            dim_extents=None,
            boundary_type="percentile",
            seed=args.seed
        )


    # Set the fitness aggregator
    if args.fitness_aggregator == "avg":
        fitness_aggregator = np.average
    elif args.fitness_aggregator == "hmean":
        fitness_aggregator = stats.hmean
    elif args.fitness_aggregator == "min":
        fitness_aggregator = lambda x, weights: np.min(x)
    else:
        raise ValueError(f"Fitness aggregator {args.fitness_aggregator} not recognized")

    map_elites = MAPElitesSearch(
        model=model,
        tokenizer=tokenizer,
        config=config,
        archive=archive,
        num_selections=args.num_selections,
        elite_selection_strategy=args.elite_selection_strategy, 
        mutation_selection_strategy=args.mutation_selection_strategy,
        mutation_strategy=args.mutation_strategy,
        num_fitness_evals=args.num_fitness_evals,
        thinking_time=args.thinking_time,
        games_per_eval=args.games_per_eval,
        max_turns=args.max_turns,
        fitness_evaluation_strategy=args.fitness_evaluation_strategy,
        fitness_aggregator=fitness_aggregator,
        fitness_eval_timeout=args.fitness_eval_timeout,
        num_threads=args.num_threads,
        save_dir=args.save_dir,
        verbose=args.verbose
    )

    # Load the dataset in order to determine the games that initially seed the archive, unless the held-out set is being used
    if args.seed_with_heldout_set:
        initial_game_names = VALIDATION_GAMES
        initial_game_strs = []
        for name in initial_game_names:
            filename = glob.glob(os.path.join("ludii_data/games/expanded", "**", f"{name}.lud"), recursive=True)[0]
            initial_game_strs.append(open(filename, "r").read())

    else:
        if args.initial_archive_splits == "all":
            splits = ["train", "val", "test"]

        elif not isinstance(args.initial_archive_splits, list):
            splits = [args.initial_archive_splits]

        else:
            splits = args.initial_archive_splits

        initial_game_strs = []
        initial_game_names = []
        for split in splits:
            dataset = load_dataset(args.model_name + "-base-data", split=split)
            initial_game_strs += dataset['base_game']
            initial_game_names += dataset['name']

    map_elites.initialize_archive(initial_game_strs, initial_game_names)

    if args.spin_gpu:
        thread = threading.Thread(target=spin_gpu)
        thread.start()

    # Finally, run the search
    map_elites.search(args.num_epochs, args.save_freq)
