package app.display.dialogs.visual_editor.recs.codecompletion.domain.model;

import app.display.dialogs.visual_editor.recs.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author filreh
 */
public class Preprocessing {
    private static final boolean DEBUG = false;
    public static final String NUMBER_WILDCARD = "number";
    public static final String INTEGER_WILDCARD = "int";
    public static final String FLOAT_WILDCARD = "float";
    public static final String BOOLEAN_WILDCARD = "boolean";
    public static final String STRING_WILDCARD = "string";
    public static final String OPTION_WILDCARD = "option";

    public static final String COMPLETION_WILDCARD = "[#]";
    public final static boolean GENERIC_STRINGS = true;
    public final static boolean GENERIC_OPTIONS = true;

    /**
     * This method applies all the necessary steps of preprocessing.
     * @param gameDescription
     */
    @SuppressWarnings("all")
    public static String preprocess(String gameDescription)
    {
        if(DEBUG)System.out.println("Raw:"+gameDescription);
        gameDescription = removeMetadata(gameDescription);
        if(DEBUG)System.out.println("Removed Metadata:"+gameDescription);
        gameDescription = removeComments(gameDescription);
        if(DEBUG)System.out.println("Removed Comments:"+gameDescription);
        gameDescription = removeWhitespaces(gameDescription);
        if(DEBUG)System.out.println("Removed Withespaces:"+gameDescription);
        gameDescription = genericValues(gameDescription);
        if(DEBUG)System.out.println("Replaced specific values with generic placeholders:"+gameDescription);
        return gameDescription;
    }

    /**
     * This method removes the metadata from the game description
     *
     * @param gameDescription
     */
    @SuppressWarnings("all")
    public static String removeMetadata(String gameDescription)
    {
        String metadataLudeme = "(metadata";
        if(gameDescription.contains(metadataLudeme)) {
            int startMetadata = gameDescription.lastIndexOf(metadataLudeme);
            gameDescription = gameDescription.substring(0,startMetadata);
        }
        return gameDescription;
    }

    /**
     * This method removes all comments from the game description and ravels the description into one line.
     *
     * @param gameDescription
     */
    @SuppressWarnings("all")
    public static String removeComments(String gameDescription)
    {
        String commentLudeme = "//";
        String[] lines = gameDescription.split("\n");
        String noComments = "";
        for(int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            if(line.contains(commentLudeme))
            {
                int commentLocation = line.lastIndexOf(commentLudeme);
                line = line.substring(0,commentLocation);
            }
            noComments += line;
        }
        gameDescription = noComments;
        return gameDescription;
    }

    /**
     * This method removes all tabs and unnecessary whitespaces from the game description while adding
     * needed ones.
     *
     * @param gameDescription
     * @return
     */
    @SuppressWarnings("all")
    public static  String removeWhitespaces(String gameDescription) {
        char[] chars = gameDescription.toCharArray();
        gameDescription = "";
        char prevChar = '1';

        for(int i = 0; i < chars.length; i++) {
            char curChar = chars[i];
            // so it does not go out of bounds
            char nextChar = i < (chars.length - 1) ? chars[(i + 1)] : chars[i];
            if (prevChar == ' ' && curChar == ' ') {
                //do not add the char to the game description
            } else if(nextChar == ')' || nextChar == '}') {
                //add a space before a closing )
                gameDescription += curChar+" ";
            } else if(curChar == ')' && (nextChar != ')' && nextChar != ' ')) {
                //add a space before a closing )
                gameDescription += curChar+" ";
            } else if(curChar == '{' && nextChar != ' ') {
                //add a space before a closing )
                gameDescription += curChar+" ";
            } else {
                gameDescription += curChar;
            }
            prevChar = curChar;
        }

        chars = gameDescription.toCharArray();
        gameDescription = "";
        prevChar = '1';
        // again remove any double spaces
        for(int i = 0; i < chars.length; i++) {
            char curChar = chars[i];
            // so it does not go out of bounds
            char nextChar = i < (chars.length - 1) ? chars[(i + 1)] : chars[i];
            if ((prevChar == ' ' && curChar == ' ') || curChar == '\t' || curChar == '\n') {
                //do not add the char to the game description
            } else {
                gameDescription += curChar;
            }
            prevChar = curChar;
        }

        return gameDescription;
    }

    /**
     * This method replaces specific values with generic wildcards
     * Strings will stay in as they are of importance to the game
     * @param gameDescription
     * @return
     */
    @SuppressWarnings("all")
    public static String genericValues(String gameDescription) {

        // REPLACE NUMBERS
        char[] chars = gameDescription.toCharArray();
        gameDescription = "";
        char prevChar = '?';

        boolean foundNumber = false;
        for(int i = 0; i < chars.length; i++) {
            char curChar = chars[i];
            char nextChar = i < (chars.length - 1) ? chars[(i + 1)] : chars[i];
            switch (curChar) {
                case '-':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '.':
                    if(!foundNumber) {
                        if(prevChar == ' ' || prevChar == ':') {
                            //found a number
                            foundNumber = true;
                            // do not add
                        } else {
                            // this digit was part of a word
                            gameDescription += curChar;
                        }
                    }
                    if(foundNumber) {
                        if(nextChar == ' ') {
                            gameDescription += NUMBER_WILDCARD;
                            foundNumber = false;
                        }
                    }
                    break;
                default:
                    gameDescription += curChar;
                    break;
            }

            prevChar = curChar;
        }

        gameDescription = gameDescription.replaceAll("True",BOOLEAN_WILDCARD);
        gameDescription = gameDescription.replaceAll("False",BOOLEAN_WILDCARD);

        String[] words = gameDescription.split(" ");

        //merge strings in code back together
        List<String> wordsList = new ArrayList<>();
        boolean foundStringInCode = false;
        String stringInCode = "";
        for(int i = 0; i < words.length; i++) {
            String curWord = words[i];
            if(foundStringInCode) {
                if(curWord.endsWith("\"")) {
                    //end of the string in code
                    if(GENERIC_STRINGS) {
                        wordsList.add(STRING_WILDCARD);
                    } else {
                        stringInCode += curWord;
                        wordsList.add(stringInCode);
                    }
                    //resetting helper variables
                    foundStringInCode = false;
                    stringInCode = "";
                } else {
                    //middle of the string in code
                    stringInCode += curWord + " ";
                }
            } else if(curWord.startsWith("\"") && !curWord.endsWith("\"")) {
                //beginning of the string in code
                foundStringInCode = true;
                stringInCode += curWord + " ";
            } else {
                if(GENERIC_STRINGS && curWord.startsWith("\"") && curWord.endsWith("\"")) {
                    //string in code
                    wordsList.add(STRING_WILDCARD);
                } else if(GENERIC_OPTIONS && (curWord.startsWith("<") || curWord.endsWith(">"))){
                    wordsList.add(OPTION_WILDCARD);
                } else if(StringUtils.equals(curWord,"*")) {
                    //do nothing
                } else {
                    //just a normal word
                    wordsList.add(curWord);
                }
            }
        }
        //end merging strings

        if(wordsList.isEmpty()){
            gameDescription = "";
        } else {
            //piece the game description back together
            gameDescription = wordsList.get(0);
            if(wordsList.size() > 1) {
                for (int i = 1; i < wordsList.size(); i++) {
                    gameDescription += " " + wordsList.get(i);
                }
            }
        }


        return gameDescription;
    }

    @SuppressWarnings("all")
    public static String preprocessBegunWord(String begunWord) {
        String cleanBegunWord = "";
        // Watch out for numbers: only contains numbers
        try {
            //need to find out whether it is a float or an int
            if(begunWord.contains(".")) {
                cleanBegunWord = FLOAT_WILDCARD;
            } else {
                cleanBegunWord = INTEGER_WILDCARD;
            }
        } catch (NumberFormatException e) {
            //part of the logic: if the begunWord does not parse, then it is not a number
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Watch out for strings: begins with '"'
        if(begunWord.startsWith("\"")) {
            begunWord = STRING_WILDCARD;
        }
        //Watch out for options: begins with '<'
        if(begunWord.startsWith("<")) {
            begunWord = OPTION_WILDCARD;
        }
        return cleanBegunWord;
    }
}
