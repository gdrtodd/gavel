package supplementary.experiments.eval;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

import game.rules.play.moves.Moves;
import metrics.designer.IdealDuration;
import metrics.designer.SkillTrace;
import metrics.designer.Systematicity;
import metrics.multiple.MultiMetricFramework;
import metrics.multiple.metrics.BoardSitesOccupied;
import metrics.multiple.metrics.BranchingFactor;
import metrics.multiple.metrics.MoveDistance;
import metrics.multiple.metrics.PieceNumber;
import metrics.single.boardCoverage.BoardCoverageDefault;
import metrics.single.outcome.*;
import metrics.single.stateEvaluation.decisiveness.DecisivenessMoves;
import metrics.single.stateRepetition.PositionalRepetition;
import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.core.RandomProviderDefaultState;
import org.json.JSONObject;

import compiler.Compiler;
import game.Game;
import main.Constants;
import main.FileHandling;
import main.grammar.Report;
import main.options.Ruleset;
import main.options.UserSelections;
import manager.network.DatabaseFunctionsPublic;
import manager.utils.game_logs.MatchRecord;
import metrics.Evaluation;
import metrics.Metric;
import other.AI;
import other.GameLoader;
import other.RankUtils;
import other.action.others.ActionPass;
import other.concept.Concept;
import other.context.Context;
import other.move.Move;
import other.trial.Trial;
import utils.AIFactory;
import utils.DBGameInfo;

/**
 * Functions used when evaluating games.
 * 
 * @author Matthew.Stephenson
 */
public class EvalGames
{
	final static String outputFilePath = "EvalResults.csv";		//"../Mining/res/evaluation/Results.csv";

    private static boolean hasRun = false;

    private static ArrayList<String> games = null;

	private static ArrayList<ArrayList<Double>> gameConceptMatrix = null;

	private static Map<String, Double> gameRatings;

	public static boolean debug = true;
	
	//-------------------------------------------------------------------------
	
	/**
	 * Evaluates all games/rulesets.
	 */
	private static void evaluateAllGames
	(
		final Report report, final int numberTrials, final int maxTurns, final double thinkTime, 
		final String AIName, final boolean useDBGames
	)
	{
		final Evaluation evaluation = new Evaluation();
		final List<Metric> metrics = evaluation.dialogMetrics();
		final ArrayList<Double> weights = new ArrayList<>();
		for (int i = 0; i < metrics.size(); i++)
			weights.add(Double.valueOf(1));
		
		String outputString = "GameName,";
		for (int m = 0; m < metrics.size(); m++)
		{
			outputString += metrics.get(m).name() + ",";
		}
		outputString = outputString.substring(0, outputString.length()-1) + "\n";
		
		final String[] choices = FileHandling.listGames();
		for (final String s : choices)
		{
			if (!FileHandling.shouldIgnoreLudEvaluation(s))
			{
//				System.out.println("\n" + s);
				final String gameName = s.split("//")[s.split("//").length-1];
				final Game tempGame = GameLoader.loadGameFromName(gameName);
				final List<Ruleset> rulesets = tempGame.description().rulesets();
				
				if (tempGame.hasSubgames()) // TODO, we don't currently support matches
					continue;
				
				if (rulesets != null && !rulesets.isEmpty())
				{
					// Record ludemeplexes for each ruleset
					for (int rs = 0; rs < rulesets.size(); rs++)
						if (!rulesets.get(rs).optionSettings().isEmpty())
							outputString += evaluateGame(evaluation, report, tempGame, rulesets.get(rs).optionSettings(), AIName, numberTrials, thinkTime, maxTurns, metrics, weights, useDBGames);
				}
				else
				{
					outputString += evaluateGame(evaluation, report, tempGame, tempGame.description().gameOptions().allOptionStrings(tempGame.getOptions()), AIName, numberTrials, thinkTime, maxTurns, metrics, weights, useDBGames);
				}
			}
		}
		
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, false)))
		{
			writer.write(outputString);
			writer.close();
		}
		catch (final IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	//-------------------------------------------------------------------------

	/**
	 * Evaluates a given game, mainly for internal use.  Some default time-saving options
     * are included.
	 * @param game is the game to evaluate
	 * @param weights is the array of weights by which to multiply each metric (weights
     *                   must be in order of dialogMetrics() list) - if null, multiplies each metric
     *                by 1.0
	 * @param numGames is the number of games that are played (less will be faster, but less accurate predictions)
	 * @param thinkingTimeEach is the thinking time per player (less will be faster, but less accurate AI)
	 * @param maxTurns is the maximum number of turns per game
	 * @param useDatabaseGames allows for the use of database games for each game (if there are any)
	 * array with the total weighted sum of metric scores
	 * @param arrayForm makes the function return an array with a score for each metric
     *                   without weight if true, else it returns a size-1
	 * @return the metric evaluations, output form is determined by arrayForm parameter
	 */
	public static double[] getEvaluationScores(Game game, List<Metric> metrics, ArrayList<Double> weights, String aiType, int numGames, double thinkingTimeEach, int maxTurns, boolean useDatabaseGames, boolean arrayForm, Report report) {
		return getEvaluationScores(game, metrics, weights, aiType, numGames, thinkingTimeEach, maxTurns, useDatabaseGames, arrayForm, report, null);
	}

	public static double[] getEvaluationScores(Game game, List<Metric> metrics, ArrayList<Double> weights, String aiType, int numGames, double thinkingTimeEach, int maxTurns, boolean useDatabaseGames, boolean arrayForm, Report report, List<Trial> trials)
	{
		final Evaluation evaluation = new Evaluation();
		int numMetrics = metrics.size();
		if(weights == null)
		{
			weights = new ArrayList<>();
			for (int i = 0; i < numMetrics; i++) weights.add(1.0);
		}
		Report usedReport = report;
		if(usedReport == null) usedReport = new Report();
		String scoresStringForm = evaluateGame(evaluation, usedReport, game, game.description().gameOptions().allOptionStrings(game.getOptions()), aiType, numGames, thinkingTimeEach, maxTurns, metrics, weights, useDatabaseGames, trials);
        // the next 3 lines are to convert the output of the evaluateGame function to an array of doubles
		String[] scoresArrayWithName = scoresStringForm.split(",");
		double[] scores = new double[scoresArrayWithName.length - 1];
		for(int i = 1; i < scoresArrayWithName.length; i++)
		{
			if(scoresArrayWithName[i].equals("NULL")) scores[i - 1] = 0;
			else scores[i - 1] = Double.parseDouble(scoresArrayWithName[i]);
		}

		// the next part just deals with outputting the scores that were weighted with 0 as 0 in the output array
		if(arrayForm)
		{
			double[] outputScores = new double[numMetrics];
			int outputIndex = 0;
			int scoreIndex = 0;
			while(outputIndex < numMetrics){
				if(weights.get(outputIndex) == 0)
				{
					outputIndex++;
				}
				outputScores[outputIndex] = scores[scoreIndex];
				scoreIndex++;
				outputIndex++;
			}
			return outputScores;
		}
		else
		{
			double[] sum = new double[1];
			for(int m = 0; m < scores.length; m++)
			{
				sum[0] += scores[m];
			}
			double num = 0.0;
            for(int i = 0; i < weights.size(); i++) num += weights.get(i);
			if(num == 0) num = 1.0;
			sum[0] /= num; // normalized to be in range 0 to 1
			return sum;
		}
	}

    //-------------------------------------------------------------------------

	/**
	 * Very rough game evaluation, only uses some dialog metrics.  The exact parameters and used metrics have been acquired from testing, and are not necessarily precise.
	 * @param game the game to evaluate
	 * @return a score between 0 and 1 that is meant to be a rough estimate to how good a game is (1 is best)
	 */
	public static double defaultEvaluationFast(Game game)
	{
		if(gameHasNoMoves(game)) return 0.0;

		// setting up good default values for metrics
		List<Metric> metrics = new ArrayList<>();
		{
			metrics.add(new AdvantageP1());
			metrics.add(new Balance());
			metrics.add(new Completion());
			metrics.add(new Drawishness());
			metrics.add(new Timeouts());
			metrics.add(new BoardCoverageDefault());
			metrics.add(new DecisivenessMoves());
			metrics.add(new IdealDuration());
			metrics.add(new SkillTrace());
			metrics.add(new Systematicity());
		}
		ArrayList<Double> weights = new ArrayList<>();
		{
			weights.add(1.0); // AdvantageP1
			weights.add(1.0); // Balance
			weights.add(1.0); // Completion
			weights.add(1.0); // Drawishness
			weights.add(0.0); // Timeouts
			weights.add(1.0); // BoardCoverageDefault
			weights.add(0.0); // DecisivenessMoves
			weights.add(0.0); // IdealDuration
			weights.add(0.0); // SkillTrace
			weights.add(0.0); // Systematicity
		}
		return getEvaluationScores(game, metrics, weights, "Random", 40, 1, 50, true, false, null)[0];
	}

    //-------------------------------------------------------------------------

    /**
     * Better game evaluation, uses all dialog metrics.  The exact parameters and used metrics have been acquired from testing, and are not necessarily precise.
     * @param game the game to evaluate
     * @return a score between 0 and 1 that is meant to be a rough estimate to how good a game is (1 is best)
     */
	public static double defaultEvaluationSlow(Game game)
	{
		if(gameHasNoMoves(game)) return 0.0;

		SkillTrace skillTrace = new SkillTrace();
		skillTrace.debug = debug;
		skillTrace.setNumTrialsPerMatch(5);
		skillTrace.setHardTimeLimit(60);
		List<Metric> metrics = new ArrayList<>();
		{
			metrics.add(new AdvantageP1());
			metrics.add(new Balance());
			metrics.add(new Completion());
			metrics.add(new Drawishness());
			metrics.add(new Timeouts());
			metrics.add(new BoardCoverageDefault());
			metrics.add(new OutcomeUniformity());
			metrics.add(new IdealDuration());
			metrics.add(skillTrace);
			metrics.add(new Systematicity());
		}
        ArrayList<Double> weights = new ArrayList<>();
        {
            weights.add(0.5); // AdvantageP1
            weights.add(1.0); // Balance
            weights.add(0.5); // Completion
            weights.add(0.5); // Drawishness
            weights.add(0.5); // Timeouts
            weights.add(2.0); // BoardCoverageDefault
            weights.add(1.0); // OutcomeUniformity
            weights.add(0.5); // IdealDuration
            weights.add(4.0); // SkillTrace
            weights.add(0.5); // Systematicity
        }
		return getEvaluationScores(game, metrics, weights, "UCT", 10, 0.5, 100, true, false, null)[0];
	}

    //-------------------------------------------------------------------------

    /**
     * Used to find out if the input game has any legal moves or if the only move is to pass the turn.
     * @param game the game to examine
     * @return true if the game has no legal moves or if the only move is to pass the turn
     */
    public static boolean gameHasNoMoves(Game game)
    {
        final Trial trial = new Trial(game);
        final Context context = new Context(game, trial);
        game.start(context);
		Moves initialMoves = game.moves(context);
        return initialMoves.count() == 0 || (initialMoves.count() == 1 && (initialMoves.get(0).actions().get(0)).isForced());
    }

    //-------------------------------------------------------------------------

    private static void loadDB(boolean fromCode)
    {
        gameConceptMatrix = new ArrayList<>();
        games = new ArrayList<>();
		gameRatings = new HashMap<>();
		String path1 = "../Common/res/recs/game_concept_matrix_allconcepts_new.csv";
		String path2 = "../Common/res/recs/RatingGameMatrix.csv";
		if(fromCode)
		{
			path1 = "Common/res/recs/game_concept_matrix_allconcepts_new.csv";
			path2 = "Common/res/recs/RatingGameMatrix.csv";
		}
        try
        {
            // what is attempted here is to load the game concepts correctly into a form that can be readily accessed and used in the recommendScore method
            FileReader fr = new FileReader(path1);
            BufferedReader br = new BufferedReader(fr);
            String line;
            int lineCount = 0;
            while((line = br.readLine()) != null)
            {
                // first line in the file is just the concept IDs, which are implicitly defined in the matrix, so it is ignored
                if(lineCount != 0)
                {
                    String[] lineValues = line.split(",");
                    games.add(lineValues[0]);
                    gameConceptMatrix.add(new ArrayList<>());
                    // only non-string values should be added to the matrix, which is why the first column of the csv is ignored
                    for(int i = 1; i < lineValues.length; i++)
                    {
                        if(lineValues[i].isEmpty())
                        {
                            gameConceptMatrix.get(lineCount - 1).add(null);
                            continue;
                        }
                        gameConceptMatrix.get(lineCount - 1).add(Double.parseDouble(lineValues[i]));
                    }
                }
                lineCount++;
            }
			br.close();
			fr.close();

			fr = new FileReader(path2);
			br = new BufferedReader(fr);
			String[] gameNames = br.readLine().split(",");
			String[] gameScores = br.readLine().split(",");
			for(int i = 0; i < gameNames.length; i++)
			{
				gameRatings.put(gameNames[i], Double.parseDouble(gameScores[i]));
			}
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    //-------------------------------------------------------------------------

    /**
     * Meant for internal use in the nearest neighbor algorithm.  Returns whether integer b
     * is contained in array a.
     * @param a is the input array
     * @param b is the integer to find in a
     * @return true if b is in a
     */
	private static boolean containedIn(int[] a, int b)
	{
		for(int i = 0; i < a.length; i++)
		{
			if(a[i] == b) return true;
		}
		return false;
	}

    //-------------------------------------------------------------------------

	/**
	 * Meant for internal use for the nearest-neighbor algorithm.  Used to compute the
	 * mean squared error between two game-metric vectors.
	 * @param a is the first array
	 * @param b is the second array
	 * @return the mean squared error of the two arrays
	 */
    private static double meanSquaredError(double[] a, double[] b)
    {
        double MSE = 0.0;
        double num = a.length;
        for(int i = 0; i < num; i++)
        {
            MSE += Math.pow((a[i] - b[i]), 2);
        }
        return MSE / num;
    }

    //-------------------------------------------------------------------------

    /**
     * Recommends a score for a game.  Uses the nearest neighbor algorithm based on
     * game boolean and non-boolean concepts to find the k nearest games (based on
     * either cosine similarity or euclidean distance as a measure).  Based on this, both
     * game scores from BoardGameGeek.com or from selected metrics can be collected
     * for those games as an indicator of whether a game is good.  Scores from BGG are
     * less accurate, but can be used as an indicator even when a game is not finished/does
     * not compile, the only prerequisite being the ability to compute the boolean and
     * non-boolean concepts.
     * @param game is the input game to analyze
     * @param k is the desired number of nearest games.  If k is greater than the number
     *          of games in the database, method returns 0.0.
     * @param euclidean indicates whether euclidean distance or cosine similarity should
     *                  be used as a distance measure.  Cosine similarity generally works better.
     * @param compareMetrics indicates whether metrics should be computed for the input
     *                       game and the nearest games, and then compared.
     * @return the score averaged by the k nearest games
     */
	public static double recommendScore(Game game, int k, boolean euclidean, boolean compareMetrics, Report report)
    {
        if(!hasRun)
        {
            loadDB(report == null);
            hasRun = true;
        }
		// if the desired number of nearest neighbors is more than the total number of
		// stored games, returns 0.0
		if(k > gameConceptMatrix.size() || k <= 0) return 0.0;

        // calculate the boolean and non-boolean concepts for the game
        BitSet booleanMetrics = game.computeBooleanConcepts();
        Map<Integer, String> nonBooleanMetrics = game.computeNonBooleanConcepts();

        String[] nearestGames = new String[k];
        int[] nearestGameIndices = new int[k];
        Arrays.fill(nearestGameIndices, -1);
        for(int neighbor = 0; neighbor < k; neighbor++)
        {
            double maximum = -1;
            double minimum = Integer.MAX_VALUE;
            for (int i = 0; i < gameConceptMatrix.size(); i++)
            {
                // following line "removes" the already found nearest neighbors from the search
                // for the next nearest neighbor
				if(containedIn(nearestGameIndices, i)) continue;
                double dotProductAB = 0;
				double sumA = 0;
				double sumB = 0;
                double euclideanSum = 0;
                for (int j = 0; j < gameConceptMatrix.get(i).size(); j++)
                {
                    // each metric for both the input game and the examined possible neighboring
                    // game either are boolean or non-boolean, the latter of which is a string
                    // therefore, an initial value of 0 is assumed and changed based on what
                    // actual value the metric for a game has
                    double inputGameValue = 0; // could be pre-computed for better performance
                    double tempExaminedGameValue = 0;
                    Double unprocessedValue = gameConceptMatrix.get(i).get(j);

                    if (booleanMetrics.get(j))
                    {
                        inputGameValue = 1;
                    }
                    // log10 is applied to non-boolean metrics due to some metrics having very large quantities (>100000)
                    // not using log10 would cause all distance metrics to be heavily skewed with
                    // boolean metrics having little to no impact on the final neighboring games
                    else if (nonBooleanMetrics.get(j) != null && Double.parseDouble(nonBooleanMetrics.get(j)) != 0) inputGameValue = Math.log10(Double.parseDouble(nonBooleanMetrics.get(j)));
                    if(unprocessedValue != null)
                    {
                        if(unprocessedValue == 1 || unprocessedValue == 0) tempExaminedGameValue = unprocessedValue;
                        else tempExaminedGameValue = Math.log10(unprocessedValue);
                    }

                    // distance measurement calculations
                    dotProductAB += inputGameValue * tempExaminedGameValue;
					sumA += Math.pow(inputGameValue, 2);
					sumB += Math.pow(tempExaminedGameValue, 2);
                    euclideanSum += Math.pow((inputGameValue - tempExaminedGameValue), 2);
                }
                double cosineSimilarity = dotProductAB / Math.sqrt(sumA * sumB);
                double distance = Math.sqrt(euclideanSum);
                if(!euclidean && cosineSimilarity > maximum)
                {
                    maximum = cosineSimilarity;
                    nearestGameIndices[neighbor] = i;
                }
                else if(euclidean && distance < minimum)
                {
                    minimum = distance;
                    nearestGameIndices[neighbor] = i;
                }
            }
            nearestGames[neighbor] = games.get(nearestGameIndices[neighbor]);
        }

		if(report != null)
		{
            for (int i = 0; i < k; i++)
            {
                report.getReportMessageFunctions().printMessageInAnalysisPanel( (i + 1) + " nearest game is " + nearestGames[i] + "\n");
            }
		}
        else{
            for (int i = 0; i < k; i++)
            {
                System.out.println((i + 1) + " nearest game is " + nearestGames[i]);
            }
        }

		// here the nearest neighbors are evaluated based on the compareMetrics value
		double sum = 0;
		int total = 0;
        List<Metric> metrics = new ArrayList<>();
        {
            metrics.add(new AdvantageP1());
            metrics.add(new Balance());
            metrics.add(new Completion());
            metrics.add(new Drawishness());
            metrics.add(new Timeouts());
            metrics.add(new BoardCoverageDefault());
            metrics.add(new IdealDuration());
            metrics.add(new MoveDistance(MultiMetricFramework.MultiMetricValue.Average, Concept.MoveDistanceAverage));
            metrics.add(new PositionalRepetition());
			metrics.add(new BoardSitesOccupied(MultiMetricFramework.MultiMetricValue.Average, Concept.BoardSitesOccupiedAverage));
        }
        double[] inputMetricScores = compareMetrics ? getEvaluationScores(game, metrics, null, "UCT", 10, 0.1, 30, true, true, report) : null;
		for(int neighbor = 0; neighbor < k; neighbor++)
		{
			Double gameRating = gameRatings.get(nearestGames[neighbor]);
            if(compareMetrics)
            {
				// comparison between the metric scores of the neighboring game and the input game
                double[] neighborMetricScores = getEvaluationScores(GameLoader.loadGameFromName(nearestGames[neighbor] + ".lud"), metrics, null, "UCT", 10, 0.1, 30, true, true, report);
                sum += meanSquaredError(inputMetricScores, neighborMetricScores);
            }
            else
            {
				// the following happens if there are no BGG entries
                if (gameRating == null || gameRating.isNaN()) continue;
                sum += gameRating;
            }
			total++;
		}
        if(total == 0)
		{
			if(report != null && !compareMetrics) report.getReportMessageFunctions().printMessageInAnalysisPanel("Games have no BoardGameGeek entries\n");
			else if(report != null) report.getReportMessageFunctions().printMessageInAnalysisPanel("Games have no difference score-wise\n");
			return 0;
		}
		double finalResult = sum / total;
		if(report != null && compareMetrics)
		{
			report.getReportMessageFunctions().printMessageInAnalysisPanel("The average mean squared error between the nearest games is " + finalResult + "\n");
		}
		else if(report != null)
		{
			report.getReportMessageFunctions().printMessageInAnalysisPanel("The average BoardGameGeek rating of the nearest games (that have an entry) is " + finalResult + "\n");
		}
		return finalResult;
	}

    //-------------------------------------------------------------------------

    /**
     * Method used to compute the game-rating csv from the game-user rating csv
     */
	public static void calculateGameScores()
	{
		try
		{
			FileReader fr = new FileReader("Common/res/recs/UserRatingMatrix_Correct.csv");
			BufferedReader br = new BufferedReader(fr);
			String line;
			int lineCount = 0;
			StringBuilder outputString = new StringBuilder();
			ArrayList<Double> ratings = new ArrayList<>();
			ArrayList<Integer> ratingCounts = new ArrayList<>();
			while((line = br.readLine()) != null)
			{
//				System.out.println("examining line " + (lineCount + 1) + ", which is as follows: ");
//				System.out.println(line);
				String[] splitLine = line.split(",");
				String[] splitLineCorrect = new String[splitLine.length - 1];
				for(int i = 0; i < splitLine.length - 1; i++)
				{
					splitLineCorrect[i] = splitLine[i + 1];
				}
				if(lineCount == 0)
				{
					for(int i = 0; i < splitLineCorrect.length; i++)
					{
						if(i == splitLineCorrect.length - 1) outputString.append(splitLineCorrect[i]).append("\n");
						else outputString.append(splitLineCorrect[i]).append(",");
						ratings.add(0.0);
						ratingCounts.add(0);
					}
				}
				else
				{
					for(int i = 0; i < splitLineCorrect.length; i++)
					{
						if(splitLineCorrect[i].isEmpty()) continue;
						ratingCounts.set(i, ratingCounts.get(i) + 1);
						ratings.set(i, (ratings.get(i) + Double.parseDouble(splitLineCorrect[i])));
					}
				}
				lineCount++;
			}
//			System.out.println(outputString);
			for(int i = 0; i < ratings.size(); i++)
			{
//				System.out.println("running, iteration " + i);
				ratings.set(i, ratings.get(i) / ratingCounts.get(i));
				if(i == ratings.size() - 1) outputString.append(ratings.get(i));
				else outputString.append(ratings.get(i)).append(",");
			}
//			System.out.println("averaging and output string complete");
			BufferedWriter bw = new BufferedWriter(new FileWriter("Common/res/recs/RatingGameMatrix.csv"));
			bw.write(outputString.toString());
			bw.close();
			br.close();
		}
		catch(IOException e)
		{
			System.out.println("Something went wrong with reading the specified file");
		}
	}

	//-------------------------------------------------------------------------

	/**
	 * Evaluates a given game.
	 */
	public static String evaluateGame
			(
					final Evaluation evaluation,
					final Report report,
					final Game originalGame,
					final List<String> gameOptions,
					final String AIName,
					final int numGames,
					final double thinkingTimeEach,
					final int maxNumTurns,
					final List<Metric> metricsToEvaluate,
					final ArrayList<Double> weights,
					final boolean useDatabaseGames
			) {
		return evaluateGame(evaluation, report, originalGame, gameOptions, AIName, numGames, thinkingTimeEach, maxNumTurns, metricsToEvaluate, weights, useDatabaseGames, null);
	}
	public static String evaluateGame
	(
		final Evaluation evaluation,
		final Report report,
		final Game originalGame,
		final List<String> gameOptions,
		final String AIName,
		final int numGames,
		final double thinkingTimeEach,
		final int maxNumTurns, 
		final List<Metric> metricsToEvaluate, 
		final ArrayList<Double> weights,
		final boolean useDatabaseGames,
		final List<Trial> trialsOutput
	)
	{
		final Game game = (Game)Compiler.compile(originalGame.description(), new UserSelections(gameOptions), report, false);		
		game.setMaxTurns(maxNumTurns);
		
		final List<AI> aiPlayers = new ArrayList<>();
		for (int i = 0; i < Constants.MAX_PLAYERS+1; i++)
		{
			final JSONObject json = new JSONObject().put("AI",new JSONObject().put("algorithm", AIName));
			aiPlayers.add(AIFactory.fromJson(json));
		}
		
		final double[] thinkingTime = new double[aiPlayers.size()];
		for (int p = 1; p < aiPlayers.size(); ++p)
			thinkingTime[p] = thinkingTimeEach;
		
		final DatabaseFunctionsPublic databaseFunctionsPublic = DatabaseFunctionsPublic.construct();
		String analysisPanelString = "";
		
		// Initialise the AI agents needed.
		final int numPlayers = game.players().count();
		for (int i = 0; i < numPlayers; ++i)
		{
			final AI ai = aiPlayers.get(i + 1);
			final int playerIdx = i + 1;
			
			if (ai == null)
			{
				final String message = "Cannot run evaluation; Player " + playerIdx + " is not AI.\n";
				try
				{
					report.getReportMessageFunctions().printMessageInAnalysisPanel(message);
				}
				catch(final Exception e)
				{
					// probably running from command line.
					if (debug) System.out.println(message);
				}
				return "\n";
			}
			else if (!ai.supportsGame(game))
			{
				final String message = "Cannot run evaluation; " + ai.friendlyName() + " does not support this game.\n";
				try
				{
					report.getReportMessageFunctions().printMessageInAnalysisPanel(message);
				}
				catch(final Exception e)
				{
					// probably running from command line.
					if (debug) System.out.println(message);
				}
				return "\n";
			}
		}
		
		final String message = "Evaluating " + game.name() + ". \nPlease don't touch anything until complete! \nGenerating trials: \n";
		try
		{
			report.getReportMessageFunctions().printMessageInAnalysisPanel(message);
		}
		catch(final Exception e)
		{
			// probably running from command line.
			if (debug) System.out.println(message);
		}

		// If using Ludii AI, need to get the algorithm used.
		for (int p = 1; p <= game.players().count(); ++p)
			aiPlayers.get(p).initAI(game, p);
		String aiAlgorihtm = aiPlayers.get(1).name();
		if (aiAlgorihtm.length() > 7 && aiAlgorihtm.substring(0, 5).equals("Ludii"))
			aiAlgorihtm = aiAlgorihtm.substring(7, aiAlgorihtm.length()-1);
		
		// Get any valid trials that were in database.
		ArrayList<String> databaseTrials = new ArrayList<>();
		if (useDatabaseGames)
		{
			databaseTrials = databaseFunctionsPublic.getTrialsFromDatabase
			(
				game.name(), game.description().gameOptions().allOptionStrings(game.getOptions()), 
				aiAlgorihtm, thinkingTime[1], game.getMaxTurnLimit(), 
				game.description().raw().hashCode()
			);
			
			// Load files from a specific directory instead.
//			final String dirName = "/home/matthew/Downloads/Banqi";
//			final File dir = new File(dirName);
//			final File[] allFiles = dir.listFiles();
//			for(final File file : allFiles)
//			{
//				String totalContents = "";
//				BufferedReader br;
//				try
//				{
//					br = new BufferedReader(new FileReader(file));
//					String line = null;
//				    while ((line = br.readLine()) != null) 
//				    {
//				    	totalContents += line + "\n";
//				    }
//				}
//				catch (final IOException e)
//				{
//					e.printStackTrace();
//				} 
//				databaseTrials.add(totalContents);
//			}
		}

		// Generate trials and print generic results.
		final List<Trial> allStoredTrials = new ArrayList<>();
		final List<RandomProviderState> allStoredRNG = new ArrayList<>();
		final double[] sumScores = new double[game.players().count() + 1];
		int numDraws = 0;
		int numTimeouts = 0;
		long sumNumMoves = 0L;
		final Context context = new Context(game, new Trial(game));
		
		try
		{
			for (int gameCounter = 0; gameCounter < numGames; ++gameCounter)
			{
				RandomProviderDefaultState rngState = (RandomProviderDefaultState) context.rng().saveState();
				boolean usingSavedTrial = false;
				List<Move> savedTrialMoves = new ArrayList<>();
				
				if (databaseTrials.size() > gameCounter)
				{
					usingSavedTrial = true;
					
					final Path tempFile = Files.createTempFile(null, null);
					Files.write(tempFile, databaseTrials.get(gameCounter).getBytes(StandardCharsets.UTF_8));
					final File file = new File(tempFile.toString());
					final MatchRecord savedMatchRecord = MatchRecord.loadMatchRecordFromTextFile(file, game);
					
					savedTrialMoves = savedMatchRecord.trial().generateCompleteMovesList();
					rngState = savedMatchRecord.rngState();
					context.rng().restoreState(rngState);
				}
				
				allStoredRNG.add(rngState);
				
				// Play a game
				game.start(context);
				for (int p = 1; p <= game.players().count(); ++p)
					aiPlayers.get(p).initAI(game, p);
				
				if (usingSavedTrial)
					for (int i = context.trial().numMoves(); i < savedTrialMoves.size(); i++)
						context.game().apply(context, savedTrialMoves.get(i));	

				while (!context.trial().over())
				{
					context.model().startNewStep
					(
						context, 
						aiPlayers, 
						thinkingTime, 
						-1, -1, 0.0,
						true, // block call until it returns
						false, false, 
						null, null
					);
					
					while (!context.model().isReady())
						Thread.sleep(100L);
				}
				
				final double[] utils = RankUtils.agentUtilities(context);
				for (int p = 1; p <= game.players().count(); ++p)
					sumScores[p] += (utils[p] + 1.0) / 2.0;	// convert [-1, 1] to [0, 1]
								
				if (context.trial().status().winner() == 0)
					++numDraws;
				
				if 
				(
					(
						context.state().numTurn() 
						>= 
						game.getMaxTurnLimit() * game.players().count()					
					)
					|| 
					(
						context.trial().numMoves() - context.trial().numInitialPlacementMoves() 
						>= 
						game.getMaxMoveLimit()
					)
				)
				{
					++numTimeouts;
				}
				
				sumNumMoves += context.trial().numMoves() - context.trial().numInitialPlacementMoves();
				
				try
				{
					report.getReportMessageFunctions().printMessageInAnalysisPanel(".");
				}
				catch(final Exception e)
				{
					// probably running from command line.
					if (debug) System.out.print(".");
				}
				
				allStoredTrials.add(new Trial(context.trial()));
				
				if (!usingSavedTrial)					
					databaseFunctionsPublic.storeTrialInDatabase
					(
						game.name(), 
						game.description().gameOptions().allOptionStrings(game.getOptions()), 
						aiAlgorihtm, thinkingTime[1], game.getMaxTurnLimit(), 
						game.description().raw().hashCode(), new Trial(context.trial()), rngState
					);
				
				// Close AIs
				for (int p = 1; p < aiPlayers.size(); ++p)
					aiPlayers.get(p).closeAI();
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}

		if (trialsOutput != null)
			trialsOutput.addAll(allStoredTrials);
		
		try
		{
			report.getReportMessageFunctions().printMessageInAnalysisPanel("\nCalculating metrics: \n");
		}
		catch(final Exception e)
		{
			// probably running from command line.
			if (debug) System.out.print("\nTrials completed.\n");
		}
		
		final DecimalFormat df = new DecimalFormat("#.#####");
		final String drawPercentage = df.format(numDraws*100.0/numGames) + "%";
		final String timeoutPercentage = df.format(numTimeouts*100.0/numGames) + "%";
		
		analysisPanelString += "\n\nAgent type: " + aiPlayers.get(0).friendlyName();
		analysisPanelString += "\nDraw likelihood: " + drawPercentage;
		analysisPanelString += "\nTimeout likelihood: " + timeoutPercentage;
		analysisPanelString += "\nAverage number of moves per game: " + df.format(sumNumMoves/(double)numGames);
		
		for (int i = 1; i < sumScores.length; i++)
			analysisPanelString += "\nPlayer " + (i) + " win rate: " + df.format(sumScores[i]*100.0/numGames) + "%";
		
		analysisPanelString += "\n\n";
		
		double finalScore = 0.0;
		
		String csvOutputString = DBGameInfo.getUniqueName(game) + ",";
		
		final Trial[] trials = allStoredTrials.toArray(new Trial[allStoredTrials.size()]);
		final RandomProviderState[] randomProviderStates = allStoredRNG.toArray(new RandomProviderState[allStoredRNG.size()]);

		// Specific Metric results
		for (int m = 0; m < metricsToEvaluate.size(); m++)
		{
			if (weights.get(m).doubleValue() == 0)
				continue;
			
			final Metric metric = metricsToEvaluate.get(m);
			
			try
			{
				report.getReportMessageFunctions().printMessageInAnalysisPanel(metric.name() + "\n");
			}
			catch(final Exception e)
			{
				// probably running from command line.
				if (debug) System.out.print(metric.name() + "\n");
			}
			
			final Double score = metric.apply(game, evaluation, trials, randomProviderStates);			
			if (score == null)
			{
				csvOutputString += "NULL,";
			}
			else
			{
				final double weight = weights.get(m).doubleValue();
				analysisPanelString += metric.name() + ": " + df.format(score) + " (weight: " + weight + ")\n";
				finalScore += score.doubleValue() * weight;
				csvOutputString += score + ",";
			}
		}
		
		analysisPanelString += "Final Score: " + df.format(finalScore) + "\n\n";
				
		try
		{
			report.getReportMessageFunctions().printMessageInAnalysisPanel(analysisPanelString);
		}
		catch (final Exception e)
		{
			// Probably running from command line
			if (debug) System.out.println(analysisPanelString);
		}

		return csvOutputString.substring(0, csvOutputString.length()-1) + "\n";
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param args
	 */
	public static void main(final String[] args)
    {
		// Game tictactoe =
		//getEvaluationScore()
		// Define options for arg parser
		/*final CommandLineArgParse argParse =
			new CommandLineArgParse
			(
				true,
				"Evaluate all games in ludii using gameplay metrics."
			);

		argParse.addOption(new ArgOption()
				.withNames("--numTrials")
				.help("Number of trials to run for each game.")
				.withDefault(Integer.valueOf(10))
				.withNumVals(1)
				.withType(OptionTypes.Int));
		argParse.addOption(new ArgOption()
				.withNames("--maxTurns")
				.help("Turn limit.")
				.withDefault(Integer.valueOf(50))
				.withNumVals(1)
				.withType(OptionTypes.Int));
		argParse.addOption(new ArgOption()
				.withNames("--thinkTime")
				.help("Thinking time per move.")
				.withDefault(Double.valueOf(0.1))
				.withNumVals(1)
				.withType(OptionTypes.Double));
		argParse.addOption(new ArgOption()
				.withNames("--AIName")
				.help("Name of the Agent to use.")
				.withDefault("UCT")
				.withNumVals(1)
				.withType(OptionTypes.String));
		argParse.addOption(new ArgOption()
				.withNames("--useDatabaseGames")
				.help("Use database games when available.")
				.withDefault(Boolean.valueOf(true))
				.withNumVals(1)
				.withType(OptionTypes.Boolean));

		if (!argParse.parseArguments(args))
			return;

		final int numberTrials = argParse.getValueInt("--numTrials");
		final int maxTurns = argParse.getValueInt("--maxTurns");
		final double thinkTime = argParse.getValueDouble("--thinkTime");
		final String AIName = argParse.getValueString("--AIName");
		final boolean useDatabaseGames = argParse.getValueBool("--useDatabaseGames");

		evaluateAllGames(new Report(), numberTrials, maxTurns, thinkTime, AIName, useDatabaseGames);*/
		Game[] tempGames =
				{
						GameLoader.loadGameFromName("Mu Torere.lud"),
						GameLoader.loadGameFromName("Tic-Tac-Toe.lud"),
						GameLoader.loadGameFromName("Three Men's Morris.lud"),
						GameLoader.loadGameFromName("Sim.lud"),
						GameLoader.loadGameFromName("Tablut.lud"),
						GameLoader.loadGameFromName("Reversi.lud"),
						GameLoader.loadGameFromName("Breakthrough.lud"),
						GameLoader.loadGameFromName("Seega.lud"),
						GameLoader.loadGameFromName("Lines of Action.lud"),
						GameLoader.loadGameFromName("Quantum Leap.lud"),
						GameLoader.loadGameFromName("Connect Four.lud"),
						GameLoader.loadGameFromName("Yavalath.lud"),
						GameLoader.loadGameFromName("Fanorona.lud"),
						GameLoader.loadGameFromName("Nine Men's Morris.lud"),
						GameLoader.loadGameFromName("Chess.lud"),
						GameLoader.loadGameFromName("Gomoku.lud"),
						GameLoader.loadGameFromName("Havannah.lud"),
						GameLoader.loadGameFromName("Amazons.lud"),
						GameLoader.loadGameFromName("Gonnect.lud"),
						GameLoader.loadGameFromName("Hex.lud"),
						GameLoader.loadGameFromName("Dots and Boxes.lud"),
						GameLoader.loadGameFromName("Go.lud"),
				};
        Game tempGame = GameLoader.loadGameFromName("FastEvalTest.lud");
        /*Systematicity systematicity = new Systematicity();
        systematicity.setMaxIterationMultiplier(2.0);
        systematicity.setNumMatches(100);
        List<Metric> metrics = new ArrayList<>();
        {
            metrics.add(systematicity);
        }
        ArrayList<Double> weights = new ArrayList<>();
        {
            weights.add(1.0);
        }
        System.out.println(getEvaluationScores(tempGames, metrics, weights, "Random", 50, 3, 1000, true, false)[0]);*/
//		long startTime = System.currentTimeMillis();
		System.out.println(defaultEvaluationFast(tempGame));
//		long endTimeFast = System.currentTimeMillis();
//		System.out.println("Fast evaluation time: " + ((endTimeFast - startTime) / 1000) + " seconds");
//		System.out.println(defaultEvaluationSlow(tempGame));
//		long endTimeSlow = System.currentTimeMillis();
//		System.out.println("Slow evaluation time: " + ((endTimeSlow - endTimeFast) / 1000) + " seconds");
//		System.out.println("The average rating of nearest games with BGG entries is " + recommendScore(tempGames, 5, false, true));
//		calculateGameScores();
		/*for(int i = 0; i < tempGames.length; i++)
		{
			System.out.println("===========================================================================");
			System.out.println("Recommending score for: " + tempGames[i].name());
			System.out.println(recommendScore(tempGames[i], 3, false, false, null));
		}*/
		/*Systematicity systematicity = new Systematicity();
		systematicity.setHardTimeLimit(1000);
		systematicity.setNumMatches(50);
		systematicity.setMaxIterationMultiplier(4);
		List<Metric> metrics = new ArrayList<>();
		metrics.add(new Systematicity());
		for(int i = 0; i < tempGames.length; i++)
		{
			System.out.println("===========================================================================");
			System.out.println("Systematicity score for " + tempGames[i].name() + " is " + getEvaluationScores(tempGames[i], metrics, null, "Random", 30, 5, 1000, true, false, null)[0]);
		}*/
        /*Systematicity systematicity = new Systematicity();
        systematicity.setHardTimeLimit(1000);
        systematicity.setNumMatches(100);
        List<Metric> metrics = new ArrayList<>();
        for(int i = 1; i < 5; i++)
        {
            systematicity.setMaxIterationMultiplier(Math.pow(2, i));
            metrics.add(systematicity);
            System.out.println("===========================================================================");
            System.out.println("Systematicity score for " + tempGame.name() + " with iteration count " + Math.pow(2, i) + " is " + getEvaluationScores(tempGame, metrics, null, "Random", 10, 1, 50, true, false, null)[0]);
            metrics.remove(systematicity);
        }*/
	}
}
