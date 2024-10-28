package approaches.symbolic;

import grammar.Grammar;
import main.grammar.Clause;
import main.grammar.ClauseArg;

import java.util.List;

public class AndGroups {
    public static void main(String[] args) {
        // TODO I don't understand and groups
        List<Clause> clauses = Grammar.grammar().findSymbolByPath("game.rules.play.moves.decision.Move").rule().rhs();
        for (int i = 0; i < clauses.size(); i++) {
            System.out.println("Clause " + i + ": " + clauses.get(i));
            System.out.println("          " + clauses.get(i).args().stream().map(ClauseArg::andGroup).toList());
        }
    }
}
