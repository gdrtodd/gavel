package reconstruction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import compiler.Compiler;
import completer.Completion;
import game.Game;
import game.rules.play.moves.Moves;
import main.FileHandling;
import main.Status.EndType;
import main.StringRoutines;
import main.UnixPrintWriter;
import main.grammar.Description;
import other.AI;
import other.GameLoader;
import other.concept.Concept;
import other.context.Context;
import other.move.Move;
import other.trial.Trial;
import reconstruction.completer.CompleterWithPrepro;
import reconstruction.utils.FormatReconstructionOutputs;
import utils.RandomAI;

/**
 * Reconstruction Generator.
 *
 * @author Eric.Piette
 */
public class ReconstructionGenerator
{
	final static String defaultOutputPath        = "./res/recons/output/";
	final static int    defaultNumReconsExpected = 10;
	final static int    defaultNumAttempts       = 20000;
	final static String defaultReconsPath        = "/lud/reconstruction/pending/board/sow/other/Mandinka Game";
	final static String defaultOptionName        = "Variant/Incomplete";
	
	final static double  defaultConceptualWeight = 0.0;
	final static double  defaultHistoricalWeight = 0.5;
	final static double  defaultGeographicalWeight = 0.5;
	final static double  defaultThreshold = 0.99;
	final static boolean geographicalOrder = true;
	
	final static boolean checkTimeoutRandomPlayout = false;
	final static int     defaultPlayoutsAttempts = 100;
	
	/**
	 * Main method to call the reconstruction with command lines.
	 * @param args
	 */
	public static void main(final String[] args)
	{
		String outputPath = args.length == 0 ?                      defaultOutputPath : args[0];
		int numReconsNoWarningExpectedConcepts = args.length < 1 ?  defaultNumReconsExpected : Integer.parseInt(args[1]);
		int maxNumberAttempts = args.length < 2 ?                   defaultNumAttempts : Integer.parseInt(args[2]);
		double conceptualWeight = args.length < 3 ?                 defaultConceptualWeight : Double.parseDouble(args[3]);
		double historicalWeight = args.length < 4 ?                 defaultHistoricalWeight : Double.parseDouble(args[4]);
		double geoWeight = args.length < 5 ?                 	    defaultGeographicalWeight : Double.parseDouble(args[5]);
		String reconsPath = args.length < 6 ?                       defaultReconsPath : args[6];
		String optionName = args.length < 7 ?                       defaultOptionName : args[7];
	
		reconstruction(outputPath, numReconsNoWarningExpectedConcepts, maxNumberAttempts, conceptualWeight, historicalWeight, geoWeight, reconsPath, optionName);
	}
	
/**
	 * @param outputPath         The path of the folder to place the reconstructions.
	 * @param numReconsExpected  The number of reconstruction expected to generate.
	 * @param maxNumberAttempts  The number of attempts.
	 * @param conceptualWeight   The weight of the expected concepts.
	 * @param historicalWeight   The weight of the historical similarity.
	 * @param reconsPath         The path of the file to recons.
	 */
	public static void reconstruction
	(
		String outputPath, 
		int    numReconsExpected, 
		int    maxNumberAttempts,
		double conceptualWeight,
		double historicalWeight,
		double geographicalWeight,
		String reconsPath,
		String optionName
	)
	{
		System.out.println("\n=========================================\nStart reconstruction:\n");
		System.out.println("Output Path = " + outputPath);
		System.out.println("Historical Weight = " + historicalWeight + " Conceptual Weight = " + conceptualWeight + " Geographical Weight = " + geographicalWeight);
		final long startAt = System.nanoTime();

		// Load from memory
		final String[] choices = FileHandling.listGames();
		CompleterWithPrepro completer = new CompleterWithPrepro(conceptualWeight, historicalWeight, geographicalWeight, defaultThreshold, (geographicalOrder ? 0.99 : -1));
		for (final String fileName : choices)
		{
			if (!fileName.replaceAll(Pattern.quote("\\"), "/").contains(reconsPath))
				continue;
			
			final String gameName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length() - 4);
			
			// Get game description from resource
			System.out.println("Game: " + gameName);

			String path = fileName.replaceAll(Pattern.quote("\\"), "/");
			path = path.substring(path.indexOf("/lud/"));

			String desc = "";
			String line;
			try
			(
				final InputStream in = GameLoader.class.getResourceAsStream(path);
				final BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
			)
			{
				while ((line = rdr.readLine()) != null)
					desc += line + "\n";
			}
			catch (final IOException e1)
			{
				e1.printStackTrace();
			}
			
			// To check the expected concepts detected.
//			final List<Concept> expectedConcepts = ComputeCommonExpectedConcepts.computeCommonExpectedConcepts(desc);
//			for(Concept c: expectedConcepts)
//				System.out.println(c.name());
			
			// Extract the metadata.
			final String metadata = desc.contains("(metadata") ? desc.substring(desc.indexOf("(metadata")) : "";
			String reconsMetadata = "";
			if(metadata.contains("(recon"))
			{
				reconsMetadata = metadata.substring(metadata.indexOf("(recon")); 
				int countParenthesis = 0;
				int charIndex = 0;
				for(; charIndex < reconsMetadata.length(); charIndex++)
				{
					if(reconsMetadata.charAt(charIndex) == '(')
						countParenthesis++;
					else
						if(reconsMetadata.charAt(charIndex) == ')')
							countParenthesis--;
					if(countParenthesis == -1)
					{
						charIndex--;
						break;
					}
				}
				reconsMetadata = reconsMetadata.substring(0, charIndex);
				reconsMetadata = "(metadata " + reconsMetadata + ")";
			}
			
			// Extract the id of the reconstruction.
			String idStr = metadata.contains("(id") ? metadata.substring(metadata.indexOf("(id") + 5) : "";
			idStr = idStr.substring(0, idStr.indexOf(')') - 1);
			final int idRulesetToRecons = Integer.valueOf(idStr).intValue();

			//System.out.println(desc);
			final Description description = new Description(desc);
			CompleterWithPrepro.expandRecons(description, optionName);
			desc = StringRoutines.formatOneLineDesc(description.expanded());
//			System.out.println(desc);
//			System.out.println(FormatReconstructionOutputs.indentNicely(StringRoutines.unformatOneLineDesc(desc)));
			
			int numAttempts = 0;
			List<Completion> correctCompletions = new ArrayList<Completion>();
			
			// Run the recons process until enough attempts are executed or all reconstructions are generated.
			while(numAttempts < maxNumberAttempts && correctCompletions.size() < numReconsExpected)
			{
				Completion completion = null;
				
				// Run the completer.
				try
				{
					completion = completer.completeSampled(desc, idRulesetToRecons);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}

				// Check the completions.
				if (completion != null)
				{
						final String completionRaw = FormatReconstructionOutputs.indentNicely(StringRoutines.unformatOneLineDesc(completion.raw()));
						// Test if the completion compiles.
						Game game = null;
						//System.out.println(completionRaw);
						try{game = (Game) Compiler.compileReconsTest(new Description(completionRaw), false);}
						catch(final Exception e)
						{
//							System.out.println("Impossible to compile");
//							System.out.println("DESC IS");
//							System.out.println(completionRaw);
//							e.printStackTrace();
						}
						
						// It compiles.
						if(game != null)
						{
							final String rawDescMetadata = completionRaw + "\n" + reconsMetadata;
							completion.setRaw(rawDescMetadata);
							System.out.print("One Completion found");
							
							// Check if no warning and if no potential crash.
							if(!game.hasMissingRequirement() && !game.willCrash())
							{
								System.out.print( " with no warning");
								
								// Check if the concepts expected are present.
								//System.out.println(rawDescMetadata);
								if(Concept.isExpectedConcepts(rawDescMetadata))
								{
									System.out.print( " and with the expected concepts");
									final Context context = new Context(game, new Trial(game));
									game.start(context);
									final Moves legalMoves = context.game().moves(context);
									
									// Check if a non pass move is part of the first legal moves (BE CAREFUL WITH DICE GAMES)
									boolean aNonPassMove = false;
									for(Move move: legalMoves.moves())
										if(!move.isPass())
										{
											aNonPassMove = true;
											break;
										}
									
									if(aNonPassMove)
									{
										System.out.print( " and with legal moves");
										
										boolean allGood = true;
										
										if(checkTimeoutRandomPlayout)
										{
											// Run 10 random playouts and check if at least one of them is not timeout.
											allGood = false;
											int playoutAttempts = 0;
											while (!allGood && playoutAttempts <= defaultPlayoutsAttempts)
											{
												final Context contextRandomPlayout = new Context(game, new Trial(game));
												final List<AI> ais = new ArrayList<AI>();
												ais.add(null);
												// Init the ais.
												for (int p = 1; p <= game.players().count(); ++p)
												{
													ais.add(new RandomAI());
													ais.get(p).initAI(game, p);
												}
												game.start(contextRandomPlayout);
												game.playout(contextRandomPlayout, ais, 1.0, null, 0, -1, null);
												final Trial trial = contextRandomPlayout.trial();
//												System.out.println("run playout");
//												System.out.println("num Turns = " + trial.numTurns());
//												System.out.println("num real moves = " + trial.numberRealMoves());
//												System.out.println("game max Turn limits = " + game.getMaxTurnLimit());
												boolean trialTimedOut = trial.status().endType() == EndType.MoveLimit || trial.status().endType() == EndType.TurnLimit;
												if(!trialTimedOut)
													allGood = true;
											}
										}
										else
										{
											System.out.println( " and with at least a complete playout");
										}
										
										// All good, add to the list of correct completions.
										if(allGood)
										{
											boolean descAlreadyObtained = false;
											for(Completion correctCompletion: correctCompletions)
											{
												if(correctCompletion.raw().hashCode() == completion.raw().hashCode()) // We check if we already obtained this description.
												{
													correctCompletion.addOtherIds(completion.idsUsed());
													System.out.println("FOUND ONE MORE COMBINATION OF A COMPLETION ALREADY REACHED");
													System.out.println("Still " + correctCompletions.size() + " COMPLETIONS GENERATED.");
													descAlreadyObtained = true;
													break;
												}
											}
											
											if(!descAlreadyObtained)
											{
												correctCompletions.add(completion);
												System.out.println("Score = " + completion.score() + " Cultural Score = " + completion.culturalScore() + " Conceptual Score = " + completion.conceptualScore() + " Geographical Score = " + completion.geographicalScore()) ; 
												System.out.println("ids used = " + completion.idsUsed());
												System.out.println(completion.raw());
												System.out.println(correctCompletions.size() + " COMPLETIONS GENERATED.");
											}
										}
									}
								}
							}
							System.out.println();
						}
				}
				numAttempts++;
				System.out.println("Current Num Attempts = " + numAttempts);
				System.out.println(correctCompletions.size() + " recons generated for now");
			}

			// We rank the completions.
			Collections.sort(correctCompletions, (c1, c2) -> c1.score() < c2.score() ? 1 : c1.score() == c2.score() ? 0 : -1);
			
			for (int n = 1; n < correctCompletions.size() + 1; n++) 
			{
				System.out.println("Completion " + n  + " has a score of " + correctCompletions.get(n -1).score() + " Cultural Score = " + correctCompletions.get(n -1).culturalScore() + " conceptual score = " + correctCompletions.get(n - 1).conceptualScore() + " geographical score = " + correctCompletions.get(n - 1).geographicalScore() + " IDS used = " + correctCompletions.get(n -1).idsUsed() + (correctCompletions.get(n -1).otherIdsUsed().isEmpty() ? "" : " other possible IDS = " + correctCompletions.get(n -1).otherIdsUsed()));
				CompleterWithPrepro.saveCompletion(outputPath + gameName + "/", gameName + " (Ludii " + n + ")", correctCompletions.get(n -1).raw());
			}

			System.out.println("Num Attempts = " + numAttempts);
			System.out.println(correctCompletions.size() + " recons generated");

			final String outputReconsData = outputPath + gameName + ".csv";
			try (final PrintWriter writer = new UnixPrintWriter(new File(outputReconsData), "UTF-8"))
			{
				for (int n = 1; n < correctCompletions.size() + 1; n++) 
				{
					final List<String> lineToWrite = new ArrayList<String>();
					lineToWrite.add(gameName + " (Ludii " + n + ")");
					lineToWrite.add(idRulesetToRecons+"");
					lineToWrite.add(correctCompletions.get(n-1).score() +"");
					lineToWrite.add(correctCompletions.get(n-1).culturalScore() +"");
					lineToWrite.add(correctCompletions.get(n-1).conceptualScore() +"");
					lineToWrite.add(correctCompletions.get(n-1).geographicalScore() +"");
					lineToWrite.add(correctCompletions.get(n-1).idsUsed() +"");
					lineToWrite.add(correctCompletions.get(n-1).otherIdsUsed() +"");
					writer.println(StringRoutines.join(",", lineToWrite));
				}
			}
			catch (final FileNotFoundException | UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
		}
		
		final long stopAt = System.nanoTime();
		final double secs = (stopAt - startAt) / 1000000000.0;
		System.out.println("\nDone in " + secs + "s.");
	}
}
