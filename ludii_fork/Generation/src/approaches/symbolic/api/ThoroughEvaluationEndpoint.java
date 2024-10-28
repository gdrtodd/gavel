package approaches.symbolic.api;

import approaches.symbolic.nodes.GameNode;
import supplementary.experiments.eval.EvalGames;

import static approaches.symbolic.FractionalCompiler.*;

/**
 * Provides a thorough evaluation of a game description.
 */
public class ThoroughEvaluationEndpoint extends CachedEndpoint {
    public static void main(String[] args) {
        new ThoroughEvaluationEndpoint().start();
    }

    @Override
    String cachedResponse() {
        CompilationCheckpoint partialCompilation = compileFraction(standardize(rawInput), symbolMap);
        boolean compiles = partialCompilation.longest.get(0).exceptions.isEmpty();
        if (!compiles)
            return "";

        GameNode gameNode = partialCompilation.longest.get(0).consistentGame.root();
        return String.valueOf(EvalGames.defaultEvaluationSlow(gameNode.instantiate()));
    }
}
