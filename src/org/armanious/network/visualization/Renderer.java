package org.armanious.network.visualization;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
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
	
	private static final Font DEFAULT_FONT = new Font("Dialog", Font.PLAIN, 12);
	private final ForceDirectedLayout<K> fdl;
	
	private Function<K, String> labelFunction = k -> String.valueOf(k);
	private Function<K, Font> labelFontFunction = k -> DEFAULT_FONT;
	private Function<K, Color> labelColorFunction = k -> Color.BLACK;
	
	private Function<K, Color> nodeColorFunction = k -> Color.RED;
	//private Function<K, Float> nodeOpaquenessFunction = k -> 1f;
	
	private Function<Edge<K>, Color> edgeColorFunction = k -> Color.BLACK;
	private Function<Edge<K>, Float> edgeThicknessFunction = k -> 1f;
	
	private Function<K, Color> nodeBorderColorFunction = k -> Color.BLACK;
	private Function<K, Float> nodeBorderThicknessFunction = k -> 1f;
	
	private BufferedImage image = null;
	
	public Renderer(ForceDirectedLayout<K> fdl){
		this.fdl = fdl;
	}
	
	private void redraw(){
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
		
		if((long) width * height > Integer.MAX_VALUE){
			System.out.println("Integer overflow error; (" + width + ", " + height + ")");
			throw new RuntimeException();
		}
		
		System.out.println("Attempting to create buffered image with width=" + width + ", height=" + height);
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHints(new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON));
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, width, height);
		g.setComposite(AlphaComposite.SrcOver);
		
		//TODO cache BasicStroke objects by width
		for(int i = 0; i < positions.length; i++){
			final Point2D.Double start = positions[i];
			final Line2D.Double line = new Line2D.Double(start.x, start.y, 0, 0);
			for(int j = 0; j < fdl.neighbors[i].length; j++){
				line.setLine(start, positions[fdl.neighbors[i][j]]);
				final Edge<K> edge = fdl.edges.get(i).get(j);
				g.setColor(edgeColorFunction.apply(edge));
				g.setStroke(new BasicStroke(edgeThicknessFunction.apply(edge)));
				g.draw(line);
			}
		}
		final Ellipse2D.Double node = new Ellipse2D.Double();
		for(int i = 0; i < positions.length; i++){
			node.setFrame(positions[i].x - fdl.radii[i], positions[i].y - fdl.radii[i], fdl.radii[i] * 2, fdl.radii[i] * 2);
			g.setColor(nodeColorFunction.apply(fdl.nodes[i]));
			g.setComposite(AlphaComposite.Clear);
			g.fill(node);
			g.setComposite(AlphaComposite.SrcOver);
			g.fill(node);
			g.setColor(nodeBorderColorFunction.apply(fdl.nodes[i]));
			g.setStroke(new BasicStroke(nodeBorderThicknessFunction.apply(fdl.nodes[i])));
			g.draw(node);
			
			g.setFont(labelFontFunction.apply(fdl.nodes[i]));
		    final FontMetrics metrics = g.getFontMetrics(g.getFont());
		    
			final String nodeLabel = labelFunction.apply(fdl.nodes[i]);
			//nodeLabel += " (" + fdl.neighbors[i].length + ", " + Math.round(fdl.radii[i]*100)/100.0 + ")";
			final int strWidth = metrics.stringWidth(nodeLabel);
			final int strHeight = metrics.getHeight();
			g.setColor(labelColorFunction.apply(fdl.nodes[i]));
			//TODO FIXME change font until (and cache results) until size fits
			g.drawString(nodeLabel,
					(float) (positions[i].x - fdl.radii[i] * 0.9 + (1.8 * fdl.radii[i] - strWidth) * 0.5),
					(float) (positions[i].y - strHeight * 0.5 + metrics.getAscent()));
		}
	}
	
	public void setLabelFunction(Function<K, String> labelFunction){
		this.labelFunction = labelFunction;
		image = null;
	}
	
	public void setLabelFontFunction(Function<K, Font> labelFontFunction){
		this.labelFontFunction = labelFontFunction;
		image = null;
	}
	
	public void setLabelColorFunction(Function<K, Color> labelColorFunction){
		this.labelColorFunction = labelColorFunction;
		image = null;
	}
	
	public void setNodeColorFunction(Function<K, Color> nodeColorFunction){
		this.nodeColorFunction = nodeColorFunction;
		image = null;
	}
	
	/*public void setNodeOpaquenessFunction(Function<K, Float> nodeOpaquenessFunction){
		this.nodeOpaquenessFunction = nodeOpaquenessFunction;
		image = null;
	}*/
	
	public void setEdgeColorFunction(Function<Edge<K>, Color> edgeColorFunction){
		this.edgeColorFunction = edgeColorFunction;
		image = null;
	}
	
	public void setEdgeThicknessFunction(Function<Edge<K>, Float> edgeThicknessFunction){
		this.edgeThicknessFunction = edgeThicknessFunction;
		image = null;
	}
	
	public void setNodeBorderColorFunction(Function<K, Color> nodeBorderColorFunction){
		this.nodeBorderColorFunction = nodeBorderColorFunction;
		image = null;
	}
	
	public void setNodeBorderThicknessFunction(Function<K, Float> nodeBorderThicknessFunction){
		this.nodeBorderThicknessFunction = nodeBorderThicknessFunction;
		image = null;
	}

	public void saveTo(File file) throws IOException {
		saveTo(new FileOutputStream(file));
	}

	public void saveTo(OutputStream out) throws IOException {
		if(image == null) redraw();
		final BufferedOutputStream bos = (BufferedOutputStream) (out instanceof BufferedOutputStream ? out : new BufferedOutputStream(out));
		ImageIO.write(image, "PNG", bos);
	}

}
