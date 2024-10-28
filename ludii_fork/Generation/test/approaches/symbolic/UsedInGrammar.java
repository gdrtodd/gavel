package approaches.symbolic;
import grammar.Grammar;
import main.grammar.Symbol;

import java.util.List;

public class UsedInGrammar {
    public static void main(String[] args) {
        SymbolMap symbolMap = new SymbolMap();
        System.out.println("Finished mapping symbols. Found " + symbolMap.parameterMap.values().stream().mapToInt(List::size).sum() + " parameter sets.");

        // TODO Why is TrackStep used in the grammar?
        Symbol trackStep = Grammar.grammar().findSymbolByPath("game.util.equipment.TrackStep");
        //Symbol trackStep = Grammar.grammar().findSymbolByPath("game.functions.trackStep.TrackStep");
        System.out.println(trackStep);
        System.out.println(trackStep.rule());
        System.out.println(trackStep.usedInGrammar());
        System.out.println(symbolMap.nextValidParameters(trackStep, List.of()));
    }
}
