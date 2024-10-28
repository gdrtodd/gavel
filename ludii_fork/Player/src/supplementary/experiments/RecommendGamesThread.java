package supplementary.experiments;

import game.Game;
import main.grammar.Report;
import supplementary.experiments.eval.EvalGames;

/**
 * Thread in which the nearest-neighbor algorithm for games can be run.
 */
public class RecommendGamesThread extends Thread
{
    /** Our runnable */
    protected final RecommendGamesThreadRunnable runnable;

    //-------------------------------------------------------------------------

    /**
     *
     * @param game
     * @param k
     * @param euclidean
     * @param compareMetrics
     * @param report
     * @return Constructs a thread for the kNN algorithm
     */
    public static RecommendGamesThread construct
    (
            final Game game,
            final int k,
            final boolean euclidean,
            final boolean compareMetrics,
            final Report report
    )
    {
        final RecommendGamesThreadRunnable runnable =
                new RecommendGamesThreadRunnable
                        (
                                game,
                                k,
                                euclidean,
                                compareMetrics,
                                report
                        );

        return new RecommendGamesThread(runnable);
    }

    /**
     * Constructor
     * @param runnable
     */
    protected RecommendGamesThread(final RecommendGamesThreadRunnable runnable)
    {
        super(runnable);
        this.runnable = runnable;
    }

    //-------------------------------------------------------------------------

    /**
     * Runnable class for Eval AIs Thread
     *
     * @author Dennis Soemers
     */
    private static class RecommendGamesThreadRunnable implements Runnable
    {

        //---------------------------------------------------------------------

        /** The game we want to evaluate */
        protected final Game game;

        /** The desired number of neighbors */
        protected final int k;

        /** The desired distance metric */
        protected final boolean euclidean;

        /** The requirement of comparing the metrics of the nearest games or just the BGG entries */
        protected final boolean compareMetrics;

        /** The report used for writing things into the GUI */
        protected final Report report;


        //---------------------------------------------------------------------

        public RecommendGamesThreadRunnable
        (
                final Game game,
                final int k,
                final boolean euclidean,
                final boolean compareMetrics,
                final Report report
        )
        {
            this.game = game;
            this.k = k;
            this.euclidean = euclidean;
            this.compareMetrics = compareMetrics;
            this.report = report;
        }

        //---------------------------------------------------------------------

        @Override
        public void run()
        {
            EvalGames.recommendScore(game, k, euclidean, compareMetrics, report);
        }
    }

    //-------------------------------------------------------------------------

}
