package app.display.util;

import java.awt.image.BufferedImage;

import app.move.animation.AnimationParameters;

/**
 * Display parameters that are passed to the web side client.
 * Consists of:
 * - Animation parameters for last made move.
 * - Message to be displayed in browser.
 * 
 * @author Matthew.Stephenson
 */
public class DisplayParameters 
{
	private BufferedImage preAnimationBoardImage = null;
	private BufferedImage postAnimationBoardImage = null;
	private AnimationParameters animationParameters = new AnimationParameters();
	private String message = "";
	private boolean continuation = false;
	
	public void reset()
	{
		preAnimationBoardImage = null;
		postAnimationBoardImage = null;
		animationParameters = new AnimationParameters();
		message = "";
		continuation = false;
	}
	
	public BufferedImage preAnimationBoardImage() 
	{
		return preAnimationBoardImage;
	}
	
	public void setPreAnimationBoardImage(final BufferedImage preAnimationBoardImage) 
	{
		this.preAnimationBoardImage = preAnimationBoardImage;
	}
	
	public BufferedImage postAnimationBoardImage() 
	{
		return postAnimationBoardImage;
	}
	
	public void setPostAnimationBoardImage(final BufferedImage postAnimationBoardImage) 
	{
		this.postAnimationBoardImage = postAnimationBoardImage;
	}
	
	public AnimationParameters animationParameters() 
	{
		return animationParameters;
	}
	
	public void setAnimationParameters(final AnimationParameters animationParameters) 
	{
		this.animationParameters = animationParameters;
	}
	
	public String message() 
	{
		return message;
	}
	
	public void setMessage(final String message) 
	{
		this.message = message;
	}
	
	public boolean continuation() 
	{
		return continuation;
	}
	
	public void setContinuation(final boolean continuation) 
	{
		this.continuation = continuation;
	}
}
