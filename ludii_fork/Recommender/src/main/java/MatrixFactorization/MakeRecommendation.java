package MatrixFactorization;

import FileManager.Csv_handler;
import GUI.User_Rec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

import static FileManager.Csv_handler.*;

public class MakeRecommendation {
    float [] new_user_ratings;
    float [][] user_rating_matrix;
    float [][] u_matrix;
    float [][] q_matrix;
    int k;
    ArrayList<Float> r_vector = new ArrayList<>();
    ArrayList<Integer> inds_no_rating = new ArrayList<>();
    float [] recs;
    String [] fav_games;
    // Make sure q_matrix is transposed (so it's k x no. of total items)
    public MakeRecommendation(float [] new_user_ratings, float [][]u_matrix, float[][] q_matrix){
        this.new_user_ratings = new_user_ratings;
        this.user_rating_matrix = MatrixUtility.multiply_2_matrices(u_matrix, q_matrix);
        this.u_matrix = u_matrix;
        this.q_matrix = q_matrix;
        this.k = q_matrix.length;
        this.recs = new float[new_user_ratings.length];
    }
    public void update_recs(){
        float [][] q_u = find_q_u_matrix();    // Should be k x n_u (n_u = no. of provided ratings by the user)
        float [][] r = new float[1][r_vector.size()];   // Should be 1 x n_u, user vector
        for(int i=0; i<r[0].length; i++){
            r[0][i] = r_vector.get(i);
        }
        float [][] q_uT = MatrixUtility.transpose(q_u);   // Should be n_u x k
        float [][] q_u_q_uT = MatrixUtility.multiply_2_matrices(q_u, q_uT);  // Should be k x k
        float [][] q_u_q_uTInv = MatrixUtility.inverse(q_u_q_uT);   // Should be k x k
        float [][] q_uT_q_u_q_uTInv = MatrixUtility.multiply_2_matrices(q_uT, q_u_q_uTInv);   // Should be n_u x k
        float [][] r_q_uT_q_u_q_uTInv = MatrixUtility.multiply_2_matrices(r, q_uT_q_u_q_uTInv); // Should be 1 x k
//        System.out.println(r_q_uT_q_u_q_uTInv[0].length);
//        System.out.println(k);
//        System.out.println(q_matrix.length);
//        System.out.println(q_matrix[0].length);
//        System.out.println(recs.length);
        for (Integer i : inds_no_rating){
            recs[i] = 0;
            for(int j=0; j < r_q_uT_q_u_q_uTInv[0].length; j++){
                recs[i] += r_q_uT_q_u_q_uTInv[0][j] * this.q_matrix[j][i];
            }
        }
    }
    public void user_n_most_liked_games(int n) {
        String[] all_games = parse_games();
        fav_games = new String[n];
        int[] fav_games_index = new int[n];
        for (int i = 0; i < n; i++) {
            float biggest_val = -100f;
            int candidate = 0;
            for (int j = 0; j < recs.length; j++) {
                int finalJ = j;
                boolean contains = IntStream.of(fav_games_index).anyMatch(x -> x== finalJ);
//                boolean contains2 = IntStream.of(User_Rec.get_indices_arr()).anyMatch(x -> x==finalJ);
                if (recs[j] > biggest_val && !contains) {
                    biggest_val = recs[j];
                    candidate = j;
                }
            }
            fav_games_index[i] = candidate;
        }
        for (int i = 0; i < fav_games_index.length; i++) {
            fav_games[i] = all_games[fav_games_index[i]];
        }
//        System.out.println("We recommend " + Arrays.toString(fav_games));
    }
    public String fav_game_desc(){
        StringBuilder str = new StringBuilder();
        ArrayList<String[]> games_w_desc = Csv_handler.return_game_w_desc();
        for(String s : fav_games){
//            System.out.println(s);
            String s2 = " \" " + s + " \" ";
            for(String [] strarr : games_w_desc){
                if(Objects.equals(strarr[0].replaceAll("\\s+",""), s.replaceAll("\\s+",""))){
                    str.append(s).append(" : ").append(strarr[1]).append("\\n");
                }else if(Objects.equals(strarr[0].replaceAll("\\s+",""), s2.replaceAll("\\s+",""))) {
                    str.append(s).append(" : ").append(strarr[1]).append("\\n");
                }
            }
        }
        return str.toString();
    }

    /**
     * Extract the item vectors for the items the user rated
     * @return
     */
    public float [][] find_q_u_matrix(){
        ArrayList<float[]>  al = new ArrayList<>();
        for (int i=0; i<new_user_ratings.length; i++){
            if(new_user_ratings[i] != -1.0f){
                r_vector.add(new_user_ratings[i]);
                float [] temp = new float [k];
                for(int j=0; j<q_matrix.length; j++){
                    temp[j] = q_matrix[j][i];
                }
                al.add(temp);
                recs[i] = new_user_ratings[i];
            }
            else {
                inds_no_rating.add(i);
            }
        }
        float [][] q_u_matrix = new float [k][al.size()];
        int idx = 0;
        for (float [] f : al){
            for(int j = 0; j<k; j++){
                q_u_matrix[j][idx] = f[j];
            }
            idx++;
        }
        return q_u_matrix;
    }

    public static void main(String[] args) {
        StringBuilder str = new StringBuilder();
        String [] fav_games = new String [] {"Chess", "Doov", "Diviyan Keliya"};
        ArrayList<String[]> games_w_desc = Csv_handler.return_game_w_desc();
        for(String s : fav_games){
            System.out.println(s);
            String s2 = " \" " + s + " \" ";
            for(String [] strarr : games_w_desc){
                if(Objects.equals(strarr[0].replaceAll("\\s+",""), s.replaceAll("\\s+",""))){
                    str.append(strarr[1]).append("\n");
                }else if(Objects.equals(strarr[0].replaceAll("\\s+",""), s2.replaceAll("\\s+",""))) {
                    str.append(strarr[1]).append("\n");
                }
            }
        }
        System.out.println(str.toString());
    }
}
