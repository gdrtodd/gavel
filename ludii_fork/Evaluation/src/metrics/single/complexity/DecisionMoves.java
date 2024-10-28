package metrics.single.complexity;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Evaluation;
import metrics.Metric;
import metrics.Utils;
import other.concept.Concept;
import other.context.Context;
import other.move.Move;
import other.trial.Trial;

/**
 * Percentage number of states in the trial where there was more than 1 possible move.
 * 
 * @author matthew.stephenson
 */
public class DecisionMoves extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public DecisionMoves()
	{
		super
		(
			"Decision Moves", 
			"Percentage number of states in the trial where there was more than 1 possible move.", 
			0.0, 
			1.0,
			Concept.DecisionMoves
		);
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public Double apply
	(
			final Game game,
			final Evaluation evaluation,
			final Trial[] trials,
			final RandomProviderState[] randomProviderStates
	)
	{
		double avgNumDecisionMoves = 0;
		for (int trialIndex = 0; trialIndex < trials.length; trialIndex++)
		{
			// Get trial and RNG information
			final Trial trial = trials[trialIndex];
			final RandomProviderState rngState = randomProviderStates[trialIndex];
			
			// Setup a new instance of the game
			final Context context = Utils.setupNewContext(game, rngState);
			
			// Record the number of possible options for each move.
			double numDecisionMoves = 0;

			for (final Move m : trial.generateRealMovesList())
			{
				if (context.game().moves(context).moves().size() > 1)
					numDecisionMoves++;
				
				context.game().apply(context, m);
			}
			
			avgNumDecisionMoves += numDecisionMoves / trial.generateRealMovesList().size();
		}

		return Double.valueOf(avgNumDecisionMoves / trials.length);
	}

	//-------------------------------------------------------------------------

}
