package app.display.util;

import java.awt.EventQueue;
import java.awt.Point;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import app.WebApp;
import app.move.MouseHandler;
import app.utils.UpdateTabMessages;
import game.Game;
import other.context.Context;

/**
 * All functions that can/should be called by the web player.
 * 
 * @author Matthew.Stephenson
 */
public class WebPlayerFunctions
{
	
	//-------------------------------------------------------------------------
	
	/**
	 * Click made at a given point.
	 */
	public static void clickedPoint(final WebApp app, final Point p)
	{
		app.setApplyingAnimation(false);
		app.view().latestDisplayParameters().reset();
		
		final int currentMoveNumber = app.manager().ref().context().trial().numMoves();
		
		if (!app.view().checkPointOverlapsButton(app, p))
		{
			MouseHandler.mousePressedCode(app, p);
			MouseHandler.mouseClickedCode(app, p);
			MouseHandler.mouseReleasedCode(app, p);
		}
		
		app.view().latestDisplayParameters().setPreAnimationBoardImage(app.view().getviewImage(app));
		app.view().latestDisplayParameters().setPostAnimationBoardImage(app.view().getviewImage(app));

		EventQueue.invokeLater(() -> 
		{
			final Context context =  app.manager().ref().context();
			final Game game = context.game();
			
			// Update the players which have been humans.
			for (int i = 0; i <= game.players().count(); i++)
			{
				if (app.manager().aiSelected()[app.manager().playerToAgent(i)].ai() != null)
					app.settingsPlayer().setAgentArray(i, true);
				else
					app.settingsPlayer().setAgentArray(i, false);
			}
			
			if(!app.isApplyingAnimation())
			{
				if (currentMoveNumber == app.manager().ref().context().trial().numMoves() || app.manager().undoneMoves().size() > 0)
				{
					if (app.manager().aiSelected()[app.manager().moverToAgent()].ai() == null || app.manager().settingsManager().agentsPaused())
						app.view().setCalculatingDisplayParameters(false);
				}
				
				if (context.trial().over())
				{
					if (app.manager().isWebApp())
						app.setTemporaryMessage(UpdateTabMessages.gameOverMessage(app.manager().ref().context(), app.manager().ref().context().trial()));
					
					app.view().setCalculatingDisplayParameters(false);
				}
			}
		});
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Gets the current display parameters.
	 */
	public static DisplayParameters getDisplayParameters(final WebApp app)
	{
		
		final Future<DisplayParameters> future;
		
		try
		{
			final ExecutorService executor = Executors.newFixedThreadPool(1);
			final CountDownLatch latch = new CountDownLatch(1);
			
			future =
			(
				executor.submit
				(
					() -> 
					{
						while (app.view().isCalculatingDisplayParameters())
							Thread.sleep(100);
						
						latch.countDown();
						
						app.view().setCalculatingDisplayParameters(true);
						return app.view().latestDisplayParameters();
					}
				)
			);
			
			latch.await();
			executor.shutdown();
			
			return future.get();
		}
		catch (final Exception E)
		{
			return app.view().latestDisplayParameters();
		}
	}
	
	//-------------------------------------------------------------------------
	
}
