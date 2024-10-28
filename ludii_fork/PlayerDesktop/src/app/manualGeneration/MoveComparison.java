package app.manualGeneration;

import java.util.ArrayList;
import java.util.List;

import game.Game;
import other.context.Context;
import other.move.Move;

public class MoveComparison
{

	// Change these parameters to influence what is important when comparing moves.
	private static boolean compareMover = false;							// The mover
	private final static boolean comparePieceName = true;				// Piece being moved
	private final static boolean compareEnglishDescription = true;		// movesLudemes.toEnglish()
	private final static boolean compareActions = true;					// The actions in the move
	
	//-------------------------------------------------------------------------
	
	/**
	 * Determines if two moves can be merged due to them containing the same key information.
	 */
	public final static boolean movesCanBeMerged(final Game game, final MoveCompleteInformation m1, final MoveCompleteInformation m2)
	{
		// compareMover can be set dynamically based on the properties of the game.
		compareMover = !game.noPieceOwnedBySpecificPlayer();
		
		if (isCompareMover())
			if (m1.move().mover() != m2.move().mover())
				return false;
		
		if (isComparePieceName())
			if (!m1.pieceName().equals(m2.pieceName()))
				return false;

		if (isCompareEnglishDescription())
			if (!m1.englishDescription().equals(m2.englishDescription()))
				return false;
		
		if (isCompareActions())
		{
			if (m1.move().actions().size() != m2.move().actions().size())
				return false;
			
			for (int i = 0; i < m1.move().actions().size(); i++)
			{
				final String m1ActionDescription = m1.move().actions().get(i).getDescription();
				final String m2ActionDescription = m2.move().actions().get(i).getDescription();
				if (!m1ActionDescription.equals(m2ActionDescription))
					return false;
			}
		}
		
		return true;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Returns a list off all similar legal moves at the point in the context where the trueMove was applied.
	 * Similar moves are those that can be merged, and are from the same location.
	 */
	public final static List<Move> similarMoves(final Context context, final Move trueMove)
	{
		final int trueMoveWhat = ManualGenerationUtils.getWhatOfMove(context, trueMove);
		final MoveCompleteInformation trueMoveCompleteInfo = new MoveCompleteInformation(context.game(), null, null, trueMove, -1, ManualGenerationUtils.getComponentNameFromIndex(context, trueMoveWhat), null);
		
		final List<Move> similarMoves = new ArrayList<>();
		for (final Move move : context.moves(context).moves())
		{
			final Move moveWithConsequences = new Move(move.getMoveWithConsequences(context));
			moveWithConsequences.setMovesLudeme(move.movesLudeme());
			
			final int moveWhat = ManualGenerationUtils.getWhatOfMove(context, moveWithConsequences);
			final MoveCompleteInformation moveCompleteInfo = new MoveCompleteInformation(context.game(), null, null, moveWithConsequences, -1, ManualGenerationUtils.getComponentNameFromIndex(context, moveWhat), null);
			
			if (movesCanBeMerged(context.game(), trueMoveCompleteInfo, moveCompleteInfo) && moveWithConsequences.getFromLocation().equals(trueMove.getFromLocation()))
				similarMoves.add(new Move(moveWithConsequences));
		}
		
		if (similarMoves.isEmpty())
			System.out.println("ERROR! similarMoves was empty");
		
		return similarMoves;
	}
	
	//-------------------------------------------------------------------------

	public static boolean isCompareMover()
	{
		return compareMover;
	}

	public static boolean isComparePieceName()
	{
		return comparePieceName;
	}

	public static boolean isCompareEnglishDescription()
	{
		return compareEnglishDescription;
	}

	public static boolean isCompareActions()
	{
		return compareActions;
	}
	
	//-------------------------------------------------------------------------
	
}
