package org.armanious.network.visualization;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.armanious.network.Configuration.RendererConfig;
import org.armanious.network.analysis.NetworkAnalysis;

public class RenderingUtils {

	private static BufferedImage graphLegendImage;
	private static BufferedImage dendrogramLegendImage;

	private static final int GRAPH_NUM_LEGEND_BARS = 5;
	private static final int DENDROGRAM_NUM_LEGEND_BARS = 2;

	private static final int LEGEND_BAR_WIDTH = 40;
	private static final int LEGEND_BAR_HEIGHT = 200;
	private static final int LEGEND_BAR_PADDING = 20;

	private static final int GRAPH_LEGEND_WIDTH = LEGEND_BAR_WIDTH * GRAPH_NUM_LEGEND_BARS + LEGEND_BAR_PADDING * (GRAPH_NUM_LEGEND_BARS + 1);
	private static final int DENDROGRAM_LEGEND_WIDTH = LEGEND_BAR_WIDTH * DENDROGRAM_NUM_LEGEND_BARS + LEGEND_BAR_PADDING * DENDROGRAM_NUM_LEGEND_BARS;
	private static final int LEGEND_HEIGHT = LEGEND_BAR_HEIGHT + LEGEND_BAR_PADDING * 2;
	
	private RenderingUtils(){}

	public static enum Direction {
		NORTH,
		EAST,
		SOUTH,
		WEST
	}

	public static enum Justification {
		LEADING,
		CENTER,
		TRAILING
	}

	public static BufferedImage appendImage(RendererConfig rc, BufferedImage original,
			BufferedImage appendage, Direction direction, Justification justification){
		//TODO auto-reverse call
		switch(direction){
		case NORTH:
		case SOUTH:
			if(appendage.getWidth() > original.getWidth())
				return appendImage(rc, appendage, original, direction == Direction.NORTH ? Direction.SOUTH : Direction.NORTH, justification);
			break;
		case EAST:
		case WEST:
			if(appendage.getHeight() > original.getHeight())
				return appendImage(rc, appendage, original, direction == Direction.EAST ? Direction.WEST : Direction.EAST, justification);
			break;
		}
		
		int width = -1;
		int height = -1;
		int ox = -1;
		int oy = -1;
		int ax = -1;
		int ay = -1;

		switch(direction){
		case NORTH:
			width = original.getWidth();
			height = original.getHeight() + appendage.getHeight();
			ox = 0;
			oy = appendage.getHeight();
			ay = 0;
			break;
		case EAST:
			width = original.getWidth() + appendage.getWidth();
			height = original.getHeight();
			ox = 0;
			oy = 0;
			ax = original.getWidth();
			break;
		case SOUTH:
			width = Math.max(original.getWidth(), appendage.getWidth());
			height = original.getHeight() + appendage.getHeight();
			ox = 0;
			oy = 0;
			ay = original.getHeight();
			break;
		case WEST:
			width = original.getWidth() + appendage.getWidth();
			height = Math.max(original.getHeight(), appendage.getHeight());
			ox = appendage.getWidth();
			oy = 0;
			ax = 0;
			break;
		}

		switch(direction){
		case NORTH:
		case SOUTH:
			switch(justification){
			case LEADING:
				ax = 0;
				break;
			case CENTER:
				ax = (original.getWidth() - appendage.getWidth()) / 2;
				break;
			case TRAILING:
				ax = width - appendage.getWidth();
				break;
			}
			break;
		case EAST:
		case WEST:
			switch(justification){
			case LEADING:
				ay = 0;
				break;
			case CENTER:
				ay = (original.getHeight() - appendage.getHeight()) / 2;
				break;
			case TRAILING:
				ay = height - appendage.getHeight();
				break;
			}
		}
		
		if(!(ox >= 0 && oy >= 0 && ax >= 0 && ay >= 0)){
			System.out.println("direction = " + direction + "\njustification = " + justification + "\nox = " + ox + "\noy = " + oy + "\nax = " + ax + "\nay = " + ay);
			assert(ox >= 0 && oy >= 0 && ax >= 0 && ay >= 0);
		}

		final BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = prepareBufferedImageGraphics(rc, bi);

		g.drawImage(original, ox, oy, original.getWidth(), original.getHeight(), null);
		g.drawImage(appendage, ax, ay, appendage.getWidth(), appendage.getHeight(), null);
		return bi;
	}


	public static BufferedImage addLegend(RendererConfig rc, BufferedImage image, boolean isDendrogram){
		return appendImage(rc, image, getLegendImage(rc, isDendrogram), Direction.EAST, Justification.CENTER);
	}

	public static Graphics2D prepareBufferedImageGraphics(RendererConfig rc, BufferedImage bi){
		final Graphics2D g = (Graphics2D) bi.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		g.setColor(Color.WHITE);
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g.setComposite(AlphaComposite.SrcOver);

		g.setColor(Color.BLACK);

		return g;
	}

	private static BufferedImage getLegendImage(RendererConfig rc, boolean isDendrogram){
		if(graphLegendImage == null || dendrogramLegendImage == null){
			//graphLegendImage
			{
				final BufferedImage bi = new BufferedImage(GRAPH_LEGEND_WIDTH, LEGEND_HEIGHT, BufferedImage.TYPE_INT_ARGB);
				final Graphics2D g = prepareBufferedImageGraphics(rc, bi);

				//g.drawRect(0, 0, LEGEND_WIDTH-1, LEGEND_HEIGHT-1);
				final Color[] colors = new Color[]{
						null,
						NetworkAnalysis.parseColorOrDefault(rc.group1VertexColor, null),
						NetworkAnalysis.parseColorOrDefault(rc.group2VertexColor, null),
						NetworkAnalysis.parseColorOrDefault(rc.bothGroupsVertexColor, null),
						NetworkAnalysis.parseColorOrDefault(rc.defaultVertexColor, null)};
				final String[] labels = new String[]{
						null,
						"Group 1",
						"Group 2",
						"Both",
						"Imputed"
				};
				final FontMetrics fm = g.getFontMetrics();
				final Consumer<Integer> ithLegendBar = (i) -> {
					if(i == 0){
						try {
							final BufferedImage cur = ImageIO.read(RenderingUtils.class.getResource("/resources/count_gradient.png"));
							g.drawImage(cur, LEGEND_BAR_PADDING + (LEGEND_BAR_WIDTH + LEGEND_BAR_PADDING) * i, LEGEND_BAR_PADDING, null);
						} catch (IOException e) {
							return;
						}
					}else{
						final Color c1 = colors[i];
						final Color c2 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), rc.minVertexAlpha);
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
					}
				};
				for(int i = 0; i < GRAPH_NUM_LEGEND_BARS; i++)
					ithLegendBar.accept(i);
				graphLegendImage = bi;
			}
			
			//dendrogramLegendImage
			{
				final BufferedImage bi = new BufferedImage(DENDROGRAM_LEGEND_WIDTH, LEGEND_HEIGHT, BufferedImage.TYPE_INT_ARGB);
				final Graphics2D g = prepareBufferedImageGraphics(rc, bi);
				g.drawImage(graphLegendImage, 0, 0, bi.getWidth(), bi.getHeight(),
						LEGEND_BAR_WIDTH + (int) (LEGEND_BAR_PADDING * 1.5),
						0,
						LEGEND_BAR_WIDTH * 3 + (int) (LEGEND_BAR_PADDING * 3.5),
						LEGEND_HEIGHT, null);
				dendrogramLegendImage = bi;
			}
		}
		return isDendrogram ? dendrogramLegendImage : graphLegendImage;
	}

	private static final Font TITLE_FONT = new Font("Dialog", Font.PLAIN, 28);

	private static BufferedImage createTitleImage(RendererConfig rc, String title){
		return createTextImage(rc, title.replace('_', ' '), TITLE_FONT, Color.BLACK);
	}

	private static BufferedImage createTextImage(RendererConfig rc, String text, Font font, Color color){
		BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bi.getGraphics();
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();

		bi = new BufferedImage(fm.stringWidth(text), fm.getHeight(), BufferedImage.TYPE_INT_ARGB);
		g = prepareBufferedImageGraphics(rc, bi);
		g.setFont(font);
		g.setColor(color);
		g.drawString(text, 0, fm.getMaxAscent());
		return bi;
	}

	public static BufferedImage addTitle(RendererConfig rc, BufferedImage image, String title){
		return appendImage(rc, image, createTitleImage(rc, title), Direction.NORTH, Justification.CENTER);
	}

	public static BufferedImage postProcess(RendererConfig rc, BufferedImage image, String title, boolean isDendrogram) {
		return addTitle(rc, addLegend(rc, image, isDendrogram), title);
	}

}
