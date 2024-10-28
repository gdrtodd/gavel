package approaches.symbolic;

import compiler.Compiler;
import game.Game;
import game.rules.play.moves.Moves;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;
import metrics.designer.SkillTrace;
import metrics.single.duration.DurationTurns;
import metrics.single.outcome.*;
import other.context.Context;
import other.trial.Trial;
import supplementary.experiments.eval.EvalGames;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static approaches.symbolic.api.evaluation.FastTraceEndpoint.safePlayVsRandom;


public class EvalTest {

    public static void main(String[] args) throws IOException {
        Game game = null;
        try {
            String rawInput = "(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each) (regions P1 {(sites Side NE) (sites Side SW)}) (regions P2 {(sites Side NW) (sites Side SE)})}) (rules (meta (swap)) (play (move Add (to (sites Empty)))) (end (if (is Connected Mover) (result Mover Win)))))";
            game = (Game) Compiler.compile(new Description(rawInput), new UserSelections(new ArrayList<>()), new Report(), false);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        //        evalTestBoring(game, "Alpha-Beta", 100, 1, 1000);
//        AI ai = MCTS.createUCT();
//        AI ai = AlphaBetaSearch.createAlphaBeta();
//        AI ai = new UBFM();
//        AI ai = new RandomAI();

//        System.out.println("Hex:" + playVsRandom(game, ai, 500, 20, 0.01));

//        game = GameLoader.loadGameFromName("Backgammon.lud");
//        System.out.println("Backgammon:" + playVsRandom(game, ai, 100, 10, 0.01));

//        List<AI> allAIs = AIFactory.buildAIList();
//        Map<AI, Double> scores = new LinkedHashMap<>();
//        for (AI ai : allAIs) {
//            double score = -1;
//            try {
//                score = playVsRandom(game, ai, 500, 20, 0.0025);
//            } catch (Exception e) {
////                e.printStackTrace();
//            }
//            scores.put(ai, score);
//            System.out.println(ai.name() + ": " + score);
//        }
//
//        System.out.println("\nSorted by score:");
//        scores.entrySet().stream()
//                .sorted(Map.Entry.<AI, Double>comparingByValue().reversed()) // Sort in descending order of score
//                .forEach(entry -> System.out.println(entry.getKey().name() + ": " + entry.getValue()));

//        System.out.println("\n\n0.001:\n");
//        System.out.println(evaluateAgainstAll(MCTS.createUCT(), 250, 10, 0.001));
        System.out.println("\n\n0.0025:\n");
        System.out.println(evaluateAgainstAll("UCT", 250, 10, 0.0025));
        System.out.println("\n\n0.005:\n");
        System.out.println(evaluateAgainstAll("UCT", 250, 10, 0.005));
        System.out.println("\n\n0.0075:\n");
        System.out.println(evaluateAgainstAll("UCT", 250, 10, 0.0075));
        System.out.println("\n\n0.01:\n");
        System.out.println(evaluateAgainstAll("UCT", 250, 10, 0.01));

    }

    public static List<Double> evaluateAgainstAll(String strongAI, int maxTurns, int trialsPerGroup, double thinkingTime) throws IOException {
        Set<String> skip = Set.of("Pachisi.lud", "Pachih.lud");
        List<Path> unfiltered_paths = Files.walk(Paths.get("./Common/res/lud/good")).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).filter(path -> !skip.contains(path.getFileName().toString())).sorted().toList();
        List<Double> scores = new ArrayList<>(unfiltered_paths.size());

        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.println("Evaluating " + unfiltered_paths.size() + " games");
        int i = 0;
        for (Path path : unfiltered_paths) {
            // Time it
            long startTime = System.nanoTime();
            long compileTime = System.nanoTime();

            double score = -1;
            try {
                score = safePlayVsRandom(Files.readString(path), strongAI, maxTurns, trialsPerGroup, thinkingTime, 15);
            } catch (OutOfMemoryError | InterruptedException | IOException e) {
                e.printStackTrace();
                break;
            }

            long evalTime = System.nanoTime();
            long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.gc();
            System.out.println(100.0*i/unfiltered_paths.size() + "%, " + path.getFileName() + ": " + score + " (" + (compileTime - startTime) * 1e-9 + "s compile, " + (evalTime - compileTime) * 1e-9 + "s eval, " + (memEnd - memStart) / 1e6 + "MB mem)");
            scores.add(score);
            i++;
        }

        return scores;
    }

    public static void evalTestBoring(Game game, String aiName, int trials, int thinkTime, int maxTurns)
    {
        Report report = new Report();
        double[] results = null;
        // "Alpha-Beta"
        // "UCT
        try {
            SkillTrace skillTrace = new SkillTrace();
            skillTrace.debug = true;
            skillTrace.setNumTrialsPerMatch(5);
            skillTrace.setHardTimeLimit(60);

            long startTime = System.nanoTime();
            results = EvalGames.getEvaluationScores(game, List.of(new Balance(), new Completion(), new Drawishness(), new DurationTurns()), null, aiName, 100, thinkTime, maxTurns, false, true, report, null);
            long endTime = System.nanoTime();
            System.out.println("Time taken 1: " + (endTime - startTime) / 1e-9 + " seconds");
        } catch (Exception ignored) {
            System.out.println("Exception:\n" + report.errors());
            ignored.printStackTrace();
            return;
        }

        if (report.isError()) {
            System.out.println("Error:\n" + report.errors());
            return;
        }

        System.out.println(String.join("|", Arrays.stream(results).mapToObj(String::valueOf).toList()));
    }

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
}
