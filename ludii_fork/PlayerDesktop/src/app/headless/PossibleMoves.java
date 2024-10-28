package app.headless;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.jfree.graphics2d.svg.SVGGraphics2D;

import app.PlayerApp;
import app.utils.BufferedImageUtil;
import app.utils.SVGUtil;
import game.equipment.component.Component;
import graphics.ImageUtil;
import graphics.svg.SVGtoImage;
import main.collections.FastArrayList;
import other.action.Action;
import other.action.ActionType;
import other.action.cards.ActionSetTrumpSuit;
import other.action.move.move.ActionMove;
import other.action.others.ActionPropose;
import other.action.others.ActionVote;
import other.action.state.ActionBet;
import other.action.state.ActionSetNextPlayer;
import other.action.state.ActionSetRotation;
import other.context.Context;
import other.move.Move;
import other.state.container.ContainerState;
import util.ContainerUtil;
import util.HiddenUtil;
import view.component.ComponentStyle;

/**
 * @author Matthew.Stephenson
 */
public class PossibleMoves
{
	private final List<BufferedImage> allButtonImages = new ArrayList<>();
	
	private final List<Move> allButtonMoves = new ArrayList<>();
	
	private final int imageSize = 50;
	
	//-------------------------------------------------------------------------

	/**
	 * Create the dialog.
	 */
	public void calculateButtonImages(final PlayerApp app, final Context context, final FastArrayList<Move> validMoves)
	{		
		allButtonImages.clear();
		allButtonMoves.clear();
		
		final SVGGraphics2D g2dBackground = new SVGGraphics2D(imageSize, imageSize);
		g2dBackground.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2dBackground.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2dBackground.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2dBackground.setColor(Color.GRAY);
		g2dBackground.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		g2dBackground.fillRect(1, 1, imageSize-2, imageSize-2);
		final BufferedImage backgroundImage = SVGUtil.createSVGImage(g2dBackground.getSVGDocument(), imageSize, imageSize);
		
		for (final Move m : validMoves)
		{	
			allButtonMoves.add(m);
			
			boolean moveShown = false;
			
			// Check for displaying special actions
			for (final Action a : m.actions())
			{		
				final int fromContainerIndex = ContainerUtil.getContainerId(context, m.from(), m.fromType());
				
				// Rotation move
				if (a instanceof ActionSetRotation)
				{			
					final int componentValue = app.contextSnapshot().getContext(app).containerState(fromContainerIndex).what(m.from(), m.fromType());
					if (componentValue != 0)
					{
						final Component c = context.components()[componentValue];
						final BufferedImage componentImage = app.graphicsCache().getComponentImage(app.bridge(), fromContainerIndex, c, c.owner(), 0, 0, m.from(), 0, m.fromType(), imageSize, app.contextSnapshot().getContext(app), 0, a.rotation(), true);
						allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, componentImage));
						
						moveShown = true;
						break;
					}
				}
				
				// Adding/Promoting a component
				else if (a.actionType() == ActionType.Add || a.actionType() == ActionType.Promote)
				{
					final ContainerState cs = app.contextSnapshot().getContext(app).containerState(fromContainerIndex);
					final int hiddenValue = HiddenUtil.siteHiddenBitsetInteger(context, cs, a.levelFrom(), a.levelFrom(), a.who(), a.fromType());
					final int componentWhat = a.what();
					final int componentValue = a.value();
					final int componentState = a.state();
					final ComponentStyle componentStyle = app.bridge().getComponentStyle(componentWhat);
					componentStyle.renderImageSVG(context, fromContainerIndex, imageSize, componentState, componentValue, true, hiddenValue, a.rotation());
					final SVGGraphics2D svg = componentStyle.getImageSVG(componentState);
					BufferedImage componentImage = null;
					if (svg != null)
						componentImage = SVGUtil.createSVGImage(svg.getSVGDocument(),imageSize,imageSize);	
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, componentImage));
					
					moveShown = true;
					break;
				}
				
				// Set trump move
				else if (a instanceof ActionSetTrumpSuit)
				{
					final int trumpValue = ((ActionSetTrumpSuit) a).what();
					String trumpImage = "";
					Color imageColor = Color.BLACK;
					switch(trumpValue)
					{
						case 1: trumpImage = "card-suit-club"; break;
						case 2: trumpImage = "card-suit-spade"; break;
						case 3: trumpImage = "card-suit-diamond"; imageColor = Color.RED; break;
						case 4: trumpImage = "card-suit-heart"; imageColor = Color.RED; break;
					}
					BufferedImage componentImage = SVGUtil.createSVGImage(trumpImage, (int) (imageSize*0.8), (int) (imageSize*0.8));
					componentImage = BufferedImageUtil.setPixelsToColour(componentImage, imageColor);
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, componentImage));
					
					moveShown = true;
					break;
				}
				
				// Set next player move
				else if (a instanceof ActionSetNextPlayer && !m.isSwap())
				{
					final int nextPlayerValue = ((ActionSetNextPlayer) a).who();
					final String buttonText = "Next player: " + nextPlayerValue;
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, convertTextToGraphic(buttonText)));
					
					moveShown = true;
					break;
				}
				
				// Pick the bet
				else if (a instanceof ActionBet)
				{
					final int betValue = ((ActionBet) a).count();
					final int betWho = ((ActionBet) a).who();
					final String buttonText = "P" + betWho + ", Bet: " + betValue;
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, convertTextToGraphic(buttonText)));
					
					moveShown = true;
					break;
				}
				
				// Propose
				else if (a instanceof ActionPropose)
				{
					final String proposition = ((ActionPropose) a).proposition();
					final String buttonText = "Propose: " + proposition;
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, convertTextToGraphic(buttonText)));
					
					moveShown = true;
					break;
				}
				
				// Vote
				else if (a instanceof ActionVote)
				{
					final String vote = ((ActionVote) a).vote();
					final String buttonText = "Vote: " + vote;
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, convertTextToGraphic(buttonText)));
					
					moveShown = true;
					break;
				}
				
				// Moving a large piece
				if (a instanceof ActionMove)
				{			
					final int componentValue = app.contextSnapshot().getContext(app).containerState(fromContainerIndex).what(m.from(), m.fromType());
					if (componentValue != 0)
					{
						final Component c = context.components()[componentValue];
						if (c.isLargePiece())
						{
							final ComponentStyle componentStyle = app.bridge().getComponentStyle(c.index());
							final int maxSize = Math.max(componentStyle.largePieceSize().x, componentStyle.largePieceSize().y);
							final double scaleFactor = 0.9 * imageSize / maxSize;
							BufferedImage componentImage = app.graphicsCache().getComponentImage(app.bridge(), fromContainerIndex, c, c.owner(), a.state(), 0, m.from(), 0, m.fromType(), imageSize, app.contextSnapshot().getContext(app), 0, 0, true);
							componentImage = BufferedImageUtil.resize(componentImage, (int)(scaleFactor * componentStyle.largePieceSize().x), (int)(scaleFactor * componentStyle.largePieceSize().y));
							allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, componentImage));
						
							moveShown = true;
							break;
						}
					}
				}
			}
			
			// If no "special" action found
			if (!moveShown)
			{
				// Pass
				if (m.isPass())
				{ 
					final SVGGraphics2D g2d = new SVGGraphics2D(imageSize, imageSize);
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2d.setColor(Color.BLACK);
					g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
	
					SVGtoImage.loadFromFilePath
					(
						g2d, ImageUtil.getImageFullPath("button-pass"), new Rectangle(0,0,imageSize,imageSize), 
						Color.BLACK, Color.WHITE, 0
					);
					final BufferedImage passImage = SVGUtil.createSVGImage(g2d.getSVGDocument(), imageSize, imageSize);
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, passImage));
				}
				
				// Swap
				else if (m.isSwap())
				{ 
					final SVGGraphics2D g2d = new SVGGraphics2D(imageSize, imageSize);
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g2d.setColor(Color.BLACK);
					g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
	
					SVGtoImage.loadFromFilePath
					(
						g2d, ImageUtil.getImageFullPath("button-swap"), new Rectangle(0,0,imageSize,imageSize), 
						Color.BLACK, Color.WHITE, 0
					);
					final BufferedImage swapImage = SVGUtil.createSVGImage(g2d.getSVGDocument(), imageSize, imageSize);
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, swapImage));
				}
				
				// Default fallback
				else
				{
					// Only display non-duplicated moves.
					final List<Action> moveActions = m.getActionsWithConsequences(context.currentInstanceContext());
					final List<Action> nonDuplicateActions = new ArrayList<>();
					for (final Action a1 : moveActions)
					{
						for (final Move m2 : validMoves)
						{	
							if (!m2.getActionsWithConsequences(context.currentInstanceContext()).contains(a1))
							{
								nonDuplicateActions.add(a1);
								break;
							}
						}
					}
					
					String actionString = "";
					for (final Action a : nonDuplicateActions)
						actionString += a.toString() + "<br>";
					
					allButtonImages().add(BufferedImageUtil.joinBufferedImages(backgroundImage, convertTextToGraphic(actionString)));
				}
			}
		}
	}

	//-------------------------------------------------------------------------
	
	private BufferedImage convertTextToGraphic(final String text) 
	{
		final Font font = new Font("Arial", Font.BOLD, 12);
		
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        String newline = "";
        String newString = "";
        for (final char c : text.toCharArray())
        {
        	newline += c;
        	if (fm.stringWidth(newline) > imageSize-g2d.getFontMetrics().getHeight())
        	{
        		newString += newline + "\n";
        		newline = "";
        	}
        }
        newString += newline;
        //final int width = fm.stringWidth(text);
        //final int height = fm.getHeight();
        g2d.dispose();

        img = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);

        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setFont(font);
        fm = g2d.getFontMetrics();
        g2d.setColor(Color.BLACK);
        int y = 0;
        for (final String line : newString.split("\n"))
        	g2d.drawString(line, 0, y += g2d.getFontMetrics().getHeight());
        g2d.dispose();
        return img;
    }
	//-------------------------------------------------------------------------

	public List<BufferedImage> allButtonImages() 
	{
		return allButtonImages;
	}
	
	public List<Move> allButtonMoves() 
	{
		return allButtonMoves;
	}
	
}
