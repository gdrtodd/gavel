package approaches.symbolic.api;

import java.util.Scanner;
import GUI.RecommenderStarter;

/**
 * Wrapper used by the VSCode extension to interact with the recommender system.
 */
public class RecommenderEndpoint extends Endpoint {
    @Override
    public String respond() {
        new RecommenderStarter();
        return "";
    }

    public static void main(String[] args) {
        new RecommenderEndpoint().start();
    }
}
