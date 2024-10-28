package approaches.symbolic;

import main.StringRoutines;
import main.grammar.Description;
import main.grammar.ParseItem;
import main.grammar.Report;
import main.options.Option;
import main.options.OptionCategory;
import main.options.Ruleset;
import main.options.UserSelections;
import parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static approaches.symbolic.FractionalCompiler.endOfParameter;
import static approaches.symbolic.FractionalCompiler.standardize;

public class Dataset {
    static Set<String> skip = Set.of("Kriegspiel (Chess).lud", "Throngs.lud", "Tai Shogi.lud", "Taikyoku Shogi.lud", "Yonin Seireigi.lud", "Yonin Shogi.lud"); // "To Kinegi tou Lagou.lud"
    static Set<String> validation = Set.of("Kanguruh.lud", "Dara.lud", "Tennessee Waltz.lud", "Ploy.lud", "Quinze Tablas.lud", "Upper Hand.lud", "Had.lud", "Seesaw Draughts.lud", "Rwandan Alignment Game.lud", "All Queens Chess.lud", "Lights Out.lud", "Banqi.lud", "Groups.lud", "Siga (Sri Lanka).lud", "Santaraj.lud", "Starchess.lud", "Paintbucket.lud", "HexGo.lud", "Toads and Frogs.lud", "Duqurjin.lud", "Damenspiel.lud", "Rabbit Warrens.lud", "Tshuba.lud", "Cram.lud", "Chatur.lud", "Okwe (Nigeria).lud", "Gonnect.lud", "Tridoku.lud", "Hat Diviyan Keliya.lud", "L Game.lud", "Owela (Benguela).lud", "Ho-Bag Gonu.lud", "Yavalanchor.lud", "Dubblets.lud", "Dame.lud", "Macheng.lud", "Orissa Tiger Game (Four Tigers).lud", "Shataranja.lud", "Frangieh.lud", "Quick Chess.lud", "Dum Blas.lud", "Spava.lud", "Peralikatuma.lud", "Yote.lud", "Qirkat.lud", "Pong Hau K'i.lud", "Kioz.lud", "Diris.lud", "Anti-Knight Sudoku.lud", "Tara.lud", "RootZone.lud", "Spartan Chess.lud", "Latin Square.lud", "Korkserschach.lud", "Wali.lud", "Chaturanga (al-Adli).lud", "Susan.lud", "Bagh Batti.lud", "Blue Nile.lud", "Parsi Chess.lud", "Sig wa Duqqan (Houmt Taourit).lud", "Tavlej.lud", "Sig (Western Sahara).lud", "Madelinette.lud", "Sumi Naga Game (Hunt).lud", "Capture the Queen.lud", "Janes Soppi (Symmetrical).lud", "Alquerque.lud", "Gurgaldaj.lud", "Msuwa wa Kunja.lud", "Unnee Tugalluulax.lud", "Shatranj al-Husun.lud", "Monty Hall Problem.lud", "Ishighan.lud", "Damas.lud", "Deka.lud", "Dama (Kenya).lud", "HexTrike.lud", "Chomp.lud", "Poprad Game.lud", "Infuse.lud", "Branching Coral.lud", "Terhuchu (Small).lud", "Caturvimsatikosthakatmiki Krida.lud", "Driesticken.lud", "Mu Torere.lud", "Marelle Quadruple.lud", "Mini Hexchess.lud", "Komikan.lud", "Shatranj (Algeria).lud", "Claim Jumpers.lud", "Ring.lud", "Mysore Tiger Game.lud", "A Simple Game.lud", "Xonin Shatar (Complex).lud", "Alquerque de Doze (Portugal).lud", "Six Insect Game.lud", "Dama (Alquerque).lud", "Sudoku.lud", "Hackenbush.lud", "Diagonal Hex.lud", "Cannon.lud", "Mogul Putt'han.lud", "Chase.lud", "Leyla Gobale (Somaliland).lud", "Ratio.lud", "Tsukkalavde.lud", "Schuster.lud", "Tab.lud", "Seesaw.lud", "Oriath.lud", "Sudoku Mine.lud", "Coyote.lud", "Xonin Shatar (Simple).lud", "Sig (Mauritania).lud", "Awari.lud", "Dragonchess.lud", "Camelot.lud", "Hund efter Hare (Thy).lud", "Dama (Philippines).lud", "Bagha Guti.lud", "Radran.lud", "Maze.lud", "Baralie.lud", "Tsoro (Reentered Captures).lud", "Yavalax.lud", "Pareia de Entrada.lud", "Main Dam.lud", "Chess (Siberia).lud", "Game of Solomon.lud", "Chaturanga (Kridakausalya 14x14).lud", "TacTix.lud", "Brandub.lud", "Tant Fant.lud", "Tsoro Yemutatu (Triangle).lud", "Patok.lud", "Taptana.lud", "Murus Gallicus.lud", "Greater Loss.lud", "Motiq.lud", "Moruba.lud", "Bara Guti (Bihar).lud", "Epelle.lud", "Shogun.lud", "Knossos Game.lud", "Theseus and the Minotaur.lud", "Uxrijn Ever.lud", "Shatranj at-Tamma.lud", "Parry.lud", "Astralesce and Constellation.lud", "Shogi Puzzle.lud", "Residuel.lud", "Pente.lud", "Kulaochal.lud", "Uril.lud", "Zola.lud", "Royal Game of Ur.lud", "Quantum Leap.lud", "Puhulmutu.lud", "Davxar Zirge (Type 1).lud", "Queah Game.lud", "Nin Adnai Kit Adnat.lud", "Guerrilla Checkers.lud", "Goldilocks Stones.lud", "Fibonacci Nim.lud", "Hoshi.lud", "Huli-Mane Ata.lud", "Ashanti Alignment Game.lud", "Koti Keliya.lud", "Currierspiel.lud", "Juroku Musashi.lud", "Breakthru.lud", "Tasholiwe.lud", "Futoshiki.lud", "Hindustani Chess.lud", "Selbia.lud", "Game of Dwarfs.lud", "Atari Go.lud", "Janes Soppi.lud", "Short Assize.lud", "Mefuvha.lud", "Aj Sakakil.lud", "Boseog Gonu.lud", "Shui Yen Ho-Shang.lud", "Official Football Chess.lud", "Ethiopian Capture Game.lud", "Selayar Game.lud", "Whyo.lud", "Baqura.lud", "Adjiboto.lud", "Welschschach.lud", "Koro.lud", "Mylna.lud", "Skirmish (GDL).lud", "Tides.lud", "Bao Kiswahili (DR Congo).lud", "Lau Kata Kati.lud", "Lange Puff.lud", "Andantino.lud", "Alea Evangelii.lud", "Mlabalaba.lud", "Dala.lud", "Safe Passage.lud", "Awagagae.lud", "Terhuchu.lud", "Petol.lud", "Horde Chess.lud", "La Yagua.lud", "Fenix.lud", "Chong (Sakhalin).lud", "Omega.lud", "Morabaraba.lud", "Ijil Buga.lud", "Crossway.lud", "Kiz Tavlasi.lud", "Chinese Checkers.lud", "Snailtrail.lud", "Backgammon.lud", "Tsoro (Additional Capture).lud", "La Chascona.lud", "Sabou'iyya.lud", "Buffa de Baldrac.lud", "Pasakakrida (Type 5).lud", "Pulijudamu.lud", "Wagner.lud", "Zuz Mel (7x7).lud", "Pasakakrida (Type 3).lud", "Yup'ik Checkers.lud", "Chuka.lud", "Msuwa.lud", "Saxun.lud", "Namudilakunze.lud", "Siryu (Race).lud", "Lisolo.lud", "Ketch-Dolt.lud", "Overflow.lud", "Beirut Chess.lud", "Smasandyutakankarikrida (Allahabad).lud", "Tapata.lud", "Ufuba wa Hulana.lud", "Reversi.lud", "Buttons And Lights.lud", "Hawalis.lud", "Dig Dig.lud", "Intotoi.lud", "The Pawn Game.lud", "Keryo-Pente.lud", "Adzua.lud", "Nard.lud", "Konane.lud", "Lian Qi (Bohai).lud", "Kensington.lud", "Tauru.lud", "Zuz Mel (5x5).lud", "Chameleon.lud", "Choko.lud", "Ngre E E.lud", "Bamboo.lud", "Knight's Tour.lud", "Svensk Bradspel.lud", "Sokkattan.lud", "Eleven-Fang.lud", "Li'b al-Sidr.lud", "Level Chess.lud", "Pachgarhwa.lud", "Shono.lud", "Castello.lud", "Wouri.lud", "Cheng Fang Cheng Long.lud", "Schachzabel.lud", "Ssang-Ryouk.lud", "I Pere.lud", "Ikh Buga.lud", "Janggi.lud", "Mangola.lud", "Tsoro (Baia).lud", "Sudoku X.lud", "Xiangqi.lud", "Challis Ghutia.lud", "English Draughts.lud", "Mbenga Alignment Game.lud", "Ashta-kashte.lud", "Mandinka Game.lud", "Windflowers.lud", "Dama (Italy).lud", "Tumbleweed.lud", "Boukerourou.lud", "Resolve.lud", "Nerenchi Keliya.lud", "Kawasukuts.lud", "Tank Tactics.lud", "Sfenj.lud", "Gabata (Oromo).lud", "Ataxx.lud", "Tayam Sonalu.lud", "Luuth.lud", "Knightthrough.lud", "Kubuguza.lud", "Medio Emperador.lud", "Omny.lud");
    static String gamesRoot = "./Common/res/lud/good";
    static String nestedRoot = "./expanded";
    static Path nestedPath = Paths.get(nestedRoot);

    static String flatTrainingFile = "./training_expanded.txt";
    static String flatValidationFile = "./validation_expanded.txt";

    List<Path> paths = new ArrayList<>();
    List<Description> descriptions = new ArrayList<>();
    List<UserSelections> userSelections = new ArrayList<>();

    public Dataset(int limit) throws IOException {
        List<Path> unfiltered_paths = Files.walk(Paths.get(gamesRoot)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".lud")).sorted().limit(limit).toList();
        int count = 0;
        for (Path path: unfiltered_paths) {
            String gameStr = Files.readString(path);
            count++;

            System.out.println("\nLoading " + path.getFileName() + " (" + (count) + " of " + unfiltered_paths.size() + " games)");

            if (gameStr.contains("match")) {
                System.out.println("Skipping match " + path.getFileName());
                continue;
            }

            if (skip.contains(path.getFileName().toString())) {
                System.out.println("Skipping skip " + path.getFileName());
                continue;
            }

            Description description = new Description(gameStr);
            final UserSelections userSelections = new UserSelections(new ArrayList<>());
            final Report report = new Report();
            Parser.expandAndParse(description, userSelections, report, true, false);
            String expandedDescription = standardize(description.expanded());

            if (expandedDescription.length() > 10000) {
                System.out.println("Skipping too long " + path.getFileName());
                continue;
            }

            paths.add(path);
            descriptions.add(description);
            this.userSelections.add(userSelections);
        }
    }

    void buildNestedDataset() throws IOException {

        // Create the output root directory if it doesn't exist
        if (!Files.exists(nestedPath)) {
            Files.createDirectory(nestedPath);
        }

        for (int i = 0; i < paths.size(); i++) {
            StringBuilder entree = new StringBuilder();
            Path path = paths.get(i);
            Description description = descriptions.get(i);
            UserSelections userSelections = this.userSelections.get(i);

            if (description.metadata() == null) {
                System.out.println("Skipping " + path.getFileName() + " because it has no metadata");
                continue;
            }

            String rules = getLudemeContent(description.metadata(), "(rules");

            if (rules.isBlank() && !description.rulesets().isEmpty()) {
                Ruleset defaultRuleset = description.rulesets().get(0);
                for (Ruleset ruleset: description.rulesets()) {
                    if (ruleset.priority() > defaultRuleset.priority())
                        defaultRuleset = ruleset;
                }
                rules = getLudemeContent(description.raw(), "(useFor \"" + defaultRuleset.heading() + "\" (rules");
            }


            if (rules.isBlank()) {
                System.out.println("Skipping " + path.getFileName() + " because it has no rules");
                System.out.println("Description:" + getLudemeContent(description.metadata(), "(description"));
                continue;
            }

            if (rules.length() < 60) {
                System.out.println("Skipping " + path.getFileName() + " because the rules are too short");
                System.out.println("Rules: " + rules);
                continue;
            }

            List<List<Option>> sortedOptions = new ArrayList<>();
            for (OptionCategory category: description.gameOptions().categories()) {
                List<Option> sorted = new ArrayList<>(category.options());
                if (!sorted.isEmpty()) {
                    sorted.sort(Comparator.comparingInt(o -> -o.priority()));
                    sortedOptions.add(sorted);
                }
            }

            entree.append(rules.replaceAll("\\s+", " ").strip());

            if (!sortedOptions.isEmpty()) {
                for (List<Option> option: sortedOptions) {
                    entree.append(" ").append(option.get(0).description().strip());
                }
            }

            entree.append("\n").append(standardize(description.expanded())).append("\n");



            Path relativePath = Paths.get(gamesRoot).relativize(path.getParent());
            Path outputPathDirectory = Paths.get(nestedRoot, relativePath.toString());

            // Create directories as needed
            if (!Files.exists(outputPathDirectory)) {
                Files.createDirectories(outputPathDirectory);
            }

            Path outputPath = outputPathDirectory.resolve(path.getFileName().toString());
            Files.writeString(outputPath, entree.toString().replace("\\", ""));
        }
    }


    // Instruction Dataset
    void buildFlatDataset(int optionDuplicates) throws IOException {
        StringBuilder training_dataset = new StringBuilder();
        StringBuilder validation_dataset = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            StringBuilder entree = new StringBuilder();
            Path path = paths.get(i);
            Description description = descriptions.get(i);
            UserSelections userSelections = this.userSelections.get(i);

            if (description.metadata() == null) {
                System.out.println("Skipping " + path.getFileName() + " because it has no metadata");
                continue;
            }

            String rules = getLudemeContent(description.metadata(), "(rules");

            if (rules.isBlank() && !description.rulesets().isEmpty()) {
                Ruleset defaultRuleset = description.rulesets().get(0);
                for (Ruleset ruleset: description.rulesets()) {
                    if (ruleset.priority() > defaultRuleset.priority())
                        defaultRuleset = ruleset;
                }
                rules = getLudemeContent(description.raw(), "(useFor \"" + defaultRuleset.heading() + "\" (rules");
            }


            if (rules.isBlank()) {
                System.out.println("Skipping " + path.getFileName() + " because it has no rules");
                System.out.println("Description:" + getLudemeContent(description.metadata(), "(description"));
                continue;
            }

            if (rules.length() < 60) {
                System.out.println("Skipping " + path.getFileName() + " because the rules are too short");
                System.out.println("Rules: " + rules);
                continue;
            }

            List<List<Option>> sortedOptions = new ArrayList<>();
            for (OptionCategory category: description.gameOptions().categories()) {
                List<Option> sorted = new ArrayList<>(category.options());
                if (!sorted.isEmpty()) {
                    sorted.sort(Comparator.comparingInt(o -> -o.priority()));
                    sortedOptions.add(sorted);
                }
            }

            for (int j = 0; j < optionDuplicates; j++) {
                int finalJ = j;

                if (j > 0 && !sortedOptions.isEmpty()) {
//                    System.out.println("Selections: " + sortedOptions.stream().map(option -> String.join("/", option.get(finalJ % option.size()).menuHeadings())).toList());
                    userSelections = new UserSelections(sortedOptions.stream().map(option -> String.join("/", option.get(finalJ % option.size()).menuHeadings())).toList());
                    description = new Description(description.raw());
                    Parser.expandAndParse(description, userSelections, new Report(), true, false);
                }

                entree.append(rules.replaceAll("\\s+", " ").strip());

//                entree.append(String.join(" ", sortedOptions.stream().map(option -> option.get(finalJ % option.size()).description()).toList()));
                if (!sortedOptions.isEmpty()) {
                    for (List<Option> option: sortedOptions) {
//                        entree.append(option.get(j % option.size()).description().replaceAll("\\s+", " ").strip()).append(" ");
                        entree.append(" ").append(option.get(j % option.size()).description().strip());
                    }
                }

                entree.append("\n").append(standardize(description.expanded())).append("\n");
            }

            String entreeString = entree.toString().replace("\\", "");
            if (validation.contains(path.getFileName().toString()))
                validation_dataset.append(entreeString);
            else
                training_dataset.append(entreeString);
        }

        Files.writeString(Paths.get(flatTrainingFile), training_dataset);
        Files.writeString(Paths.get(flatValidationFile), validation_dataset);
    }

    String getLudemeContent(String parent, String ludeme) {
        int c = parent.indexOf(ludeme);
        if (c == -1)
            return "";

        // Find matching closing bracket
        int closing = StringRoutines.matchingBracketAt(parent, c);
        if (closing == -1)
            return "";

        // Check if there is a duplicate ludeme in the content
        if (parent.indexOf(ludeme, closing) != -1)
            return "";

        String quoted = parent.substring(c + ludeme.length(), closing).strip();
        return quoted.substring(1, quoted.length() - 1);
    }

    ParseItem findItem(ParseItem item, List<String> path) {
        for (ParseItem child: item.arguments()) {
            System.out.println("name: " + child.token().name());
            if (child.token().name().equals(path.get(0))) {
                if (path.size() == 1) {
                    return child;
                } else {
                    return findItem(child, path.subList(1, path.size()));
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        new Dataset(10000).buildNestedDataset();
//        new Dataset(1000).buildFlatDataset(3);
//        List<Path> paths = new ArrayList<>(new Dataset(2000).paths);
//        Collections.shuffle(paths);
//        System.out.println(paths.stream().limit(300).map(p -> '"' + p.getFileName().toString() + '"').toList());
    }
}
