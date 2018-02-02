package org.armanious.network.visualization;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.armanious.network.Configuration.RendererConfig;
import org.armanious.network.analysis.ClusterAnalysis;
import org.armanious.network.analysis.PhylogeneticTreeNode;

public class DendrogramRenderer {

	private static final int X_PADDING = 2;
	private static final int Y_PADDING = 20;

	private final RendererConfig rc;
	private final File outputDirectory;

	private Function<PhylogeneticTreeNode, Color> clusterEdgeColorFunction = c -> Color.BLACK;
	private Function<PhylogeneticTreeNode, Color> clusterLabelColorFunction = c -> Color.BLACK;

	public DendrogramRenderer(RendererConfig rc, File outputDirectory){
		this.rc = rc;
		this.outputDirectory = outputDirectory;
	}

	public void setClusterEdgeColorFunction(Function<PhylogeneticTreeNode, Color> clusterEdgeColorFunction){
		this.clusterEdgeColorFunction = clusterEdgeColorFunction;
	}

	//p-value CDF

	//normalize heights to C1 is 1 DONE [check it]
	//C12 label coloring for significant branches [check it]
	//meta-clustering bar on left; filter out matrix analyses
	//legend for dendrogram: only yellow and blue bars (no gradient)
	//remove alpha values for legend edge colors
	//add patient names for each row in Excel sheet
	//for yellow in primary-secondary type thing: fill red, draw yellow

	//TODO explanation of coloring = group1 + group2 (in tutorial?)

	private PhylogeneticTreeNode[] getMetaClusters(Map<String, ClusterAnalysis> clusters){
		final Map<PhylogeneticTreeNode, ClusterAnalysis> remapping = new HashMap<>();
		clusters.forEach((k, v) -> remapping.put(v.getNode(), v));

		final LinkedList<PhylogeneticTreeNode> metaClusters = new LinkedList<>();
		PhylogeneticTreeNode curMetaCluster = null;
		for(PhylogeneticTreeNode leaf : clusters.get("C1").getNode().getLeaves()){
			PhylogeneticTreeNode metaCluster = getMetaCluster(leaf, remapping);
			if(metaCluster != curMetaCluster){
				if(curMetaCluster != null)
					metaClusters.add(curMetaCluster);
				curMetaCluster = metaCluster;
			}
		}
		if(curMetaCluster != null)
			metaClusters.add(curMetaCluster);
		return metaClusters.toArray(new PhylogeneticTreeNode[metaClusters.size()]);
	}

	private Map<PhylogeneticTreeNode, Point2D.Double> layout(Map<String, ClusterAnalysis> clusters, boolean simplified){
		final Map<PhylogeneticTreeNode, ClusterAnalysis> remapping = new HashMap<>();
		clusters.forEach((k, v) -> remapping.put(v.getNode(), v));

		final Map<PhylogeneticTreeNode, Point2D.Double> positions = new HashMap<>();
		final PhylogeneticTreeNode[] leaves = simplified ? getMetaClusters(clusters) : clusters.get("C1").getNode().getLeaves();
		
		final double offsetX = Arrays.stream(leaves).mapToDouble(PhylogeneticTreeNode::getHeight).min().getAsDouble();
		
		final Set<PhylogeneticTreeNode> addedToQueueSet = new HashSet<>();
		final Queue<PhylogeneticTreeNode> queue = new LinkedList<>();
		for(int i = 0; i < leaves.length; i++){
			assert(simplified || leaves[i].getHeight() == 0);
			positions.put(leaves[i], new Point2D.Double(leaves[i].getHeight() - offsetX, i * Y_PADDING));
			if(addedToQueueSet.add(leaves[i].getParent()))
				queue.add(leaves[i].getParent());
		}

		double minDeltaX = Double.MAX_VALUE;
		
		while(!queue.isEmpty()){
			final PhylogeneticTreeNode cur = queue.remove();
			//System.out.println("Popped " + remapping.get(cur).getClusterId() + " off queue");
			final Point2D.Double l = positions.get(cur.getLeftChild());
			final Point2D.Double r = positions.get(cur.getRightChild());
			if(l == null || r == null){
				assert(queue.size() > 0);
				//System.out.println("Added " + remapping.get(cur).getClusterId() + " back onto queue");
				queue.add(cur);
				continue;
			}

			final Point2D.Double point = new Point2D.Double();
			point.x = cur.getHeight() - offsetX;
			point.y = Math.min(l.y, r.y) + Math.abs(l.y - r.y) * 0.5;
			positions.put(cur, point);

			double candidate = point.x - Math.max(l.x, r.x);
			if(candidate > 0 && candidate < minDeltaX)
				minDeltaX = candidate;
			//TODO FIXME BUG: dendrograms are NOT the same for each run

			if(cur.getParent() != null && addedToQueueSet.add(cur.getParent()))
				queue.add(cur.getParent());
		}

		double xMultiplier = X_PADDING / minDeltaX;
		double maxX = Double.MIN_VALUE;
		//System.out.println("minDeltaX: " + minDeltaX + "\npadding: " + xPadding + "\nxMultiplier: " + xMultiplier);
		for(Point2D.Double point : positions.values()){
			point.x *= xMultiplier;
			if(point.x > maxX)
				maxX = point.x;
		}
		
		if(maxX < 150){
			xMultiplier = 150 / maxX;
			for(Point2D.Double point : positions.values())
				point.x *= xMultiplier;
		}

		//System.out.println(positions);


		return positions;
	}

	BufferedImage generateDendrogramImage(Map<String, ClusterAnalysis> clusters, String name, boolean simplified){
		final Map<PhylogeneticTreeNode, ClusterAnalysis> remapping = new HashMap<>();
		clusters.forEach((k, v) -> remapping.put(v.getNode(), v));

		final Map<PhylogeneticTreeNode, Point2D.Double> locations = layout(clusters, simplified);
		
		int maxX = 0;
		int maxY = 0;
		for(Point2D.Double point : locations.values()){
			if(point.x > maxX) maxX = (int) Math.ceil(point.x);
			if(point.y > maxY) maxY = (int) Math.ceil(point.y);
		}
		maxX += 30 + X_PADDING * 2; // for labels TODO make smarter
		maxY += Y_PADDING * 2;


		BufferedImage image = new BufferedImage(maxX, maxY, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = RenderingUtils.prepareBufferedImageGraphics(rc, image);
		g.translate(X_PADDING, Y_PADDING);

		final Queue<PhylogeneticTreeNode> toDraw = new LinkedList<>();
		toDraw.add(clusters.get("C1").getNode());

		g.setFont(new Font("Dialog", Font.PLAIN, 12));

		while(!toDraw.isEmpty()){
			final PhylogeneticTreeNode curNode = toDraw.remove();
			final Point2D.Double cur = locations.get(curNode);

			if(curNode.getLeftChild() == null && curNode.getRightChild() == null){

				g.setColor(Color.BLACK);
				g.drawString(curNode.getLabel(), (int) (cur.x + X_PADDING), (int) (cur.y - 2));

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

				g.setColor(clusterLabelColorFunction.apply(curNode));

				g.drawString(remapping.get(curNode).getClusterId(), (int) (cur.x + X_PADDING), (int) (cur.y - 2));
			}

		}

		if(!simplified){
			final BufferedImage metaClusteringBar = generateMetaClusteringBar(clusters);

			image = RenderingUtils.appendImage(rc, image, metaClusteringBar,
					RenderingUtils.Direction.WEST, RenderingUtils.Justification.LEADING);
		}

		return RenderingUtils.postProcess(rc, image, name, true);
	}

	private BufferedImage generateMetaClusteringBar(Map<String, ClusterAnalysis> clusters){
		final Map<PhylogeneticTreeNode, ClusterAnalysis> remapping = new HashMap<>();
		clusters.forEach((k,v) -> remapping.put(v.getNode(), v));

		final PhylogeneticTreeNode[] leaves = clusters.get("C1").getNode().getLeaves();

		final BufferedImage bi = new BufferedImage(70, leaves.length * Y_PADDING, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = RenderingUtils.prepareBufferedImageGraphics(rc, bi);
		int metaClusterIndex = 0;
		int metaClusterStartIndex = 0;
		PhylogeneticTreeNode currentMetaCluster = getMetaCluster(leaves[0], remapping);
		final Color[] colors = new Color[]{Color.GRAY, Color.LIGHT_GRAY};
		for(int i = 1; i < leaves.length; i++){
			PhylogeneticTreeNode metaCluster = getMetaCluster(leaves[i], remapping);
			if(metaCluster != currentMetaCluster){
				g.setColor(colors[metaClusterIndex % colors.length]);
				g.fillRect(50, metaClusterStartIndex * Y_PADDING, 15, (i - metaClusterStartIndex) * Y_PADDING);
				g.setColor(clusterLabelColorFunction.apply(currentMetaCluster));
				int midY = metaClusterStartIndex * Y_PADDING + (i - metaClusterStartIndex) * Y_PADDING / 2 + 5;
				g.drawString(remapping.get(currentMetaCluster).getClusterId(), 5, midY);

				metaClusterStartIndex = i;
				metaClusterIndex++;
				currentMetaCluster = metaCluster;
			}
		}
		if(currentMetaCluster != null){
			g.setColor(colors[metaClusterIndex % colors.length]);
			g.fillRect(50, metaClusterStartIndex * Y_PADDING, 15, (leaves.length - metaClusterStartIndex) * Y_PADDING);
			g.setColor(clusterLabelColorFunction.apply(currentMetaCluster));
			int midY = metaClusterStartIndex * Y_PADDING + (leaves.length - metaClusterStartIndex) * Y_PADDING / 2 + 5;
			g.drawString(remapping.get(currentMetaCluster).getClusterId(), 5, midY);
		}

		return bi;
	}

	private PhylogeneticTreeNode getMetaCluster(PhylogeneticTreeNode leaf, Map<PhylogeneticTreeNode, ClusterAnalysis> remapping){
		PhylogeneticTreeNode cur = leaf;
		do {
			cur = cur.getParent();
		} while(remapping.get(cur).getNormalizedHeight() < rc.metaClusterThreshold && cur.getParent() != null);
		return cur;
	}

	public File render(Map<String, ClusterAnalysis> clusterAnalysis, String name) throws IOException {
		System.out.println("Generating image of " + name + " for output to file...");

		if(!outputDirectory.exists()) outputDirectory.mkdirs();

		File imageFile = new File(outputDirectory, name + ".png");
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imageFile));
		ImageIO.write(generateDendrogramImage(clusterAnalysis, name, false), "png", bos);


		//imageFile = new File(outputDirectory, name + "_Simplified." + rc.imageExtension);
		//bos = new BufferedOutputStream(new FileOutputStream(imageFile));
		//ImageIO.write(generateDendrogramImage(clusterAnalysis, name, true), rc.imageExtension, bos);


		return imageFile;
	}

	public void setClusterLabelColorFunction(Function<PhylogeneticTreeNode, Color> clusterLabelColorFunction) {
		this.clusterLabelColorFunction = clusterLabelColorFunction;
	}

}
