package org.armanious.network.visualization;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;

import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.network.Configuration.ForceDirectedLayoutConfig;

public class ForceDirectedLayout<K> {
	
	private static final Random random = new Random();
	
	private final ForceDirectedLayoutConfig fdlc;
	private final GraphLayoutData<K> data;
	private final Renderer<K> renderer;
	private final String name;
	
	public ForceDirectedLayout(ForceDirectedLayoutConfig fdlc, Graph<K> graph, Renderer<K> renderer, String name){
		this.fdlc = fdlc;
		final double minSize = fdlc.minNodeSize;
		final double maxSize = fdlc.maxNodeSize < 0 ? Math.max(2 * fdlc.minNodeSize, Math.min(100, graph.getNodes().size())) : fdlc.maxNodeSize;
		assert(maxSize >= minSize);
		final double deltaSize = (maxSize - minSize) / graph.getNodes().size();
				
		data = new GraphLayoutData<>(graph,
				k -> minSize + deltaSize * graph.getNeighbors(k).size());
		this.renderer = renderer;
		this.name = name;
	}
	
	public void layoutAndRender(){
		long itersRemaining = fdlc.maxIterations;
		long endTime = fdlc.maxTime;

		final Point2D.Double delta = new Point2D.Double();
		while(itersRemaining-- > 0 && System.currentTimeMillis() <= endTime){
			double totalDelta = 0;
			
			for(int i = 0; i < data.nodes.length; i++){
				delta.x = delta.y = 0;
				final Point2D.Double pos = data.positions[i];
				for(int j = 0; j < data.nodes.length; j++)
					if(i != j) repel(data, delta, i, j);
				for(int j : data.neighbors[i])
					attract(data, delta, i, j);
				pos.x += delta.x;
				pos.y += delta.y;
				totalDelta += Math.abs(delta.x) + Math.abs(delta.y);
			}
			
			try {
				renderer.handleLayoutIteration(data, name);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(totalDelta <= fdlc.deltaThreshold) break;
		}
		
		try {
			renderer.handleLayoutFinal(data, name);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static <K> double distanceFromEdges(GraphLayoutData<K> data, int i, int j){
		assert(i != j);
		return Math.max(data.positions[i].distance(data.positions[j]), 1);
	}
	
	private void repel(GraphLayoutData<K> data, Point2D.Double delta, int i, int j){
		final double dist = distanceFromEdges(data, i, j);//logDistanceFromEdges(i, j);
		final double magnitude = -fdlc.repulsionConstant * data.radii[i] * data.radii[i] * data.radii[j] * data.radii[j] / (dist * dist);
		calculateOffset(data, magnitude, delta, i, j);
	}

	private void attract(GraphLayoutData<K> data, Point2D.Double delta, int i, int j){
		final double dist = distanceFromEdges(data, i, j);
		final double magnitude = fdlc.attractionConstant * dist;
		calculateOffset(data, magnitude, delta, i, j);
	}
	
	private static <K> void calculateOffset(GraphLayoutData<K> data, double magnitude, Point2D.Double delta, int i, int j){
		final double dx = data.positions[j].x - data.positions[i].x;
		final double dy = data.positions[j].y - data.positions[i].y;
		final double degree = Math.atan2(dy, dx);

		final double mass = data.radii[i] * data.radii[i];

		delta.x += magnitude * Math.cos(degree) / mass;
		delta.y += magnitude * Math.sin(degree) / mass;
	}
	
	protected static class GraphLayoutData<K> { 
		
		public final K[] nodes;
		public final int[][] neighbors;
		public final Edge<K>[][] edges;
		public final double[] radii;
		public Point2D.Double[] positions;
		
		@SuppressWarnings("unchecked")
		private GraphLayoutData(Graph<K> g, Function<K, Double> nodeSizeFunction){
			nodes = g.getNodes().toArray((K[]) Array.newInstance(g.getNodes().iterator().next().getClass(), g.getNodes().size()));
			neighbors = new int[nodes.length][];
			edges = new Edge[nodes.length][];
			radii = new double[nodes.length];

			positions = new Point2D.Double[nodes.length];

			final HashMap<K, Integer> map = new HashMap<>();
			for(int i = 0; i < nodes.length; i++){
				map.put(nodes[i], i);
				final Point2D.Double pos = new Point2D.Double(
						Math.cos(Math.PI / 8 * i) * i,
						Math.sin(Math.PI / 8 * i) * i
						/*random.nextGaussian() * nodes.length,
						random.nextGaussian() * nodes.length*/
						);
				positions[i] = pos;
				radii[i] = nodeSizeFunction.apply(nodes[i]);
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
				this.edges[i] = edgesList.toArray(new Edge[edgesList.size()]);
			}
		}
		
	}

}
