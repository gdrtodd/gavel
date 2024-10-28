package approaches.symbolic;

import grammar.Grammar;
import main.StringRoutines;
import main.grammar.Clause;
import main.grammar.ClauseArg;
import main.grammar.Symbol;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SyntaxHighlightTool {
    public static void main(String[] args) {
        List<Symbol> symbols = Grammar.grammar().symbols();
        Set<String> classes = new HashSet<>();
        Set<String> enums = new HashSet<>();
        Set<String> labels = new HashSet<>();
        for (Symbol s : symbols) {
            if (s.usedInGrammar()) {
                if (s.cls().isEnum()) {
                    if (!s.cls().getTypeName().equals(s.path()))
                        enums.add(s.token());
                } else {
                    classes.add(s.token());
                    if (s.hasAlias())
                        classes.add(StringRoutines.toDromedaryCase(s.name()));

                    if (s.hasAlias())
                        System.out.println(s.token() + " == " + StringRoutines.toDromedaryCase(s.name()));
                }

                if (s.rule() != null && s.rule().rhs() != null)
                    for (Clause r : s.rule().rhs())
                        if (r.args() != null)
                            for (ClauseArg a : r.args())
                                if (a.label() != null)
                                    labels.add(StringRoutines.toDromedaryCase(a.label()));

                }
        }

        System.out.println(enums.stream().reduce((a, b) -> a + "|" + b).orElse(""));
        System.out.println(classes.stream().reduce((a, b) -> a + "|" + b).orElse(""));
        System.out.println(labels.stream().reduce((a, b) -> a + "|" + b).orElse(""));
    }
}
