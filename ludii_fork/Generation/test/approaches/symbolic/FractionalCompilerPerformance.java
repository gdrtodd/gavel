package approaches.symbolic;

import approaches.symbolic.nodes.GameNode;
import compiler.Compiler;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static approaches.symbolic.FractionalCompiler.*;

public class FractionalCompilerPerformance {
    static void testLudiiLibrary(SymbolMap symbolMap, int start, int limit) throws IOException {
        List<String> skip = List.of("Kriegspiel (Chess).lud", "Throngs.lud", "Tai Shogi.lud", "Taikyoku Shogi.lud", "Yonin Seireigi.lud", "Yonin Shogi.lud", "MensaSpiel.lud", "Kriegsspiel.lud", "Mini Wars.lud", "Netted.lud"); // "To Kinegi tou Lagou.lud"

        String gamesRoot = "./Common/res/lud/good";
        List<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).sorted().limit(limit).toList();
        int count = 0;
        int preCompilation = 0;
        int compile = 0;
        int recompile = 0;
        int fromString = 0;
        for (Path path : paths) {
            count++;
            if (count < start)
                continue;

            String gameStr = Files.readString(path);

            if (gameStr.contains("match")) {
                System.out.println("Skipping match " + path.getFileName());
                continue;
            }

            if (skip.contains(path.getFileName().toString())) {
                System.out.println("Skipping skip" + path.getFileName());
                continue;
            }

            System.out.println("\nLoading " + path.getFileName() + " (" + (count) + " of " + paths.size() + " games)");

            Description description = new Description(gameStr);

            final UserSelections userSelections = new UserSelections(new ArrayList<>());
            final Report report = new Report();

            final long startPreCompilation = System.currentTimeMillis();
            try {
                Compiler.compile(description, userSelections, report, false);
            } catch (Exception e) {
                System.out.println("Could not pre-compile " + path.getFileName());
                continue;
            }
            final long endPreCompilation = System.currentTimeMillis();
            //System.out.println("Old compile: " + (endPreCompilation - startPreCompilation) + "ms");

            //Playground.printCallTree(originalGame.description().callTree(), 0);

            GameNode rootNode;
            try {
                rootNode = compileComplete(standardize(description.expanded()), symbolMap);
                rootNode.instantiate();
            } catch (Exception e) {
                System.out.println("Could not compile description " + path.getFileName());
                System.out.println(e.getMessage());
                System.out.println("Skipping for now...");
                //throw e;
                continue;
            }
            final long endCompile = System.currentTimeMillis();
            //System.out.println("My Compile: " + (endCompile - endClone) + "ms");

            try {
                rootNode.rulesNode().clearCache();
                rootNode.instantiate();
            } catch (Exception e) {
                System.out.println("Could not recompile " + path.getFileName());
                throw e;
            }
            final long endRecompile = System.currentTimeMillis();
            //System.out.println("My Recompile: " + (endRecompile - endCompile) + "ms");

            try {
                Compiler.compile(new Description(rootNode.description()), new UserSelections(new ArrayList<>()), new Report(), false);
            } catch (Exception e) {
                System.out.println("Could not compile from description " + path.getFileName());
                System.out.println(standardize(rootNode.description()));
                System.out.println(standardize(description.expanded()));
                throw e;
                //continue;
            }
            final long endDescription = System.currentTimeMillis();

            preCompilation += (int) (endPreCompilation - startPreCompilation);
            compile += (int) (endCompile - endPreCompilation);
            recompile += (int) (endRecompile - endCompile);
            fromString += (int) (endDescription - endRecompile);

            System.out.println("pre-compile:  " + (endPreCompilation - startPreCompilation) + "ms");
            System.out.println("my-compile:   " + (endCompile - endPreCompilation) + "ms");
            System.out.println("my-recompile: " + (endRecompile - endCompile) + "ms");
            System.out.println("from-string:  " + (endDescription - endRecompile) + "ms");

        }

        System.out.println("Games:           " + count);
        System.out.println("Pre-compilation: " + preCompilation + "ms");
        System.out.println("Compile:         " + compile + "ms");
        System.out.println("Recompile:       " + recompile + "ms");
        System.out.println("From string:     " + fromString + "ms");

    }

    public static void main(String[] args) throws IOException {
        CachedMap symbolMapper = new CachedMap();
        testLudiiLibrary(symbolMapper, 0, 2000);
        System.out.println("cache:" + symbolMapper.cachedQueries.size() + " of " + symbolMapper.requests);

//        String gameName = "Netted.lud";
////        Path gamePath = Files.walk(Paths.get("./Common/res/lud/good")).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(gameName)).findAny().orElseThrow();
//        Description description = new Description(Files.readString(Path.of("./Common/res/" + GameLoader.getFilePath(gameName))));
//        Compiler.compile(description, new UserSelections(new ArrayList<>()), new Report(), false);
//        System.out.println(standardize(description.expanded()));
////        printCallTree(description.callTree(), 0);
//        GameNode gameNode = compileComplete(standardize(description.expanded()), symbolMapper);
//        System.out.println(gameNode.isRecursivelyComplete());

//        System.out.println(standardize("0.0 hjbhjbjhj 9.70 9.09 (9.0) 8888.000  3.36000 3. (5.0} 9.2 or: 9 (game a  :     (g)"));

//        String gameString = "(game \"Hex\" (players 2)\n\n\n (equipment         { (board (hex Diamond 10)) (piece \"Marker\" Each) (regions P1 {(    sites Side NE) (sites Side SW)\n}) (regions P2 {(sites Side NW) (sites Side SE)    }\n\n)\n}    ) (rules (meta (swap\n\n\n)) (play (move \n\nAdd (to (sites \n\nEmpty)))) (end\n\n (if (is Connected      Mover   ) (   result Mover Win))   )))";
//        String standard = standardize(gameString).substring(0, 289);
//        System.out.println(standard);
//        System.out.println(destandardize(gameString, standard));

//        String gameDescription = Files.readString(Path.of("./Common/res/" + GameLoader.getFilePath("Adugo"))).substring(0, 110); //
//        System.out.println(gameDescription);
//        Description description = new Description(gameDescription);
//        Parser.expandAndParse(description, new UserSelections(List.of()), new Report(), true, true);
//        System.out.println("Expanded: " + description.expanded());

    }
}
