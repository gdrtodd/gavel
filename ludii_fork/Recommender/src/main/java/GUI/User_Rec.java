package GUI;

import FileManager.Csv_handler;

import java.util.ArrayList;
import java.util.Arrays;

public class User_Rec {
    public float[] rating_vector = new float [1102];
    public ArrayList <Integer> indices = new ArrayList<>();
    private static final int [] indices_arr = new int [] {13, 26, 44, 82, 526, 14, 864, 866, 868, 865, 881, 892, 197, 877, 771, 822, 103, 104, 120, 43, 362, 953, 39, 46, 66, 107, 148, 257};
    private final static int chess_ind = 13;
    private final static int checkers_ind = 26;
    private final static int backgammon_ind = 44;
    private final static int sudoku_ind = 82;
    private final static int [] multiple_pieces_inds = new int [] {526, 14, 864};  // Kyoto Shogi, Double Chess, Tsatsarandi (cluster 1)
    private final static int [] one_piece_inds = new int [] {866, 868, 865}; // Six Insect Game, BlooGo, Dig Dig (cluster 2)
    private final static int [] mancala_inds = new int [] {881, 892, 197}; //Raja Pasu Mandiri, Uthi, Chisolo (cluster 3)
    private final static int [] puzzle_inds = new int [] {877, 771, 822};// Jiu Zi Xian Qi, Chuka, Shisenshō (puzzle concept)
    private final static int [] chance_inds = new int [] {103, 104, 120}; // Pachisi, Nard, Kioz (chance concept)
    private final static int [] hidden_element_inds = new int [] {43, 362, 953}; // Sneakthrough, L'attaque, Geister (hidden element concept)
    private final static int [] multiplayer_inds = new int [] {39, 46, 66}; // Chinese Checkers, Gyan Chaupar, Tic-Tac-Mo (TwoPlayer and MultiPlayer concept)
    private final static int [] cooperation_inds = new int [] {107, 148, 257}; // Awithlaknakwe, Chaupar, Los Palos (cooperation concept)

    public User_Rec(){
        Arrays.fill(rating_vector, -1.0f);
    }
    public static int[] get_indices_arr(){
        return indices_arr;
    }
    public void set_chess_rating(float rating){
        rating_vector[chess_ind] = rating;
    }
    public void set_checkers_rating(float rating){
        rating_vector[checkers_ind] = rating;
    }
    public void set_backgammon_rating(float rating){
        rating_vector[backgammon_ind] = rating;
    }
    public void set_sudoku_rating(float rating){
        rating_vector[sudoku_ind] = rating;
    }
    public void set_multiple_pieces_ratings(float rating){
        final float average_multiple_pieces_rating = 7.159033260533334f;
        for (int multiplePiecesInd : multiple_pieces_inds) {
            rating_vector[multiplePiecesInd] = average_multiple_pieces_rating + rating;
        }
    }
    public void set_one_piece_ratings(float rating){
        for (int onePieceInd : one_piece_inds) {
            rating_vector[onePieceInd] = rating;
        }

    }public void set_mancala_ratings(float rating){
        for (int mancalaInd : mancala_inds) {
            rating_vector[mancalaInd] = rating;
        }
    }public void set_puzzle_ratings(float rating){
        for (int puzzleInds : puzzle_inds) {
            rating_vector[puzzleInds] = rating;
        }
    }public void set_chance_ratings(float rating){
        for (int chanceInd : chance_inds) {
            rating_vector[chanceInd] = rating;
        }
    }
    public void set_hidden_element_ratings(float rating){
        for (int hiddenInd : hidden_element_inds) {
            rating_vector[hiddenInd] = rating;
        }
    }public void set_multiplayer_ratings(float rating){
        for (int multiplayerInd : multiplayer_inds) {
            rating_vector[multiplayerInd] = rating;
        }
    }
    public void set_coordination_ratings(float rating){
        for (int coordinationInd : cooperation_inds) {
            rating_vector[coordinationInd] = rating;
        }
    }

    public static void main(String[] args) {
        String [] games = Csv_handler.parse_games();
        for(int i=0; i<indices_arr.length; i++){
            System.out.println(games[indices_arr[i]]);
        }
    }

}
