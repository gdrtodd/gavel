package game.functions.ints.count.simple;

import java.util.BitSet;

import annotations.Hide;
import game.Game;
import game.functions.ints.BaseIntFunction;
import game.types.state.GameType;
import other.context.Context;

/**
 * Returns the number of edges.
 * 
 * @author Eric.Piette
 */
@Hide
public final class CountEdges extends BaseIntFunction
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------

	/** If we can, we'll precompute once and cache. */
	private Integer preComputedInteger = null;

	//-------------------------------------------------------------------------

	/**
	 * 
	 */
	public CountEdges()
	{
	}

	//-------------------------------------------------------------------------

	@Override
	public int eval(final Context context)
	{
		if (preComputedInteger != null)
			return preComputedInteger.intValue();

		return context.game().board().topology().edges().size();
	}

	//-------------------------------------------------------------------------

	@Override
	public boolean isStatic()
	{
		return true;
	}

	@Override
	public String toString()
	{
		return "Edges()";
	}

	@Override
	public long gameFlags(final Game game)
	{
		return GameType.Edge;
	}

	@Override
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		return concepts;
	}

	@Override
	public BitSet writesEvalContextRecursive()
	{
		final BitSet writeEvalContext = new BitSet();
		return writeEvalContext;
	}

	@Override
	public BitSet readsEvalContextRecursive()
	{
		final BitSet readEvalContext = new BitSet();
		return readEvalContext;
	}

	@Override
	public void preprocess(final Game game)
	{
		preComputedInteger = Integer.valueOf(eval(new Context(game, null)));
	}
}
