package app.display;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.json.JSONObject;

import app.PlayerApp;
import app.display.utils.DisplayParameters;
import app.move.MoveHandler;
import app.move.animation.MoveAnimation;
import app.utils.AnimationVisualsType;
import app.utils.GUIUtil;
import app.utils.PuzzleSelectionType;
import app.utils.SVGUtil;
import app.views.BoardView;
import app.views.View;
import app.views.players.PlayerView;
import app.views.tools.ToolView;
import main.Constants;
import manager.ai.AIDetails;
import manager.ai.AIUtil;
import other.move.Move;

//-----------------------------------------------------------------------------

/**
 * Main Window for displaying the application
 *
 * @author Matthew.Stephenson
 */
public final class MainWindowWeb
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
	
	private final List<Rectangle> buttonAreas =  new ArrayList<>();
	
	/** display parameters for passing to web player. */
	private DisplayParameters latestDisplayParameters = new DisplayParameters();
	private boolean calculatingDisplayParameters = false;

	//-------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public MainWindowWeb(final int width, final int height)
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
		
		app.settingsPlayer().setAnimationType(AnimationVisualsType.Single);
		MoveAnimation.MOVE_PIECE_FRAMES = 1;	// Speed up the local animation.
		
		app.settingsPlayer().setPuzzleDialogOption(PuzzleSelectionType.Cycle);
		
		// Reset animation values and update the context.
		MoveAnimation.resetAnimationValues(app);
		app.contextSnapshot().setContext(app);
		
		final BufferedImage startingBoardImage = getviewImage(app);
		latestDisplayParameters().setPreAnimationBoardImage(startingBoardImage);
		latestDisplayParameters().setPostAnimationBoardImage(startingBoardImage);
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

			// Draw possible move images
			drawPossibleMoveOptions(app, g2dSVG);
			
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
	
	private void drawPossibleMoveOptions(final PlayerApp app, final SVGGraphics2D g2dSVG)
	{
		buttonAreas.clear();
		final List<BufferedImage> allButtonImages = possibleMoves().allButtonImages();
		
		if (allButtonImages.size() > 0)
		{
			final int imageSize = allButtonImages.get(0).getWidth();
			final boolean portraitMode = width < height;
			
			Point startingPoint = new Point(boardPanel.boardSize() + 20, app.height() - imageSize - 60);
			if (portraitMode)
				startingPoint = new Point(20, (int)(boardPanel.placement().getHeight() + playerPanel.placement().getHeight() + toolPanel.placement().getHeight()));
			
			for (int i = 0; i < allButtonImages.size(); i++)
			{
				final BufferedImage image = allButtonImages.get(i);
				final int currentPointX = startingPoint.x + i*imageSize;
				final int currentPointY = startingPoint.y;
				g2dSVG.drawImage(image, currentPointX, currentPointY, null);
				buttonAreas.add(new Rectangle(currentPointX, currentPointY, imageSize, imageSize));
			}
		}
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Check if a given point overlaps a button.
	 */
	public boolean checkPointOverlapsButton(final PlayerApp app, final Point p)
	{
		// Tool buttons
		if (toolPanel.placement().contains(p))
		{
			toolPanel.clickAt(p);
			app.contextSnapshot().setContext(app);
			return true;
		}
		
		// swap player between humand and AI
		for (int i = 0; i < playerSwatchList.length; i++)
		{
			final Rectangle rSwatch = playerSwatchList[i];
			final Rectangle rName = playerNameList[i];
			if (GUIUtil.pointOverlapsRectangle(p, rSwatch) || GUIUtil.pointOverlapsRectangle(p, rName))
			{
				final int realPlayerIndex = app.manager().playerToAgent(i);
				
				// If previously human, set to AI
				if (app.manager().aiSelected()[realPlayerIndex].ai() == null)		
				{
					final JSONObject json = new JSONObject().put("AI", new JSONObject().put("algorithm", "Ludii AI"));
					AIUtil.updateSelectedAI(app.manager(), json, realPlayerIndex, "Ludii AI");
					app.manager().aiSelected()[realPlayerIndex].ai().initIfNeeded(app.contextSnapshot().getContext(app).game(), realPlayerIndex);
				}
				// If previously AI, set to Human
				else		
				{
					final JSONObject json = new JSONObject().put("AI", new JSONObject().put("algorithm", "Human"));
					app.manager().aiSelected()[realPlayerIndex] = new AIDetails(app.manager(), json, realPlayerIndex, "Human");
				}
				
				AIUtil.pauseAgentsIfNeeded(app.manager());
				
				// If the game has already started, setting and AI invalidates the result
				if (app.manager().ref().context().trial().numberRealMoves() > 0)
					app.settingsPlayer().setWebGameResultValid(false);
			}
		}
		
		// Possible moves above tool buttons.
		for (int i = 0; i < buttonAreas.size(); i++)
		{
			if (buttonAreas.get(i).contains(p))
			{
				final Move move = possibleMoves().allButtonMoves().get(i);
				if (MoveHandler.moveChecks(app, move))
					app.manager().ref().applyHumanMoveToGame(app.manager(), move);
			}
		}
		
		possibleMoves().allButtonImages().clear();
		possibleMoves().allButtonMoves().clear();
		
		return false;
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

	public boolean isCalculatingDisplayParameters() 
	{
		return calculatingDisplayParameters;
	}

	public void setCalculatingDisplayParameters(final boolean calculatingDisplayParameters) 
	{
		this.calculatingDisplayParameters = calculatingDisplayParameters;
	}

	public DisplayParameters latestDisplayParameters() 
	{
		return latestDisplayParameters;
	}

	public void setLatestDisplayParameters(final DisplayParameters latestDisplayParameters) 
	{
		this.latestDisplayParameters = latestDisplayParameters;
	}

	public PossibleMoves possibleMoves() 
	{
		return possibleMoves;
	}
	
	//-------------------------------------------------------------------------

}
