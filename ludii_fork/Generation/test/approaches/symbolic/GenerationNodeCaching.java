package approaches.symbolic;

import approaches.symbolic.nodes.GameNode;
import main.grammar.Description;
import main.grammar.Report;
import main.options.UserSelections;
import parser.Parser;
import supplementary.experiments.eval.EvalGames;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static approaches.symbolic.FractionalCompiler.compileComplete;
import static approaches.symbolic.FractionalCompiler.standardize;

public class GenerationNodeCaching {


    static void testNodeCaches(SymbolMap symbolMap, int start, int limit) throws IOException {
        EvalGames.debug = false;
        List<String> skip = List.of("Kriegspiel (Chess).lud", "Throngs.lud", "Tai Shogi.lud", "Taikyoku Shogi.lud", "Yonin Seireigi.lud", "Yonin Shogi.lud", "MensaSpiel.lud", "Kriegsspiel.lud", "Mini Wars.lud", "Netted.lud"); // "To Kinegi tou Lagou.lud"

        String gamesRoot = "./Common/res/lud/good";
        List<Path> paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).sorted().limit(limit).toList();
        int count = 0;
        long compile = 0;
        long rules = 0;
        long equipment = 0;
        for (Path path : paths) {
            count++;
            if (count < start)
                continue;

            String gameStr = Files.readString(path);

            if (gameStr.contains("match")) {
//                System.out.println("Skipping match " + path.getFileName());
                continue;
            }

            if (skip.contains(path.getFileName().toString())) {
//                System.out.println("Skipping skip" + path.getFileName());
                continue;
            }

            System.out.println("\nLoading " + path.getFileName() + " (" + (count) + " of " + paths.size() + " games)");

            Description description = new Description(gameStr);

            final UserSelections userSelections = new UserSelections(new ArrayList<>());
            final Report report = new Report();

            Parser.expandAndParse(description, userSelections, report, false);

            GameNode rootNode = compileComplete(standardize(description.expanded()), symbolMap);
            final long startCompile = System.nanoTime();
            rootNode.instantiate();
            final long endCompile = System.nanoTime();
//            System.out.println("eval: " + EvalGames.defaultEvaluationFast(rootNode.instantiate()));

            try {
                rootNode.rulesNode().clearCache();
                rootNode.instantiate();
            } catch (Exception e) {
                System.out.println("Rules " + path.getFileName());
                throw e;
            }
            final long endRules = System.nanoTime();
//            System.out.println("eval rules: " + EvalGames.defaultEvaluationFast(rootNode.instantiate()));


            try {
                rootNode.equipmentNode().clearCache();
                rootNode.instantiate();
            } catch (Exception e) {
                System.out.println("Equipment " + path.getFileName());
                throw e;
            }
            final long endEquipment = System.nanoTime();
//            System.out.println("eval equipment: " + EvalGames.defaultEvaluationFast(rootNode.instantiate()));

            compile += endCompile - startCompile;
            rules += endRules - endCompile;
            equipment += endEquipment - endRules;

            System.out.println("Compile:   " + (endCompile - startCompile) + "ns");
            System.out.println("Rules:     " + (endRules - endCompile) + "ns");
            System.out.println("Equipment: " + (endEquipment - endRules) + "ns");
        }

        System.out.println("Games:     " + count);
        System.out.println("Compile:   " + compile + "ns");
        System.out.println("Rules:     " + rules + "ns");
        System.out.println("Equipment: " + equipment + "ns");

    }

    public static void main(String[] args) throws IOException {
        CachedMap symbolMapper = new CachedMap();
        testNodeCaches(symbolMapper, 0, 100);
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
