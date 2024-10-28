package metrics.single.outcome;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import main.Status.EndType;
import metrics.Evaluation;
import metrics.Metric;
import other.concept.Concept;
import other.trial.Trial;

/**
 * Once minus the percentage of games which end via timeout.
 * 
 * @author cambolbro and matthew.stephenson
 */
public class Timeouts extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public Timeouts()
	{
		super
		(
			"Timeouts", 
			"One minus the percentage of games which end via timeout.",
			0.0, 
			1.0,
			Concept.Timeouts
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
		// Count number of timeouts.
		double timeouts = 0.0;
		for (int i = 0; i < trials.length; i++)
		{
			final Trial trial = trials[i];
			
			// Trial ended by timeout.
			final boolean trialTimedOut = trial.status().endType() == EndType.MoveLimit || trial.status().endType() == EndType.TurnLimit;
			
			if (trialTimedOut)
				timeouts++;
		}

		return Double.valueOf(1 - (timeouts / trials.length)); // the more timeouts there are, the further this is from 1
	}

	//-------------------------------------------------------------------------

}
