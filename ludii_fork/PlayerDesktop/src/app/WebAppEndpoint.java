package app;

import app.WebApp;
import app.display.util.WebPlayerFunctions;
import approaches.symbolic.api.Endpoint;
import gnu.trove.list.array.TIntArrayList;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.UUID.randomUUID;

public class WebAppEndpoint extends Endpoint {

    WebApp app = new WebApp();

    @Override
    public String respond() {
//        logToFile = false;

        List<String> args = Arrays.asList(rawInput.split("\\|"));

        switch (args.get(0)) {
            case "setup" -> {
                app = new WebApp();

                final Map<String, String> inputMap = new HashMap<>();
                inputMap.put("width", args.get(1));
                inputMap.put("height", args.get(2));
                inputMap.put("player", args.get(3));
                inputMap.put("ludeme", args.get(4));
                app.createPlayerApp(app, inputMap);
            }
            case "click" -> {
                if (app == null)
                    return "Game is undefined";

                WebPlayerFunctions.clickedPoint(app, new Point(Integer.parseInt(args.get(1)), Integer.parseInt(args.get(2))));
            }
            default -> {
                return "Unknown command";
            }
        }

        BufferedImage bufferedImage = app.view().getviewImage(app);
        System.out.println(bufferedImage);

        File outImage = new File("./temp/" + randomUUID() + ".png");
        try {
            Files.createDirectories(Paths.get("./temp"));
            ImageIO.write(bufferedImage, "png", outImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        outImage.deleteOnExit();

        int currentPlayer = app.manager().ref().context().state().mover();

        int[] winningPlayers = app.manager().ref().context().winners().toArray();

        String winningPlayersStr = String.join(",", Arrays.stream(winningPlayers).mapToObj(String::valueOf).toArray(String[]::new));
        boolean gameOver = !app.manager().ref().context().active();

//        int winningPlayer = app.manager().ref().context().trial().status().winner();
//        boolean gameOver = app.manager().ref().context().trial().over();

        return outImage.getAbsolutePath() + "|" + currentPlayer + "|" + gameOver + "|" + winningPlayersStr;
    }

    public static void main(String[] args) {
        new WebAppEndpoint().start();
    }

}
