package game.functions.graph.operators;

import java.util.BitSet;

import annotations.Opt;
import game.Game;
import game.functions.graph.BaseGraphFunction;
import game.functions.graph.GraphFunction;
import game.types.board.SiteType;
import game.util.graph.Graph;
import other.context.Context;

//-----------------------------------------------------------------------------

/**
 * Regenerates the coordinate labels for the elements of a graph.
 * 
 * @author cambolbro
 */
public final class Recoordinate extends BaseGraphFunction
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------
		
	private final SiteType siteTypeA;
	private final SiteType siteTypeB;
	private final SiteType siteTypeC;
	
	private final GraphFunction graphFn;

	/** Precompute once and cache, if possible. */
	private Graph precomputedGraph = null;

	//-------------------------------------------------------------------------

	/**
	 * @param siteTypeA First site type to recoordinate (Vertex/Edge/Cell).
	 * @param siteTypeB Second site type to recoordinate (Vertex/Edge/Cell).
	 * @param siteTypeC Third site type to recoordinate (Vertex/Edge/Cell).
	 * @param graph     The graph whose vertices are to be renumbered.
	 * 
	 * @example (recoordinate (merge (rectangle 2 5) (square 5)))
	 * @example (recoordinate Vertex (merge (rectangle 2 5) (square 5)))
	 */
	public Recoordinate
	(
		@Opt final SiteType      siteTypeA,
		@Opt final SiteType      siteTypeB,
		@Opt final SiteType      siteTypeC,
		     final GraphFunction graph
	) 
	{
		this.graphFn   = graph;
		this.siteTypeA = siteTypeA;
		this.siteTypeB = siteTypeB;
		this.siteTypeC = siteTypeC;
	}
	
	//-------------------------------------------------------------------------

	@Override
	public Graph eval(final Context context, final SiteType siteType)
	{
		if (precomputedGraph != null)
			return precomputedGraph;
		
		final Graph graph = graphFn.eval(context, siteType);

		final BitSet types = new BitSet();
		if (siteTypeA != null)
			types.set(siteTypeA.ordinal());
		if (siteTypeB != null)
			types.set(siteTypeB.ordinal());
		if (siteTypeC != null)
			types.set(siteTypeC.ordinal());
		
//		if (types.cardinality() == 0)
//		{
//			// No site type specified, renumber everything
//			graph.setCoordinates();
//		}
//		else
//		{
//			// Only renumber those site types specified
//			if (types.get(0)) 
//				graph.setCoordinates(SiteType.Vertex);
//
//			if (types.get(1)) 
//				graph.setCoordinates(SiteType.Edge);
//		
//			if (types.get(2)) 
//			{
//				graph.setCoordinates(SiteType.Cell);
//				
//				// Already called in Graph.orderFaces()
//				//result.setFacePhases();  // as face 0 might have changed
//			}
//		}		

		return graph;
	}

	//-------------------------------------------------------------------------

	@Override
	public boolean isStatic() 
	{
		return graphFn.isStatic();
	}

	@Override
	public long gameFlags(final Game game)
	{
		final long flags = graphFn.gameFlags(game);

		return flags;
	}
	
	@Override
	public void preprocess(final Game game)
	{
//		type = SiteType.use(type, game);

		graphFn.preprocess(game);
		
		if (isStatic())
			precomputedGraph = eval(new Context(game, null),
					(game.board().defaultSite() == SiteType.Vertex ? SiteType.Vertex : SiteType.Cell));
	}

	@Override
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		concepts.or(super.concepts(game));
		concepts.or(graphFn.concepts(game));
		return concepts;
	}
}
