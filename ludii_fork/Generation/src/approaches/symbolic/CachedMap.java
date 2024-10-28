package approaches.symbolic;

import main.grammar.Symbol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extends SymbolMapper by memoizing the results of the nextPossibilities method.
 */
public class CachedMap extends SymbolMap {
    public long requests = 0;
    public Map<String, List<MappedSymbol>> cachedQueries = new HashMap<>();

    @Override
    public List<MappedSymbol> nextValidParameters(Symbol parent, List<? extends Symbol> partialArguments) {
        String key = buildKey(parent, partialArguments);
        List<MappedSymbol> cachedSymbols = cachedQueries.get(key);

        if (cachedSymbols == null) {
            cachedSymbols = super.nextValidParameters(parent, partialArguments);
            cachedQueries.put(key, cachedSymbols);
        }

        requests++;
        return cachedSymbols;
    }

    static String buildKey(Symbol parent, List<? extends Symbol> partialArguments) {
        StringBuilder sb = new StringBuilder();
        sb.append(parent.path()).append('|').append(parent.nesting()).append('(');
        for (Symbol s : partialArguments) {
            sb.append(s.path()).append('|').append(s.nesting()).append(',');
        }
        return sb.append(')').toString();
    }
}
