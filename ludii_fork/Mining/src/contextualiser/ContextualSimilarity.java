package contextualiser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import game.Game;
import utils.DBGameInfo;

public class ContextualSimilarity 
{
	
	//-------------------------------------------------------------------------
	
	public static final String rulesetIdsFilePath = "../Mining/res/concepts/input/GameRulesets.csv";
	public static final String rulesetContextualiserFilePath = "../Mining/res/recons/input/contextualiser_1000/similarity_";
	public static final String rulesetGeographicDistanceFilePath = "../Mining/res/recons/input/rulesetGeographicalDistances.csv";
	public static final String rulesetYearDistanceFilePath = "../Mining/res/recons/input/rulesetYearDistances.csv";
	
	//-------------------------------------------------------------------------

	/**
	 * @param game Game to compare similarity against.
	 * @param conceptSimilarity true if using concept similarity, otherwise using cultural similarity.
	 * @return Map of game/ruleset names to similarity values.
	 */
	public static final Map<String, Double> getRulesetSimilarities(final Game game, final boolean conceptSimilarity)
	{
		// Get all ruleset ids from DB
		final String name = DBGameInfo.getUniqueName(game);
		final Map<String, Integer> rulesetIds = DBGameInfo.getRulesetIds(rulesetIdsFilePath);
		final int rulesetId = rulesetIds.get(name).intValue();
		
		final Map<Integer, Double> rulesetSimilaritiesIds = new HashMap<>();			// Map of ruleset ids to similarity
		final Map<String, Double> rulesetSimilaritiesNames = new HashMap<>();			// Map of game/ruleset names to similarity
		final String fileName = rulesetContextualiserFilePath + rulesetId + ".csv";
				
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) 
		{
			br.readLine();		// Skip first line of column headers.
		    String line;
		    while ((line = br.readLine()) != null) 
		    {
		        final String[] values = line.split(",");
		        
		        double similarity = -1.0;
		        if (conceptSimilarity)
		        	similarity = Double.valueOf(values[2]).doubleValue();
		        else
		        	similarity = Double.valueOf(values[1]).doubleValue();
		        
		        rulesetSimilaritiesIds.put(Integer.valueOf(values[0]), Double.valueOf(similarity));
		        
		        if (!rulesetIds.containsValue(Integer.valueOf(values[0])))
		        	System.out.println("ERROR, two rulesets with the same name. ruleset id: " + Integer.valueOf(values[0]));

		        // Convert ruleset ids to corresponding names.
		        for (final Map.Entry<String, Integer> entry : rulesetIds.entrySet()) 
		        	if (entry.getValue().equals(Integer.valueOf(values[0])))
		        		rulesetSimilaritiesNames.put(entry.getKey(), Double.valueOf(similarity));
		    }
		}
		catch (final Exception e)
		{
			System.out.println("Could not find similarity file, ruleset probably has no evidence.");
			e.printStackTrace();
		}

		return rulesetSimilaritiesNames;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param game Game to compare geographic similarity against.
	 * @return Map of game/ruleset names to similarity values.
	 */
	public static final Map<String, Double> getRulesetGeographicSimilarities(final Game game)
	{
		// Get all ruleset ids from DB
		final String name = DBGameInfo.getUniqueName(game);
		final Map<String, Integer> rulesetIds = DBGameInfo.getRulesetIds(rulesetIdsFilePath);
		final int rulesetId = rulesetIds.get(name).intValue();
		
		final Map<Integer, Double> rulesetSimilaritiesIds = new HashMap<>();			// Map of ruleset ids to similarity
		final Map<String, Double> rulesetSimilaritiesNames = new HashMap<>();			// Map of game/ruleset names to similarity
				
		try (BufferedReader br = new BufferedReader(new FileReader(rulesetGeographicDistanceFilePath))) 
		{
			br.readLine();		// Skip first line of column headers.
		    String line;
		    while ((line = br.readLine()) != null) 
		    {
		        final String[] values = line.split(",");
		        
		        if (Integer.valueOf(values[0]).intValue() != rulesetId)
		        	continue;
		        
		        final double similarity = Math.max((20000 - Double.valueOf(values[2]).doubleValue()) / 20000, 0);	// 20000km is the maximum possible distance
		        rulesetSimilaritiesIds.put(Integer.valueOf(values[1]), Double.valueOf(similarity));
		        
		        if (!rulesetIds.containsValue(Integer.valueOf(values[0])))
		        	System.out.println("ERROR, two rulesets with the same name. ruleset id: " + Integer.valueOf(values[0]));

		        // Convert ruleset ids to corresponding names.
		        for (final Map.Entry<String, Integer> entry : rulesetIds.entrySet()) 
		        	if (entry.getValue().equals(Integer.valueOf(values[1])))
		        		rulesetSimilaritiesNames.put(entry.getKey(), Double.valueOf(similarity));
		    }
		}
		catch (final Exception e)
		{
			System.out.println("Could not find similarity file, ruleset probably has no evidence.");
			e.printStackTrace();
		}

		return rulesetSimilaritiesNames;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param game Game to compare year similarity against.
	 * @return Map of game/ruleset names to similarity values.
	 */
	public static final Map<String, Double> getRulesetYearSimilarities(final Game game)
	{
		// Get all ruleset ids from DB
		final String name = DBGameInfo.getUniqueName(game);
		final Map<String, Integer> rulesetIds = DBGameInfo.getRulesetIds(rulesetIdsFilePath);
		final int rulesetId = rulesetIds.get(name).intValue();
		
		final Map<Integer, Double> rulesetSimilaritiesIds = new HashMap<>();			// Map of ruleset ids to similarity
		final Map<String, Double> rulesetSimilaritiesNames = new HashMap<>();			// Map of game/ruleset names to similarity
				
		try (BufferedReader br = new BufferedReader(new FileReader(rulesetYearDistanceFilePath))) 
		{
			br.readLine();		// Skip first line of column headers.
		    String line;
		    while ((line = br.readLine()) != null) 
		    {
		        final String[] values = line.split(",");
		        
		        if (Integer.valueOf(values[0]).intValue() != rulesetId)
		        	continue;
		        
		        final double similarity = Math.max((5520 - Double.valueOf(values[2]).doubleValue()) / 5520, 0);	// 5520 years is the maximum possible distance
		        rulesetSimilaritiesIds.put(Integer.valueOf(values[1]), Double.valueOf(similarity));
		        
		        if (!rulesetIds.containsValue(Integer.valueOf(values[0])))
		        	System.out.println("ERROR, two rulesets with the same name. ruleset id: " + Integer.valueOf(values[0]));

		        // Convert ruleset ids to corresponding names.
		        for (final Map.Entry<String, Integer> entry : rulesetIds.entrySet()) 
		        	if (entry.getValue().equals(Integer.valueOf(values[1])))
		        		rulesetSimilaritiesNames.put(entry.getKey(), Double.valueOf(similarity));
		    }
		}
		catch (final Exception e)
		{
			System.out.println("Could not find similarity file, ruleset probably has no evidence.");
			e.printStackTrace();
		}

		return rulesetSimilaritiesNames;
	}
	
	//-------------------------------------------------------------------------
	
}
