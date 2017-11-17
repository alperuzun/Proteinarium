package org.armanious.network.visualization;

import java.awt.geom.Point2D;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;

import org.armanious.graph.Edge;
import org.armanious.graph.Graph;

public class ForceDirectedLayout<K> {
	
	private static final boolean DEBUG = false;
	
	private static final Random random = new Random();
	private static final double STANDARD_REPULSION_CONSTANT = 0.1;//0.001;
	private static final double STANDARD_ATTRACTION_CONSTANT = 1;//0.1;
	private static final double STANDARD_THRESHOLD = 1E-3;
	//private static final double STANDARD_GRAVITY_CONSTANT = 1;
	//private static final double STANDARD_NONOVERLAP_FORCE = 20;
	//private static final double STANDARD_REPULSION_THRESHOLD = 50;
	//private static final double INITIAL_DAMPING_FACTOR = 100;
	public static final Function<Integer, Double> STANDARD_SIZE_FUNCTION = x -> 50D / (1 + Math.exp(-.08 * (x - 20)));

	private final double repulsionConstant;
	private final double attractionConstant;
	private final double threshold;

	public final K[] nodes;
	public final int[][] neighbors;
	public final ArrayList<ArrayList<Edge<K>>> edges;
	public final double[] radii;
	public Point2D.Double[] positions;
	private Point2D.Double[] nextPositions;

	public ForceDirectedLayout(Graph<K> g){
		this(g, k -> STANDARD_SIZE_FUNCTION.apply(g.getNeighbors(k).size()));
	}

	public ForceDirectedLayout(Graph<K> g, double repulsionConstant, double attractionConstant, double threshold){
		this(g, repulsionConstant, attractionConstant, threshold, k -> STANDARD_SIZE_FUNCTION.apply(g.getNeighbors(k).size()));
	}

	public ForceDirectedLayout(Graph<K> g,  Function<K,Double> size){
		this(g, STANDARD_REPULSION_CONSTANT, STANDARD_ATTRACTION_CONSTANT, STANDARD_THRESHOLD, size);
	}

	@SuppressWarnings("unchecked")
	public ForceDirectedLayout(Graph<K> g, double repulsionConstant, double attractionConstant, double threshold, Function<K,Double> size){
		System.out.println(g.getNodes().size());
		
		this.repulsionConstant = repulsionConstant;
		this.attractionConstant = attractionConstant;
		this.threshold = threshold;

		nodes = g.getNodes().toArray((K[]) Array.newInstance(g.getNodes().iterator().next().getClass(), g.getNodes().size()));
		neighbors = new int[nodes.length][];
		edges = new ArrayList<>(nodes.length);
		for(int i = 0; i < nodes.length; i++) edges.add(null);
		radii = new double[nodes.length];

		positions = new Point2D.Double[nodes.length];
		nextPositions = new Point2D.Double[nodes.length];

		final HashMap<K, Integer> map = new HashMap<>();
		for(int i = 0; i < nodes.length; i++){
			map.put(nodes[i], i);
			final Point2D.Double pos = new Point2D.Double(random.nextGaussian() * nodes.length, random.nextGaussian() * nodes.length);
			positions[i] = pos;
			nextPositions[i] = new Point2D.Double();
			radii[i] = size.apply(nodes[i]);
			assert(radii[i] >= 1);
		}

		for(int i = 0; i < nodes.length; i++){
			final Collection<Edge<K>> edges = g.getNeighbors(nodes[i]);
			final ArrayList<Edge<K>> edgesList = edges instanceof ArrayList ? (ArrayList<Edge<K>>) edges : new ArrayList<>(edges);
			final int[] n = new int[edgesList.size()];
			for(int j = 0; j < edgesList.size(); j++){
				n[j] = map.get(edgesList.get(j).getTarget());
			}
			neighbors[i] = n;
			this.edges.set(i, edgesList);
		}

	}

	public void layout(){
		System.out.println("Laying out graph with " + nodes.length + " nodes...");
		System.out.flush();
		int max = 5000;
		int cur = 0;
		while(!Thread.interrupted() && cur++ < max)
			if(!iterateLayout())
				break;
			else if(cur % 100 == 0)
				check(cur);
		System.out.println("Completed graph layout");
	}
	
	private void check(int iters){
		if(DEBUG) System.out.println(iters + " iterations completed.");
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		for(int i = 0; i < positions.length; i++){
			final Point2D.Double pos = positions[i];
			if(pos.x < minX) minX = pos.x ;
			if(pos.x > maxX) maxX = pos.x;
			if(pos.y < minY) minY = pos.y;
			if(pos.y > maxY) maxY = pos.y; 
		}
		final int width = (int) Math.ceil(maxX - minX);
		final int height = (int) Math.ceil(maxY - minY);
		if(DEBUG) System.out.println("(" + width + ", " + height + ")");
		if( (long) width * height > Integer.MAX_VALUE){
			throw new RuntimeException("(" + width + ", " + height + ") = " + (long) width * height + " overflows...");
		}
	}

	private boolean iterateLayout(){
		double totalDelta = 0;
		final Point2D.Double delta = new Point2D.Double();
		for(int i = 0; i < nodes.length; i++){
			delta.x = delta.y = 0;
			final Point2D.Double pos = positions[i];
			for(int j = 0; j < nodes.length; j++)
				if(i != j) repel(delta, i, j);
			for(int j : neighbors[i])
				attract(delta, i, j);
			pos.x += delta.x;
			pos.y += delta.y;
			totalDelta += Math.abs(delta.x) + Math.abs(delta.y);
		}
		if(DEBUG) System.out.println(totalDelta);
		return totalDelta >= threshold;
	}

	private final double distanceFromEdges(int i, int j){
		assert(i != j);
		return Math.max(positions[i].distance(positions[j]), 1);
	}
	
	private final double logDistanceFromEdges(int i, int j){
		assert(i != j);
		return Math.max(Math.log(distanceFromEdges(i, j)), 1);
	}

	/*
	 * Do either log distance or log magnitude to prevent "explosive" repulsion
	 */
	private void repel(Point2D.Double delta, int i, int j){
		final double dist = distanceFromEdges(i, j);//logDistanceFromEdges(i, j);
		final double magnitude = -repulsionConstant * radii[i] * radii[i] * radii[j] * radii[j] / (dist * dist);
		//System.out.println("(" + i + "," + j + ") repel magnitude: " + magnitude);
		calculateOffset(magnitude, delta, i, j);
	}

	private void attract(Point2D.Double delta, int i, int j){
		final double dist = distanceFromEdges(i, j);
		final double magnitude = attractionConstant * dist;
		//System.out.println("(" + i + "," + j + ") attract magnitude: " + magnitude);
		calculateOffset(magnitude, delta, i, j);
	}


	/*
	 * F = m * a
	 * F = m * dV / dt
	 * F = m * d^2x / dt^2
	 * x = F / m
	 * 
	 * m = radii[m] ^ 2
	 */
	private void calculateOffset(double magnitude, Point2D.Double delta, int i, int j){
		final double dx = positions[j].x - positions[i].x;
		final double dy = positions[j].y - positions[i].y;
		final double degree = Math.atan2(dy, dx);

		final double mass = radii[i] * radii[i];

		delta.x += magnitude * Math.cos(degree) / mass;
		delta.y += magnitude * Math.sin(degree) / mass;
		
		if(delta.x == Double.NaN || delta.x == Double.NEGATIVE_INFINITY || delta.x == Double.POSITIVE_INFINITY ||
				delta.y == Double.NaN || delta.y == Double.NEGATIVE_INFINITY || delta.y == Double.POSITIVE_INFINITY){
			System.err.println("Why does this shit happen?");
		}
	}

	/*public static void main(String...args) throws IOException {
		final MutableGraph<Integer> g = new MutableGraph<>();
		final Random random = new Random();
		final double percentEdgesPresent = 0.2;
		final int count = 150;
		for(int i = 0; i < count; i++)
			g.addNode(i);
		for(int i = 0; i < count; i++)
			for(int j = i + 1; j < count; j++)
				if(random.nextDouble() <= percentEdgesPresent)
					g.addEdge(i, j);

		final ForceDirectedLayout<Integer> fdl = new ForceDirectedLayout<>(g, 0.01, 0.5, 1);
		fdl.layout();
		final Renderer<Integer> r = new Renderer<>(fdl);
		final File file = new File("image.png");
		r.saveTo(file);
		Desktop.getDesktop().open(file);
	}*/

}

