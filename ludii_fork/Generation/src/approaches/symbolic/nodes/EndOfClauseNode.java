package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMap;

import java.util.List;

/**
 * Node representing the end of a clause. This is a terminal node.
 */
public class EndOfClauseNode extends GenerationNode {
    public EndOfClauseNode(GenerationNode parent) {
        super(SymbolMap.endOfClauseSymbol, parent);
    }

    @Override
    Object instantiateLudeme() {
        throw new RuntimeException("EndOfClauseNode should never be instantiated");
    }

    @Override
    public List<GenerationNode> nextPossibleParameters(SymbolMap symbolMap) {
        return List.of();
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public String buildString() {
        return "END";
    }

    @Override
    public String buildDescription() {
        throw new RuntimeException("End nodes don't have a description");
    }
}
