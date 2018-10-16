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
	
	public void setHeat(K node, double heat){
		setAnnotation(node, heat);
	}
	
	public double getHeat(K node){
		return getAnnotation(node);
	}
	
	public void diffuseHeat(){
		diffuseHeat(STANDARD_DIFFUSION_THRESHOLD, -1);
	}
	
	public void diffuseHeat(double threshold, int maxIterations){
		//int numIters = 0;
		HashMap<K, Double> heatMap = new HashMap<>();
		for(K node : getNodes()) heatMap.put(node, getHeat(node));
		while(maxIterations < 0 || maxIterations-- > 0){
			final Tuple<HashMap<K, Double>, Double> res = diffuseHeatIteration(heatMap);
			heatMap = res.val1();
			if(res.val2() <= threshold) break;
		}
		for(K node : getNodes()) setHeat(node, heatMap.get(node));
		//System.out.println(numIters);
	}
	
	private Tuple<HashMap<K, Double>, Double> diffuseHeatIteration(HashMap<K, Double> heatMap){
		HashMap<K, Double> nextHeatMap = new HashMap<>();
		
		final Function<K, Double> diffusivityFunction = k -> 0.5D;
		for(K node : getNodes()){
			if(!nextHeatMap.containsKey(node)) nextHeatMap.put(node, heatMap.get(node));
			
			final double diffusitivity = diffusivityFunction.apply(node);
			final double toDiffuse = heatMap.get(node) * diffusitivity;
			final Collection<Edge<K>> edges = getNeighbors(node);
			int sum = 0;
			for(Edge<K> edge : edges){
				if(!nextHeatMap.containsKey(edge.getTarget())) nextHeatMap.put(edge.getTarget(), heatMap.get(edge.getTarget()));
				sum += edge.getWeight();
			}
			for(Edge<K> edge : edges){
				final double ratio = edge.getWeight() / (double) sum;
				final double diffused = ratio * toDiffuse;
				nextHeatMap.put(edge.getTarget(), nextHeatMap.get(edge.getTarget()) + diffused);
				nextHeatMap.put(node, nextHeatMap.get(node) - diffused);
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
