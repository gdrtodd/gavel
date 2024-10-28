package app.headless;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import app.PlayerApp;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import app.utils.PuzzleSelectionType;
import app.utils.SVGUtil;
import app.views.BoardView;
import app.views.View;
import app.views.players.PlayerView;
import app.views.tools.ToolView;
import main.Constants;

//-----------------------------------------------------------------------------

/**
 * Main Window for displaying the application
 *
 * @author Matthew.Stephenson
 */
public final class MainWindowHeadless
{
	
	/** List of view panels, including all sub-panels. */
	private final List<View> panels = new CopyOnWriteArrayList<>();
	
	private final PossibleMoves possibleMoves = new PossibleMoves();
	
	/** View that covers the left half of the window, where the board is drawn. */
	private BoardView boardPanel;
	/** View that covers the top right area of the window, where the player views are drawn. */
	protected PlayerView playerPanel;
	/** View that covers the bottom right area of the window, where the tool buttons are drawn. */
	protected ToolView toolPanel;

	/** Width of the view. */
	private final int width;
	/** Height of the view (also the height of the board). */
	private final int height;
	
	/** Array of bounding rectangles for each player's swatch. */
	private final Rectangle[] playerSwatchList = new Rectangle[Constants.MAX_PLAYERS+1];
	/** Array of bounding rectangles for each player's name. */
	private final Rectangle[] playerNameList = new Rectangle[Constants.MAX_PLAYERS+1];

	//-------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public MainWindowHeadless(final int width, final int height)
	{
		this.width = width;
		this.height = height;
	}

	//-------------------------------------------------------------------------

	/**
	 * Create UI panels.
	 */
	public void createPanels(final PlayerApp app)
	{
		panels.clear();
		
		final boolean portraitMode = width < height;
		
		// Create board panel
		boardPanel = new BoardView(app, false);
		panels.add(boardPanel);
		
		// Create the player panel
		playerPanel = new PlayerView(app, portraitMode, false);
		panels.add(playerPanel);
		
		// Create tool panel
		toolPanel = new ToolView(app, portraitMode);
		panels.add(toolPanel);

		app.settingsPlayer().setPuzzleDialogOption(PuzzleSelectionType.Cycle);
	}

	//-------------------------------------------------------------------------

	/**
	 * Gets the view image based on the GUI context (not updated until after animation complete).
	 */
	public BufferedImage getviewImage(final PlayerApp app)
	{		
		try
		{
			app.bridge().settingsVC().setDisplayFont(new Font("Arial", Font.BOLD, 20));

			final SVGGraphics2D g2dSVG = new SVGGraphics2D(width, height);
			
			g2dSVG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2dSVG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2dSVG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2dSVG.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2dSVG.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g2dSVG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			g2dSVG.setColor(Color.WHITE);
			g2dSVG.fillRect(0, 0, width, height);

			for (final View panel : panels)
				panel.paint(g2dSVG);
			
			final BufferedImage boardImage = SVGUtil.createSVGImage(g2dSVG.getSVGDocument(), width, height);
			return boardImage;
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		
		return null;
	}

	//-------------------------------------------------------------------------
	
	public List<View> getPanels()
	{
		return panels;
	}
	
	public BoardView getBoardPanel()
	{
		return boardPanel;
	}
	
	//-------------------------------------------------------------------------
	
	public int width()
	{
		return width;
	}

	public int height()
	{
		return height;
	}
	
	//-------------------------------------------------------------------------
	
	public Rectangle[] playerSwatchList()
	{
		return playerSwatchList;
	}

	public Rectangle[] playerNameList()
	{
		return playerNameList;
	}

	//-------------------------------------------------------------------------

	public PossibleMoves possibleMoves()
	{
		return possibleMoves;
	}

	//-------------------------------------------------------------------------

}
