package org.armanious.network.visualization;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.Function;

import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.network.Configuration.ForceDirectedLayoutConfig;

public class ForceDirectedLayout<K extends Comparable<K>> {
	
	private final ForceDirectedLayoutConfig fdlc;
	private final GraphLayoutData<K> data;
	private final Renderer<K> renderer;
	private final String name;
	
	public ForceDirectedLayout(ForceDirectedLayoutConfig fdlc, Graph<K> graph, Renderer<K> renderer, String name){
		this.fdlc = fdlc;
		final double minRadius = fdlc.minVertexRadius;
		final double maxRadius = fdlc.maxVertexRadius < 0 ? Math.max(2 * fdlc.minVertexRadius, Math.min(100, graph.getVertices().size())) : fdlc.maxVertexRadius;
		assert(maxRadius >= minRadius);
		double maxDegree = 1;
		for(K k : graph.getVertices()) {
			final int degree = graph.getNeighbors(k).size();
			if(degree > maxDegree) maxDegree = degree;
		}
		final double deltaSize = (maxRadius - minRadius) / maxDegree;
		
		data = new GraphLayoutData<>(graph,
				k -> minRadius + deltaSize * graph.getNeighbors(k).size());
		this.renderer = renderer;
		this.name = name;
	}
	
	public void start(){
		long itersRemaining = fdlc.maxIterations;
		long endTime = fdlc.maxTime;

		final Point2D.Double delta = new Point2D.Double();
		while(itersRemaining-- > 0 && System.currentTimeMillis() <= endTime){
			double totalDelta = 0;
			
			for(int i = 0; i < data.vertices.length; i++){
				delta.x = delta.y = 0;
				final Point2D.Double pos = data.positions[i];
				for(int j = 0; j < data.vertices.length; j++)
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
	
	private double distanceFromCenters(GraphLayoutData<K> data, int i, int j){
		assert(i != j);
		return data.positions[i].distance(data.positions[j]);
	}
	
	private double distanceFromEdges(GraphLayoutData<K> data, int i, int j){
		assert(i != j);
		return data.positions[i].distance(data.positions[j]) - data.radii[i] - data.radii[j];
	}
	
	private void repel(GraphLayoutData<K> data, Point2D.Double delta, int i, int j){
		final double dist = distanceFromCenters(data, i, j);
		final double magnitude = -fdlc.repulsionConstant * data.radii[i] * data.radii[j] / (dist * dist);
		
		calculateOffset(data, magnitude, delta, i, j);
	}

	private void attract(GraphLayoutData<K> data, Point2D.Double delta, int i, int j){
		final double dist = distanceFromCenters(data, i, j);
		final double magnitude = fdlc.attractionConstant * dist;
		calculateOffset(data, magnitude, delta, i, j);
	}
	
	private static <K extends Comparable<K>> void calculateOffset(GraphLayoutData<K> data, double magnitude, Point2D.Double delta, int i, int j){
		final double dx = data.positions[j].x - data.positions[i].x;
		final double dy = data.positions[j].y - data.positions[i].y;
		final double degree = Math.atan2(dy, dx);

		delta.x += magnitude * Math.cos(degree);
		delta.y += magnitude * Math.sin(degree);
		
	}
	
	protected static class GraphLayoutData<K extends Comparable<K>> { 
		
		public final K[] vertices;
		public final int[][] neighbors;
		public final int maxDegree;
		public final Edge<K>[][] edges;
		public final double[] radii;
		public Point2D.Double[] positions;
		
		@SuppressWarnings("unchecked")
		private GraphLayoutData(Graph<K> g, Function<K, Double> vertexRadiusFunction){
			vertices = g.getVertices().toArray((K[]) Array.newInstance(g.getVertices().iterator().next().getClass(), g.getVertices().size()));
			Arrays.sort(vertices, Comparator.comparing(k -> String.valueOf(k)));
			
			neighbors = new int[vertices.length][];
			edges = new Edge[vertices.length][];
			radii = new double[vertices.length];

			positions = new Point2D.Double[vertices.length];

			final HashMap<K, Integer> map = new HashMap<>();
			double curCumulativeRadius = 0;
			for(int i = 0; i < vertices.length; i++){
				map.put(vertices[i], i);
				final Point2D.Double pos = new Point2D.Double(
						Math.cos(Math.PI / 8 * i) * curCumulativeRadius,
						Math.sin(Math.PI / 8 * i) * curCumulativeRadius
						);
				positions[i] = pos;
				radii[i] = vertexRadiusFunction.apply(vertices[i]);
				curCumulativeRadius += radii[i] / 4;
				assert(radii[i] >= 1);
			}
			int maxDegree = 0;
			for(int i = 0; i < vertices.length; i++){
				final Collection<Edge<K>> edges = g.getNeighbors(vertices[i]);
				final ArrayList<Edge<K>> edgesList = edges instanceof ArrayList ? (ArrayList<Edge<K>>) edges : new ArrayList<>(edges);
				final int[] n = new int[edgesList.size()];
				for(int j = 0; j < edgesList.size(); j++){
					n[j] = map.get(edgesList.get(j).getTarget());
				}
				//Arrays.sort(n);
				neighbors[i] = n;
				if(n.length > maxDegree) maxDegree = n.length;
				this.edges[i] = edgesList.toArray(new Edge[edgesList.size()]);
			}
			this.maxDegree = maxDegree;
		}
		
	}

}
