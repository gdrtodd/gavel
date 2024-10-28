from functools import partial
import itertools
import multiprocessing as mp
import os
import psutil
import typing
import sys

import numpy as np
import scipy.stats as stats
from tqdm import tqdm

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from fitness_helpers import FITNESS_METRIC_KEYS, _get_fast_evaluation, _close_fast_evaluation, _compute_balance, _compute_drawishness, _evaluate_fitness
from java_api import Compile

MODEL_NAME = "gpt-4o"
EVALUATION_FN = partial(_get_fast_evaluation, ai_name="UCT", thinking_time=0.25, num_games=10, max_turns=50, timeout_duration=300)
AGGREGATION_FN = stats.hmean

def _get_fitnesses(game_strs: typing.List[str], num_evals: int, num_threads: int = 1):
    '''
    Performs k evaluations of each of n game strings, returning a list of lists of fitness values.
    Optionally performs evaluations in parallel across multiple threads.
    '''

    # Repeat the input game strings k times
    repeated_game_strs = game_strs * num_evals

    if num_threads > 1:

        batched_evaluations = []
        with mp.Pool(num_threads) as pool:
            with tqdm(total=len(repeated_game_strs), desc="Evaluating fitnesses (MP)", leave=False) as pbar:
                for evaluation in pool.imap(EVALUATION_FN, repeated_game_strs):
                    batched_evaluations.append(evaluation)
                    pbar.update(1)
                    pbar.set_postfix({"cpu": np.mean(psutil.cpu_percent(percpu=True)), "mem": psutil.virtual_memory().percent})

                pool.map(_close_fast_evaluation, repeated_game_strs)

    else:
        batched_evaluations = [EVALUATION_FN(game_str) for game_str in tqdm(repeated_game_strs, desc="Evaluating fitnesses", leave=False)]
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
        average_evaluation["game_str"] = eval_group[0]["game_str"]

        # Compute the average evaluation metrics across the group
        # BUG: in cases where some evaluations of a given game time out and others don't, this
        # results in incorrect averages (e.g. averaging a completion score of 0.5 with one of 
        # -1 from a timed-out evaluation).
        for key in FITNESS_METRIC_KEYS:
            average_evaluation[key] = np.mean([evaluation[key] for evaluation in eval_group])

        # For balance and drawishness, in particular, we manually compute the average from the raw
        # win counts because the micro-average of balance and drawishness is not meaningful
        collected_wins = sum([evaluation["wins"] for evaluation in eval_group], [])
        average_evaluation["balance"] = _compute_balance(collected_wins)
        average_evaluation["drawishness"] = _compute_drawishness(collected_wins)

        fitness_score = _evaluate_fitness(average_evaluation, AGGREGATION_FN)

        fitness_scores.append(fitness_score)
        evaluations.append(average_evaluation)

    return fitness_scores, evaluations


all_game_strs = np.load(f"./baselines/gpt_outputs/{MODEL_NAME}_outputs_9000-13500.npy", allow_pickle=True)

# Clean up game strings by removing text outside the outer parentheses
cleaned_game_strs = []
for game_str in all_game_strs:
    cleaned = game_str[game_str.find("("):game_str.rfind(")") + 1]
    cleaned_game_strs.append(cleaned)

fitnesses, evaluations = _get_fitnesses(cleaned_game_strs, num_evals=1, num_threads=6)
with open(f"./baselines/gpt_outputs/{MODEL_NAME}_fitnesses_9000-13500.npy", "wb") as f:
    np.save(f, fitnesses)

with open(f"./baselines/gpt_outputs/{MODEL_NAME}_evaluations_9000-13500.npy", "wb") as f:
    np.save(f, evaluations)