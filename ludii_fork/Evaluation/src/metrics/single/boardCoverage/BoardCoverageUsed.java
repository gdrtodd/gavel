package metrics.single.boardCoverage;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Evaluation;
import metrics.Metric;
import metrics.Utils;
import other.concept.Concept;
import other.context.Context;
import other.move.Move;
import other.topology.TopologyElement;
import other.trial.Trial;

/**
 * Percentage of used board sites (detected automatically based on the games rule description) which a piece was placed on at some point.
 * 
 * @author matthew.stephenson
 */
public class BoardCoverageUsed extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public BoardCoverageUsed()
	{
		super
		(
			"Board Coverage Used", 
			"Percentage of used board sites (detected automatically based on the games rule description) which a piece was placed on at some point.",
			0.0, 
			1.0,
			Concept.BoardCoverageUsed
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
		double numSitesCovered = 0;
		for (int trialIndex = 0; trialIndex < trials.length; trialIndex++)
		{
			// Get trial and RNG information
			final Trial trial = trials[trialIndex];
			final RandomProviderState rngState = randomProviderStates[trialIndex];
			
			// Setup a new instance of the game
			final Context context = Utils.setupNewContext(game, rngState);
			
			// Record all sites covered in this trial.
			final Set<TopologyElement> sitesCovered = new HashSet<TopologyElement>();
			
			sitesCovered.addAll(Utils.boardUsedSitesCovered(context));
			for (final Move m : trial.generateRealMovesList())
			{
				context.game().apply(context, m);
				sitesCovered.addAll(Utils.boardUsedSitesCovered(context));
			}
			
			numSitesCovered += ((double) sitesCovered.size()) / context.board().topology().getAllUsedGraphElements(context.game()).size();
		}

		return Double.valueOf(numSitesCovered / trials.length);
	}

}
