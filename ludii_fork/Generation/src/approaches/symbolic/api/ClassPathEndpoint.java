package approaches.symbolic.api;

import approaches.symbolic.nodes.EndOfClauseNode;
import approaches.symbolic.nodes.GameNode;
import approaches.symbolic.nodes.GenerationNode;
import approaches.symbolic.nodes.PlaceholderNode;
import supplementary.experiments.eval.EvalGames;

import static approaches.symbolic.FractionalCompiler.*;

/*
    * Used by the extension to evaluate up to what point a game description compiles.
    * It does not yet support definitions. But it can tell you where the error is.
    *
    * @author Alexander Padula
 */
public class ClassPathEndpoint extends CachedEndpoint {

    public static void main(String[] args) {
        new ClassPathEndpoint().start();
    }

    @Override
    String cachedResponse() {
        CompilationCheckpoint partialCompilation = compileFraction(standardInput, symbolMap);

        GameNode gameNode = partialCompilation.longest.get(0).consistentGame.root();

        return buildDescription(gameNode).replaceAll("\\s+", " ").strip();
    }

    String buildDescription(GenerationNode node) {

        if (node instanceof PlaceholderNode || node instanceof EndOfClauseNode)
            return "";

        return node.symbol().path() + " " + node.parameterSet().stream().map(this::buildDescription).reduce("", (a, b) -> a + " " + b);
    }
}
