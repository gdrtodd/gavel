package game.functions.region.math;

import java.util.BitSet;

import game.Game;
import game.functions.region.BaseRegionFunction;
import game.functions.region.RegionFunction;
import game.util.equipment.Region;
import other.concept.Concept;
import other.context.Context;

/**
 * Merges many regions into one.
 * 
 * @author Eric.Piette
 */
public class Union extends BaseRegionFunction
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------
	
	/** Which region 1. */
	private final RegionFunction region1;

	/** Which region 2. */
	private final RegionFunction region2;

	/** Which regions. */
	private final RegionFunction[] regions;
	
	/** If we can, we'll precompute once and cache. */
	private Region precomputedRegion = null;
	
	//-------------------------------------------------------------------------

	/**
	 * For the union of two regions.
	 * 
	 * @param region1 The first region.
	 * @param region2 The second region.
	 * 
	 * @example (union (sites P1) (sites P2))
	 */
	public Union
	(
		final RegionFunction region1,
		final RegionFunction region2
	)
	{ 
		this.region1 = region1;
		this.region2 = region2;
		regions = null;
	}
	
	/**
	 * For the union of many regions.
	 * 
	 * @param regions The different regions.
	 * 
	 * @example (union {(sites P1) (sites P2) (sites P3)})
	 */
	public Union
	(
		final RegionFunction[] regions
	) 
	{
		region1 = null;
		region2 = null;
		this.regions = regions;
	}
	
	//-------------------------------------------------------------------------

	@Override
	public Region eval(final Context context)
	{
		if (precomputedRegion != null)
			return precomputedRegion;
		
		if (regions == null) 
		{
			final Region sites1 = new Region(region1.eval(context));
			final Region sites2 = region2.eval(context);
			sites1.union(sites2);
			return sites1;
		}
		else 
		{
			if (regions.length == 0)
				return new Region();
			
			final Region sites = new Region(regions[0].eval(context));
			for (int i = 1; i < regions.length; i++) 
				sites.union(new Region(regions[i].eval(context)));
			
			return sites;
		}
	}

	@Override
	public long gameFlags(final Game game)
	{
		if (regions == null)
		{	
			return region1.gameFlags(game) | region2.gameFlags(game);
		}
		else
		{
			long gameFlags = 0;
			for (final RegionFunction region : regions)
				gameFlags |= region.gameFlags(game);
			return gameFlags;
		}
	}

	@Override
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		if (regions == null)
		{
			concepts.or(region1.concepts(game));
			concepts.or(region2.concepts(game));
		}
		else
		{
			for (final RegionFunction region : regions)
				concepts.or(region.concepts(game));
		}

		concepts.set(Concept.Union.id(), true);

		return concepts;
	}

	@Override
	public BitSet writesEvalContextRecursive()
	{
		final BitSet writeEvalContext = new BitSet();
		if (regions == null)
		{
			writeEvalContext.or(region1.writesEvalContextRecursive());
			writeEvalContext.or(region2.writesEvalContextRecursive());
		}
		else
		{
			for (final RegionFunction region : regions)
				writeEvalContext.or(region.writesEvalContextRecursive());
		}
		return writeEvalContext;
	}

	@Override
	public BitSet readsEvalContextRecursive()
	{
		final BitSet readEvalContext = new BitSet();
		if (regions == null)
		{
			readEvalContext.or(region1.readsEvalContextRecursive());
			readEvalContext.or(region2.readsEvalContextRecursive());
		}
		else
		{
			for (final RegionFunction region : regions)
				readEvalContext.or(region.readsEvalContextRecursive());
		}
		return readEvalContext;
	}

	@Override
	public boolean isStatic()
	{
		if (regions == null)
		{
			return region1.isStatic() && region2.isStatic();
		}
		else
		{
			for (final RegionFunction region : regions)
				if (!region.isStatic())
					return false;
			return true;
		}
	}
	
	@Override
	public boolean missingRequirement(final Game game)
	{
		boolean missingRequirement = false;
		if (regions == null)
		{
			missingRequirement |= region1.missingRequirement(game);
			missingRequirement |= region2.missingRequirement(game);
		}
		else
		{
			for (final RegionFunction region : regions)
				missingRequirement |= region.missingRequirement(game);
		}
		return missingRequirement;
	}

	@Override
	public boolean willCrash(final Game game)
	{
		boolean willCrash = false;
		if (regions == null)
		{
			willCrash |= region1.willCrash(game);
			willCrash |= region2.willCrash(game);
		}
		else
		{
			for (final RegionFunction region : regions)
				willCrash |= region.willCrash(game);
		}
		return willCrash;
	}

	@Override
	public void preprocess(final Game game)
	{
		if (regions == null)
		{
			region1.preprocess(game);
			region2.preprocess(game);
		}
		else
		{
			for (final RegionFunction region : regions)
				region.preprocess(game);
		}
		
		if (isStatic())
		{
			precomputedRegion = eval(new Context(game, null));
		}
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return First region
	 */
	public RegionFunction region1()
	{
		return region1;
	}
	
	/**
	 * @return Second region
	 */
	public RegionFunction region2()
	{
		return region2;
	}
	
	/**
	 * @return List of regions
	 */
	public RegionFunction[] regions()
	{
		return regions;
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public String toEnglish(final Game game) 
	{
		if (regions == null)
		{
			return "the union of " + region1.toEnglish(game) + " and " + region2.toEnglish(game);
		}
		else
		{
			String englishString = "the union of ";
			for (int i = 0; i < regions.length-1; i++)
				englishString += regions[i].toEnglish(game) + ", ";
			englishString = englishString.substring(0, englishString.length()-2);
			englishString += " and " + regions[regions.length-1].toEnglish(game);
			return englishString;
		}
	}
	
	//-------------------------------------------------------------------------
}
