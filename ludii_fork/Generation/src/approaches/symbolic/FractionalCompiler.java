package approaches.symbolic;
import approaches.symbolic.nodes.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    * Parses a description of a game into a tree of GeneratorNodes.
    * TODO Definition support
    * TODO Support too many consistent games
    *
    * @author Alexander Padula
 */
public class FractionalCompiler {
    static final Pattern endOfParameter = Pattern.compile("[ )}]");

    public static class InternalException extends Exception {
        public InternalException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class MissmatchException extends InternalException {
        public MissmatchException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class CompilationException extends InternalException {
        public CompilationException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static class CompilationState {
        public final GenerationNode consistentGame;
        public final List<GenerationNode> remainingOptions;
        public final List<InternalException> exceptions;

        public CompilationState(GenerationNode consistentGame, List<GenerationNode> remainingOptions) {
            this.consistentGame = consistentGame;
            this.remainingOptions = remainingOptions;
            this.exceptions = new ArrayList<>();
        }

        public CompilationState(GenerationNode consistentGame, List<GenerationNode> remainingOptions, List<InternalException> exceptions) {
            this.consistentGame = consistentGame;
            this.remainingOptions = remainingOptions;
            this.exceptions = exceptions;
        }
    }


    public static class CompilationCheckpoint implements Iterable<CompilationState> {
        int longestLength = 0;
        int secondLongestLength = 0;
        public List<CompilationState> longest = new ArrayList<>();
        public List<CompilationState> secondLongest = new ArrayList<>();

        public CompilationCheckpoint() {}
        public CompilationCheckpoint(GenerationNode node) {
            longest.add(new CompilationState(node, List.of()));
            longestLength = node.nodeCount();
        }

        public CompilationCheckpoint(CompilationCheckpoint checkpoint) {
            this.longestLength = checkpoint.longestLength;
            this.secondLongestLength = checkpoint.secondLongestLength;
            this.longest = new ArrayList<>(checkpoint.longest);
            this.secondLongest = new ArrayList<>(checkpoint.secondLongest);
        }

        public void consider(CompilationState state) {
//            String symbolTree = state.consistentGame.root().toString();
//            System.out.println("symbolTree:" + symbolTree);

//            if (longestIndex.get(symbolTree) != null) {
////                System.out.println("excluded:" + symbolTree);
//                longest.set(longestIndex.get(symbolTree), state);
//                return;
//            } else if (secondLongestIndex.get(symbolTree) != null) {
////                System.out.println("excluded:" + symbolTree);
//                secondLongest.set(secondLongestIndex.get(symbolTree), state);
//                return;
//            }

            int length = state.consistentGame.root().nodeCount();

//            System.out.println("con: " + length + " - " + state.consistentGame.root().description());
//            System.out.println("before: " + longestLength + ", " + secondLongestLength);
            if (length > longestLength) {
                secondLongest = longest;
                secondLongestLength = longestLength;
                longest = new ArrayList<>();
                longestLength = length;
            }

            if (length < longestLength && length > secondLongestLength) {
                secondLongest = new ArrayList<>();
                secondLongestLength = length;
            }

//            System.out.println("after: " + longestLength + ", " + secondLongestLength);

            if (length == longestLength) {
                longest.add(state);
            }
            else if (length == secondLongestLength) {
                secondLongest.add(state);
            }
        }

        @Override
        public Iterator<CompilationState> iterator() {
            return new Iterator<>() {
                private final Iterator<CompilationState> longestIterator = longest.iterator();
                private Iterator<CompilationState> currentIterator = longestIterator;

                @Override
                public boolean hasNext() {
                    if (!currentIterator.hasNext() && currentIterator == longestIterator)
                        currentIterator = secondLongest.iterator();

                    return currentIterator.hasNext();
                }

                @Override
                public CompilationState next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return currentIterator.next();
                }
            };
        }
    }

    /*
     * Compiles a description of a game into a tree of GeneratorNodes.
     * @param standardInput The standardized description of the game
     * @param symbolMapper The SymbolMapper to use
     * @return The root of the tree of GeneratorNodes. Crashes if it encounters an exception
     */
    public static GameNode compileComplete(String standardInput, SymbolMap symbolMap) {
        CompilationCheckpoint partialCompilation = compileFraction(standardInput, symbolMap);
        if (!partialCompilation.longest.get(0).exceptions.isEmpty()) {
            partialCompilation.longest.get(0).exceptions.forEach(Throwable::printStackTrace); // TODO display most important errors
            System.out.println(partialCompilation.longest.get(0).consistentGame.root().description());
            throw new RuntimeException("Error compiling " + standardInput);
        }

        if (partialCompilation.longest.size() != 1)
            throw new RuntimeException("Failed to compile:" + standardInput);


        return partialCompilation.longest.get(0).consistentGame.root();
    }

    /*
        * Compiles a description of a game into a tree of GeneratorNodes.
        * @param standardInput The standardized description of the game
        * @param symbolMapper The SymbolMapper to use
        * @return A PartialCompilation containing the consistent games and any exceptions that occurred
     */
    public static CompilationCheckpoint compileFraction(String standardInput, SymbolMap symbolMap) {
        return compileFraction(standardInput, new CompilationCheckpoint(new GameNode()), symbolMap);
    }

    /*
     * Compiles a description of a game into a tree of GeneratorNodes, starting from a tree that has already been
     * partially compiled.
     * @param standardInput The standardized description of the game
     * @param consistentGames The stack of consistent games to start from
     * @param symbolMapper The SymbolMapper to use
     * @return A PartialCompilation containing the consistent games and any exceptions that occurred
     */
    public static CompilationCheckpoint compileFraction(String standardInput, CompilationCheckpoint previousCheckpoint, SymbolMap symbolMap) {

        // The stack of all the consistent games found so far
        Stack<CompilationState> currentStack = new Stack<>();
        HashMap<String, CompilationState> longest = new HashMap<>();
        HashMap<String, CompilationState> secondLongest = new HashMap<>();
        for (CompilationState state: previousCheckpoint.secondLongest) {
            state.consistentGame.stripTrailingPlaceholderNodes();
            String key = state.consistentGame.ancestry() + state.consistentGame.description();
//            if (secondLongest.containsKey(key) && !secondLongest.get(key).consistentGame.root().toString().equals(state.consistentGame.root().toString())) {
//                System.out.println("duplicate second: " + key);
//                System.out.println("old: " + secondLongest.get(key).consistentGame.root().toString());
//                System.out.println("new: " + state.consistentGame.root().toString());
//            }

            secondLongest.put(key, state);
        }
        for (CompilationState state: previousCheckpoint.longest) {
            state.consistentGame.stripTrailingPlaceholderNodes();
            String key = state.consistentGame.ancestry() + state.consistentGame.description();
//            if (longest.containsKey(key) && !longest.get(key).consistentGame.root().toString().equals(state.consistentGame.root().toString())) {
//                System.out.println("duplicate longest: " + key);
//                System.out.println("old: " + longest.get(key).consistentGame.root().toString());
//                System.out.println("new: " + state.consistentGame.root().toString());
//            }

            longest.put(key, state);
        }

        for (CompilationState state: secondLongest.values()) {
            List<GenerationNode> nextOptions = state.consistentGame.nextPossibleParameters(symbolMap, null, true, false);
            currentStack.push(new CompilationState(state.consistentGame, nextOptions));
        }

        for (CompilationState state: longest.values()) {
            List<GenerationNode> nextOptions = state.consistentGame.nextPossibleParameters(symbolMap, null, true, false);
            currentStack.push(new CompilationState(state.consistentGame, nextOptions));
        }


        CompilationCheckpoint nextCheckpoint = new CompilationCheckpoint(previousCheckpoint);


        //(game "Tablan" (players 2) (equipment {(board (rectangle 4 12) {(track "Track1" "0,E,N1,W,N1,E,N1,W" P1 directed:True) (track "Track2" "47,W,S1,E,S1,W,S1,E" P2 directed:True)} use:Vertex) (piece "Stick" Each (if (and (not (is In (from) (sites Next "Home"))) (!= 0 (mapEntry "Throw" (count Pips)))) (or (if (not (= 1 (var))) (move (from (from) if:(if (=
        // If a complete game isn't found, the longest consistent games are returned


        // Loop until a game
        while (true) {

            // If the stack is empty, all paths lead to dead ends
            if (currentStack.isEmpty())
                return nextCheckpoint;

            // Since we are performing a depth-first search, we can just pop the most recent game, option pair
            CompilationState state = currentStack.pop();
//            System.out.println("Considering:" + state.consistentGame.root() + " + " + state.remainingOptions.get(0));

            // If we haven't reached a dead end, remember to consider the next option
            if (state.remainingOptions.size() > 1)
                currentStack.add(new CompilationState(state.consistentGame, state.remainingOptions.subList(1, state.remainingOptions.size()), state.exceptions));

            // Loops through all options and adds them to the stack if they are consistent with the standardInput description
            try {
                GenerationNode newNode = appendOption(state.consistentGame, state.remainingOptions.get(0), standardInput);

                assert !newNode.isComplete() || newNode instanceof GameNode;
                List<GenerationNode> nextOptions = newNode.nextPossibleParameters(symbolMap, null, true, false);
                CompilationState newCompilationState = new CompilationState(newNode, nextOptions);
                currentStack.push(newCompilationState);
                nextCheckpoint.consider(newCompilationState);

                // Successful termination condition
                if (newNode instanceof GameNode && newNode.isComplete()) {
                    return new CompilationCheckpoint(newNode);
                }

            } catch (InternalException e) {
                state.exceptions.add(e);
            }
        }
    }

    /*
     * Appends an option to a game, if it is consistent with the standardInput description.
     * @param node The game to append to
     * @param option The option to append
     * @param standardInput The standardized description
     * @return The new game
     * @throws MissmatchException If the option is not consistent with the standardInput description
     */
    static GenerationNode appendOption(GenerationNode node, GenerationNode option, String standardInput) throws InternalException {
        String currentDescription = node.root().description();

        if (currentDescription.length() >= standardInput.length())
            throw new MissmatchException("Node's description is longer then the input's");

        String trailingDescription = standardInput.substring(currentDescription.length()).strip();

        // Parse primitive options
        if (option instanceof PrimitiveNode primitiveOption) {

            // Manually deal with possible labels
            if (option.symbol().label != null) {
                String prefix = option.symbol().label + ":";
                if (!trailingDescription.startsWith(prefix))
                    throw new MissmatchException("Missing primitive label");
                trailingDescription = trailingDescription.substring(prefix.length()).strip();
            }

            switch (primitiveOption.getType()) {
                case STRING -> {
                    if (trailingDescription.isEmpty() || trailingDescription.charAt(0) != '"')
                        throw new MissmatchException("Missing the opening quote");

                    int end = trailingDescription.indexOf('"', 1);
                    if (end == -1)
                        throw new MissmatchException("Missing the terminal quote");

                    primitiveOption.setValue(trailingDescription.substring(1, end));
                }
                case INT, DIM, FLOAT -> {
                    Matcher match = endOfParameter.matcher(trailingDescription);

                    int end = trailingDescription.length();
                    if (match.find())
                        end = match.start();
//                        throw new MissmatchException("Can't find closing end of parameter");

                    try {
                        primitiveOption.setUnparsedValue(trailingDescription.substring(0, end));
                    } catch (NumberFormatException e) {
                        throw new MissmatchException("Not a number");
                    }
                }
                case BOOLEAN -> { // TODO maybe check if after the True/False there is a space or bracket
                    if (trailingDescription.startsWith("True")) {
                        primitiveOption.setUnparsedValue("True");
                    } else if (trailingDescription.startsWith("False")) {
                        primitiveOption.setUnparsedValue("False");
                    } else {
                        throw new MissmatchException("Not a boolean");
                    }
                }
            }

            GenerationNode newNode = node.copyUp();
            option.setParent(newNode);
            newNode.addParameter(option);

            if (!standardInput.startsWith(newNode.root().description()))
                throw new MissmatchException("Now node does not match the input"); // TODO CHECK

            return newNode;
        }

        // Parse non-primitive options
        else {
            // option.description accounts for the label already
            if (!(option instanceof PlaceholderNode) && !(option instanceof EndOfClauseNode)) {

                if (!trailingDescription.startsWith(option.description()))
                    throw new MissmatchException("Wrong class " + option.description());

                if (trailingDescription.length() > option.description().length()) {
                    char nextChar = trailingDescription.charAt(option.description().length());
                    char currentChar = trailingDescription.charAt(option.description().length() - 1);
                    boolean isEnd = nextChar == ' ' || nextChar == ')' || nextChar == '}' || nextChar == '(' || nextChar == '{' || currentChar == '(' || currentChar == '{';

                    if (!isEnd)
                        throw new MissmatchException("Wrong class 2"); // TODO why not use end of parameter like before?
                }

            }

            if (option instanceof EndOfClauseNode && !trailingDescription.isEmpty()) {
                char currentChar = trailingDescription.charAt(0);
                if (currentChar != ')' && currentChar != '}')
                    throw new MissmatchException("Not a closing bracket"); // TODO I could probably handle this with the trailingDescription
            }

            GenerationNode nodeCopy = node.copyUp();
            option.setParent(nodeCopy);
            nodeCopy.addParameter(option);

            if (!standardInput.startsWith(nodeCopy.root().description())) // If slow, remove for complete games
                throw new MissmatchException("Now node does not match the input"); // TODO CHECK

            if (!option.isComplete())
                return option;

            if (nodeCopy.isComplete()) {
                assert nodeCopy.isRecursivelyComplete();

                if (nodeCopy instanceof GameNode)
                    return nodeCopy;

                try {
                    nodeCopy.instantiate();
                } catch (Exception e) {
                    throw new CompilationException(e.getMessage());
                }

                return nodeCopy.parent();
            }

            return nodeCopy;
        }
    }

    /*
     * Standardizes a description such that any description that is equivalent to another description has the same
     * standardized form. This is done by removing all unnecessary whitespace, replacing constants with their values,
     * and standardizing numeric values.
     * @param str The description to standardize
     * @return The standardized description
     */
    public static String standardize(String str) {
        str = str.stripLeading();
        str = str.replaceAll("\\s+", " ");
        str = str.replace("( ", "(");
        str = str.replace(" )", ")");
        str = str.replace("{ ", "{");
        str = str.replace(" }", "}");
        str = str.replaceAll("\\s*:\\s*", ":"); // (forEach of : (... -> (forEach of:(...
        str = str.replaceAll("(?<![\\d])\\.(\\d)", "0.$1"); // .5 -> 0.5    //TODO this is not correct 1 is not 1.0
        str = str.replaceAll("(\\d+\\.\\d*?)0+\\b", "$1"); // 0.50 -> 0.5
        str = str.replaceAll("(\\d)+\\.([^0-9])", "$1$2"); // 0. -> 0

        // Aliases
        str = str.replace("(* ", "(mul ");
        str = str.replace("(% ", "(mod ");
        str = str.replace("(/ ", "(div ");
        str = str.replace("(+ ", "(add ");
        str = str.replace("(- ", "(sub ");
        str = str.replace("(^ ", "(pow ");
        str = str.replace("(baseDimFunction ", "(dim ");
        str = str.replace("(!= ", "(notEqual ");
        str = str.replace("(>= ", "(ge ");
        str = str.replace("(< ", "(lt ");
        str = str.replace("(<= ", "(le ");
        str = str.replace("(= ", "(equals ");
        str = str.replace("(> ", "(gt ");


        // Constants
        str = str.replaceAll("([ ({:])Off([ )}])", "$1-1$2");
        str = str.replaceAll("([ ({:])End([ )}])", "$1-2$2");
        str = str.replaceAll("([ ({:])Undefined([ )}])", "$1-1$2");
        str = str.replace("Infinity", "1000000000");

        return str;
    }

    /*
     * Undes the standardization of a description. Given the compilable portion of the description in standard form,
     *  it recovers the compilable portion of the description in its original form.
     * It does this py converting the standard form into a regular expression and then using that regular expression to
     *  match the equivalent section in the original description.
     * @param original The original description
     * @param standard A standardized substring of the description
     */
    public static String destandardize(String original, String standard) {
        String pattern = standard;

        pattern = pattern.replace(" ", "\\s+");
        pattern = pattern.replace("(", "\\(\\s*");
        pattern = pattern.replace(")", "\\s*\\)");
        pattern = pattern.replace("{", "\\{\\s*");
        pattern = pattern.replace("}", "\\s*\\}");
        pattern = pattern.replace(":", "\\s*:\\s*");
        pattern = pattern.replaceAll("\\d+\\.?\\d*", "\\\\d+\\\\.?\\\\d*");
        pattern = pattern.replaceAll("[ ({:]-1[ )}]", "[ ({](Off)|(-1)[ )}]");
        pattern = pattern.replaceAll("[ ({:]-2[ )}]", "[ ({](End)|(-2)[ )}]");
        pattern = pattern.replaceAll("[ ({:]-1[ )}]", "[ ({](Undefined)|(-1)[ )}]");
        pattern = pattern.replace("1000000000", "[ ({:](Infinity)|(1000000000)[ )}]");

        pattern = pattern.replace("\\(\\s*mul\\s+", "\\(\\s*(mul|\\*)\\s+");
        pattern = pattern.replace("\\(\\s*mod\\s+", "\\(\\s*(mod|%)\\s+");
        pattern = pattern.replace("\\(\\s*div\\s+", "\\(\\s*(div|/)\\s+");
        pattern = pattern.replace("\\(\\s*add\\s+", "\\(\\s*(add|\\+)\\s+");
        pattern = pattern.replace("\\(\\s*sub\\s+", "\\(\\s*(sub|-)\\s+");
        pattern = pattern.replace("\\(\\s*pow\\s+", "\\(\\s*(pow|\\^)\\s+");
        pattern = pattern.replace("\\(\\s*dim\\s+", "\\(\\s*(baseDimFunction|dim)\\s+");
        pattern = pattern.replace("\\(\\s*notEqual\\s+", "\\(\\s*(notEqual|!=)\\s+");
        pattern = pattern.replace("\\(\\s*ge\\s+", "\\(\\s*(ge|>=)\\s+");
        pattern = pattern.replace("\\(\\s*lt\\s+", "\\(\\s*(lt|<)\\s+");
        pattern = pattern.replace("\\(\\s*le\\s+", "\\(\\s*(le|<=)\\s+");
        pattern = pattern.replace("\\(\\s*equals\\s+", "\\(\\s*(equals|=)\\s+");
        pattern = pattern.replace("\\(\\s*gt\\s+", "\\(\\s*(gt|>)\\s+");

//        System.out.println("Pattern: " + pattern);

        pattern = "\\s*" + pattern;

        Matcher matcher = Pattern.compile(pattern).matcher(original);



        if (matcher.lookingAt())
            return original.substring(0, matcher.end());

        return "";
    }

}
