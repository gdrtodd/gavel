package metrics.single.outcome;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Evaluation;
import metrics.Metric;
import metrics.Utils;
import other.RankUtils;
import other.concept.Concept;
import other.context.Context;
import other.trial.Trial;

/**
 * Percentage of games where player 1 did not finish last. Draws and multi-player results calculated as partial wins.  The closer the score is to 1.0, the closer player 1's win rate is to 50%.
 * 
 * @author matthew.stephenson
 */
public class AdvantageP1 extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public AdvantageP1()
	{
		super
		(
			"AdvantageP1", 
			"Percentage of games where player 1 did not finish last. Draws and multi-player results calculated as partial wins.  The closer the score is to 1.0, the closer player 1's win rate is to 50%.",
			0.0, 
			1.0,
			Concept.AdvantageP1
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
		if (game.players().count() <= 1)
			return null;
		
		double p1Wins = 0.0;
		final Context initialContext = Utils.setupTrialContext(game, randomProviderStates[0], trials[0]);
		final int numPlayers = initialContext.players().size() - 1; // gets the number of players currently in the game (in notation of Ludii player count starts at 1, so list will have initial null item)
		
		for (int i = 0; i < trials.length; i++)
		{
			final Trial trial = trials[i];
			final RandomProviderState rng = randomProviderStates[i];
			final Context context = Utils.setupTrialContext(game, rng, trial);
			p1Wins += (RankUtils.agentUtilities(context)[1] + 1.0) / 2.0; // players are scored from -1.0 to 1.0, so this method only adds 0 to p1 wins if p1 finishes last, not just when p1
			// finishes 1st, or "wins", in the case of multiplayer
		}

		return Double.valueOf(Math.max(1.0 - numPlayers * Math.abs((1.0 / numPlayers) - (p1Wins / trials.length)), 0)); // the further away from the ideal win rate p1Wins / trials.length is, the worse the score will be
	}

	//-------------------------------------------------------------------------

}