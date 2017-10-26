package org.armanious.network.visualization;

import java.awt.Desktop;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Function;

import org.armanious.graph.Edge;
import org.armanious.graph.Graph;

public class ForceDirectedLayout<K> {
	
	private static final boolean PRINT_DELTA = false;
	
	private static final Random random = new Random();
	private static final double STANDARD_REPULSION_CONSTANT = 0.1;//0.001;
	private static final double STANDARD_ATTRACTION_CONSTANT = 1;//0.1;
	private static final double STANDARD_THRESHOLD = 1E-10;
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
		this.repulsionConstant = repulsionConstant;
		this.attractionConstant = attractionConstant;
		this.threshold = threshold;

		nodes = g.getNodes().toArray((K[]) Array.newInstance(g.getNodes().iterator().next().getClass(), g.getNodes().size()));
		neighbors = new int[nodes.length][];		
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
		}

		for(int i = 0; i < nodes.length; i++){
			final Collection<Edge<K>> edges = g.getNeighbors(nodes[i]);
			final int[] n = new int[edges.size()];
			final Iterator<Edge<K>> iter = edges.iterator();
			for(int j = 0; j < edges.size(); j++){
				n[j] = map.get(iter.next().getTarget());
			}
			neighbors[i] = n;
		}

	}

	public void layout(){
		int max = 10000;
		while(!Thread.interrupted() && max-- > 0)
			if(!iterateLayout())
				break;
			//else
				//check();
	}

	//private void check(){
		//System.out.println(Arrays.toString(positions));
	//}

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
		if(PRINT_DELTA) System.out.println(totalDelta);
		return totalDelta >= threshold;
	}

	private final double distanceFromEdges(int i, int j){
		assert(i != j);
		return positions[i].distance(positions[j]);
	}

	/*
	 * Do either log distance or log magnitude to prevent "explosive" repulsion
	 */
	private void repel(Point2D.Double delta, int i, int j){
		final double dist = Math.log(distanceFromEdges(i, j));
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
	}

	public static void main(String...args) throws IOException {
		final Graph<Integer> g = new Graph<>();
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
	}

}

