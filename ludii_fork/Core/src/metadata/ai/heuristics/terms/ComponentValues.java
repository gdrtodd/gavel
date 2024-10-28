package metadata.ai.heuristics.terms;

import java.util.Arrays;
import java.util.List;

import annotations.Name;
import annotations.Opt;
import game.Game;
import game.equipment.component.Component;
import game.equipment.container.Container;
import game.types.board.SiteType;
import game.types.state.GameType;
import main.Constants;
import main.StringRoutines;
import main.collections.FVector;
import metadata.ai.heuristics.HeuristicUtil;
import metadata.ai.heuristics.transformations.HeuristicTransformation;
import metadata.ai.misc.Pair;
import other.context.Context;
import other.location.Location;
import other.state.container.ContainerState;
import other.state.owned.Owned;

/**
 * Defines a heuristic term based on the values of sites that contain
 * components owned by a player.
 * 
 * @author Dennis Soemers
 */
public class ComponentValues extends HeuristicTerm
{
	
	//-------------------------------------------------------------------------
	
	/** Array of names specified for piece types */
	private String[] pieceWeightNames;
	
	/** 
	 * Array of weights as specified in metadata. Will be used to initialise
	 * a weight vector for a specific game when init() is called.
	 */
	private float[] gameAgnosticWeightsArray;
	
	/** If true, only count pieces on the main board (i.e., container 0) */
	private final boolean boardOnly;
	
	/** Vector with weights for every piece type */
	private FVector pieceWeights = null;
	
	/** Indices of hand containers per player */
	private int[] handIndices = null;
	
	/** Does our current game have more than 1 container? */
	private boolean gameHasMultipleContainers = false;
	
	//-------------------------------------------------------------------------
	
	/**
	 * Constructor
	 * 
	 * @param transformation An optional transformation to be applied to any 
	 * raw heuristic score outputs.
	 * @param weight The weight for this term in a linear combination of multiple terms.
	 * If not specified, a default weight of $1.0$ is used.
	 * @param pieceWeights Weights for different piece types. If no piece weights are
	 * specified at all, all piece types are given an equal weight of $1.0$. If piece
	 * weights are only specified for some piece types, all other piece types get a
	 * weight of $0$. These weights are multiplied with the values on sites where these
	 * pieces are present.
	 * @param boardOnly If true, only pieces that are on the game's main board are counted,
	 * and pieces that are, for instance, in players' hands are excluded. False by default.
	 * 
	 * @example (componentValues boardOnly:True)
	 */
	public ComponentValues
	(
		@Name @Opt final HeuristicTransformation transformation,
		@Name @Opt final Float weight,
		@Name @Opt final Pair[] pieceWeights,
		@Name @Opt final Boolean boardOnly
	)
	{
		super(transformation, weight);
		
		if (pieceWeights == null)
		{
			// We want a weight of 1.0 for everything
			pieceWeightNames = new String[]{""};
			gameAgnosticWeightsArray = new float[]{1.f};
		}
		else
		{
			pieceWeightNames = new String[pieceWeights.length];
			gameAgnosticWeightsArray = new float[pieceWeights.length];
			
			for (int i = 0; i < pieceWeights.length; ++i)
			{
				pieceWeightNames[i] = pieceWeights[i].key();
				gameAgnosticWeightsArray[i] = pieceWeights[i].floatVal();
			}
		}
		
		this.boardOnly = (boardOnly == null) ? false : boardOnly.booleanValue();
	}
	
	@Override
	public HeuristicTerm copy()
	{
		return new ComponentValues(this);
	}
	
	/**
	 * Copy constructor (private, so not visible to grammar)
	 * @param other
	 */
	private ComponentValues(final ComponentValues other)
	{
		super(other.transformation, Float.valueOf(other.weight));
		pieceWeightNames = Arrays.copyOf(other.pieceWeightNames, other.pieceWeightNames.length);
		gameAgnosticWeightsArray = Arrays.copyOf(other.gameAgnosticWeightsArray, other.gameAgnosticWeightsArray.length);
		boardOnly = other.boardOnly;
	}
	
	//-------------------------------------------------------------------------

	@Override
	public float computeValue(final Context context, final int player, final float absWeightThreshold)
	{
		final Owned owned = context.state().owned();
		
		final List<? extends Location>[] pieces = owned.positions(player);
		float value = 0.f;
		
		if (!boardOnly || !gameHasMultipleContainers)
		{
			for (int i = 0; i < pieces.length; ++i)
			{
				if (pieces[i].isEmpty())
					continue;
				
				final float pieceWeight = pieceWeights.get(owned.reverseMap(player, i));
				if (Math.abs(pieceWeight) >= absWeightThreshold)
				{
					for (final Location loc : pieces[i])
					{
						final int cid = loc.siteType() != SiteType.Cell ? 0 : context.containerId()[loc.site()];
						value += pieceWeight * context.containerState(cid).value(loc.site(), loc.level(), loc.siteType());
					}
				}
			}
		}
		else
		{
			for (int i = 0; i < pieces.length; ++i)
			{
				if (pieces[i].isEmpty())
					continue;
				
				final float pieceWeight = pieceWeights.get(owned.reverseMap(player, i));
				if (Math.abs(pieceWeight) >= absWeightThreshold)
				{
					for (final Location loc : pieces[i])
					{
						if (loc.siteType() != SiteType.Cell || context.containerId()[loc.site()] == 0)
							value += pieceWeight * context.containerState(0).value(loc.site(), loc.level(), loc.siteType());
					}
				}
			}
		}
			
		if (!boardOnly && handIndices != null)
		{
			final List<? extends Location>[] neutralPieces = owned.positions(0);

			for (int i = 0; i < neutralPieces.length; ++i)
			{
				if (neutralPieces[i].isEmpty())
					continue;
				
				final float pieceWeight = pieceWeights.get(owned.reverseMap(0, i));

				if (Math.abs(pieceWeight) >= absWeightThreshold)
				{
					for (final Location pos : neutralPieces[i])
					{
						final int site = pos.site();
						final int cid = handIndices[player];

						if (pos.siteType() == SiteType.Cell && context.containerId()[site] == cid)
						{
							final ContainerState cs = context.containerState(cid);
							value += pieceWeight * cs.countCell(site) * cs.valueCell(site);
						}
					}
				}
			}
		}
		
		return value;
	}
	
	@Override
	public FVector computeStateFeatureVector(final Context context, final int player)
	{
		final FVector featureVector = new FVector(pieceWeights.dim());
		
		final Owned owned = context.state().owned();
		final List<? extends Location>[] pieces = owned.positions(player);
		
		if (!boardOnly || !gameHasMultipleContainers)
		{
			for (int i = 0; i < pieces.length; ++i)
			{
				if (pieces[i].isEmpty())
					continue;
				
				final int compIdx = owned.reverseMap(player, i);
				for (final Location loc : pieces[i])
				{
					final int cid = loc.siteType() != SiteType.Cell ? 0 : context.containerId()[loc.site()];
					featureVector.addToEntry(compIdx, context.containerState(cid).value(loc.site(), loc.level(), loc.siteType()));
				}
			}
			
			if (handIndices != null)
			{
				final List<? extends Location>[] neutralPieces = owned.positions(0);
				
				for (int i = 0; i < neutralPieces.length; ++i)
				{
					if (neutralPieces[i].isEmpty())
						continue;
					
					final int compIdx = owned.reverseMap(player, i);
					
					for (final Location pos : neutralPieces[i])
					{
						final int site = pos.site();
						final int cid = handIndices[player];

						if (pos.siteType() == SiteType.Cell && context.containerId()[site] == cid)
						{
							final ContainerState cs = context.containerState(cid);
							featureVector.addToEntry(compIdx, cs.countCell(site) * cs.valueCell(site));
						}
					}
				}
			}
		}
		else
		{
			for (int i = 0; i < pieces.length; ++i)
			{
				if (pieces[i].isEmpty())
					continue;
				
				final int compIdx = owned.reverseMap(player, i);
				
				for (final Location loc : pieces[i])
				{
					if (loc.siteType() != SiteType.Cell || context.containerId()[loc.site()] == 0)
						featureVector.addToEntry(compIdx, context.containerState(0).value(loc.site(), loc.level(), loc.siteType()));
				}
			}
		}
		
		return featureVector;
	}
	
	@Override
	public FVector paramsVector()
	{
		return pieceWeights;
	}
	
	@Override
	public void init(final Game game)
	{
		// Compute vector of piece weights
		pieceWeights = HeuristicTerm.pieceWeightsVector(game, pieceWeightNames, gameAgnosticWeightsArray);
		
		// Precompute hand indices for this game
		computeHandIndices(game);
		
		gameHasMultipleContainers = (game.equipment().containers().length > 1);
	}
	
	@Override
	public int updateParams(final Game game, final FVector newParams, final int startIdx)
	{
		final int retVal = super.updateParams(game, newParams, startIdx);
		
		// Need to update the array of weights we were passed in constructor
		// in case we decide to write ourselves to a file
		final Object[] returnArrays = updateGameAgnosticWeights(game, pieceWeights, pieceWeightNames, gameAgnosticWeightsArray);
		pieceWeightNames = (String[]) returnArrays[0];
		gameAgnosticWeightsArray = (float[]) returnArrays[1];
		
		return retVal;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param game
	 */
	private void computeHandIndices(final Game game)
	{
		boolean foundHands = false;
		final int[] handContainerIndices = new int[game.players().count() + 1];
		
		for (final Container c : game.equipment().containers())
		{
			if (c instanceof game.equipment.container.other.Hand)
			{
				final int owner = ((game.equipment.container.other.Hand)c).owner();
				if (owner > 0 && owner < handContainerIndices.length && handContainerIndices[owner] == 0)
				{
					foundHands = true;
					handContainerIndices[owner] = c.index();
				}
			}
		}
		
		if (!foundHands)
			handIndices = null;
		else
			handIndices = handContainerIndices;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param game
	 * @return True if heuristic of this type could be applicable to given game
	 */
	public static boolean isApplicableToGame(final Game game)
	{
		if ((game.gameFlags() & GameType.Value) == 0L)
			return false;
		
		final Component[] components = game.equipment().components();
		
		if (components.length <= 1)
			return false;
		
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
		final StringBuilder sb = new StringBuilder();
		
		sb.append("(componentValues");
		if (transformation != null)
			sb.append(" transformation:" + transformation.toString());
		if (weight != 1.f)
			sb.append(" weight:" + weight);
		
		if (pieceWeightNames.length > 1 || (pieceWeightNames.length == 1 && pieceWeightNames[0].length() > 0))
		{
			sb.append(" pieceWeights:{\n");
			
			for (int i = 0; i < pieceWeightNames.length; ++i)
			{
				if (gameAgnosticWeightsArray[i] != 0.f)
					sb.append("        (pair " + StringRoutines.quote(pieceWeightNames[i]) + " " + gameAgnosticWeightsArray[i] + ")\n");
			}
			
			sb.append("    }");
			
			if (boardOnly)
				sb.append("\n    boardOnly:True\n");
		}
		else if (boardOnly)
		{
			sb.append(" boardOnly:True");
		}
		
		sb.append(")");
		
		return sb.toString();
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public String toStringThresholded(final float threshold)
	{
		boolean shouldPrint = false;
		boolean haveRelevantPieces = false;
		final StringBuilder pieceWeightsSb = new StringBuilder();
		
		if (pieceWeightNames.length > 1 || (pieceWeightNames.length == 1 && pieceWeightNames[0].length() > 0))
		{
			for (int i = 0; i < pieceWeightNames.length; ++i)
			{
				if (Math.abs(weight * gameAgnosticWeightsArray[i]) >= threshold)
				{
					pieceWeightsSb.append("        (pair " + StringRoutines.quote(pieceWeightNames[i]) + " " + gameAgnosticWeightsArray[i] + ")\n");
					haveRelevantPieces = true;
					shouldPrint = true;
				}
			}
		}
		else if (Math.abs(weight) >= threshold)
		{
			// No manually specified weights, so they will all default to 1.0,
			// and we have a large enough term-wide weight
			shouldPrint = true;
		}
		
		if (shouldPrint)
		{
			final StringBuilder sb = new StringBuilder();
		
			sb.append("(componentValues");
			if (transformation != null)
				sb.append(" transformation:" + transformation.toString());
			if (weight != 1.f)
				sb.append(" weight:" + weight);
			
			if (haveRelevantPieces)
			{
				sb.append(" pieceWeights:{\n");
				sb.append(pieceWeightsSb);
				sb.append("    }");
				
				if (boardOnly)
					sb.append("\n    boardOnly:True\n");
			}
			else if (boardOnly)
			{
				sb.append(" boardOnly:True");
			}
			sb.append(")");
			
			return sb.toString();
		}
		else
		{
			return null;
		}
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public void merge(final HeuristicTerm term) 
	{
		final ComponentValues castTerm = (ComponentValues) term;
		for (int i = 0; i < pieceWeightNames.length; i++)
			for (int j = 0; j < castTerm.pieceWeightNames.length; j++)
				if (pieceWeightNames[i].equals(castTerm.pieceWeightNames[j]))
					gameAgnosticWeightsArray[i] = gameAgnosticWeightsArray[i] + castTerm.gameAgnosticWeightsArray[j] * (castTerm.weight()/weight());
	}
	
	@Override
	public void simplify()
	{
		if (Math.abs(weight() - 1.f) > Constants.EPSILON)
		{
			for (int i = 0; i < gameAgnosticWeightsArray.length; i++)
				gameAgnosticWeightsArray[i] *= weight();
	
			setWeight(1.f);
		}
	}
	
	@Override
	public float maxAbsWeight() 
	{
		float maxWeight = Math.abs(weight());
		for (final float f : gameAgnosticWeightsArray)
			maxWeight = Math.max(maxWeight, Math.abs(f));
		return maxWeight;
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public String description() 
	{
		return "Sum of values of sites occupied by owned pieces.";
	}
	
	@Override
	public String toEnglishString(final Context context, final int playerIndex) 
	{
		final StringBuilder sb = new StringBuilder();
		
		final String extraString = boardOnly ? " on the board" : "";

		if (pieceWeightNames.length > 1 || (pieceWeightNames.length == 1 && pieceWeightNames[0].length() > 0))
		{
			for (int i = 0; i < pieceWeightNames.length; ++i)
			{
				if (gameAgnosticWeightsArray[i] != 0.f)
				{
					final String pieceTrailingNumbers = StringRoutines.getTrailingNumbers(pieceWeightNames[i]);

					if (pieceTrailingNumbers.length() == 0 || playerIndex < 0 || Integer.valueOf(pieceTrailingNumbers).intValue() == playerIndex)
					{
						if (weight > 0)
							sb.append("You should try to maximise the value of your " + StringRoutines.removeTrailingNumbers(pieceWeightNames[i]) + "(s)");
						else
							sb.append("You should try to minimise the value of your " + StringRoutines.removeTrailingNumbers(pieceWeightNames[i]) + "(s)");
						
						sb.append(extraString + " (" + HeuristicUtil.convertWeightToString(gameAgnosticWeightsArray[i]) + ")\n");
					}
				}
			}
		}
		else
		{
			if (weight > 0)
				sb.append("You should try to maximise the value of your piece(s)");
			else
				sb.append("You should try to minimise the value of your piece(s)");
			
			sb.append(extraString + " (" + HeuristicUtil.convertWeightToString(weight) + ")\n");
		}
		
		return sb.toString();
	}
	
	//-------------------------------------------------------------------------

	@Override
	public float[] gameAgnosticWeightsArray() {
		return gameAgnosticWeightsArray;
	}
	@Override
	public FVector pieceWeights() {
		return pieceWeights;
	}
}
