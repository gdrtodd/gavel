package MatrixFactorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.lang.Math;

public class MF {
    protected float [][] original_matrix;
    private ArrayList<int[]> coord_list;
    private int N;
    protected float [][] u_matrix;
    protected float [][]  q_matrix;
    private int maxIter;
    private float alpha;  // momentum
    private float lambda; // lambda
    private float learning_rate; // learning rate
    private int k;        // k (number of communities/clusters)
    private ArrayList<float [][]> lowest_cost_u_q = new ArrayList<>();    // u and q matrices with lowest cost
    private float lowest_cost = Float.MAX_VALUE;

    public MF(float alpha, float lambda, float learning_rate,  int k, float [][] matrix, int maxIter){
        this.alpha = alpha;
        this.k = k;
        this.original_matrix = matrix;
        this.lambda = lambda;
        u_matrix = new float[original_matrix.length][k];     // n x k
        q_matrix = new float[original_matrix[0].length][k];   // m x k
        lowest_cost_u_q.add(null);
        lowest_cost_u_q.add(null);
        this.maxIter = maxIter;
        this.coord_list = collect_coords();
        this.learning_rate = learning_rate;
    }
    // Create getters for the sake of hyper-parameter optimization
    public int get_maxIter(){
        return maxIter;
    }
    public int get_k(){
        return k;
    }
    public float get_alpha(){
        return alpha;
    }
    public float get_lambda(){
        return lambda;
    }
    public ArrayList<float [][]> get_lowest_cost_uq (){
        return lowest_cost_u_q;
    }
    public void initialize_uq_matrices (){
        // Change to something else, sample from a gaussian distribution for each user rating matrix
        //
        Random r = new Random();
        for (int i=0; i<u_matrix.length; i++){
            for(int j=0; j<u_matrix[0].length; j++){
                u_matrix[i][j] = (float) Math.sqrt(6.204555251974264f/k);
            }
        }
        for (int i=0; i<q_matrix.length; i++){
            for(int j=0; j<q_matrix[0].length; j++){
                q_matrix[i][j] = (float) Math.sqrt(6.204555251974264f/k);
            }
        }
    }
    public float predict_rating(int user, int item){
        float rating=0;
        for (int i=0; i<k; i++){
            rating += u_matrix[user][i] * q_matrix[item][i];
        }
        return rating;
    }
    public ArrayList<int[]> collect_coords(){
        ArrayList<int[]> coord_list = new ArrayList<int[]>();
        for (int i=0; i<original_matrix.length; i++){
            for (int j=0; j<original_matrix[i].length; j++){
                if (original_matrix[i][j]!=-1){
                    coord_list.add(new int[]{i, j});
                    this.N++;
                }
            }
        }
        return coord_list;
    }
    public float cost (){
        float sum =0;
        for (int i=0; i< coord_list.size(); i++){
            int [] coord = coord_list.get(i);
            float predicted = predict_rating(coord[0], coord[1]);
            float diff = original_matrix[coord[0]][coord[1]] - predicted;
            sum += diff*diff;
        }
        return (float) Math.sqrt(sum/this.N);
    }
    public float calc_mean_rating(){
        float sum = 0;
        for(int[] coord: coord_list){
            sum += predict_rating(coord[0], coord[1]);
        }
        return sum/this.N;
    }
    public ArrayList<float[][]> calculate_gradient (float[][] u, float[][] q){
        float [][] delta = new float[original_matrix.length][original_matrix[0].length];
        for (int [] coord : coord_list){
            delta[coord[0]][coord[1]] = original_matrix[coord[0]][coord[1]] - predict_rating(coord[0], coord[1]);
        }
        float [][] delta_T = MatrixUtility.transpose(delta);
        float [][] deltaQ = MatrixUtility.multiply_2_matrices(delta, q);
        float [][] scaled_deltaQ = MatrixUtility.scale_matrix((-2f/(float)N), deltaQ);
        float [][] delta_TU = MatrixUtility.multiply_2_matrices(delta_T, u);
        float [][] scaled_deltaTU = MatrixUtility.scale_matrix((-2f/(float)N), delta_TU);
        float [][] scaled_U = MatrixUtility.scale_matrix(2*lambda, u);
        float [][] scaled_Q = MatrixUtility.scale_matrix(2*lambda, q);
        float [][] grad_user = MatrixUtility.add_2_matrices(scaled_deltaQ, scaled_U);
        float [][] grad_item = MatrixUtility.add_2_matrices(scaled_deltaTU, scaled_Q);
        ArrayList <float[][]> gradients = new ArrayList<float[][]>();
        gradients.add(grad_user);
        gradients.add(grad_item);
        return gradients;
    }
    // Fit Matrix using Gradient Descent
    public void find_uq_matrices(){
        initialize_uq_matrices();
        ArrayList<float[][]> gradients = calculate_gradient(u_matrix ,q_matrix);
        float [][] u_grad = gradients.get(0);
        float [][] q_grad = gradients.get(1);
        float [][] v_user = u_grad.clone();
        float [][] v_item = q_grad.clone();
        float prevcost = 0;
        for (int i=0; i<maxIter; i++){
            if (i!=0) {
                gradients = calculate_gradient(u_matrix, q_matrix);
            }
            u_grad = gradients.get(0);
            q_grad = gradients.get(1);
            v_user = MatrixUtility.add_2_matrices(MatrixUtility.scale_matrix(alpha, v_user), MatrixUtility.scale_matrix(1f-alpha, u_grad));
            v_item = MatrixUtility.add_2_matrices(MatrixUtility.scale_matrix(alpha, v_item), MatrixUtility.scale_matrix(1f-alpha, q_grad));
            u_matrix = MatrixUtility.minus_2_matrices(u_matrix, MatrixUtility.scale_matrix(learning_rate, v_user));
            q_matrix = MatrixUtility.minus_2_matrices(q_matrix, MatrixUtility.scale_matrix(learning_rate, v_item));
            float cost = cost();
            if (cost < lowest_cost){
                lowest_cost_u_q.set(0, u_matrix);
                lowest_cost_u_q.set(1, q_matrix);
            }
            if (prevcost < cost){
                learning_rate -= 0.2*learning_rate;
            }
            if (prevcost == cost){
                randomize_uq_matrices();
            }
            prevcost = cost;
            System.out.println("Iteration " + i + " Cost: " + cost + " Mean rating: " + calc_mean_rating());
        }
    }
    public void randomize_uq_matrices(){
        float change_or_not = 0.5f;
        float max_change = 0.0002f;
        for(int i = 0; i< u_matrix.length; i++){
            for(int j = 0; j< u_matrix[i].length; j++){
                Random r = new Random();
                if (r.nextFloat() > change_or_not) {
                    u_matrix[i][j] += r.nextFloat() * (max_change - (-max_change)) - max_change;
                }
            }
        }
        for(int i = 0; i< q_matrix.length; i++){
            for(int j = 0; j< q_matrix[i].length; j++){
                Random r = new Random();
                if (r.nextFloat() > change_or_not) {
                    q_matrix[i][j] += r.nextFloat() * (max_change - (-max_change)) - max_change;
                }
            }
        }
    }
    public float [][] get_matrix_final_uq_values(){
        return MatrixUtility.multiply_2_matrices(u_matrix, MatrixUtility.transpose(q_matrix));
    }
    public float [][] get_matrix_lowest_uq_values(){
        return MatrixUtility.multiply_2_matrices(lowest_cost_u_q.get(0), MatrixUtility.transpose(lowest_cost_u_q.get(1)));
    }
}
