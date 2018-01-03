package org.armanious.network.visualization;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import org.armanious.network.Configuration.RendererConfig;
import org.armanious.network.analysis.NetworkAnalysis;

public class RenderingUtils {
	
	private static final int LEGEND_BAR_WIDTH = 40;
	private static final int LEGEND_BAR_HEIGHT = 200;
	private static final int LEGEND_BAR_PADDING = 20;
	
	private static final int LEGEND_WIDTH = LEGEND_BAR_WIDTH * 4 + LEGEND_BAR_PADDING * 5;
	private static final int LEGEND_HEIGHT = LEGEND_BAR_HEIGHT + LEGEND_BAR_PADDING * 2;
	
	private RenderingUtils(){}
	
	public static BufferedImage addLegend(RendererConfig rc, BufferedImage image){
		final BufferedImage legend = createLegendImage(rc);
		
		final BufferedImage bi = new BufferedImage(
				image.getWidth() + legend.getWidth(),
				Math.max(image.getHeight(), legend.getHeight()),
				BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = (Graphics2D) bi.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		
		g.setColor(NetworkAnalysis.parseColorOrDefault(rc.backgroundColor, Color.WHITE));
		if(rc.transparentBackground) g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g.setComposite(AlphaComposite.SrcOver);
		
		g.drawImage(image, 0, 0, null);
		g.drawImage(legend, image.getWidth(), 0, null);
		
		return bi;
	}
	
	private static BufferedImage createLegendImage(RendererConfig rc){
		final BufferedImage bi = new BufferedImage(LEGEND_WIDTH, LEGEND_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = (Graphics2D) bi.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		
		g.setColor(NetworkAnalysis.parseColorOrDefault(rc.backgroundColor, Color.WHITE));
		if(rc.transparentBackground) g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g.setComposite(AlphaComposite.SrcOver);
		
		g.setColor(Color.BLACK);
		//g.drawRect(0, 0, LEGEND_WIDTH-1, LEGEND_HEIGHT-1);
		final Color[] colors = new Color[]{
				NetworkAnalysis.parseColorOrDefault(rc.primaryGroupNodeColor, null),
				NetworkAnalysis.parseColorOrDefault(rc.secondaryGroupNodeColor, null),
				NetworkAnalysis.parseColorOrDefault(rc.bothGroupsNodeColor, null),
				NetworkAnalysis.parseColorOrDefault(rc.defaultNodeColor, null)};
		final String[] labels = new String[]{
				"Primary",
				"Secondary",
				"Both",
				"Neither"
		};
		final FontMetrics fm = g.getFontMetrics();
		final Consumer<Integer> ithLegendBar = (i) -> {
			final Color c1 = colors[i];
			final Color c2 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), rc.varyNodeAlphaValues ? rc.minNodeAlpha : 255);
			g.setPaint(new GradientPaint(
					LEGEND_BAR_PADDING + LEGEND_BAR_WIDTH * (i + 0.5f), LEGEND_BAR_PADDING,
					c1,
					LEGEND_BAR_PADDING + LEGEND_BAR_WIDTH * (i + 0.5f), LEGEND_BAR_PADDING + LEGEND_BAR_HEIGHT,
					c2));
			g.fillRect(LEGEND_BAR_PADDING + (LEGEND_BAR_WIDTH + LEGEND_BAR_PADDING) * i, LEGEND_BAR_PADDING, LEGEND_BAR_WIDTH, LEGEND_BAR_HEIGHT);
			g.setColor(Color.BLACK);
			g.drawRect(LEGEND_BAR_PADDING + (LEGEND_BAR_WIDTH + LEGEND_BAR_PADDING) * i, LEGEND_BAR_PADDING, LEGEND_BAR_WIDTH, LEGEND_BAR_HEIGHT);
			final String label = labels[i];
			float startx = LEGEND_BAR_PADDING * 0.5f + (LEGEND_BAR_WIDTH + LEGEND_BAR_PADDING) * i;
			float starty = LEGEND_BAR_PADDING + LEGEND_BAR_HEIGHT + fm.getHeight() + 2;
			float dif = (LEGEND_BAR_PADDING + LEGEND_BAR_WIDTH - fm.stringWidth(label)) / 2;
			g.drawString(label, startx + dif, starty);
		};
		for(int i = 0; i < 4; i++)
			ithLegendBar.accept(i);
		
		return bi;
	}

}
