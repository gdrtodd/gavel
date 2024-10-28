package app.headless;


import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import app.PlayerApp;
import app.move.MouseHandler;
import org.json.JSONObject;

import app.utils.GameSetup;
import app.utils.GameUtil;
import app.views.View;
import main.Constants;
import main.collections.FastArrayList;
import manager.ai.AIDetails;
import other.GameLoader;
import other.context.Context;
import other.location.Location;
import other.move.Move;
import tournament.Tournament;
import java.io.*;

/**
 * The main player object.
 *
 * @author Matthew.Stephenson
 */
public final class HeadlessApp extends PlayerApp
{

	/** Main view. */
	private MainWindowHeadless view;
	private final double agentThinkTime = 3.0;	// Needs to be at least a few seconds to prevent latency issues.
	
	//-------------------------------------------------------------------------

	public HeadlessApp(final int width, final int height, String game, final String rulesetName)
	{
		// Read input parameters
//		app.manager().settingsNetwork().setLoginUsername(parameters.get("username"));
		
		manager().setWebApp(true);
		
		// Invoke UI in the correct thread, otherwise menu may not draw
		view = new MainWindowHeadless(width, height);

		// All players are Humans initially.
		for (int i = 0; i < Constants.MAX_PLAYERS + 1; i++) // one extra for the shared player
			manager().aiSelected()[i] = new AIDetails(manager(), new JSONObject().put("AI", new JSONObject().put("algorithm", "Human")), i, "Human");
		
		// Load the game.
		loadInitialGame(this, game, null);
		
		// Set all but player 1 to Ludii AI after the game has loaded.
//		for (int i = 2; i <= app.manager().ref().context().game().players().count(); i++) // one extra for the shared player
//		{
//			final JSONObject json = new JSONObject()
//					.put("AI", new JSONObject()
//					.put("algorithm", "Ludii AI")
//					);
//
//			manager().aiSelected()[i] = new AIDetails(manager(), json, i, "Ludii AI");
//			manager().aiSelected()[i].setThinkTime(agentThinkTime);
//			manager().settingsManager().setMinimumAgentThinkTime(agentThinkTime);
//		}
		
		view.createPanels(this);
	}

	//-------------------------------------------------------------------------

	private void loadInitialGame(final PlayerApp app, final String gameDescription, final String rulesetName)
	{
		GameSetup.compileAndShowGame(this, gameDescription, false);

		// If a ruleset was selected, need to recompile game.
		if (rulesetName != null && !rulesetName.isEmpty())
			if (GameUtil.checkMatchingRulesets(app, app.manager().ref().context().game(), rulesetName))
				GameSetup.compileAndShowGame(this, gameDescription, false);
	}

	/**
	 * Click made at a given point.
	 */
	public void clickedPoint(final Point p)
	{
		MouseHandler.mousePressedCode(this, p);
		MouseHandler.mouseClickedCode(this, p);
		MouseHandler.mouseReleasedCode(this, p);
	}

	//-------------------------------------------------------------------------

	/**
	 * @return Main view.
	 */
	public MainWindowHeadless view()
	{
		return view;
	}
	
	//-------------------------------------------------------------------------

	@Override
	public void updateFrameTitle(final boolean alsoUpdateMenu)
	{
		// nothing
	}

	@Override
	public void refreshNetworkDialog()
	{
		//nothing
	}
	
	@Override
	public Tournament tournament() 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTournament(final Tournament tournament) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(final ActionEvent arg0) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void itemStateChanged(final ItemEvent arg0) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportError(final String error) 
	{
		// TODO Auto-generated method stub
		
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public void loadGameFromName(final String name, final List<String> options, final boolean debug)
	{
		// nothing
	}

	@Override
	public JSONObject getNameFromJar()
	{
		return null;
	}
	
	@Override
	public JSONObject getNameFromJson()
	{
		return null;
	}
	
	@Override
	public JSONObject getNameFromAiDef()
	{
		return null;
	}

	@Override
	public void addTextToStatusPanel(final String text)
	{
		//nothing
	}
	
	@Override
	public void addTextToAnalysisPanel(final String text)
	{
		//nothing
	}

	@Override
	public void showPuzzleDialog(final int site)
	{
		//nothing
	}

	@Override
	public void showPossibleMovesDialog(final Context context, final FastArrayList<Move> possibleMoves)
	{
		view.possibleMoves().calculateButtonImages(this, context, possibleMoves);
	}

	@Override
	public void selectAnalysisTab()
	{
		//nothing
	}

	@Override
	public void repaint()
	{
		//nothing
	}

	@Override
	public void reportDrawAgreed()
	{
		//nothing
	}
	
	@Override
	public void reportForfeit(final int playerForfeitNumber)
	{		
		//nothing
	}

	@Override
	public void reportTimeout(final int playerForfeitNumber)
	{
		//nothing
	}

	@Override
	public void updateTabs(final Context context)
	{
		//nothing
	}

	@Override
	public void playSound(final String soundName)
	{
		//nothing
	}

	@Override
	public void setVolatileMessage(String text)
	{
		//nothing
	}

	@Override
	public void saveTrial()
	{
		//nothing
	}
	
	//-------------------------------------------------------------------------

	@Override
	public void repaintTimerForPlayer(final int playerId)
	{
		//nothing
	}

	@Override
	public void setTemporaryMessage(String text)
	{
		//nothing
	}

	@Override
	public void repaintComponentBetweenPoints(final Context context, final Location moveFrom, final Point startPoint, final Point endPoint) 
	{
		repaint();
	}

	@Override
	public void writeTextToFile(final String fileName, final String log) 
	{
		//nothing
	}

	@Override
	public void resetMenuGUI() 
	{
		//nothing
	}

	@Override
	public void showSettingsDialog() 
	{
		//nothing
	}

	@Override
	public void showOtherDialog(final FastArrayList<Move> otherPossibleMoves) 
	{
		view.possibleMoves().calculateButtonImages(this, contextSnapshot().getContext(this), otherPossibleMoves);
	}

	@Override
	public void showInfoDialog() 
	{
		//nothing
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public int width()
	{
		return view.width();
	}

	@Override
	public int height()
	{
		return view.height();
	}
	
	@Override
	public Rectangle[] playerSwatchList()
	{
		return view.playerSwatchList();
	}

	@Override
	public Rectangle[] playerNameList()
	{
		return view.playerNameList();
	}
	
	@Override
	public boolean[] playerSwatchHover()
	{
		return new boolean[Constants.MAX_PLAYERS+1];
	}

	@Override
	public boolean[] playerNameHover()
	{
		return new boolean[Constants.MAX_PLAYERS+1];
	}
	
	@Override
	public List<View> getPanels()
	{
		return view.getPanels();
	}

	@Override
	public void repaint(final Rectangle rect) 
	{
		//nothing
	}

}