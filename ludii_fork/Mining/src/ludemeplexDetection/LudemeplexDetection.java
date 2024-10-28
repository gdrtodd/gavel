package ludemeplexDetection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import game.Game;
import main.StringRoutines;
import main.grammar.Call;
import other.GameLoader;
import utils.DBGameInfo;

/**
 * Detects all ludemes and ludemeplexes within all games in Ludii.
 * 
 * The database csv tables will be created in "Mining/res/ludemeplexDetection/output"
 * Import all of these (except for "NOTFOUNDLUDEMES.csv") to the Ludii database.
 * 
 * Make sure the file located at "Ludii/Mining/res/concepts/input/GameRulesets.csv" is up to date.
 * You can run the SQL command inside the "SQL_command.txt" file on the Ludii database to export the required file.
 *
 * @author matthew.stephenson
 */
public class LudemeplexDetection 
{

	final static boolean DETECTLUDEMEPLEXES = false;		// Set to true to also detect ludemeplexes (slow)
	final static int MINLUDMEPLEXSIZE = 4;					// Minimum number of ludemes inside a ludemeplex
	final static int MAXLUDEMEPLEXSIZE = 6;					// Maximum number of ludemes inside a ludemeplex
	final static int MAXDEFINELUDEMEPLEXDIFFERENCE = 2;		// Maximum number of # symbols inside define ludemeplexes
	
	//-------------------------------------------------------------------------
	// Stored results
	
	// Map of all ludemeplexes and the games they are in.
	final static Map<Call, Set<String>> allLudemeplexes = new HashMap<>();
	
	// Map of all ludemeplexes and the number of times they occur across all games.
	final static Map<Call, Integer> allLudemeplexesCount = new HashMap<>();

	//-------------------------------------------------------------------------
	
	/**
	 * Records all ludemeplexes for a given Game.
	 */
	private static void recordLudemeplexesInGame(final Game game)
	{
		final Call callTree = game.description().callTree();
		final String gameName = DBGameInfo.getUniqueName(game);
		System.out.println(gameName);

		// Convert callTree for the game into a list tokens, with unique Ids for each token.
		storeludemeplexes(callTree, gameName);
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Stores all ludemeplexes found within a Call object, for an associated game name.
	 */
	private static void storeludemeplexes(final Call c, final String gameName)
	{		
		// Count the number of ludemes used in the ludemeplex.
		//final int numTokens = c.countClassesAndTerminals();
		final String ludemeList = StringRoutines.join("", c.ludemeFormat(0));
		final int ludemeCount = ludemeList.split(" ").length;
		
		// Don't store arrays.
		if (c.toString().charAt(0) != '{' && ludemeCount >= MINLUDMEPLEXSIZE && ludemeCount <= MAXLUDEMEPLEXSIZE)
		{
			Set<String> gameNameArray = new HashSet<>();
			if (allLudemeplexes.containsKey(c))
				gameNameArray = allLudemeplexes.get(c);
	
			gameNameArray.add(gameName);
			allLudemeplexes.put(c, gameNameArray);
			
			Integer count = Integer.valueOf(0);
			if (allLudemeplexesCount.containsKey(c))
				count = allLudemeplexesCount.get(c);
			count = Integer.valueOf((count.intValue() + 1));
			allLudemeplexesCount.put(c, count);
		}
		
		for (final Call arg : c.args())
			if (arg.args().size() > 0)
				storeludemeplexes(arg, gameName);
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Returns the count for every ludemeplex in a call object.
	 */
	@SuppressWarnings("unused")  // DO NOT KILL: May be used in future.
	private static Map<Call, Integer> countLudemeplexes(final Call c, final Map<Call, Integer> currentCount)
	{
		if (currentCount.containsKey(c))
			currentCount.put(c, Integer.valueOf(currentCount.get(c).intValue()+1));
		else
			currentCount.put(c, Integer.valueOf(1));
		
		for (final Call arg : c.args())
			if (arg.args().size() > 0)
				countLudemeplexes(arg, currentCount);
		
		return currentCount;
	}

	//-------------------------------------------------------------------------
	
	/**
	 * Predicts the win-rate for a variety of games, AI agents and prediction algorithms.
	 */
	public static void main(final String[] args)
	{		
		// All rulesets to be analysed.
		final List<String[]> chosenGames = GameLoader.allAnalysisGameRulesetNames();
		
		System.out.println("//-------------------------------------------------------------------------");
		
		// Record ludemes across all rulesets.
		DatabaseFunctions.storeLudemeInfo();
		DatabaseFunctions.storeLudemesInGames(GetLudemeInfo.getLudemeInfo(), chosenGames);
		System.out.println("Ludemes Recorded");
		
		System.out.println("//-------------------------------------------------------------------------");
		
		if (DETECTLUDEMEPLEXES)
		{
			// Record ludemeplexes across all rulesets.
			for (final String[] gameRulesetName : chosenGames)
				recordLudemeplexesInGame(GameLoader.loadGameFromName(gameRulesetName[0], gameRulesetName[1]));
			DatabaseFunctions.storeLudemeplexInfo(allLudemeplexes, allLudemeplexesCount);
			DatabaseFunctions.storeLudemesInLudemeplex(allLudemeplexes);
			DatabaseFunctions.storeLudemeplexRulesetPairs(allLudemeplexes);
			System.out.println("Ludemeplexes Recorded");
		
			System.out.println("//-------------------------------------------------------------------------");
			
			// Record possible define ludemeplexes.
			final Map<String, Set<String>> allDefineLudemeplexes = DatabaseFunctions.storeDefineLudemeplexInfo(allLudemeplexes, allLudemeplexesCount, MAXDEFINELUDEMEPLEXDIFFERENCE);
			DatabaseFunctions.storeDefineLudemeplexRulesetPairs(allDefineLudemeplexes);
			System.out.println("Define Ruleset Ludemeplexes Recorded");
			
			System.out.println("//-------------------------------------------------------------------------");
		}
	}

	//-------------------------------------------------------------------------
	
}
