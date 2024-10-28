package MatrixFactorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class ItemMSMF extends MF {
                                            // Order of indices and columns of items is the same as the order of items in
    private float [][] similarity_matrix;   // similarity Matrix
    private int [] to_be_replaced;         // Indices of items to be replaced

    public ItemMSMF(float alpha, float lambda, float learning_rate, int k, float[][] matrix, int maxIter, float[][] similarity_matrix) {
        super(alpha, lambda, learning_rate,k ,matrix, maxIter);
        this.similarity_matrix = similarity_matrix;
        ArrayList <Integer> al = new ArrayList<>();
        for (int j=0; j<matrix[0].length; j++){
            boolean flag = true;
            for(int i=0; i<matrix.length; i++){
                if(matrix[i][j]!=-1){
                    flag = false;
                    break;
                }
            }
            if(flag){
                al.add(j);
            }
        }
        to_be_replaced = new int [al.size()];
        for(int i=0; i<to_be_replaced.length; i++){
            to_be_replaced[i] = al.get(i);
        }
    }


    class Pair {
        float similarity;
        int idx;
        Pair(float similarity, int idx){
            this.similarity = similarity;
            this.idx = idx;
        }
    }
    public ArrayList <Pair> return_k_similar_elements (int k, int item){
        float [] row = MatrixUtility.return_vector_from_matrix(true, item, similarity_matrix);
        ArrayList <Pair> list_items = new ArrayList<Pair>();
        int idx_smallest = 0;
        for(int i=0; i<k; i++){
            float smallest = Integer.MAX_VALUE;
            for(int j=0; j<similarity_matrix.length; j++) {
                IntStream stream = Arrays.stream(to_be_replaced);
                int finalJ = j;
                if (row[j] != 0 && row[j] < smallest && !list_items.contains(j) && stream.anyMatch(x -> x == finalJ)) {
                    smallest = row[j];
                    idx_smallest = j;
                }
            }
            Pair p = new Pair(1f-smallest, idx_smallest);
            list_items.add(p);
        }
        return list_items;
    }

    /**
     * TODO: change k value (k similar items) to something other than the k hyperparameter
     */
    public void replace_latent_vectors(float [][] q_matrix_used){   // True if lowest u, q matrices used, False if final u, q matrices used
        for (int row_idx=0; row_idx<to_be_replaced.length; row_idx++){
            int item = to_be_replaced[row_idx];
            int n = 5;
            ArrayList <Pair> k_similar_items = return_k_similar_elements(n, item);
            float [] average_vector = new float [super.get_k()];
            float sum = 0;
            for (int i=0; i<n; i++){
                Pair p = k_similar_items.get(i);
                int row_index = p.idx;
                float [] row = MatrixUtility.return_vector_from_matrix(true, row_index, super.q_matrix);
                for (int j=0; j<row.length; j++){
                    average_vector[j] += row[j] * p.similarity;
                }
                sum += p.similarity;
            }
            for (int i=0; i<average_vector.length; i++){
                float temp = average_vector[i];
                average_vector[i] = temp / sum;
            }
            for (int i=0; i<super.q_matrix[0].length; i++){
                q_matrix_used[row_idx][i] = average_vector[i];
            }
        }
    }
    @Override
    public float [][] get_matrix_final_uq_values() {
        replace_latent_vectors(super.q_matrix);
        return MatrixUtility.multiply_2_matrices(super.u_matrix, MatrixUtility.transpose(super.q_matrix));
    }
    @Override
    public float [][] get_matrix_lowest_uq_values() {
        replace_latent_vectors(super.get_lowest_cost_uq().get(0));
        return MatrixUtility.multiply_2_matrices(super.get_lowest_cost_uq().get(0), MatrixUtility.transpose(super.get_lowest_cost_uq().get(1)));
    }
}
