package game.functions.range.math;

import java.util.BitSet;

import game.Game;
import game.functions.ints.IntConstant;
import game.functions.ints.IntFunction;
import game.functions.range.BaseRangeFunction;
import game.functions.range.Range;
import other.context.Context;

/**
 * Returns a range of exactly one value.
 * 
 * @author Eric.Piette and cambolbro
 * 
 * @remarks The exact value is both the minimum and maximum of its range.
 */
public final class Exact extends BaseRangeFunction
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------

	/**
	 * @param value The value in question.
	 *
	 * @example (exact 4)
	 */
	public Exact
	(
		final IntFunction value
	)
	{
		super(value, value);
	}

	//-------------------------------------------------------------------------

	@Override
	public Range eval(final Context context)
	{
		if (precomputedRange != null)
			return precomputedRange;

		//return new Range(Integer.valueOf(minFn.eval(context)), Integer.valueOf(maxFn.eval(context)));
		return new Range(new IntConstant(minFn.eval(context)), new IntConstant(maxFn.eval(context)));
	}

	//-------------------------------------------------------------------------

	@Override
	public boolean isStatic()
	{
		return minFn.isStatic() && maxFn.isStatic();
	}

	@Override
	public long gameFlags(final Game game)
	{
		return minFn.gameFlags(game) | maxFn.gameFlags(game);
	}

	@Override
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		concepts.or(minFn.concepts(game));
		concepts.or(maxFn.concepts(game));
		return concepts;
	}

	@Override
	public BitSet writesEvalContextRecursive()
	{
		final BitSet writeEvalContext = new BitSet();
		writeEvalContext.or(minFn.writesEvalContextRecursive());
		writeEvalContext.or(maxFn.writesEvalContextRecursive());
		return writeEvalContext;
	}

	@Override
	public BitSet readsEvalContextRecursive()
	{
		final BitSet readEvalContext = new BitSet();
		readEvalContext.or(minFn.readsEvalContextRecursive());
		readEvalContext.or(maxFn.readsEvalContextRecursive());
		return readEvalContext;
	}

	@Override
	public boolean missingRequirement(final Game game)
	{
		boolean missingRequirement = false;
		missingRequirement |= minFn.missingRequirement(game);
		missingRequirement |= maxFn.missingRequirement(game);
		return missingRequirement;
	}

	@Override
	public boolean willCrash(final Game game)
	{
		boolean willCrash = false;
		willCrash |= minFn.willCrash(game);
		willCrash |= maxFn.willCrash(game);
		return willCrash;
	}

	@Override
	public void preprocess(final Game game)
	{
		minFn.preprocess(game);
		maxFn.preprocess(game);

		if (isStatic())
			precomputedRange = eval(new Context(game, null));
	}

	//-------------------------------------------------------------------------

}
