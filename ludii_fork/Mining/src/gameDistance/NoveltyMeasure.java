package gameDistance;

import compiler.Compiler;
import game.Game;
import gameDistance.datasets.treeEdit.LudemeDataset;
import gameDistance.utils.apted.costmodel.StringUnitCostModel;
import gameDistance.utils.apted.distance.APTED;
import gameDistance.utils.apted.node.Node;
import gameDistance.utils.apted.node.StringNodeData;
import gameDistance.utils.apted.parser.BracketStringInputParser;
import main.FileHandling;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;
import other.GameLoader;
import utils.data_structures.support.zhang_shasha.Tree;

import java.util.*;

public class NoveltyMeasure
{
    static final int MAXIMUMTREESIZE = 1000;     // Discard any big trees to help speed up the distance calculations.

    static LudemeDataset ludemeDataset = new LudemeDataset();   // Dataset of all possible ludemes.
    static Map<String, Tree> gameTrees = new HashMap<>();       // Map of ludeme trees for all existing Ludii games.

    public static boolean verbose = true;

    /**
     * Loads the game trees for all provided game descriptions into memory for later comparison.
     * If gameDescriptions is empty, then will use all Ludii games.
     */
    public static void loadGameTrees(String[] gameDescriptions)
    {
        if (verbose) {
            System.out.println("Loading Game Trees (this may take some time...)");
        }

        if (gameDescriptions == null) {
            for (final String s : FileHandling.listGames()) {
                if (!FileHandling.shouldIgnoreLudAnalysis(s)) {
                    final Game game = GameLoader.loadGameFromName(s);
                    Tree gameTree = ludemeDataset.getTree(game);
                    if (gameTree.size() < MAXIMUMTREESIZE)
                        gameTrees.put(game.name(), gameTree);
                }
            }
        }
        else {
            for (String description : gameDescriptions)
            {
                final Game game = (Game) Compiler.compile
                        (
                                new Description(description),
                                new UserSelections(new ArrayList<String>()),
                                new Report(),
                                false
                        );
                Tree gameTree = ludemeDataset.getTree(game);
                gameTrees.put(game.name(), gameTree);
            }
        }

        if (verbose) {
            System.out.println("Game Trees Loaded");
        }
    }

    /** Optimised version of Zhang-Sasha tree edit Distance.
     https://www.researchgate.net/publication/220618233_Simple_Fast_Algorithms_for_the_Editing_Distance_Between_Trees_and_Related_Problems
     Optimisation References:
     * [1] M. Pawlik and N. Augsten. Efficient Computation of the Tree Edit Distance. ACM Transactions on Database Systems (TODS) 40(1). 2015.
     * [2] M. Pawlik and N. Augsten. Tree edit distance: Robust and memory-efficient. Information Systems 56. 2016.
     * [3] M. Pawlik and N. Augsten. RTED: A Robust Algorithm for the Tree Edit Distance. PVLDB 5(4). 2011.
     */
    private static double calculateAptedDistance(Tree treeA, Tree treeB)
    {
        final String treeABracketNotation = treeA.bracketNotation();
        final String treeBBracketNotation = treeB.bracketNotation();

        final BracketStringInputParser parser = new BracketStringInputParser();
        final Node<StringNodeData> t1 = parser.fromString(treeABracketNotation);
        final Node<StringNodeData> t2 = parser.fromString(treeBBracketNotation);

        final APTED<StringUnitCostModel, StringNodeData> apted = new APTED<>(new StringUnitCostModel());
        apted.computeEditDistance(t1, t2);
        final List<int[]> mapping = apted.computeEditMapping();

        final int maxTreeSize = Math.max(treeA.size(), treeB.size());

        return (double) apted.mappingCost(mapping) / maxTreeSize;
    }

    /**
     * @param game  The game that you want to compare against all others (i.e., the generated game)
     * @return      Map of the distance from game parameter to all other games loaded by loadGameTrees().
     */
    public static Map<String, Double> getDistanceToOtherGames(Game game)
    {
        if (gameTrees.size() == 0)
            loadGameTrees(null);    // Load all the game Trees for existing Ludii games into memory for later comparison

        Map<String, Double> gameDistances = new HashMap<>();
        final Tree treeA = ludemeDataset.getTree(game);
        for (String gameName : gameTrees.keySet())
        {
            Tree treeB = gameTrees.get(gameName);
            double distance = calculateAptedDistance(treeA, treeB);
            gameDistances.put(gameName, distance);
        }
        return gameDistances;
    }

    /**
     * @param game  The game that you want to compare against all others (i.e., the generated game)
     * @return      The minimum distance to ANY other game (measure of novelty)
     */
    public static double findClosestGameDistance(Game game)
    {
        Map<String, Double> gameDistances = getDistanceToOtherGames(game);
        double smallestDistance = 1.0;
        String closestGame = null;
        for (String gameName : gameDistances.keySet())
        {
            double distance = gameDistances.get(gameName);
            if (distance < smallestDistance)
            {
                smallestDistance = distance;
                closestGame = gameName;
            }
        }
        if (verbose)
            System.out.println("\nClosest = " + closestGame);
        return smallestDistance;
    }

    public static void main(String[] args)
    {
        String[] gameDescriptionsTest = new String[4];
        gameDescriptionsTest[0] = GameLoader.loadGameFromName("Chess.lud").description().expanded();
        gameDescriptionsTest[1] = GameLoader.loadGameFromName("Tic-Tac-Toe.lud").description().expanded();
        gameDescriptionsTest[2] = GameLoader.loadGameFromName("Hex.lud").description().expanded();
        gameDescriptionsTest[3] = GameLoader.loadGameFromName("Bara Guti (Bihar).lud").description().expanded();

        loadGameTrees(gameDescriptionsTest);

        String generatedGameDescription = "(game \"GAME_NAME\" \n" +
                "    (players 2)\n" +
                "\n" +
                "    (equipment {\n" +
                "            (board \n" +
                "                (concentric {1 8 8 8})\n" +
                "            use:Vertex)\n" +
                "            (piece \"PIECE_ALPHA\" Each \n" +
                "                (or \n" +
                "                    (move Hop Rotational \n" +
                "                        (between if: \n" +
                "                            (is Enemy (who at:\n" +
                "                                    (between)\n" +
                "                            ))\n" +
                "                            (apply \n" +
                "                                (remove \n" +
                "                                    (between)\n" +
                "                                )\n" +
                "                            )\n" +
                "                        )\n" +
                "                        (to if:(is Empty (to)))\n" +
                "                    )\n" +
                "                    (move Step Rotational \n" +
                "                        (to if: (is Empty (to)) )\n" +
                "                    )\n" +
                "                )\n" +
                "            )\n" +
                "    })\n" +
                "\n" +
                "    (rules \n" +
                "        (start {\n" +
                "                (place \"PIECE_ALPHA1\" \n" +
                "                    (sites {2 3 4 5 10 11 12 13 18 19 20 21})\n" +
                "                )\n" +
                "                (place \"PIECE_ALPHA2\" \n" +
                "                    (sites {1 9 17 6 7 8 14 15 16 22 23 24})\n" +
                "                )\n" +
                "        })\n" +
                "        (play \n" +
                "            (forEach Piece)\n" +
                "        )\n" +
                "\n" +
                "        (end \n" +
                "            (if (no Pieces Next) (result Next Loss))\n" +
                "        )\n" +
                "    )\n" +
                ")";

        final Game generatedGame = (Game) Compiler.compile
                (
                        new Description(generatedGameDescription),
                        new UserSelections(new ArrayList<String>()),
                        new Report(),
                        false
                );

        double novelty = findClosestGameDistance(generatedGame);
        System.out.println("Novelty = " + novelty);
    }

}
