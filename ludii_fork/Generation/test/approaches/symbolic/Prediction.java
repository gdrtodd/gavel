package approaches.symbolic;

import grammar.Grammar;
import main.grammar.Symbol;

import java.util.List;

public class Prediction {
    public static void main(String[] args) {
        SymbolMap symbolMap = new SymbolMap();
        Symbol symbol = Grammar.grammar().findSymbolByPath("game.functions.region.sites.Sites");
        List<Symbol> arguments = List.of();
        System.out.println(symbolMap.nextValidParameters(symbol, arguments).stream().map(s -> s.path() + "|" + s.nesting()).toList());
    }
}
