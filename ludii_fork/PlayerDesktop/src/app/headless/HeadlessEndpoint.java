package app.headless;

import approaches.symbolic.api.Endpoint;
import approaches.symbolic.api.FractionalCompilerEndpoint;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.UUID.randomUUID;

public class HeadlessEndpoint extends Endpoint {

    HeadlessApp app = null;

    @Override
    public String respond() {
//        logToFile = false;

        List<String> args = Arrays.asList(rawInput.split("\\|"));

        switch (args.get(0)) {
            case "setup" -> {
                app = new HeadlessApp(Integer.parseInt(args.get(1)), Integer.parseInt(args.get(2)), args.get(3), null);
            }
            case "click" -> {
                if (app == null)
                    return "Game is undefined";

                app.clickedPoint(new Point(Integer.parseInt(args.get(1)), Integer.parseInt(args.get(2))));
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

        return outImage.getAbsolutePath();
    }

    public static void main(String[] args) {
        new HeadlessEndpoint().start();
    }

}
