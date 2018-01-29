package org.armanious.network.visualization;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.armanious.graph.Edge;
import org.armanious.network.Configuration.RendererConfig;
import org.armanious.network.visualization.ForceDirectedLayout.GraphLayoutData;

public class Renderer<K> {
	
	private final RendererConfig rc;
	private final File outputDirectory;

	private Function<K, String> labelFunction = k -> String.valueOf(k);
	private Function<K, Color> labelColorFunction = k -> Color.BLACK;

	private Function<K, Color> nodeColorFunction = k -> Color.RED;
	//private Function<K, Float> nodeOpaquenessFunction = k -> 1f;

	private Function<Edge<K>, Color> edgeColorFunction = k -> Color.BLACK;
	private Function<Edge<K>, Float> edgeThicknessFunction = k -> 1f;

	private Function<K, Color> nodeBorderColorFunction = k -> Color.BLACK;
	private Function<K, Float> nodeBorderThicknessFunction = k -> 1f;

	public Renderer(RendererConfig rc, File outputDirectory){
		this.outputDirectory = outputDirectory;
		if(!outputDirectory.exists()) outputDirectory.mkdirs();
		this.rc = rc;
	}

	public void setLabelFunction(Function<K, String> labelFunction){
		this.labelFunction = labelFunction;
	}

	/*public void setLabelFontFunction(Function<K, Font> labelFontFunction){
		this.labelFontFunction = labelFontFunction;
	}*/

	public void setLabelColorFunction(Function<K, Color> labelColorFunction){
		this.labelColorFunction = labelColorFunction;
	}

	public void setNodeColorFunction(Function<K, Color> nodeColorFunction){
		this.nodeColorFunction = nodeColorFunction;
	}

	/*public void setNodeOpaquenessFunction(Function<K, Float> nodeOpaquenessFunction){
		this.nodeOpaquenessFunction = nodeOpaquenessFunction;
	}*/

	public void setEdgeColorFunction(Function<Edge<K>, Color> edgeColorFunction){
		this.edgeColorFunction = edgeColorFunction;
	}

	public void setEdgeThicknessFunction(Function<Edge<K>, Float> edgeThicknessFunction){
		this.edgeThicknessFunction = edgeThicknessFunction;
	}

	public void setNodeBorderColorFunction(Function<K, Color> nodeBorderColorFunction){
		this.nodeBorderColorFunction = nodeBorderColorFunction;
	}

	public void setNodeBorderThicknessFunction(Function<K, Float> nodeBorderThicknessFunction){
		this.nodeBorderThicknessFunction = nodeBorderThicknessFunction;
	}

	BufferedImage generateBufferedImage(GraphLayoutData<K> data, String name){
		// TODO FIXME
		final double padding = 0.25;

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		double sumX = 0;
		double sumY = 0;
		for(int i = 0; i < data.positions.length; i++){
			final Point2D.Double pos = data.positions[i];
			final double radius = data.radii[i];
			if(pos.x - radius * 2 < minX) minX = pos.x - radius * 2;
			if(pos.x + radius * 2 > maxX) maxX = pos.x + radius * 2;
			if(pos.y - radius * 2 < minY) minY = pos.y - radius * 2;
			if(pos.y + radius * 2 > maxY) maxY = pos.y + radius * 2;
			sumX += pos.x;
			sumY += pos.y;
		}
		final double centerX = sumX / data.positions.length;
		final double centerY = sumY / data.positions.length;

		final int width = (int) Math.ceil((maxX - minX) * (1 + padding));
		final int height = (int) Math.ceil((maxY - minY) * (1 + padding));

		//want half the padding translated
		final double translationX = width * 0.5 - centerX; 
		final double translationY = height * 0.5 - centerY;

		final Point2D.Double[] positions = new Point2D.Double[data.positions.length];
		for(int i = 0; i < positions.length; i++){
			final Point2D.Double old = data.positions[i];
			//invert horizontally to account for graphic's library y-axis
			positions[i] = new Point2D.Double(old.x + translationX, height - (old.y + translationY));
		}

		if((long) width * height > Integer.MAX_VALUE){
			System.out.println("Integer overflow error; (" + width + ", " + height + ")");
			throw new RuntimeException();
		}

		//System.out.println("Attempting to create buffered image with width=" + width + ", height=" + height);
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = RenderingUtils.prepareBufferedImageGraphics(rc, image);

		//TODO cache BasicStroke objects by width
		for(int i = 0; i < positions.length; i++){
			final Point2D.Double start = positions[i];
			final Line2D.Double line = new Line2D.Double(start.x, start.y, 0, 0);
			for(int j = 0; j < data.neighbors[i].length; j++){
				line.setLine(start, positions[data.neighbors[i][j]]);
				final Edge<K> edge = data.edges[i][j];
				g.setColor(edgeColorFunction.apply(edge));
				g.setStroke(new BasicStroke(edgeThicknessFunction.apply(edge)));
				g.draw(line);
			}
		}
		final Ellipse2D.Double node = new Ellipse2D.Double();
		
		g.setFont(new Font(rc.fontName, Font.PLAIN, rc.fontSize));
		final FontMetrics metrics = g.getFontMetrics();
		
		for(int i = 0; i < positions.length; i++){
			node.setFrame(positions[i].x - data.radii[i], positions[i].y - data.radii[i], data.radii[i] * 2, data.radii[i] * 2);
			g.setColor(nodeColorFunction.apply(data.nodes[i]));
			g.setComposite(AlphaComposite.Clear);
			g.fill(node);
			g.setComposite(AlphaComposite.SrcOver);
			g.fill(node);
			g.setColor(nodeBorderColorFunction.apply(data.nodes[i]));
			g.setStroke(new BasicStroke(nodeBorderThicknessFunction.apply(data.nodes[i])));
			g.draw(node);
			
			if(rc.drawGeneSymbols){
				final String nodeLabel = labelFunction.apply(data.nodes[i]);
				//nodeLabel += " (" + data.neighbors[i].length + ", " + Math.round(data.radii[i]*100)/100.0 + ")";
				final int strWidth = metrics.stringWidth(nodeLabel);
				final int strHeight = metrics.getHeight();
				g.setColor(labelColorFunction.apply(data.nodes[i]));
				//TODO FIXME change font until (and cache results) until size fits
				g.drawString(nodeLabel,
						(float) (positions[i].x - data.radii[i] * 0.9 + (1.8 * data.radii[i] - strWidth) * 0.5),
						(float) (positions[i].y - strHeight * 0.5 + metrics.getAscent()));
			}
		}
		return RenderingUtils.postProcess(rc, image, name, false);
	}

	public void handleLayoutIteration(GraphLayoutData<K> data, String name) throws IOException {

	}

	public void handleLayoutFinal(GraphLayoutData<K> data, String name) throws IOException {
		System.out.println("Generating image of " + name + " for output to file...");
		final BufferedImage image = generateBufferedImage(data, name);

		final File imageFile = new File(outputDirectory, name + "." + rc.imageExtension);

		final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imageFile));
		ImageIO.write(image, rc.imageExtension, bos);
	}

}
