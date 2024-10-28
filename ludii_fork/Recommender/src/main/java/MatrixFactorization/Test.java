package MatrixFactorization;
import FileManager.Csv_handler;
import FileManager.Csv_handler.*;

import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        float [][] original_urm = Csv_handler.parse_csv_to_matrix_1(Csv_handler.path_file_user_rating, -1f);
        float [][] sim_matrix = Csv_handler.parse_csv_to_matrix_1("Recommender/resources/MARBLE_Data_Organised/Similarity Matrices/cosine_concept_matrix_new.csv", 0f);
//        MF mf = new MF(0.9f, 0.002f, 0.1f,25, original_urm, 2000);
//        mf.find_uq_matrices();
//        Csv_handler.writeMatrixToCSV(mf.get_matrix_final_uq_values(), "resources/MF Results/test_1c.csv");
//        Csv_handler.writeMatrixToCSV(mf.get_matrix_lowest_uq_values(), "resources/MF Results/test_2c.csv");
        ItemMSMF item_msmf = new ItemMSMF(0.9f, 0.002f, 0.1f,25, original_urm,1200, sim_matrix);
        item_msmf.find_uq_matrices();
//        float [][] matrix = item_msmf.get_matrix_final_uq_values();
//        System.out.println(Arrays.deepToString(matrix));
        Csv_handler.writeMatrixToCSV(item_msmf.get_matrix_final_uq_values(), "Recommender/resources/MF Results/first_use_final.csv");
        Csv_handler.writeMatrixToCSV(item_msmf.get_matrix_lowest_uq_values(), "Recommender/resources/MF Results/first_use_lowest.csv");
        Csv_handler.writeMatrixToCSV(item_msmf.q_matrix, "Recommender/resources/MF Results/first_use_q_matrix_final.csv");
        Csv_handler.writeMatrixToCSV(item_msmf.u_matrix, "Recommender/resources/MF Results/first_use_u_matrix_final.csv");
        Csv_handler.writeMatrixToCSV(item_msmf.get_lowest_cost_uq().get(0), "Recommender/resources/MF Results/first_use_u_matrix_lowest.csv");
        Csv_handler.writeMatrixToCSV(item_msmf.get_lowest_cost_uq().get(1), "Recommender/resources/MF Results/first_use_q_matrix_lowest.csv");



    }
}
