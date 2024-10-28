package metadata.ai.heuristics.terms;

import game.Game;
import main.collections.FVector;
import other.context.Context;

/**
 * Defines a null heuristic term that always returns a value of 0.
 * 
 * @author Dennis Soemers
 */
public class NullHeuristic extends HeuristicTerm
{
	
	//-------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * 
	 * @example (nullHeuristic)
	 */
	public NullHeuristic()
	{
		super(null, null);
	}
	
	@Override
	public NullHeuristic copy()
	{
		return new NullHeuristic();
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public float computeValue(final Context context, final int player, final float absWeightThreshold)
	{
		return 0.f;
	}
	
	@Override
	public FVector computeStateFeatureVector(final Context context, final int player)
	{
		final FVector featureVector = new FVector(1);
		featureVector.set(0, computeValue(context, player, -1.f));
		return featureVector;
	}
	
	@Override
	public FVector paramsVector()
	{
		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param game
	 * @return True if heuristic of this type could be applicable to given game
	 */
	public static boolean isApplicableToGame(final Game game)
	{
		return true;
	}
	
	/**
	 * @param game
	 * @return True if the heuristic of this type is sensible for the given game
	 * 	(must be applicable, but even some applicable heuristics may be considered
	 * 	to be not sensible).
	 */
	public static boolean isSensibleForGame(final Game game)
	{
		return isApplicableToGame(game);
	}
	
	@Override
	public boolean isApplicable(final Game game)
	{
		return isApplicableToGame(game);
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public String toString()
	{
		return "(nullHeuristic)";
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public String toStringThresholded(final float threshold)
	{
		return null;
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public String description() 
	{
		return "Null.";
	}
	
	@Override
	public String toEnglishString(final Context context, final int playerIndex) 
	{
		return "Null.";
	}
	
	//-------------------------------------------------------------------------

}
