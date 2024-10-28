package approaches.symbolic;

import main.grammar.Symbol;

import java.util.ArrayList;
import java.util.List;

public class SymbolMapPerformance {
    public static void main(String[] args) {
        long startTime = System.nanoTime();
        SymbolMap symbolMap = new SymbolMap();
        long endTime = System.nanoTime();
        System.out.println("SymbolMap creation time: " + (endTime - startTime) / 1000000 + "ms");

        System.out.println("SymbolMap lookup times in ms:");
        for (Symbol symbol: symbolMap.symbols) {
//            if (!symbol.usedInGrammar())
//                continue;

            List<Symbol> arguments = new ArrayList<>();
            while (arguments.isEmpty() || arguments.get(arguments.size() - 1) != SymbolMap.endOfClauseSymbol) {
                startTime = System.nanoTime();
                List<SymbolMap.MappedSymbol> nextValidParameters = symbolMap.nextValidParameters(symbol, arguments);
                endTime = System.nanoTime();
                if (nextValidParameters.isEmpty())
                    break;
                System.out.println((endTime - startTime) / 1000000.0);
//                System.out.println(symbol + ": " + nextValidParameters);
                arguments.add(nextValidParameters.get((int) (Math.random() * nextValidParameters.size())));
            }
        }
    }
}
