package app.headless;

import org.apache.xmlgraphics.image.loader.impl.imageio.ImageIOUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Matthew.Stephenson
 */
public class StartHeadlessApp
{
	/**
	 * Useless main for Tomcat.
	 * @param args
	 */
	public static void main(final String[] args)
	{


		String game_path;
		if (args.length > 0) {
			game_path = args[args.length-1];
		} else {
			game_path = "/Users/gray/projects/ludii-lms/ludii_data/games/official/board/space/connection/Hex.lud";
		}

		String game;
		try {
			game = Files.readString(Path.of(game_path));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		final HeadlessApp app = new HeadlessApp(1000, 500, game, null);

		BufferedImage saveImage1 = app.view().getviewImage(app);
		System.out.println(saveImage1);
		File outputfile1 = new File("/Users/gray/Desktop/test_headless1.png");
		try {
			ImageIO.write(saveImage1, "png", outputfile1);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		app.manager().ref().randomMove(app.manager());

		BufferedImage saveImage2 = app.view().getviewImage(app);
		System.out.println(saveImage2);
		File outputfile2 = new File("/Users/gray/Desktop/test_headless2.png");
		try {
			ImageIO.write(saveImage2, "png", outputfile2);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		app.clickedPoint(new Point(200,200));

		BufferedImage saveImage3 = app.view().getviewImage(app);
		System.out.println(saveImage3);
		File outputfile3 = new File("/Users/gray/Desktop/test_headless3.png");
		try {
			ImageIO.write(saveImage3, "png", outputfile3);
//			ImageIO.write(saveImage3, "png", System.out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
