package approaches.symbolic;

import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;
import parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static approaches.symbolic.FractionalCompiler.standardize;

public class FractionalCompilerCorrectness {
    static void testLudiiLibrary(SymbolMap symbolMap, int limit) throws IOException {
        List<String> skip = List.of("Kriegspiel (Chess).lud", "Throngs.lud", "Tai Shogi.lud", "Taikyoku Shogi.lud", "Yonin Seireigi.lud", "Yonin Shogi.lud"); // "To Kinegi tou Lagou.lud"

        String gamesRoot = "./Common/res/lud/board";
        List<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).sorted().limit(limit).toList();
        int count = 0;
        for (Path path : paths) {
            String gameStr = Files.readString(path);

            if (gameStr.contains("match")) {
                System.out.println("Skipping match " + path.getFileName());
                continue;
            }

            if (skip.contains(path.getFileName().toString())) {
                System.out.println("Skipping skip" + path.getFileName());
                continue;
            }

            System.out.println("\nLoading " + path.getFileName() + " (" + (count + 1) + " of " + paths.size() + " games)");

            Description description = new Description(gameStr);
            final UserSelections userSelections = new UserSelections(new ArrayList<>());
            final Report report = new Report();
            Parser.expandAndParse(description, userSelections, report, true, false);

            String expandedDescription = standardize(description.expanded());
            FractionalCompiler.CompilationCheckpoint compilation = FractionalCompiler.compileFraction("(game", symbolMap);
            for (int i = 5; i < expandedDescription.length() - 1; i++) {
                System.out.println(expandedDescription.substring(0, i + 1));
                compilation = FractionalCompiler.compileFraction(expandedDescription.substring(0, i + 1), compilation, symbolMap);
                System.out.println("   --> " + compilation.longest.get(0).consistentGame.root().description());
                System.out.println("   --> " + compilation.longest.size());
//                compilation.forEach(s -> System.out.println("       --> " + s.consistentGame + " ->> " + s.remainingOptions + " -- " + s.exceptions));

//                if (i > 40)
//                    throw new RuntimeException();
            }

            System.out.println("expected:\n"+expandedDescription);


//            if (!compilation.peek().exceptions.isEmpty()) {
//                compilation.peek().exceptions.forEach(Throwable::printStackTrace);
//                throw new RuntimeException("Error compiling " + path.getFileName());
//            }

            if (!compilation.longest.get(0).consistentGame.root().isRecursivelyComplete()) {
                throw new RuntimeException("Incomplete " + path.getFileName());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        CachedMap symbolMapper = new CachedMap();
        testLudiiLibrary(symbolMapper, 2000);
        System.out.println("cache:" + symbolMapper.cachedQueries.size());

//        testLudiiLibrary(symbolMapper, 100);
//        String gameName = "Pagade Kayi Ata (Sixteen-handed)"; // TODO Throngs (memory error), There and Back, Pyrga, There and Back, Kriegspiel (Chess), Tai Shogi
//        Description description = new Description(Files.readString(Path.of("./Common/res/" + GameLoader.getFilePath(gameName))));
//        Compiler.compile(description, new UserSelections(new ArrayList<>()), new Report(), false);
//        System.out.println(description.expanded());
//        System.out.println(standardize(description.expanded()));
////        printCallTree(description.callTree(), 0);
//        GameNode gameNode = compileDescription(standardize(description.expanded()), new SymbolMapper());
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
