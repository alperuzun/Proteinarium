package org.armanious.network.visualization;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.armanious.Tuple;
import org.armanious.network.Configuration.RendererConfig;
import org.armanious.network.analysis.PhylogeneticTreeNode;

public class DendrogramRenderer {

	private final RendererConfig rc;
	
	private Function<PhylogeneticTreeNode, Color> clusterEdgeColorFunction = c -> Color.BLACK;

	public DendrogramRenderer(RendererConfig rc){
		this.rc = rc;
	}
	
	public void setClusterEdgeColorFunction(Function<PhylogeneticTreeNode, Color> clusterEdgeColorFunction){
		this.clusterEdgeColorFunction = clusterEdgeColorFunction;
	}

	private Map<PhylogeneticTreeNode, Point2D.Double> layout(PhylogeneticTreeNode root, double xPadding, double yPadding){
		final Map<PhylogeneticTreeNode, Point2D.Double> positions = new HashMap<>();
		final PhylogeneticTreeNode[] leaves = root.getLeaves();
		final Set<PhylogeneticTreeNode> addedToQueueSet = new HashSet<>();
		final Queue<PhylogeneticTreeNode> queue = new LinkedList<>();
		for(int i = 0; i < leaves.length; i++){
			assert(leaves[i].getDepth() == 0);
			positions.put(leaves[i], new Point2D.Double(leaves[i].getDepth(), i * yPadding));
			if(addedToQueueSet.add(leaves[i].getParent()))
				queue.add(leaves[i].getParent());
		}

		System.out.println(positions);

		double minDeltaX = Double.MAX_VALUE;

		while(!queue.isEmpty()){
			final PhylogeneticTreeNode cur = queue.remove();
			//System.out.println("Popped " + cur.hashCode() + " off queue");
			final Point2D.Double l = positions.get(cur.getLeftChild());
			final Point2D.Double r = positions.get(cur.getRightChild());
			if(l == null || r == null){
				assert(queue.size() > 0);
				//System.out.println("Added " + cur.hashCode() + " back onto queue");
				queue.add(cur);
				continue;
			}

			final Point2D.Double point = new Point2D.Double();
			point.x = cur.getDepth();
			point.y = Math.min(l.y, r.y) + Math.abs(l.y - r.y) * 0.5;
			positions.put(cur, point);
			
			double candidate = point.x - Math.max(l.x, r.x);
			if(candidate > 0 && candidate < minDeltaX)
				minDeltaX = candidate;
			//TODO FIXME BUG: dendrograms are NOT the same for each run

			if(cur.getParent() != null && addedToQueueSet.add(cur.getParent()))
				queue.add(cur.getParent());
		}

		final double xMultiplier = xPadding / minDeltaX;
		System.out.println("minDeltaX: " + minDeltaX + "\npadding: " + xPadding + "\nxMultiplier: " + xMultiplier);
		for(Point2D.Double point : positions.values())
			point.x *= xMultiplier;

		System.out.println(positions);


		return positions;
	}

	Tuple<BufferedImage, Map<String, PhylogeneticTreeNode>> generateBufferedImageAndMapping(PhylogeneticTreeNode root){
		final double xPadding = 2;
		final double yPadding = 15;
		final Map<PhylogeneticTreeNode, Point2D.Double> locations = layout(root, xPadding, yPadding);
		int maxX = 0;
		int maxY = 0;
		for(Point2D.Double point : locations.values()){
			if(point.x > maxX) maxX = (int) Math.ceil(point.x);
			if(point.y > maxY) maxY = (int) Math.ceil(point.y);
		}
		maxX += 30 + xPadding*2; // for labels TODO make smarter
		maxY += yPadding * 2;


		final BufferedImage image = new BufferedImage(maxX, maxY, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g.translate(xPadding, yPadding);
		
		g.setColor(Color.WHITE);
		if(rc.transparentBackground) g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, maxX, maxY);
		g.setComposite(AlphaComposite.SrcOver);
		g.setColor(Color.BLACK);

		final Queue<PhylogeneticTreeNode> toDraw = new LinkedList<>();
		toDraw.add(root);
		
		g.setFont(new Font(rc.fontName, Font.PLAIN, rc.fontSize));
		
		final Map<String, PhylogeneticTreeNode> clusterMapping = new HashMap<>();
		
		while(!toDraw.isEmpty()){
			final PhylogeneticTreeNode curNode = toDraw.remove();
			final Point2D.Double cur = locations.get(curNode);

			if(curNode.getLeftChild() == null && curNode.getRightChild() == null){
				
				g.setColor(Color.BLACK);
				g.drawString(curNode.getLabel(), (int) (cur.x + xPadding), (int) (cur.y - 2));
				
			}else{
				
				final Point2D.Double l = locations.get(curNode.getLeftChild());
				final Point2D.Double r = locations.get(curNode.getRightChild());

				final Line2D.Double verticalLine = new Line2D.Double(cur.x, l.y, cur.x, r.y);
				final Line2D.Double horizontalLine1 = new Line2D.Double(cur.x, l.y, l.x, l.y);
				final Line2D.Double horizontalLine2 = new Line2D.Double(cur.x, r.y, r.x, r.y);

				g.setColor(clusterEdgeColorFunction.apply(curNode));
				g.draw(verticalLine);
				g.setColor(clusterEdgeColorFunction.apply(curNode.getLeftChild()));
				g.draw(horizontalLine1);
				g.setColor(clusterEdgeColorFunction.apply(curNode.getRightChild()));
				g.draw(horizontalLine2);
				toDraw.add(curNode.getLeftChild());
				toDraw.add(curNode.getRightChild());
				
				g.setColor(Color.BLACK);
				
				final String clusterLabel = "C" + (clusterMapping.size() + 1);
				clusterMapping.put(clusterLabel, curNode);
				g.drawString(clusterLabel, (int) (cur.x + xPadding), (int) (cur.y - 2));
			}

		}

		return new Tuple<>(image, clusterMapping);
	}

	public Map<String, PhylogeneticTreeNode> render(PhylogeneticTreeNode root, String name) throws IOException {
		System.out.println("Generating image of " + name + " for output to file...");
		final Tuple<BufferedImage, Map<String, PhylogeneticTreeNode>> imageAndMapping = generateBufferedImageAndMapping(root);
		
		final File imageDirectory = new File(rc.imageDirectory);
		if(!imageDirectory.exists()) imageDirectory.mkdirs();
		final File imageFile = new File(imageDirectory, name + "." + rc.imageExtension);
		
		final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imageFile));
		ImageIO.write(imageAndMapping.val1(), rc.imageExtension, bos);
		Desktop.getDesktop().open(imageFile);
		
		return imageAndMapping.val2();
	}

}
