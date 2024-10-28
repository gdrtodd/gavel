package approaches.symbolic.api;

import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;
import parser.Expander;
import supplementary.experiments.eval.EvalGames;

import java.util.ArrayList;


/*
 * Used by the extension to evaluate whether a game description compiles.
 * It supports definitions but can't tell you which part of the description is causing the error.
 *
 * @author Alexander Padula
 */
public class ExpanderEndpoint extends Endpoint {

    public static void main(String[] args) {
        new ExpanderEndpoint().start();
    }

    @Override
    public String respond() {
        Description description = new Description(rawInput);
        try {
            Expander.expand(description, new UserSelections(new ArrayList<>()), new Report(), false);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return description.expanded();
    }
}
