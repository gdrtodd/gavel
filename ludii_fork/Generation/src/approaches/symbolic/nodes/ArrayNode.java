package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMap;
import approaches.symbolic.SymbolMap.MappedSymbol;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Node representing symbols with a nesting level > 0. Compiles to an array containing its compiled parameters.
 */
public class ArrayNode extends GenerationNode {
    public ArrayNode(MappedSymbol symbol, GenerationNode parent) {
        super(symbol, parent);

        assert symbol.nesting() > 0;
    }

    Object instantiateLudeme() {
        List<Object> arguments = parameterSet.stream().filter(Objects::nonNull).map(GenerationNode::instantiate).toList();

        Object array;
        if (symbol.nesting() == 1)
            array = Array.newInstance(symbol.cls(), arguments.size());
        else {
            array = Array.newInstance(arguments.get(0).getClass(), arguments.size());  // TODO Is this correct?
        }

        for (int i = 0; i < arguments.size(); i++) {
            Array.set(array, i, arguments.get(i));
        }

        return array;
    }

    // TODO do I really need to find all the permutations of IntFunctions and int? Probably not.
    public List<GenerationNode> nextPossibleParameters(SymbolMap symbolMap) {
        if (!parameterSet.isEmpty() && parameterSet.get(parameterSet.size() - 1) instanceof EndOfClauseNode)
            return List.of();

        List<GenerationNode> options = new ArrayList<>();
        if (symbol.nesting() == 1) {
            switch (symbol.path()) {
                case "int", "float", "boolean" -> {  // TODO not sure this is correct
                    options.add(new PrimitiveNode(new MappedSymbol(symbol, 0, null), this));
                }
                default -> {
                    options.addAll(symbolMap.getInstantiableSymbols(symbol).stream().map(s -> fromSymbol(new MappedSymbol(s, null), this)).toList());
                }
            }
        } else {
            MappedSymbol childSymbol = new MappedSymbol(symbol, null);
            childSymbol.setNesting(symbol.nesting() - 1);
            options.add(new ArrayNode(childSymbol, this));
        }

        options.add(new EndOfClauseNode(this));

        return options;
    }

    @Override
    public String buildString() {
        String label = "";
        if (symbol.label != null)
            label = symbol.label + ":";

        return label + "{" + symbol.grammarLabel() + "; " + String.join(" ", parameterSet.stream().map(GenerationNode::toString).toList()) + "}";
    }

    @Override
    String buildDescription() {
        String label = "";
        if (symbol.label != null)
            label = symbol.label + ":";

        String close = "";
        if (complete)
            close = "}";

        return label + "{" + String.join(" ", parameterSet.stream().filter(s -> !(s instanceof PlaceholderNode || s instanceof EndOfClauseNode)).map(GenerationNode::description).toList()) + close;
    }

}
