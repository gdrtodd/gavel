package supplementary.experiments.scripts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import game.Game;
import main.CommandLineArgParse;
import main.CommandLineArgParse.ArgOption;
import main.CommandLineArgParse.OptionTypes;
import main.StringRoutines;
import main.UnixPrintWriter;
import other.GameLoader;

/**
 * Generates job scripts to submit to SLURM for running timings of
 * playouts for a variety of trained feature sets.
 * 
 * Script generation currently made for Cartesius cluster (not RWTH Aachen)
 *
 * @author Dennis Soemers
 */
public class TrainedFeaturesPlayoutTimingScriptsGen
{
	
	/** Don't submit more than this number of jobs at a single time */
	private static final int MAX_JOBS_PER_BATCH = 800;

	/** Memory to assign to JVM, in MB (64 GB per node, 24 cores per node --> 2.6GB per core, we take 2 cores) */
	private static final String JVM_MEM = "4096";
	
	/** JVM warming up time (in seconds) */
	private static final int WARMUP_TIME = 60;
	
	/** Time over which we measure playouts */
	private static final int MEASURE_TIME = 600;
	
	/** Max wall time (in minutes) (warming up time + measure time + some safety margin) */
	private static final int MAX_WALL_TIME = 40;
	
	/** We get 24 cores per job; we'll give 2 cores per process */
	private static final int PROCESSES_PER_JOB = 12;
	
	/** Only run on the Haswell nodes */
	private static final String PROCESSOR = "haswell";
	
	/** Games we want to run */
	private static final String[] GAMES = 
			new String[]
			{
				"Alquerque.lud",
				"Amazons.lud",
				"ArdRi.lud",
				"Arimaa.lud",
				"Ataxx.lud",
				"Bao Ki Arabu (Zanzibar 1).lud",
				"Bizingo.lud",
				"Breakthrough.lud",
				"Chess.lud",
				"Chinese Checkers.lud",
				"English Draughts.lud",
				"Fanorona.lud",
				"Fox and Geese.lud",
				"Go.lud",
				"Gomoku.lud",
				"Gonnect.lud",
				"Havannah.lud",
				"Hex.lud",
				"Kensington.lud",
				"Knightthrough.lud",
				"Konane.lud",
				"Level Chess.lud",
				"Lines of Action.lud",
				"Pentalath.lud",
				"Pretwa.lud",
				"Reversi.lud",
				"Royal Game of Ur.lud",
				"Surakarta.lud",
				"Shobu.lud",
				"Tablut.lud",
				"Triad.lud",
				"XII Scripta.lud",
				"Yavalath.lud"
			};
	
	/** Different feature set implementations we want to benchmark */
	private static final String[] FEATURE_SETS =
			new String[]
			{
				"JITSPatterNet",
				"SPatterNet",
				"Legacy",
				"Naive"
			};
	
	//-------------------------------------------------------------------------
	
	/**
	 * Constructor (don't need this)
	 */
	private TrainedFeaturesPlayoutTimingScriptsGen()
	{
		// Do nothing
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Generates our scripts
	 * @param argParse
	 */
	private static void generateScripts(final CommandLineArgParse argParse)
	{
		final List<String> jobScriptNames = new ArrayList<String>();

		String scriptsDir = argParse.getValueString("--scripts-dir");
		scriptsDir = scriptsDir.replaceAll(Pattern.quote("\\"), "/");
		if (!scriptsDir.endsWith("/"))
			scriptsDir += "/";
		
		final String userName = argParse.getValueString("--user-name");

		// First create list with data for every process we want to run
		final List<ProcessData> processDataList = new ArrayList<ProcessData>();
		for (final String gameName : GAMES)
		{
			// Sanity check: make sure game with this name loads correctly
			System.out.println("gameName = " + gameName);
			final Game game = GameLoader.loadGameFromName(gameName);
			
			if (game == null)
				throw new IllegalArgumentException("Cannot load game: " + gameName);
			
			for (final String featureSet : FEATURE_SETS)
			{
				for (final String featuresToUse : new String[]{"latest-trained-uniform-", "latest-trained-"})
				{
					processDataList.add(new ProcessData(gameName, featuresToUse, featureSet));
				}
			}
		}
		
		int processIdx = 0;
		while (processIdx < processDataList.size())
		{
			// Start a new job script
			final String jobScriptFilename = "BenchmarkTrainedFeatures_" + jobScriptNames.size() + ".sh";
					
			try (final PrintWriter writer = new UnixPrintWriter(new File(scriptsDir + jobScriptFilename), "UTF-8"))
			{
				writer.println("#!/bin/bash");
				writer.println("#SBATCH -J BenchmarkTrainedFeatures");
				writer.println("#SBATCH -o /home/" + userName + "/BenchmarkTrainedFeatures/Out/Out_%J.out");
				writer.println("#SBATCH -e /home/" + userName + "/BenchmarkTrainedFeatures/Out/Err_%J.err");
				writer.println("#SBATCH -t " + MAX_WALL_TIME);
				writer.println("#SBATCH --constraint=" + PROCESSOR);
				
				// load Java modules
				writer.println("module load 2020");
				writer.println("module load Java/1.8.0_261");
				
				// Put up to PROCESSES_PER_JOB processes in this job
				int numJobProcesses = 0;
				while (numJobProcesses < PROCESSES_PER_JOB && processIdx < processDataList.size())
				{
					final ProcessData processData = processDataList.get(processIdx);
					
					// Write Java call for this process
					final String javaCall = StringRoutines.join
							(
								" ", 
								"taskset",			// Assign specific core to each process
								"-c",
								StringRoutines.join(",", String.valueOf(numJobProcesses * 2), String.valueOf(numJobProcesses * 2 + 1)),
								"java",
								"-Xms" + JVM_MEM + "M",
								"-Xmx" + JVM_MEM + "M",
								"-XX:+HeapDumpOnOutOfMemoryError",
								"-da",
								"-dsa",
								"-XX:+UseStringDeduplication",
								"-jar",
								StringRoutines.quote("/home/" + userName + "/BenchmarkTrainedFeatures/Ludii.jar"),
								"--time-playouts",
								"--warming-up-secs",
								String.valueOf(WARMUP_TIME),
								"--measure-secs",
								String.valueOf(MEASURE_TIME),
								"--game-names",
								StringRoutines.quote("/" + processData.gameName),
								"--export-csv",
								StringRoutines.quote
								(
									"/home/" + userName + "/BenchmarkTrainedFeatures/Out/" + StringRoutines.join
									(
										"_", 
										StringRoutines.cleanGameName(processData.gameName.replaceAll(Pattern.quote(".lud"), "")),
										processData.featuresToUse,
										processData.featureSet
									) + ".csv"
								),
								//"--suppress-prints",
								"--features-to-use",
								StringRoutines.quote
								(
									processData.featuresToUse + StringRoutines.join
									(
										"/", 
										"",
										"home",
										userName,
										"TrainFeatures",
										"Out",
										StringRoutines.cleanGameName(processData.gameName.replaceAll(Pattern.quote(".lud"), ""))
									)
								),
								"--feature-set-type",
								processData.featureSet,
								">",
								"/home/" + userName + "/BenchmarkFeatures/Out/Out_${SLURM_JOB_ID}_" + numJobProcesses + ".out",
								"&"		// Run processes in parallel
							);
					
					writer.println(javaCall);
					
					++processIdx;
					++numJobProcesses;
				}
				
				writer.println("wait");		// Wait for all the parallel processes to finish

				jobScriptNames.add(jobScriptFilename);
			}
			catch (final FileNotFoundException | UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
		
		final List<List<String>> jobScriptsLists = new ArrayList<List<String>>();
		List<String> remainingJobScriptNames = jobScriptNames;

		while (remainingJobScriptNames.size() > 0)
		{
			if (remainingJobScriptNames.size() > MAX_JOBS_PER_BATCH)
			{
				final List<String> subList = new ArrayList<String>();

				for (int i = 0; i < MAX_JOBS_PER_BATCH; ++i)
				{
					subList.add(remainingJobScriptNames.get(i));
				}

				jobScriptsLists.add(subList);
				remainingJobScriptNames = remainingJobScriptNames.subList(MAX_JOBS_PER_BATCH, remainingJobScriptNames.size());
			}
			else
			{
				jobScriptsLists.add(remainingJobScriptNames);
				remainingJobScriptNames = new ArrayList<String>();
			}
		}

		for (int i = 0; i < jobScriptsLists.size(); ++i)
		{
			try (final PrintWriter writer = new UnixPrintWriter(new File(scriptsDir + "SubmitJobs_Part" + i + ".sh"), "UTF-8"))
			{
				for (final String jobScriptName : jobScriptsLists.get(i))
				{
					writer.println("sbatch " + jobScriptName);
				}
			}
			catch (final FileNotFoundException | UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Wrapper around data for a single process (multiple processes per job)
	 *
	 * @author Dennis Soemers
	 */
	private static class ProcessData
	{
		public final String gameName;
		public final String featuresToUse;
		public final String featureSet;
		
		/**
		 * Constructor
		 * @param gameName
		 * @param featuresToUse
		 * @param featureSet
		 */
		public ProcessData(final String gameName, final String featuresToUse, final String featureSet)
		{
			this.gameName = gameName;
			this.featuresToUse = featuresToUse;
			this.featureSet = featureSet;
		}
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Main method to generate all our scripts
	 * @param args
	 */
	public static void main(final String[] args)
	{
		// define options for arg parser
		final CommandLineArgParse argParse = 
				new CommandLineArgParse
				(
					true,
					"Creating timing job scripts for playouts with atomic feature sets."
				);
		
		argParse.addOption(new ArgOption()
				.withNames("--user-name")
				.help("Username on the cluster.")
				.withNumVals(1)
				.withType(OptionTypes.String)
				.setRequired());
		
		argParse.addOption(new ArgOption()
				.withNames("--scripts-dir")
				.help("Directory in which to store generated scripts.")
				.withNumVals(1)
				.withType(OptionTypes.String)
				.setRequired());
		
		// parse the args
		if (!argParse.parseArguments(args))
			return;
		
		generateScripts(argParse);
	}
	
	//-------------------------------------------------------------------------

}
