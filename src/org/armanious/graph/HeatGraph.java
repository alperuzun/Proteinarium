package org.armanious.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;

import org.armanious.Tuple;

public class HeatGraph<K extends Comparable<K>> extends AnnotatedGraph<K, Double> {
	
	public HeatGraph(double maxPathCost, int maxPathLength) {
		this(1D, maxPathCost, maxPathLength);
	}
	
	public HeatGraph(double initialHeat, double maxPathCost, int maxPathLength) {
		super(initialHeat, maxPathCost, maxPathLength);
	}

	private static final double STANDARD_DIFFUSION_THRESHOLD = 1E-5;
	
	public void setHeat(K vertex, double heat){
		setAnnotation(vertex, heat);
	}
	
	public double getHeat(K vertex){
		return getAnnotation(vertex);
	}
	
	public void diffuseHeat(){
		diffuseHeat(STANDARD_DIFFUSION_THRESHOLD, -1);
	}
	
	public void diffuseHeat(double threshold, int maxIterations){
		//int numIters = 0;
		HashMap<K, Double> heatMap = new HashMap<>();
		for(K vertex : getVertices()) heatMap.put(vertex, getHeat(vertex));
		while(maxIterations < 0 || maxIterations-- > 0){
			final Tuple<HashMap<K, Double>, Double> res = diffuseHeatIteration(heatMap);
			heatMap = res.val1();
			if(res.val2() <= threshold) break;
		}
		for(K vertex : getVertices()) setHeat(vertex, heatMap.get(vertex));
		//System.out.println(numIters);
	}
	
	private Tuple<HashMap<K, Double>, Double> diffuseHeatIteration(HashMap<K, Double> heatMap){
		HashMap<K, Double> nextHeatMap = new HashMap<>();
		
		final Function<K, Double> diffusivityFunction = k -> 0.5D;
		for(K vertex : getVertices()){
			if(!nextHeatMap.containsKey(vertex)) nextHeatMap.put(vertex, heatMap.get(vertex));
			
			final double diffusitivity = diffusivityFunction.apply(vertex);
			final double toDiffuse = heatMap.get(vertex) * diffusitivity;
			final Collection<Edge<K>> edges = getNeighbors(vertex);
			int sum = 0;
			for(Edge<K> edge : edges){
				if(!nextHeatMap.containsKey(edge.getTarget())) nextHeatMap.put(edge.getTarget(), heatMap.get(edge.getTarget()));
				sum += edge.getWeight();
			}
			for(Edge<K> edge : edges){
				final double ratio = edge.getWeight() / (double) sum;
				final double diffused = ratio * toDiffuse;
				nextHeatMap.put(edge.getTarget(), nextHeatMap.get(edge.getTarget()) + diffused);
				nextHeatMap.put(vertex, nextHeatMap.get(vertex) - diffused);
			}
		}
		double sumDelta = 0;
		for(K k : heatMap.keySet())	sumDelta += Math.abs(heatMap.get(k) - nextHeatMap.get(k));

		/*for(K key : heatMap.keySet()){
			System.out.println(key + ": " + heatMap.get(key));
		}
		System.out.println();*/
		
		return new Tuple<>(nextHeatMap, sumDelta);
	}

}
