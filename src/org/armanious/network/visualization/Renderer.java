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

public class Renderer<K extends Comparable<K>> {
	
	private final RendererConfig rc;
	private final File outputDirectory;

	private Function<K, String> labelFunction = k -> String.valueOf(k);
	private Function<K, Color> labelColorFunction = k -> Color.BLACK;

	private Function<K, Color> vertexColorFunction = k -> Color.RED;
	//private Function<K, Float> vertexOpaquenessFunction = k -> 1f;

	private Function<Edge<K>, Color> edgeColorFunction = k -> Color.BLACK;
	private Function<Edge<K>, Float> edgeThicknessFunction = k -> 1f;

	private Function<K, Color> vertexBorderColorFunction = k -> Color.BLACK;
	private Function<K, Float> vertexBorderThicknessFunction = k -> 1f;

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

	public void setVertexColorFunction(Function<K, Color> vertexColorFunction){
		this.vertexColorFunction = vertexColorFunction;
	}

	/*public void setVertexOpaquenessFunction(Function<K, Float> vertexOpaquenessFunction){
		this.vertexOpaquenessFunction = vertexOpaquenessFunction;
	}*/

	public void setEdgeColorFunction(Function<Edge<K>, Color> edgeColorFunction){
		this.edgeColorFunction = edgeColorFunction;
	}

	public void setEdgeThicknessFunction(Function<Edge<K>, Float> edgeThicknessFunction){
		this.edgeThicknessFunction = edgeThicknessFunction;
	}

	public void setVertexBorderColorFunction(Function<K, Color> vertexBorderColorFunction){
		this.vertexBorderColorFunction = vertexBorderColorFunction;
	}

	public void setVertexBorderThicknessFunction(Function<K, Float> vertexBorderThicknessFunction){
		this.vertexBorderThicknessFunction = vertexBorderThicknessFunction;
	}

	BufferedImage generateBufferedImage(GraphLayoutData<K> data, String name){
		final double padding = 0;

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		for(int i = 0; i < data.positions.length; i++){
			final Point2D.Double pos = data.positions[i];
			final double radius = data.radii[i];
			if(pos.x - radius * 2 < minX) minX = pos.x - radius * 2;
			if(pos.y - radius * 2 < minY) minY = pos.y - radius * 2;
		}
		
		final Point2D.Double[] positions = new Point2D.Double[data.positions.length];
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		for(int i = 0; i < positions.length; i++){
			final Point2D.Double old = data.positions[i];
			final double radius = data.radii[i];
			positions[i] = new Point2D.Double(old.x - minX, old.y - minY);
			if(positions[i].x + radius * 2 > maxX) maxX = positions[i].x + radius * 2;
			if(positions[i].y + radius * 2 > maxY) maxY = positions[i].y + radius * 2;
		}
		
		final double width = Math.max(1, maxX);
		final double height = Math.max(1, maxY);
		
		if((long) width * height > Integer.MAX_VALUE){
			System.out.println("Integer overflow error; (" + width + ", " + height + ")");
			throw new RuntimeException();
		}

		// System.out.println("Attempting to create buffered image with width=" + width + ", height=" + height);
		final BufferedImage image = new BufferedImage((int)(width * (1 + padding)), (int)(height * (1 + padding)), BufferedImage.TYPE_INT_ARGB);
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
		final Ellipse2D.Double vertex = new Ellipse2D.Double();
		
		g.setFont(new Font("Dialog", Font.PLAIN, 12));
		final FontMetrics metrics = g.getFontMetrics();
		
		for(int i = 0; i < positions.length; i++){
			vertex.setFrame(positions[i].x - data.radii[i], positions[i].y - data.radii[i], data.radii[i] * 2, data.radii[i] * 2);
			g.setColor(vertexColorFunction.apply(data.vertices[i]));
			g.setComposite(AlphaComposite.Clear);
			g.fill(vertex);
			g.setComposite(AlphaComposite.SrcOver);
			g.fill(vertex);
			g.setColor(vertexBorderColorFunction.apply(data.vertices[i]));
			g.setStroke(new BasicStroke(vertexBorderThicknessFunction.apply(data.vertices[i])));
			g.draw(vertex);
			
			if(rc.drawGeneSymbols){
				final String vertexLabel = labelFunction.apply(data.vertices[i]);
				//vertexLabel += " (" + data.neighbors[i].length + ", " + Math.round(data.radii[i]*100)/100.0 + ")";
				final int strWidth = metrics.stringWidth(vertexLabel);
				final int strHeight = metrics.getHeight();
				g.setColor(labelColorFunction.apply(data.vertices[i]));
				//TODO FIXME change font until (and cache results) until size fits
				g.drawString(vertexLabel,
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

		final File imageFile = new File(outputDirectory, name + ".png");

		final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imageFile));
		ImageIO.write(image, "png", bos);
	}

}
