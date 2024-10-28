package approaches.symbolic.api.evaluation;

import approaches.symbolic.api.Endpoint;
import compiler.Compiler;
import game.Game;
import gameDistance.NoveltyMeasure;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;

import java.util.ArrayList;
import java.util.Arrays;

public class NoveltyEndpoint extends Endpoint {

    public NoveltyEndpoint() {
        super();
        logToFile = true;
    }

    @Override
    public String respond() {

        // TODO temp
        if (rawInput.isEmpty()) {
            rawInput = "(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each) (regions P1 {(sites Side NE) (sites Side SW)}) (regions P2 {(sites Side NW) (sites Side SE)})}) (rules (meta (swap)) (play (move Add (to (sites Empty)))) (end (if (is Connected Mover) (result Mover Win)))))";
        }

        // Initialize the game's trees for later comparison
        if (rawInput.contains("|")) {
            NoveltyMeasure.verbose = !logToFile;
            String[] games = Arrays.stream(rawInput.split("\\|")).filter(s -> !s.isEmpty()).toArray(String[]::new);
            if (games.length == 0)
                NoveltyMeasure.loadGameTrees(null);
            else
                NoveltyMeasure.loadGameTrees(games);

            return "0";
        }

        Game game = null;
        try {
            game = (Game) Compiler.compile(new Description(rawInput), new UserSelections(new ArrayList<>()), new Report(), false);
        } catch (Exception ignored) {}

        if (game == null)
            return "-1";

        System.out.println("NoveltyEndpoint: " + game.name());

        return "" + NoveltyMeasure.findClosestGameDistance(game);
    }

    public static void main(String[] args) {
        new NoveltyEndpoint().start();
    }
}
