package FileManager;

import java.util.*;
import java.io.*;

public class Extract_Ludeme_Concept {
    static String [] games_test_case = new String [] {"Senet", "Phase Chess"};

    String [] games;
    String pathfile;

    public Extract_Ludeme_Concept(String [] games_to_extract, String pathfile){
        this.games = games_to_extract;
        this.pathfile = pathfile;
    }
    class Temp{
        float orderer;
        float to_be_ordered;
        public Temp (float orderer, float to_be_ordered){
            this.orderer = orderer;
            this.to_be_ordered = to_be_ordered;
        }
    }
    public float [] get_most_used(){
        float [] ludeme_id = Csv_handler.parse_first_row(pathfile);
        float [] total_ludeme_vec = new float[0];
        float [] changing_vec;
        boolean first = true;
        for (String game : games){
            if(first) {
                total_ludeme_vec = Csv_handler.parse_game_vector(game, pathfile);
                first = false;
            }else {
                changing_vec = Csv_handler.parse_game_vector(game, pathfile);
                float [] helper = total_ludeme_vec.clone();
                total_ludeme_vec = add_2_vec(changing_vec, helper);
            }
        }
        ArrayList<Temp> for_ordering = new ArrayList<>();
        for (int i = 0; i<ludeme_id.length; i++) {
            Temp temp = new Temp(total_ludeme_vec[i], ludeme_id[i]);
            for_ordering.add(temp);
        }
        for_ordering.sort((o1, o2) -> Float.compare(o1.orderer, o2.orderer));
        Collections.reverse(for_ordering);
        float [] to_return = new float [for_ordering.size()];
        for(int i=0; i<to_return.length; i++){
            to_return[i] = for_ordering.get(i).to_be_ordered;
        }
        return to_return;
    }

    public float [] add_2_vec (float[] first, float[] second){
        float [] total = new float[first.length];
        for(int i=0; i<first.length; i++){
            total[i] = first[i] +second[i];
        }
        return total;
    }
}
