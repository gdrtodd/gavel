package FileManager;

import java.util.*;
import java.io.*;
public class Csv_handler{
    private static final String DELIMITER = ",";
    public static String path_file_user_rating = "Recommender/resources/MARBLE_Data_Organised/User-Rating Matrix/UserRatingMatrix_Correct_Trimmed.csv";

    /**
     * Test main
     */
    public static void main(String[] args) {
        ArrayList<String []> ayy = return_game_w_desc();
        for (String [] a : ayy){
            System.out.println(Arrays.toString(a));
        }
    }
    public static float [][] parse_csv_to_matrix_2(String filename){
        // Initialize nrows and ncols
        int nrows = 0;
        int ncols = 0;
        // We use an arraylist to store the rows of the csv file
        ArrayList<String[]> list_of_rows = new ArrayList<String[]>();
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String[] tempArr = new String[0];
            while((line = br.readLine())!=null){
                tempArr = line.split(DELIMITER);
                list_of_rows.add(tempArr);
                nrows++;
            }
            ncols = tempArr.length;
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        float [][] matrix_to_return = new float [nrows][ncols];
        for(int i=0; i<list_of_rows.size(); i++){
            String [] temp = list_of_rows.get(i);
            for(int j=0; j<temp.length; j++){
                matrix_to_return[i][j] = Float.parseFloat(temp[j]);
            }
        }
        return matrix_to_return;
    }


    /**
     *
     * @param filename : filepath
     * @return Matrix of the parsed csv file
     */
    public static float [][] parse_csv_to_matrix_1(String filename, float empty_val){
        // Initialize nrows and ncols
        int nrows = 0;
        int ncols = 0;
        // We use an arraylist to store the rows of the csv file
        ArrayList<String[]> list_of_rows = new ArrayList<String[]>();
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader br = new BufferedReader(fr);
            String line;
            // We don't need to copy the first row so we only start copying if the first row is passed, we use a simple
            // boolean to check for this
            boolean firstRow = true;
            String[] tempArr;
            while((line = br.readLine())!=null){
                tempArr = line.split(DELIMITER);
                if(!firstRow) {
                    String [] tempArr2 = new String[ncols];

                    for (int i=1; i<tempArr.length; i++){
                        tempArr2[i-1] = tempArr[i];
                    }
                    if (ncols==0){
                        ncols = tempArr2.length;
                    }
                    list_of_rows.add(tempArr2);
                }
                if(firstRow){
                    if (ncols==0){
                        ncols = tempArr.length-1;
                    }
                    firstRow=false;
                }
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        nrows = list_of_rows.size();
        float [][] matrix = new float[nrows][ncols];
        for (int i=0; i<nrows; i++){
            String [] row = list_of_rows.get(i);
            int col =0;
            for(String s : row){
                if(s == "" || s== null){
                    matrix[i][col] = empty_val;
                }
                else {
                    matrix[i][col] = Float.parseFloat(s);
                }
                col++;
            }
        }
        return matrix;
    }

    /**
     * Parse first row for ludeme and concept IDs from the matrices
     * @param pathfile
     * @return
     */
    public static float [] parse_first_row (String pathfile){
        try {
            FileReader fr = new FileReader(pathfile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String [] tempArr;
            while((line = br.readLine()) != null){
                tempArr = line.split(DELIMITER);
                float [] parse_vector = new float [tempArr.length-1];
                for (int i=1; i< tempArr.length; i++){
//                    if(Objects.equals(tempArr[i], "") || tempArr[i]==null){
//                        parse_vector[i-1] = 0;
//                    }
                    parse_vector[i-1] = Float.parseFloat(tempArr[i]);
                }
                return parse_vector;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return new float[0];
    }

    /**
     * Parse list of games (so the top row of user-rating matrix)
     * @return
     */
    public static String [] parse_games (){
        try {
            FileReader fr = new FileReader(path_file_user_rating);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String [] tempArr;
            while((line = br.readLine()) != null){
                tempArr = line.split(DELIMITER);
                String [] parse_vector = new String [tempArr.length-1];
                for (int i=1; i< tempArr.length; i++){
                    parse_vector[i-1] = tempArr[i];
                }
                return parse_vector;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return new String[0];
    }
    public static float [] parse_game_vector (String game_name, String pathfile){
        try{
            FileReader fr = new FileReader(pathfile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String [] tempArr;
            while((line = br.readLine()) !=null){
                tempArr = line.split(DELIMITER);
                if (tempArr[0] == game_name){
                    float [] parse_vector = new float [tempArr.length-1];
                    for(int i=1; i<tempArr.length; i++){
                        if(Objects.equals(tempArr[i], "") || tempArr[i]==null){
                            parse_vector[i-1] = 0;
                        }
                        parse_vector[i-1] = Float.parseFloat(tempArr[i]);
                    }
                    return parse_vector;
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return new float[0];
    }
    public static void writeMatrixToCSV(float[][] matrix, String filePath) {
        try{
            File file = new File(filePath);
            file.createNewFile();
        }catch(Exception e){
            e.printStackTrace();
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            for (float[] row : matrix) {
                StringBuilder rowString = new StringBuilder();
                for (float value : row) {
                    rowString.append(value).append(",");
                }
                rowString.deleteCharAt(rowString.length() - 1); // Remove the trailing comma
                writer.write(rowString.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static ArrayList<String []> return_game_w_desc(){
        ArrayList<String []> games_w_desc = new ArrayList<>();
        try{
            String pathfile = "Recommender/resources/MARBLE_Data_Organised/Games With Description.csv";
            FileReader fr = new FileReader(pathfile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            String [] tempArr;
            while((line = br.readLine()) !=null){
                String [] res = new String [2];
                int commaIndex = line.indexOf(",");
                if (commaIndex != -1) {
                    res[0] = line.substring(0, commaIndex).trim();
                    res[1] = line.substring(commaIndex + 1).trim();
                } else {
                    res[0] = line.trim();
                    res[1] = "";
                }
                games_w_desc.add(res);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return games_w_desc;
    }

}
