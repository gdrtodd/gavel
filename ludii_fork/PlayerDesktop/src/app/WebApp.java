package app;


import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.apache.commons.rng.core.RandomProviderDefaultState;
import org.json.JSONObject;

import app.display.MainWindowWeb;
import app.utils.GameSetup;
import app.utils.GameUtil;
import app.utils.UpdateTabMessages;
import app.views.View;
import game.Game;
import main.Constants;
import main.DatabaseInformation;
import main.collections.FastArrayList;
import manager.ai.AIDetails;
//import manager.network.privateFiles.DatabaseFunctionsPrivate;
import other.GameLoader;
import other.context.Context;
import other.location.Location;
import other.move.Move;
import other.trial.Trial;
import tournament.Tournament;

/**
 * The main player object.
 *
 * @author Matthew.Stephenson
 */
public final class WebApp extends PlayerApp
{

	/** Main view. */
	private MainWindowWeb view;
	
	private final double agentThinkTime = 2.5;	// Needs to be at least a few seconds to prevent latency issues.
	
	private boolean applyingAnimation = false;	// If we are currently calculating animation parameters..
	
	//-------------------------------------------------------------------------

	public void createPlayerApp(final PlayerApp app, final Map<String,String> parameters)
	{
		// Read input parameters
		final String gameDescription = parameters.get("ludeme");
		final String rulesetName = parameters.get("ruleset");
		final int player = Integer.parseInt(parameters.get("player"));
		app.manager().settingsNetwork().setLoginUsername(parameters.get("username"));
		
		manager().setWebApp(true);
		
		// Possible moves on by default
		app.bridge().settingsVC().setShowPossibleMoves(true);
		
		// Invoke UI in the correct thread, otherwise menu may not draw
		view = new MainWindowWeb(Integer.parseInt(parameters.get("width")), Integer.parseInt(parameters.get("height")));

		// All players are Humans initially.
		for (int i = 0; i < Constants.MAX_PLAYERS + 1; i++) // one extra for the shared player
			manager().aiSelected()[i] = new AIDetails(manager(), new JSONObject().put("AI", new JSONObject().put("algorithm", "Human")), i, "Human");
		
		// Load the game.
		loadInitialGame(app, gameDescription, rulesetName);
		
		// Set all but player to UCT after the game has loaded.
		for (int i = 1; i <= app.manager().ref().context().game().players().count(); i++) // one extra for the shared player
		{
			if (i == player)
				continue;

			final JSONObject json = new JSONObject()
					.put("AI", new JSONObject()
					.put("algorithm", "UCT")
					);

			manager().aiSelected()[i] = new AIDetails(manager(), json, i, "UCT");
			manager().aiSelected()[i].setThinkTime(agentThinkTime);
			manager().settingsManager().setMinimumAgentThinkTime(agentThinkTime);
		}
		
		view.createPanels(app);
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

	//-------------------------------------------------------------------------

	/**
	 * @return Main view.
	 */
	public MainWindowWeb view()
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
	public void postMoveUpdates(final Move move, final boolean noAnimation)
	{
		super.postMoveUpdates(move, noAnimation);
		setApplyingAnimation(true);
		
		view.latestDisplayParameters().setPreAnimationBoardImage(view.getviewImage(this));
		view.latestDisplayParameters().setAnimationParameters(settingsPlayer().animationParameters());
	}
	
	@Override
	public void postAnimationUpdates(final Move move)
	{
		super.postAnimationUpdates(move);

		view.latestDisplayParameters().setPostAnimationBoardImage(view.getviewImage(this));

		if (manager().aiSelected()[manager().moverToAgent()].ai() != null && !manager().settingsManager().agentsPaused() && !manager().ref().context().trial().over())
			view.latestDisplayParameters().setContinuation(true);
		else
			view.latestDisplayParameters().setContinuation(false);
		
		// Store the game's result in the database
		storeResultInDatabase();
		
		view.setCalculatingDisplayParameters(false);
		setApplyingAnimation(false);
	}

	private void storeResultInDatabase()
	{
		// Have disabled this for now, until we decide that its safe to merge into the public repo. (LUDII-LMS)
//		if (manager().ref().context().trial().over() && settingsPlayer().isWebGameResultValid())
//		{
//			final Context context =  manager().ref().context();
//			final Game game = context.game();
//
//			view().latestDisplayParameters().setMessage(UpdateTabMessages.gameOverMessage(manager().ref().context(), manager().ref().context().trial()));
//
//			final DatabaseFunctionsPrivate databaseFunctions = new DatabaseFunctionsPrivate();
//			final RandomProviderDefaultState rngState = (RandomProviderDefaultState) context.rng().saveState();
//			String rulesetHeading = game.getRuleset() == null ? "" : game.getRuleset().heading();
//
//			// Used to set the ruleset heading if the default ruleset is selected.
//			if (rulesetHeading.length() == 0 && manager().settingsManager().userSelections().ruleset() != Constants.UNDEFINED)
//				rulesetHeading = game.description().rulesets().get(manager().settingsManager().userSelections().ruleset()).heading();
//
//			databaseFunctions.storeWebTrialInDatabase
//			(
//				game.name(),
//				rulesetHeading,
//				game.description().gameOptions().allOptionStrings(game.getOptions()),
//				DatabaseInformation.getGameId(game.name()),
//				DatabaseInformation.getRulesetId(game.name(), rulesetHeading),
//				settingsPlayer().agentArray(),
//				manager().settingsNetwork().loginUsername(),
//				game.description().raw().hashCode(), new Trial(context.trial()), rngState
//			);
//		}
	}

	@Override
	public void setTemporaryMessage(final String text)
	{
		view.latestDisplayParameters().setMessage(text);
	}
	
	@Override
	public void setVolatileMessage(final String text)
	{
		view.latestDisplayParameters().setMessage(text);
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

	public boolean isApplyingAnimation()
	{
		return applyingAnimation;
	}

	public void setApplyingAnimation(final boolean applyingAnimation)
	{
		this.applyingAnimation = applyingAnimation;
	}

}