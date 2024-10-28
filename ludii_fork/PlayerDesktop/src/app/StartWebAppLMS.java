package app;

import app.display.util.WebPlayerFunctions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Matthew.Stephenson
 */
public class StartWebAppLMS
{
	/**
	 * Useless main for Tomcat.
	 * @param args
	 */
	public static void main(final String[] args)
	{
		final WebApp app = new WebApp();
		final Map<String, String> inputMap = new HashMap<>();
		inputMap.put("ludeme", "/lud/board/space/connection/Hex.lud");
		inputMap.put("width", "1000");
		inputMap.put("height", "500");
		app.createPlayerApp(app, inputMap);

		BufferedImage saveImage1 = app.view().getviewImage(app);
		System.out.println(saveImage1);
		File outputfile1 = new File("test_headless1.png");
		try {
			ImageIO.write(saveImage1, "png", outputfile1);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		app.manager().ref().randomMove(app.manager());

		BufferedImage saveImage2 = app.view().getviewImage(app);
		System.out.println(saveImage2);
		File outputfile2 = new File("test_headless2.png");
		try {
			ImageIO.write(saveImage2, "png", outputfile2);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		WebPlayerFunctions.clickedPoint(app, new Point(200,200));

		BufferedImage saveImage3 = app.view().getviewImage(app);
		System.out.println(saveImage3);
		File outputfile3 = new File("test_headless3.png");
		try {
			ImageIO.write(saveImage3, "png", outputfile3);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
