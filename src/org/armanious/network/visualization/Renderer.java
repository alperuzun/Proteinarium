package org.armanious.network.visualization;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.armanious.graph.Edge;

public class Renderer<K> {

	private final BufferedImage image;
	
	public Renderer(ForceDirectedLayout<K> fd){
		this(fd, k -> k.toString(), k -> Color.RED, e -> Color.BLACK);
	}

	public Renderer(ForceDirectedLayout<K> fdl, Function<K, String> label, Function<K, Color> nodeColor, Function<Edge<K>, Color> edgeColor){
		final double padding = 0.25;
		
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		double sumX = 0;
		double sumY = 0;
		for(int i = 0; i < fdl.positions.length; i++){
			final Point2D.Double pos = fdl.positions[i];
			final double radius = fdl.radii[i];
			if(pos.x - radius < minX) minX = pos.x - radius;
			if(pos.x + radius > maxX) maxX = pos.x + radius;
			if(pos.y - radius < minY) minY = pos.y - radius;
			if(pos.y + radius > maxY) maxY = pos.y + radius;
			sumX += pos.x;
			sumY += pos.y;
		}
		final double centerX = sumX / fdl.positions.length;
		final double centerY = sumY / fdl.positions.length;

		final int width = (int) Math.ceil((maxX - minX) * (1 + padding));
		final int height = (int) Math.ceil((maxY - minY) * (1 + padding));

		//want half the padding translated
		final double translationX = width * 0.5 - centerX; 
		final double translationY = height * 0.5 - centerY;

		final Point2D.Double[] positions = new Point2D.Double[fdl.positions.length];
		for(int i = 0; i < positions.length; i++){
			final Point2D.Double old = fdl.positions[i];
			//invert horizontally to account for graphic's library y-axis
			positions[i] = new Point2D.Double(old.x + translationX, height - (old.y + translationY));
		}
		
		if(width * height < 0){
			System.out.println("Integer overflow error; (" + width + ", " + height + ")");
			throw new RuntimeException();
		}
		
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHints(new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON));
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, width, height);
		g.setComposite(AlphaComposite.Src);
		
		for(int i = 0; i < positions.length; i++){
			final Point2D.Double start = positions[i];
			final Line2D.Double line = new Line2D.Double(start.x, start.y, 0, 0);
			for(int end : fdl.neighbors[i]){
				line.setLine(start, positions[end]);
				g.setColor(edgeColor.apply(new Edge<K>(fdl.nodes[i], fdl.nodes[end])));
				g.draw(line);
			}
		}
		g.setFont(g.getFont().deriveFont(14f));
		final Ellipse2D.Double node = new Ellipse2D.Double();
	    final FontMetrics metrics = g.getFontMetrics(g.getFont());
		for(int i = 0; i < positions.length; i++){
			node.setFrame(positions[i].x - fdl.radii[i], positions[i].y - fdl.radii[i], fdl.radii[i] * 2, fdl.radii[i] * 2);
			g.setColor(nodeColor.apply(fdl.nodes[i]));
			g.fill(node);
			g.setColor(Color.BLACK);
			g.draw(node);
			
			final String nodeLabel = label.apply(fdl.nodes[i]);
			final int strWidth = metrics.stringWidth(nodeLabel);
			final int strHeight = metrics.getHeight();
			g.drawString(nodeLabel,
					(float) (positions[i].x - fdl.radii[i] * 0.9 + (1.8 * fdl.radii[i] - strWidth) * 0.5),
					(float) (positions[i].y - strHeight * 0.5 + metrics.getAscent()));
		}
	}

	public void saveTo(File file) throws IOException {
		saveTo(new FileOutputStream(file));
	}

	public void saveTo(OutputStream out) throws IOException {
		final BufferedOutputStream bos = (BufferedOutputStream) (out instanceof BufferedOutputStream ? out : new BufferedOutputStream(out));
		ImageIO.write(image, "PNG", bos);
	}

}
