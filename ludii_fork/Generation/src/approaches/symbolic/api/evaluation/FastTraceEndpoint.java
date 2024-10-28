package approaches.symbolic.api.evaluation;

import approaches.symbolic.api.Endpoint;
import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;
import other.AI;
import other.context.Context;
import other.trial.Trial;
import utils.AIFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class FastTraceEndpoint extends Endpoint {
    public static void main(String[] args) {
        new FastTraceEndpoint().start();
    }

    @Override
    public String respond() {

        // TODO temp
        if (rawInput.isEmpty()) {
            rawInput = "(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each) (regions P1 {(sites Side NE) (sites Side SW)}) (regions P2 {(sites Side NW) (sites Side SE)})}) (rules (meta (swap)) (play (move Add (to (sites Empty)))) (end (if (is Connected Mover) (result Mover Win)))))";
        }

        String gameDescription = rawInput;
        double thinkingTime = 0.0025;
        int maxProcessTime = 20;
        int maxTurns = 500;
        int trialsPerGroup = 10;

        if (rawInput.contains("|")) {
            String[] parts = rawInput.split("\\|");
            gameDescription = parts[0];
            thinkingTime = Double.parseDouble(parts[1]);
            maxProcessTime = Integer.parseInt(parts[2]);
            maxTurns = Integer.parseInt(parts[3]);
            trialsPerGroup = Integer.parseInt(parts[4]);
        }

        // "Alpha-Beta"
        double winRate = 0;
        try {
            long startTime = System.nanoTime();
            winRate = safePlayVsRandom(gameDescription, "UCT", maxTurns, trialsPerGroup, thinkingTime, maxProcessTime);
            long endTime = System.nanoTime();
            System.out.println("Time taken 3: " + (endTime - startTime) / 1000000000.0 + " seconds");
        } catch (Exception ignored) {
            return "-2";
        }

        return winRate + "";
    }


    public class PlayVsRandomProcess {
        public static void main(String[] args) {
            // Parse arguments (game description, AI name, maxTurns, trialsPerGroup, thinkingTime)
            // For simplicity, I'm assuming these are passed as command-line arguments
            String gameDescription = args[0];
            String aiName = args[1];
            int maxTurns = Integer.parseInt(args[2]);
            int trialsPerGroup = Integer.parseInt(args[3]);
            double thinkingTime = Double.parseDouble(args[4]);
            Game game = (Game) Compiler.compile(new Description(gameDescription), new UserSelections(new ArrayList<>()), new Report(), false);
            System.out.println("score:"+playVsRandom(game, aiName, maxTurns, trialsPerGroup, thinkingTime));
        }
    }



    public static double safePlayVsRandom(String gameDescription, String strongAI, int maxTurns, int trialsPerGroup, double thinkingTime, long maxProcessTime) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "java",
                "-cp",
                System.getProperty("java.class.path"),
                "approaches.symbolic.api.evaluation.FastTraceEndpoint$PlayVsRandomProcess",
                gameDescription,
                strongAI,
                String.valueOf(maxTurns),
                String.valueOf(trialsPerGroup),
                String.valueOf(thinkingTime)
        );

        builder.redirectErrorStream(true); // Redirect error stream to the output stream
        Process process = builder.start();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Callable<String>() {
            public String call() {
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("score:"))
                            output.append(line.substring(6));
                        else
                            System.out.println("sub-process -> " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return output.toString();
            }
        });

        double score = -3;
        try {
            String result = future.get(maxProcessTime, TimeUnit.SECONDS);
            score = Double.parseDouble(result);
        } catch (TimeoutException e) {
            System.out.println("Process did not finish in time, terminating...");
            process.destroy(); // or process.destroyForcibly() if you want to terminate it immediately
            future.cancel(true); // Interrupt the process reading task
        } catch (ExecutionException e) {
            System.out.println("Error occurred in the subprocess reading task.");
            e.printStackTrace();
        } finally {
            executor.shutdownNow(); // Terminate the executor service
        }

        if (process.isAlive()) {
            // Ensure that the process is terminated if it's still running
            process.destroyForcibly();
        } else if (process.exitValue() != 0) {
            System.out.println("Process exited with error code: " + process.exitValue());
        }

        return score;
    }


    public static double playVsRandom(Game game, String strongAI, int maxTurns, int trialsPerGroup, double thinkingTime) {
        game.setMaxTurns(maxTurns);

        String weakAI = "Random";

        int allStrongWins = 0;
        int draws = 0;
        for (int i = 1; i <= game.players().count(); ++i) {

            List<AI> aiPlayers = new ArrayList<>();
            aiPlayers.add(null);

            for (int j = 1; j <= game.players().count(); ++j) {
                if (i == j) {
                    aiPlayers.add(AIFactory.createAI(strongAI));
                } else {
                    aiPlayers.add(AIFactory.createAI(weakAI));
                }
            }

//            System.out.println(aiPlayers);
//            System.out.println("Strong player: " + i);

            int groupStrongWins = 0;
            for (int j=0; j<trialsPerGroup; j++) {

                final Trial trial = new Trial(game);
                final Context context = new Context(game, trial);

                play(game, context, aiPlayers, thinkingTime);

//                System.out.println("Utils: " + Arrays.toString(RankUtils.agentUtilities(context)));
//                System.out.println("Winner: " + context.trial().status().winner());

                try {
                    if (context.trial().status().winner() == i) {
                        allStrongWins++;
                        groupStrongWins++;
                    } else if (context.trial().status().winner() == 0) {
                        draws++;
                    }
                } catch (NullPointerException e) {
                    draws++;
                    System.out.println("Null pointer exception");
                }


            }
//            System.out.println("Group strong wins: " + groupStrongWins);

        }

        int samples = trialsPerGroup * game.players().count();
        return allStrongWins/(double)samples;
    }

    public static void play(Game game, Context context, List<AI> aiPlayers, double thinkingTime) {
        game.start(context);

        // Init AIs
        for (int p = 1; p <= game.players().count(); ++p)
            aiPlayers.get(p).initAI(game, p);

        double[] thinkingTimes = new double[game.players().count() + 1];
        Arrays.fill(thinkingTimes, thinkingTime);

        double thinkingTimeNs = thinkingTime * 1e9;

        // Game loop
        long cumulativeTime = 0;
        int numTurns = 0;
        while (!context.trial().over())
        {
            long startTime = System.nanoTime();


            context.model().startNewStep
                    (
                            context,
                            aiPlayers,
                            thinkingTimes,
                            -1, -1, 0.0,
                            true, // block call until it returns
                            false, false,
                            null, null
                    );

            long endTime = System.nanoTime();
            cumulativeTime += endTime - startTime;

            if (numTurns > 5 && (double) cumulativeTime / numTurns > 10 * thinkingTimeNs) {
                System.out.println("Too slow (" + cumulativeTime/numTurns * 1e-9 + "s per turn). Aborting.");
                break;
            }

            if (!context.model().isReady()) {
                throw new RuntimeException("Model is not ready. TODO implement waiting");
            }

            numTurns++;

//            System.out.println("Turn: " + context.trial().numTurns());
        }

//        System.out.println("Total Time: " + cumulativeTime * 1e-9 + " - Average time per turn: " + cumulativeTime/numTurns * 1e-9 + "s");

        // Close AIs
        for (int p = 1; p < aiPlayers.size(); ++p)
            aiPlayers.get(p).closeAI();
    }
}
