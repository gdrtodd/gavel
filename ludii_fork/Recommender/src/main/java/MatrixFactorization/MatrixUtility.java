package MatrixFactorization;

import java.util.Arrays;

/**
 * Utility Methods for Matrix in float data type
 */
public class MatrixUtility {
    public static float[][] transpose(float[][] A){
        float [][] A_t = new float[A[0].length][A.length];
        for (int i=0; i<A.length; i++){
            for (int j=0; j<A[0].length; j++){
                A_t[j][i] = A[i][j];
            }
        }
        return A_t;
    }
    public static float[][] scale_matrix (float scl, float[][] A){
        float [][] scaled_A = new float[A.length][A[0].length];
        for (int i=0; i<A.length; i++){
            for (int j=0; j<A[0].length; j++){
                scaled_A[i][j] = scl * A[i][j];
            }
        }
        return scaled_A;
    }
    public static float [][] add_2_matrices (float[][]A, float[][] B){
        float [][] summed_matrix = new float[A.length][A[0].length];
        for (int i=0; i<A.length; i++){
            for(int j=0; j<A[0].length; j++){
                summed_matrix[i][j] = A[i][j] + B[i][j];
            }
        }
        return summed_matrix;
    }
    // True if Row, False if Column
    public static float [] return_vector_from_matrix (boolean row_or_col, int idx, float [][] A){
        if (row_or_col){
            float [] vector = new float [A[0].length];
            for (int i=0; i<A[0].length; i++){
                vector[i] = A[idx][i];
            }
            return vector;
        }else {
            float [] vector = new float [A.length];
            for (int i=0; i<A.length; i++){
                vector[i] = A[i][idx];
            }
            return vector;
        }
    }
    public static float [][] minus_2_matrices (float[][]A, float[][] B){
        float [][] minus_matrix = new float[A.length][A[0].length];
        for (int i=0; i<A.length; i++){
            for(int j=0; j<A[0].length; j++){
                minus_matrix[i][j] = A[i][j] - B[i][j];
            }
        }
        return minus_matrix;
    }
    public static float [][] element_wise_multiply_2_matrices(float[][] A, float[][] B){
        float [][] multiplied_matrix = new float[A.length][A[0].length];
        for (int i=0; i<A.length; i++){
            for(int j=0; j<A[0].length; j++){
                multiplied_matrix[i][j] = A[i][j] * B[i][j];
            }
        }
        return multiplied_matrix;
    }
    public static float [][] element_wise_divide_2_matrices(float[][] A, float[][] B){
        float [][] divided_matrix = new float[A.length][A[0].length];
        for (int i=0; i<A.length; i++){
            for(int j=0; j<A[0].length; j++){
                divided_matrix[i][j] = A[i][j] / B[i][j];
            }
        }
        return divided_matrix;
    }
    public static float[][] multiply_2_matrices(float[][] A, float[][] B){
        float [][] C = new float[A.length][B[0].length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < B[0].length; j++) {
                for (int k = 0; k < B.length; k++)
                    C[i][j] += A[i][k] * B[k][j];
            }
        }
        return C;
    }
    // https://github.com/MoQuant/InverseMatrixJava/blob/main/inverse.java
    public static float[][] inverse(float[][] z){
        float[][] X = z.clone();
        float[][] I = new float[X.length][X[0].length];
        float A, B, C, D;
        for(int i = 0; i < X.length; i++){
            I[i][i] = 1.0f;
        }
        for(int i = 1; i < X.length; i++){
            for(int j = 0; j < i; j++){
                A = X[i][j];
                B = X[j][j];
                for(int k = 0; k < X.length; k++){
                    X[i][k] = (X[i][k] - (A/B)*X[j][k]);
                    I[i][k] = (I[i][k] - (A/B)*I[j][k]);
                }
            }
        }
        for(int i = 1; i < X.length; i++){
            for(int j = 0; j < i; j++){
                C = X[j][i];
                D = X[i][i];

                for(int k = 0; k < X.length; k++){
                    X[j][k] = (X[j][k] - (C/D)*X[i][k]);
                    I[j][k] = (I[j][k] - (C/D)*I[i][k]);
                }
            }
        }
        for(int i = 0; i < X.length; i++){
            for(int j = 0; j < X[0].length; j++){
                I[i][j] = I[i][j] / X[i][i];
            }
        }
        return I;
    }

    public static void main(String[] args) {
        float [][] test = new float [][] {{1.0f, 2.0f, 3f}, {5.0f, 1f, 2f}, {1f, 2f, 1f}};

        System.out.println(Arrays.deepToString(inverse(test)));
    }
}
