package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMap;
import approaches.symbolic.SymbolMap.MappedSymbol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Node representing a most ludemes. This is usually a non-terminal node.
 */
public class ClassNode extends GenerationNode {
    public ClassNode(MappedSymbol symbol, GenerationNode parent) {
        super(symbol, parent);
        assert !symbol.path().equals("game.Game");
    }

    Object instantiateLudeme() {
        List<Object> arguments = parameterSet.stream().map(param -> param != null? param.instantiate():null).toList();

        // TODO how to know whether to use constructor or static .construct();
        for (Method method: symbol.cls().getMethods()) {
            if (method.getName().equals("construct")) {
                // TODO don't ignore InvocationTargetException
                try {
                    return method.invoke(null, arguments.toArray());
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ignored) {}
            }
        }

        for (Constructor<?> constructor: symbol.cls().getConstructors()) {
            try {
                return constructor.newInstance(arguments.toArray());
            }catch (IllegalArgumentException | IllegalAccessException | InstantiationException | InvocationTargetException ignored) {}
        }

        throw new RuntimeException("Failed to compile: " + symbol + " with parameters " + arguments);
    }

    public List<GenerationNode> nextPossibleParameters(SymbolMap symbolMap) {
        List<MappedSymbol> partialParameters = parameterSet.stream().map(node -> node.symbol).toList();
        List<MappedSymbol> possibleSymbols = symbolMap.nextValidParameters(symbol, partialParameters);
        return possibleSymbols.stream().map(s -> GenerationNode.fromSymbol(s, this)).toList();
    }

    @Override
    public String buildString() {
        String label = "";
        if (symbol.label != null)
            label = symbol.label + ":";

        return label + "(" + symbol.path() + "; " + String.join(" ", parameterSet.stream().map(GenerationNode::toString).toList()) + ")";
    }

    @Override
    String buildDescription() {
        String label = "";
        if (symbol.label != null)
            label = symbol.label + ":";

        String parameterString = String.join(" ", parameterSet.stream().filter(s -> !(s instanceof PlaceholderNode || s instanceof EndOfClauseNode)).map(GenerationNode::description).toList());
        if (!parameterString.isEmpty())
            parameterString = " " + parameterString;

        String close = "";
        if (complete)
            close = ")";

        return label + "(" + symbol.token() + parameterString + close;
    }
}
