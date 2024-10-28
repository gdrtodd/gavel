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
import grammar.Grammar;
import main.grammar.Description;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Node representing a game Ludeme. Since this is the root of the tree, it has no parent.
 */
public class GameNode extends GenerationNode {
    static final MappedSymbol gameSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.Game"), null);
    static MappedSymbol nameSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("java.lang.String"), null);
    static MappedSymbol playersSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.players.Players"), null);
    static MappedSymbol equipmentSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.equipment.Equipment"), null);
    static MappedSymbol modeSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.mode.Mode"), null);
    static MappedSymbol rulesSymbol = new MappedSymbol(Grammar.grammar().findSymbolByPath("game.rules.Rules"), null);

    public GameNode(MappedSymbol symbol) {
        super(symbol, null);
        assert symbol.path().equals("game.Game");
    }

    public GameNode() {
        super(gameSymbol, null);
    }

    @Override
    public Game instantiate() {
        boolean wasCached = this.equipmentNode().isCached();
        if (compilerCache != null) return (Game) compilerCache;
        Game game = instantiateLudeme();

        game.setDescription(new Description(description()));

        game.create();

        compilerCache = game;
        return (Game) compilerCache;
    }

    @Override
    public Game instantiateLudeme() {
        return new Game((String) nameNode().instantiate(), (Players) playersNode().instantiate(), modeNode() != null ? (Mode) modeNode().instantiate() : null, (Equipment) equipmentNode().instantiate(), (Rules) rulesNode().instantiate());
    }

    @Override
    public List<GenerationNode> nextPossibleParameters(SymbolMap symbolMap) {
        if (complete) return List.of();

        switch (parameterSet.size()) {
            case 0 -> {
                return List.of(new PrimitiveNode(nameSymbol, this));
            }
            case 1 -> {
                return List.of(new ClassNode(playersSymbol, this));
            }
            case 2 -> {
                ArrayList<GenerationNode> options = new ArrayList<>(2);
                options.add(new PlaceholderNode(this));
                options.add(new ClassNode(modeSymbol, this));
                return options;
            }
            case 3 -> {
                return List.of(new ClassNode(equipmentSymbol, this));
            }
            case 4 -> {
                return List.of(new ClassNode(rulesSymbol, this));
            }
            case 5 -> {
                return List.of(new EndOfClauseNode(this));
            }
            default -> {
                throw new IllegalStateException("Unexpected state: " + parameterSet.size());
            }
        }
    }

    @Override
    public void clearCache() {
        compilerCache = null;
        descriptionCache = null;
    }

    @Override
    public String buildString() {
        String label = "";
        if (symbol.label != null)
            label = symbol.label + ":";

        return label + "(" + symbol.grammarLabel() + ": " + String.join(", ", parameterSet.stream().map(GenerationNode::toString).toList()) + ")";
    }

    @Override
    String buildDescription() {
        String parameterString = String.join(" ", parameterSet.stream().filter(s -> !(s instanceof PlaceholderNode || s instanceof EndOfClauseNode)).map(GenerationNode::description).toList());
        if (parameterString.length() > 0)
            parameterString = " " + parameterString;

        String close = "";
        if (complete)
            close = ")";

        return "(" + symbol.token() + parameterString + close;
    }

    @Override
    public GameNode copyDown() {
        GameNode clone = new GameNode(symbol);
        clone.parameterSet.addAll(parameterSet.stream().map(GenerationNode::copyDown).toList());
        clone.complete = complete;
        clone.compilerCache = compilerCache;
        return clone;
    }

    public GenerationNode nameNode() {
        return parameterSet.isEmpty() ? null : parameterSet.get(0);
    }

    public GenerationNode playersNode() {
        return parameterSet.size() >= 2? parameterSet.get(1) : null;
    }

    public GenerationNode modeNode() {
        return parameterSet.size() >= 3? parameterSet.get(2) : null;
    }

    public GenerationNode equipmentNode() {
        return parameterSet.size() >= 4? parameterSet.get(3) : null;
    }

    public GenerationNode rulesNode() {
        return parameterSet.size() >= 5? parameterSet.get(4) : null;
    }

    @Override
    public void setParent(GenerationNode parent) {
        throw new RuntimeException("Cannot set parent of GameNode");
    }

    public Game safeInstantiate(SymbolMap symbolMap) {
        String name = nameNode() != null? (String) nameNode().instantiate() : "default";

        Players players = new Players(2);
        if (playersNode() != null) {
            playersNode().terminateClauses(symbolMap);
            Players playersInst = (Players) playersNode().nullInstantiate();
            if (playersInst != null)
                players = playersInst;
        }

        Mode mode = null;
        if (modeNode() != null) {
            modeNode().terminateClauses(symbolMap);
            mode = (Mode) modeNode().nullInstantiate();
        }

        Equipment equipment = new Equipment(new Item[]{new Boardless(TilingBoardlessType.Square, new DimConstant(2), false)});
        if (equipmentNode() != null) {
            equipmentNode().terminateClauses(symbolMap);
            Equipment equipmentInst = (Equipment) equipmentNode().nullInstantiate();
            if (equipmentInst != null)
                equipment = equipmentInst;
        }

        List<GenerationNode> params = List.of();
        if (rulesNode() != null) {
            params = rulesNode().parameterSet();
        }

        for (GenerationNode param : params) {
            param.terminateClauses(symbolMap);
        }

        Meta meta = params.isEmpty()? null : (Meta) params.get(0).nullInstantiate();
        Start start = params.size() < 2? null : (Start) params.get(1).nullInstantiate();

        Play play = new Play(new Pass(null));
        if (params.size() >= 3) {
            Play playInst = (Play) params.get(2).nullInstantiate();
            if (playInst != null)
                play = playInst;
        }

        End end = new End(new BaseEndRule(new Result(RoleType.All, ResultType.Draw)), null);
        if (params.size() >= 4) {
            End endInst = (End) params.get(3).nullInstantiate();
            if (endInst != null)
                end = endInst;
        }

        Rules rules = new Rules(meta, start, play, end);

        return new Game(name, players, mode, equipment, rules);
    }
}
