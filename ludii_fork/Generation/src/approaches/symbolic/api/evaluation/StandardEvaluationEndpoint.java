package approaches.symbolic.api.evaluation;

import approaches.symbolic.api.Endpoint;
import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;
import metrics.single.boardCoverage.BoardCoverageDefault;
import metrics.single.complexity.DecisionMoves;
import metrics.single.duration.DurationTurns;
import metrics.single.outcome.Balance;
import metrics.single.outcome.Completion;
import metrics.single.outcome.Drawishness;
import other.trial.Trial;
import supplementary.experiments.eval.EvalGames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class StandardEvaluationEndpoint extends Endpoint {
    public static void main(String[] args) {
        StandardEvaluationEndpoint ste = new StandardEvaluationEndpoint();
        ste.logToFile = true;
        ste.start();
    }

    @Override
    public String respond() {

        // TODO temp
        if (rawInput.isEmpty()) {
            rawInput = "Random|20|0.1|500|(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each) (regions P1 {(sites Side NE) (sites Side SW)}) (regions P2 {(sites Side NW) (sites Side SE)})}) (rules (meta (swap)) (play (move Add (to (sites Empty)))) (end (if (is Connected Mover) (result Mover Win)))))";
        }

//        if (rawInput.equals("Crash")) {
//            try {
//                TimeUnit.SECONDS.sleep(1);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            List<byte[]> list = new LinkedList<>();
//            while (list.size() < 10000000) {
//                byte[] b = new byte[10 * 1024 * 1024]; // 10MB byte object
//                list.add(b);
//            }
//        }
        String[] parts = rawInput.split("\\|");
        String aiName = parts[0];
        int numGames = Integer.parseInt(parts[1]);
        double thinkingTime = Double.parseDouble(parts[2]);
        int maxTurns = Integer.parseInt(parts[3]);
        String gameString = parts[4];

        System.out.println("Game: " + gameString);
        System.out.println("AI: " + aiName);
        System.out.println("Num Games: " + numGames);
        System.out.println("Thinking Time: " + thinkingTime);
        System.out.println("Max Turns: " + maxTurns);


        Game game = null;
        try {
            game = (Game) Compiler.compile(new Description(gameString), new UserSelections(new ArrayList<>()), new Report(), false);
        } catch (Exception ignored) {}

        if (game == null)
            return "-1";

        Report report = new Report();

        double[] results = null;
        List<Trial> trials = new ArrayList<>();
        // "Alpha-Beta"
        try {
            long startTime = System.nanoTime();
//            results = EvalGames.getEvaluationScores(game, List.of(new Balance(), new Completion(), new Drawishness(), new DurationTurns()), null, "UCT", 1, 0.1, 1000, false, true, report);
            results = EvalGames.getEvaluationScores(game, List.of(new Balance(), new Completion(), new Drawishness(), new DurationTurns(), new DecisionMoves(), new BoardCoverageDefault()), null, aiName, numGames, thinkingTime, maxTurns, false, true, report, trials);
            long endTime = System.nanoTime();
            System.out.println("Time taken 2: " + (endTime - startTime) / 1000000000.0 + " seconds");
            System.out.println("Trials: " + trials.size());
        } catch (Exception ignored) {
            return "-2";
        }

        if (report.isError() || results[3] < 2)
            return "-2";

        String output = String.join("|", Arrays.stream(results).mapToObj(String::valueOf).toList());

        int[] wins = new int[game.players().count() + 1];
        for (Trial trial : trials) {
            // print player with the lowest ranking
            wins[trial.status().winner()]++;
        }

        return output + "||" + String.join("|", Arrays.stream(wins).mapToObj(String::valueOf).toList());
    }
}
