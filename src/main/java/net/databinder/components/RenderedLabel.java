package net.databinder.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import wicket.Component;
import wicket.Resource;
import wicket.WicketRuntimeException;
import wicket.markup.ComponentTag;
import wicket.markup.html.image.Image;
import wicket.markup.html.image.resource.RenderedDynamicImageResource;
import wicket.model.ICompoundModel;
import wicket.model.IModel;

/**
 * Renders its model text into a PNG, using any typeface available to the JVM. The size of 
 * the image is determined by the model text and the characteristics of font selected. The
 * default font is 14pt sans, plain black on a white background. The image's alt attribute
 * will be set to the model text. <p>This class is inspired by, and draws code from, Wicket's 
 * DefaultButtonImageResource. </p>
 * @author Nathan Hamblen
 * @see wicket.markup.html.image.resource.DefaultButtonImageResource
 */
public class RenderedLabel extends Image {

	private Color backgroundColor = Color.WHITE;
	private Color color = Color.BLACK;
	private Font font = new Font("sans", Font.PLAIN, 14);
	private Integer maxWidth;
	
	private RenderedTextImageResource resource;
	
	/**
	 * Constructor to be used if model is derived from a compound property model. The 
	 * model object <b>must</b> be a string.
	 * @param id
	 */
	public RenderedLabel(String id) {
		super(id);
		setImageResource(resource = new RenderedTextImageResource());
	}
	
	/**
	 * Constructor with explicit model. The model object <b>must</b> be a string.
	 * @param id Wicket id
	 * @param model model for 
	 */
	public RenderedLabel(String id, IModel model) {
		super(id, model);
		setImageResource(resource = new RenderedTextImageResource());
	}
	
	/** Adds alt attribute for accessibility. */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		tag.put("alt", getText());
	}
	
	/** Restores  compound model resolution that is disabled in  the Image superclass. */
	@Override
	protected IModel initModel() {
		// c&p'd from Component 
		for (Component current = getParent(); current != null; current = current.getParent())
		{
			final IModel model = current.getModel();
			if (model instanceof ICompoundModel)
			{
				setVersioned(false);
				return model;
			}
		}
		return null;
	}
	
	/**
	 * Inner class that renders the model text into an image  resource.
	 * @see wicket.markup.html.image.resource.DefaultButtonImageResource
	 */
	protected class RenderedTextImageResource extends RenderedDynamicImageResource
	{
		public RenderedTextImageResource()
		{
			super(10, 10,"png");	// tiny default that will resize to fit text
		}
		
		/** Renders text into image. */
		protected boolean render(final Graphics2D graphics)
		{
			String text = getText(); // get text from outer class model
			final int width = getWidth(), height = getHeight();

			// Fill background
			graphics.setColor(backgroundColor);
			graphics.fillRect(0, 0, width, height);

			if (text == null)
				return true;	// no text? we're done here
			
			// Get size of text
			graphics.setFont(font);
			final FontMetrics metrics = graphics.getFontMetrics();
			
			List<String> lines = new LinkedList<String>();
			
			int dxText = breakLines(text, metrics, lines),
				lineHeight = metrics.getHeight(),
				dyText = lineHeight * lines.size();
			
			// grow and redraw if we need to
			if (dxText > width || dyText > height)
			{
				setWidth(dxText);
				setHeight(dyText);
				return false;
			}
			
			// Turn on anti-aliasing
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			graphics.setColor(color);
			
			// Draw each line at its baseline
			int baseline = metrics.getAscent();
			for (String line : lines) {
				graphics.drawString(line, 0, baseline);
				baseline += lineHeight;
			}
			return true;
		}

		/**
		 * Breaks source string into lines no longer than this label's maxWidth, if not null. 
		 * @param source this label's model, previously retrieved
		 * @param metrics metrics for the font we will use for display
		 * @param outputLines list to receive lines generated by this function
		 * @return length in pixels of the longest line
		 */
		protected int breakLines(String source, FontMetrics metrics, List<String> outputLines) {
			if (maxWidth == null) {
				outputLines.add(source);
				return metrics.stringWidth(source);
			}
			String sp = " ";
			String words[] = source.split(sp);
			StringBuilder line = new StringBuilder();
			int topWidth = 0;
			for (String word : words) {
				if (line.length() >0) {
					 int curWidth = metrics.stringWidth(line + sp + word);
					 if (curWidth > maxWidth) {
						 outputLines.add(line.toString());
						 line.setLength(0);
					 } else
						 line.append(sp);
				}
				 line.append(word);
				 topWidth = Math.max(metrics.stringWidth(line.toString()), topWidth);
			}
			outputLines.add(line.toString());
			return topWidth;
		}
	}
	
	/** @return String to be rendered */
	protected String getText() {
		try {
			return (String) getModelObject();
		} catch (ClassCastException e) {
			throw new WicketRuntimeException("A RenderedLabel's model object MUST be a String.", e);
		}
	}
	
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	public RenderedLabel setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
		resource.invalidate();
		return this;
	}

	public Color getColor() {
		return color;
	}

	/** @param color Color to print text */
	public RenderedLabel setColor(Color color) {
		this.color = color;
		resource.invalidate();
		return this;
	}

	public Font getFont() {
		return font;
	}

	public RenderedLabel setFont(Font font) {
		this.font = font;
		resource.invalidate();
		return this;
	}
	

	public Integer getMaxWidth() {
		return maxWidth;
	}

	/**
	 * Specify a maximum pixel width, causing longer renderings to wrap.
	 * @param maxWidth maximum width in pixels
	 * @return this, for chaining
	 */
	public RenderedLabel setMaxWidth(Integer maxWidth) {
		this.maxWidth = maxWidth;
		resource.invalidate();
		return this;
	}

	/**
	 * Utility method for creating Font objects from resources.
	 * @param fontRes Resource containing  a TrueType font descriptor.
	 * @return Plain, 16pt font derived from the resource.
	 */
	public static Font fontForResource(Resource fontRes) {
		try {
			InputStream is = fontRes.getResourceStream().getInputStream();
			Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			is.close();
			return font.deriveFont(Font.PLAIN, 16);
		} catch (Throwable e) {
			throw new WicketRuntimeException("Error loading font resources", e);
		}
	}
}