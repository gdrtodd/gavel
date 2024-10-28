package approaches.symbolic.api.evaluation;

import approaches.symbolic.api.Endpoint;
import compiler.Compiler;
import game.Game;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;
import metrics.designer.SkillTrace;

import java.util.ArrayList;

public class SkillTraceEndpoint extends Endpoint {
    final SkillTrace skillTraceMetric = new SkillTrace();

    public static void main(String[] args) {
        new SkillTraceEndpoint().start();
    }

    @Override
    public String respond() {
        Game game = null;
        try {
            game = (Game) Compiler.compile(new Description(rawInput), new UserSelections(new ArrayList<>()), new Report(), false);
        } catch (Exception ignored) {}

        if (game == null)
            return "-1";

        return "" + skillTraceMetric.apply(game, null, null, null);
    }
}
