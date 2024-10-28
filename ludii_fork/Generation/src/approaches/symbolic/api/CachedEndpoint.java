package approaches.symbolic.api;

import approaches.symbolic.CachedMap;
import approaches.symbolic.FractionalCompiler;
import approaches.symbolic.SymbolMap;

import static approaches.symbolic.FractionalCompiler.standardize;

public abstract class CachedEndpoint extends Endpoint {
    SymbolMap symbolMap = new CachedMap();
    FractionalCompiler.CompilationCheckpoint compilationCheckpoint;
    FractionalCompiler.CompilationCheckpoint compilationCache;
    String standardInput;

    boolean recordTime = false;

    abstract String cachedResponse();

    @Override
    public String respond() {
        boolean overwriteCache = true;
        if (rawInput.startsWith("@")) {
            rawInput = rawInput.substring(1);
            overwriteCache = false;
        }

        standardInput = standardize(rawInput);

        if (recordTime) {
            long startTime = System.nanoTime();
            assignCheckpoint();
            long endTime = System.nanoTime();
            System.out.println("Checkpoint Assignment Time: " + (endTime - startTime) / 1000000.0 + "ms");
        } else {
            assignCheckpoint();
        }

        if (overwriteCache)
            compilationCache = compilationCheckpoint;

        return cachedResponse();
    }

    private void assignCheckpoint() {
        compilationCheckpoint = null;

        if (compilationCache == null || compilationCache.longest.isEmpty()) {
            compilationCheckpoint = FractionalCompiler.compileFraction(standardInput, symbolMap);
        } else {
            if (compilationCache.longest.size() > 64)
                System.out.println("Warning: " + compilationCache.longest.size() + " possible caches found for " + standardInput);

            String cachedDescription = compilationCache.longest.get(0).consistentGame.root().description();

            if (!standardInput.equals(cachedDescription)) {
                if (standardInput.startsWith(cachedDescription))
                    compilationCheckpoint = FractionalCompiler.compileFraction(standardInput, compilationCache, symbolMap);
                else
                    compilationCheckpoint = FractionalCompiler.compileFraction(standardInput, symbolMap);
            } else {
                compilationCheckpoint = compilationCache;
            }
        }

        assert compilationCheckpoint != null;
    }
}
