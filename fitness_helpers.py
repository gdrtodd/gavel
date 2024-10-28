import typing

import numpy as np
import scipy.stats as stats
import timeout_decorator

from java_api import FastTrace, StandardEvaluation

COMPLETION_THRESHOLD = 0.2
MEAN_TURNS_THRESHOLD = 3
DECISION_MOVES_THRESHOLD = 0.1
BOARD_COVERAGE_THRESHOLD = 0.1
MIN_SCORE = 0.01

UNCOMPILABLE_FITNESS = -3
UNPLAYABLE_FITNESS = -2
UNINTERESTING_FITNESS = -1

DEFAULT_AI = "Random"
DEFAULT_NUM_GAMES = 20
DEFAULT_THINKING_TIME = 0.1
DEFAULT_MAX_TURNS = 500

FITNESS_METRIC_KEYS = [
    "balance",
    "completion",
    "drawishness",
    "decision_moves",
    "board_coverage_default",
    "mean_turns",
    "trace_score"
]

FITNESS_SCORE_EXCLUDED_KEYS = [
    "mean_turns",
    "board_coverage_default",
    # "trace_score"
]

STANDARD_EVALUATOR = None
FAST_TRACE_EVALUATOR = None

def _get_fast_evaluation(game_str: str,
                         evaluation_cache: dict = {},
                         ai_name: typing.Optional[str] = DEFAULT_AI,
                         num_games: typing.Optional[str] = DEFAULT_NUM_GAMES,
                         thinking_time: typing.Optional[float] = DEFAULT_THINKING_TIME,
                         max_turns: typing.Optional[int] = DEFAULT_MAX_TURNS,
                         timeout_duration: typing.Optional[int] = -1):
    '''
    Wrapper for getting the raw 'fast' Java evaluations for a given Ludii string (i.e. random agent
    playouts and the 'Fast Trace' score). This function is set up so we can multithread calls by only
    actually initializing one instance of the evaluators per thread
    '''
    global STANDARD_EVALUATOR, FAST_TRACE_EVALUATOR

    if STANDARD_EVALUATOR is None:
        STANDARD_EVALUATOR = StandardEvaluation()

    if FAST_TRACE_EVALUATOR is None:    
        FAST_TRACE_EVALUATOR = FastTrace()

    # If we've already evaluated this game, then we can just return the cached result
    if game_str in evaluation_cache:
        return evaluation_cache[game_str]

    if timeout_duration != -1:
        @timeout_decorator.timeout(timeout_duration)
        def _eval_wrapper(game_str, ai_name, num_games, thinking_time, max_turns):
            return STANDARD_EVALUATOR.two_step_evaluate(game_str, ai_name, num_games, thinking_time, max_turns)
        
    else:
        def _eval_wrapper(game_str, ai_name, num_games, thinking_time, max_turns):
            return STANDARD_EVALUATOR.two_step_evaluate(game_str, ai_name, num_games, thinking_time, max_turns)

    # If either evaluator crashes for any reason, then we eject the game and re-initialize the evaluators
    try:
        evaluation = _eval_wrapper(game_str, ai_name, num_games, thinking_time, max_turns)
        if evaluation['compilable'] and evaluation['playable'] and 'trace_score' in FITNESS_METRIC_KEYS:
            trace_score = FAST_TRACE_EVALUATOR.evaluate(game_str, thinking_time=thinking_time, max_turns=max_turns, max_time=120, trials_per_player=5)
        else:
            trace_score = -1

        evaluation.update({"trace_score": trace_score})

    except Exception as e:
        evaluation = {"compilable": False, "playable": False, "trace_score": -1, "balance": -1, "completion": -1, "drawishness": -1,
                      "mean_turns": -1, "decision_moves": -1, "board_coverage_default": -1, "trace_score": -1, "wins": [], "error": str(e)}
        
        _close_fast_evaluation(game_str)

    evaluation["game_str"] = game_str

    return evaluation

def _close_fast_evaluation(game_str):
    '''
    Clean up the evaluators used in _get_fast_evaluation
    '''
    global STANDARD_EVALUATOR, FAST_TRACE_EVALUATOR
    
    if STANDARD_EVALUATOR is not None:
        try:
            STANDARD_EVALUATOR._terminate()
        except:
            pass

        STANDARD_EVALUATOR = None

    if FAST_TRACE_EVALUATOR is not None:
        try:
            FAST_TRACE_EVALUATOR._terminate()
        except:
            pass
        
        FAST_TRACE_EVALUATOR = None


def _compute_balance(wins: typing.List[int]):
    '''
    Compute the 'balance' score for a given game from a list of the player win indices per trial. Balance
    is defined as the largest discrepancy between any two players' win rates
    '''

    # Edge case for either uncompilable games or games with no finished games
    if len(wins) == 0 or sum(wins) == 0:
        return -1
    
    num_games = sum(wins)
    win_rate_per_player = [wins[idx] / num_games for idx in range(1, len(wins))]

    max_discrepancy = max(win_rate_per_player) - min(win_rate_per_player)

    return 1 - max_discrepancy


def _compute_drawishness(wins: typing.List[int]):
    '''
    Compute the 'drawishness' score for a given game from a list of the player win indices per trial. Drawishness
    is defined as the one minus proportion of draws (so that high drawishness is good)
    '''

    if len(wins) == 0 or sum(wins) == 0:
        return -1

    return 1 - (wins[0] / sum(wins))

def _evaluate_fitness(evaluation: typing.Dict[str, float], aggregation_fn: typing.Callable , weights_dict: typing.Optional[typing.Dict[str, float]] = None,
                      verbose: bool = False) -> float:
    '''
    Converts a given evaluation (i.e. dictionary of metric values) into a single
    fitness value. Works hierarchically, so that:
    - if the game is not compilable, the fitness is -3
    - if the game is not playable, the fitness is -2
    - if the game is not completable, ends too quickly, or doesn't allow for player decisions, the fitness is -1
    - otherwise, the fitness is the average of the evaluation metrics
    '''

    # Uncompilable games are the worst
    if not evaluation["compilable"]:
        return UNCOMPILABLE_FITNESS
    
    # Followed by games that compile but are not playable
    elif not evaluation["playable"]:
        return UNPLAYABLE_FITNESS
    
    # Followed by games that (a) don't regularly finish, (b) finish too quickly, or (c) are mostly deterministic
    elif evaluation["completion"] < COMPLETION_THRESHOLD or evaluation["mean_turns"] < MEAN_TURNS_THRESHOLD \
        or evaluation["decision_moves"] < DECISION_MOVES_THRESHOLD or evaluation["board_coverage_default"] < BOARD_COVERAGE_THRESHOLD:
        return UNINTERESTING_FITNESS
    
    # Followed by everything else, computed by aggregating the (optionally weighted) fitness metrics
    scores = [max(evaluation[k], MIN_SCORE) for k in FITNESS_METRIC_KEYS if k not in FITNESS_SCORE_EXCLUDED_KEYS]
    if weights_dict is not None:
        weights = [weights_dict[k] for k in FITNESS_METRIC_KEYS if k not in FITNESS_SCORE_EXCLUDED_KEYS]
    else:
        weights = [1 for k in FITNESS_METRIC_KEYS if k not in FITNESS_SCORE_EXCLUDED_KEYS]

    # Error check for negative values in the fitness scores
    if any([score < 0 for score in scores]):
        if verbose: print(f"\nWARNING: negative fitness score in evaluation: {evaluation}")
        return UNCOMPILABLE_FITNESS

    fitness = aggregation_fn(scores, weights=weights)

    return fitness