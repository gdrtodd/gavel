package approaches.symbolic.api;

import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;

import java.util.ArrayList;

public class CompleteConceptEndpoint extends Endpoint {
    public static void main(String[] args) {
        new CompleteConceptEndpoint().start();
    }

    @Override
    public String respond() {

        if (rawInput.isEmpty()) {
            rawInput = "(game \"Hex\" (players 2) (equipment {(board (hex Diamond 11)) (piece \"Marker\" Each) (regions P1 {(sites Side NE) (sites Side SW)}) (regions P2 {(sites Side NW) (sites Side SE)})}) (rules (meta (swap)) (play (move Add (to (sites Empty)))) (end (if (is Connected Mover) (result Mover Win)))))";
        }

        Game game = null;
        try {
            game = (Game) Compiler.compile(new Description(rawInput), new UserSelections(new ArrayList<>()), new Report(), false);
        } catch (Exception ignored) {}

        if (game == null)
            return "-1";

        return PartialConceptEndpoint.toBinaryString(game.computeBooleanConcepts()) + "|" + game.computeNonBooleanConcepts();
    }
}
