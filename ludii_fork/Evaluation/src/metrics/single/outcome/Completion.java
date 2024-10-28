package metrics.single.outcome;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Evaluation;
import metrics.Metric;
import other.concept.Concept;
import other.trial.Trial;

/**
 * Percentage of games which have a winner (not draw or timeout).
 * 
 * @author cambolbro and matthew.stephenson
 */
public class Completion extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public Completion()
	{
		super
		(
			"Completion", 
			"Percentage of games which have a winner (not draw or timeout).", 
			0.0, 
			1.0,
			Concept.Completion
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
		// Count number of completed games
		double completedGames = 0.0;
		for (int i = 0; i < trials.length; i++)
		{
			final Trial trial = trials[i];
			
			if (trial.status().winner() != 0)
				completedGames++;
		}

		return Double.valueOf(completedGames / trials.length);
	}

	//-------------------------------------------------------------------------

}
