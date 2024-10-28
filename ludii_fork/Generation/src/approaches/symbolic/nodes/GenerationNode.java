package approaches.symbolic.nodes;

import approaches.symbolic.SymbolMap;
import approaches.symbolic.SymbolMap.MappedSymbol;
import game.Game;
import game.equipment.Equipment;
import game.equipment.Item;
import game.equipment.container.board.Boardless;
import game.functions.dim.DimConstant;
import game.mode.Mode;
import game.players.Players;
import game.rules.Rules;
import game.rules.end.BaseEndRule;
import game.rules.end.End;
import game.rules.end.Result;
import game.rules.meta.Meta;
import game.rules.play.Play;
import game.rules.play.moves.nonDecision.effect.Pass;
import game.rules.start.Start;
import game.types.board.TilingBoardlessType;
import game.types.play.ResultType;
import game.types.play.RoleType;
import main.StringRoutines;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a node in the generator tree. Each node tracks its symbol, parent, and parameters. Each node can tell you
 * what parameters it can take next, and can compile itself by instantiating the symbol's ludeme class.
 */
public abstract class GenerationNode {
    final MappedSymbol symbol;
    final List<GenerationNode> parameterSet = new ArrayList<>();
    GenerationNode parent;
    Object compilerCache = null;
    String descriptionCache = null;
    String stringCache = null;
    String ancestorStringCache = null;
    boolean complete;

    public GenerationNode(MappedSymbol symbol, GenerationNode parent) {
        assert symbol != null;
        this.symbol = symbol;
        this.parent = parent;
    }

    /**
     * Identifies the correct type of node to create for a given symbol.
     * @param symbol The symbol to create a node for.
     * @param parent The parent of the node.
     * @return A new node of the appropriate type for the symbol.
     */
    public static GenerationNode fromSymbol(MappedSymbol symbol, GenerationNode parent) {
        if (symbol.nesting() > 0) {
            return new ArrayNode(symbol, parent);
        }

        if (PrimitiveNode.typeOf(symbol.path()) != null)
            return new PrimitiveNode(symbol, parent);

        if (symbol.cls().isEnum())
            return new EnumNode(symbol, parent);

        switch (symbol.path()) {
            case "mapper.unused" -> {
                return new PlaceholderNode(parent);
            }
            case "mapper.endOfClause" -> {
                return new EndOfClauseNode(parent);
            }
            case "game.Game" -> {
                return new GameNode();
            }
        }

        return new ClassNode(symbol, parent);
    }

    /**
     * Recursively compiles the ludeme represented by this node. This method is memoized, so it will only compile the
     * ludeme once.
     * @return The compiled ludeme.
     */
    public Object instantiate() {
        if (compilerCache == null)
            compilerCache = instantiateLudeme();

        return compilerCache;
    }

    public Object nullInstantiate() {

        try {
            return instantiateLudeme();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isCached() {
        return compilerCache != null;
    }

    abstract Object instantiateLudeme();

    public abstract List<GenerationNode> nextPossibleParameters(SymbolMap symbolMap);

    public List<GenerationNode> nextPossibleParameters(SymbolMap symbolMap, List<GenerationNode> partialArguments, boolean includeAliases, boolean expandEmpty) {
        List<GenerationNode> options;

        if (partialArguments == null || partialArguments.isEmpty()) {
            options = nextPossibleParameters(symbolMap);
        } else {
            int i = parameterSet.size();
            parameterSet.addAll(partialArguments);
            options = nextPossibleParameters(symbolMap);
            parameterSet.subList(i, parameterSet.size()).clear();
        }

        if (includeAliases || expandEmpty)
            options = new ArrayList<>(options);

        // Expand empty nodes
        if (expandEmpty) {
            PlaceholderNode empty = options.stream().filter(n -> n instanceof PlaceholderNode).map(n -> (PlaceholderNode) n).findFirst().orElse(null);
            if (empty != null) {
                options.remove(empty);
                List<GenerationNode> nextPartialArguments = partialArguments==null? new ArrayList<>() : new ArrayList<>(partialArguments);
                nextPartialArguments.add(empty);
                options.addAll(nextPossibleParameters(symbolMap, nextPartialArguments, includeAliases, expandEmpty));
            }
        }

        // Add aliases to the options (^ ... should also include (pow ...
        if (includeAliases) {
            options.addAll(options.stream().filter(n -> n.symbol().hasAlias()).map(n -> {
                MappedSymbol noAlias = new MappedSymbol(n.symbol());
                noAlias.setToken(StringRoutines.toDromedaryCase(noAlias.name()));
                return GenerationNode.fromSymbol(noAlias, n.parent());
            }).toList());
        }

        return options;
    }

    public void addParameter(GenerationNode param) {
        assert param != null;
        param.parent = this;
        clearCache();

        if (param instanceof EndOfClauseNode) {
            complete = true;
            return;
        }

        parameterSet.add(param);
    }

    public void popParameter() {
        parameterSet.remove(parameterSet.size() - 1);
        clearCache();
        complete = false;
    }

    public void clearParameters() {
        parameterSet.clear();
        clearCache();
        complete = false;
    }

    public void clearCache() {
//        if (parent.compilerCache != null)
        parent.clearCache();

        compilerCache = null;
        descriptionCache = null;
        stringCache = null;
        ancestorStringCache = null;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isRecursivelyComplete() {
        return isComplete() && parameterSet.stream().allMatch(GenerationNode::isRecursivelyComplete);
    }

    public MappedSymbol symbol() {
        return symbol;
    }

    public GenerationNode parent() {
        return parent;
    }

    public void setParent(GenerationNode parent) {
        this.parent = parent;
    }

    public List<GenerationNode> parameterSet() {
        return Collections.unmodifiableList(parameterSet);
    }

    public GenerationNode find(String token) {
        for (GenerationNode node : parameterSet) {
            if (node.symbol.token().equals(token))
                return node;
        }

        return null;
    }

    /**
     * Copies the node and all of its grandchildren down to the leaves.
     * @return A copy of the node and all of its grandchildren.
     */
    public GenerationNode copyDown() {
        GenerationNode clone = fromSymbol(symbol, parent);
        clone.parameterSet.addAll(parameterSet.stream().map(GenerationNode::copyDown).toList());
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        return clone;
    }

    /**
     * Copies the node and all of its ancestors up to the root node.
     * @return A copy of the node and all of its ancestors.
     */
    public GenerationNode copyUp() {
        GenerationNode clone = fromSymbol(symbol, parent);
        clone.parameterSet.addAll(parameterSet);
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        if (parent != null) {
            clone.parent = parent.copyUp();
            clone.parent.parameterSet.set(parent.parameterSet.indexOf(this), clone);
        }

        return clone;
    }

    /**
     * Note: this function is memoized
     * @return A representation of the game in compilable standard form as defined in DescriptionParser
     */
    public String description() {
        if (descriptionCache == null)
            descriptionCache = buildDescription();

        return descriptionCache;
    }

    /**
     * Note: this function is memoized
     * @return A representation of the game in compilable standard form as defined in DescriptionParser
     */
    public String toString() {
        if (stringCache == null)
            stringCache = buildString();

        return stringCache;
    }

    abstract String buildString();

    abstract String buildDescription();

    public GameNode root() {
        GenerationNode node = this;
        while (node.parent != null)
            node = node.parent;

        return (GameNode) node;
    }

    public GenerationNode get(List<Integer> indexes) {
        GenerationNode node = this;
        for (int i : indexes) {
            node = node.parameterSet.get(i);
        }
        return node;
    }

    public void stripTrailingPlaceholderNodes() {
        for (int i=parameterSet.size()-1; i >= 0; i--) {
            if (parameterSet.get(i) instanceof PlaceholderNode)
                parameterSet.remove(i);
            else
                break;
        }
    }

    public String ancestry() {
        if (ancestorStringCache == null) {
            StringBuilder builder = new StringBuilder();
            buildAncestry(builder);
            ancestorStringCache = builder.toString();
        }

        return ancestorStringCache;
    }

    private void buildAncestry(StringBuilder builder) {
        if (parent != null) {
            parent.buildAncestry(builder);
            builder.append('/').append(parent.parameterSet.indexOf(this));
        }
        builder.append('/').append(symbol.path());
    }

    public int depth() {
        return parent == null ? 0 : parent.depth() + 1;
    }

    public int nodeCount() {
        int count = complete ? 1 : 0;
        for (GenerationNode n : parameterSet) {
            if (n instanceof PlaceholderNode)
                continue;
            count += 1;
            count += n.nodeCount();
        }
        return count;
    }

    public boolean terminateClauses(SymbolMap symbolMap) {

        for (int i=0; i < parameterSet.size(); i++) {
            GenerationNode child = parameterSet.get(i);
            if (!child.terminateClauses(symbolMap)) {
                // Remove the child and all the children after it
                for (int j=parameterSet.size()-1; j >= i; j--) {
                    popParameter();
                }

                // Try again without the interminable children
                if (!terminateClauses(symbolMap)) {
                    return false;
                }
            }
        }

        while (!isComplete()) {
            List<GenerationNode> options = nextPossibleParameters(symbolMap);

            AtomicBoolean fail = new AtomicBoolean(false);
            options.stream().filter(a -> a instanceof EndOfClauseNode).findFirst().ifPresentOrElse(this::addParameter, () -> {
                options.stream().filter(a -> a instanceof PlaceholderNode).findFirst().ifPresentOrElse(this::addParameter, () -> fail.set(true));
            });

            if (fail.get()) {
//                System.out.println("Failed to terminate clauses for " + node.symbol().path() + " with " + options);
                return false;
            }

//            System.out.println("Added clauses for " + node.symbol().path() + " with " + options);
        }

//        System.out.println("Terminated clauses for " + node.symbol().path());
        return true;
    }



}
