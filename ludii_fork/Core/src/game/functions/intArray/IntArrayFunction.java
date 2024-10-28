package game.functions.intArray;

import java.util.BitSet;

import game.Game;
import game.types.state.GameType;
import other.context.Context;

/**
 * Returns an int array function.
 * 
 * @author cambolbro
 */

// **
// ** Do not @Hide, or loses mapping in grammar!
// **

public interface IntArrayFunction extends GameType
{
	/**
	 * @param context
	 * @return The result of applying this function to this trial.
	 */
	int[] eval(final Context context);

	/**
	 * @param game The game.
	 * @return Accumulated flags corresponding to the game concepts.
	 */
	BitSet concepts(final Game game);

	/**
	 * @return Accumulated flags corresponding to read data in EvalContext.
	 */
	BitSet readsEvalContextRecursive();

	/**
	 * @return Accumulated flags corresponding to write data in EvalContext.
	 */
	BitSet writesEvalContextRecursive();

	/**
	 * @param game The game.
	 * @return True if a required ludeme is missing.
	 */
	boolean missingRequirement(final Game game);

	/**
	 * @param game The game.
	 * @return True if the ludeme can crash the game during its play.
	 */
	boolean willCrash(final Game game);

	/**
	 * @param game
	 * @return This Function in English.
	 */
	String toEnglish(Game game);
}
