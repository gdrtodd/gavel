package metrics.designer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Evaluation;
import metrics.Metric;
import other.AI;
import other.RankUtils;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;
import utils.AIFactory;

/**
 * Measure of random-proofness in a game
 * NOTE. This metric doesn't work with stored trials, and must instead generate new trials each time.
 * NOTE. Only works games that are supported by UCT
 */
public class Systematicity extends Metric
{
    private int numMatches = 20;

    private double maxIterationMultiplier = 2;

    private int hardTimeLimit = 30;

    public Systematicity()
    {
        super(
                "Systematicity",
                "Measure of random-proofness in a game",
                0.0,
                1.0,
                null
        );
    }

    @Override
    public Double apply
            (
                    Game game,
                    Evaluation evaluation,
                    Trial[] trials,
                    RandomProviderState[] randomProviderStates
            )
    {
        final List<Double> strongAIRanking = new ArrayList<>();

        final List<AI> ais = new ArrayList<AI>(game.players().count() + 1);
        int smartPlayerIndex = 1;
        ais.add(null);
        ais.add(MCTS.createUCT());

        for (int p = 2; p <= game.players().count(); ++p)
            ais.add(AIFactory.createAI("Random"));

        final Trial trial = new Trial(game);
        final Context context = new Context(game, trial);
        game.start(context);

        double averageSmartPlayerRanking = 0.0;
        long startTime = System.currentTimeMillis();
        for(int match = 0; match < numMatches; match++)
        {
            game.start(context);

            for (int p = 1; p <= game.players().count(); ++p)
                ais.get(p).initAI(game, p);

            final Model model = context.model();

            while (!trial.over())
            {
                model.startNewStep(context, ais, -1.0, (int) maxIterationMultiplier * game.moves(context).count(), -1, 0.0);
            }
            averageSmartPlayerRanking += RankUtils.agentUtilities(context)[smartPlayerIndex];
            if(System.currentTimeMillis() > (startTime + hardTimeLimit * 1000)) break;
        }
        averageSmartPlayerRanking /= numMatches;

        return (averageSmartPlayerRanking + 1.0) / 2.0;
    }

    public void setNumMatches(final int numMatches)
    {
        this.numMatches = numMatches;
    }

    public void setMaxIterationMultiplier(final double maxIterationMultiplier)
    {
        this.maxIterationMultiplier = maxIterationMultiplier;
    }

    public void setHardTimeLimit(final int hardTimeLimit)
    {
        this.hardTimeLimit = hardTimeLimit;
    }
}
