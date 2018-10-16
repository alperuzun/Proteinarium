package org.armanious.network.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.armanious.graph.Edge;
import org.armanious.graph.Graph;

public class BiasedWalker<K extends Comparable<K>> {

	private static final Random random = new Random();

	private final Graph<K> g;
	private final K[] startVertices;
	private final double restartProbability;

	private long totalSteps = 0;
	private final HashMap<K, Integer> nodeCounts = new HashMap<>();
	private K curNode;
	private K curStartNode;
	private ArrayList<K> curPath = new ArrayList<>();
	private Set<ArrayList<K>> successfulPaths = new HashSet<>();

	public BiasedWalker(Graph<K> graph, K[] startVertices, double restartProbability){
		this.g = graph;
		this.startVertices = startVertices.clone();
		this.restartProbability = restartProbability;
	}

	private void restart(){
		curStartNode = curNode = startVertices[random.nextInt(startVertices.length)];
		curPath.clear();
	}

	

	
	//Don't ever backtrack version
	/*final ArrayList<Edge<K>> properCandidates = new ArrayList<>(allCandidates.size());
	for(Edge<K> edge : allCandidates){
		if(!curPath.contains(edge.getTarget())){
			properCandidates.add(edge);
			sum += edge.getWeight();
		}
	}*/
	public void run(int numSteps){
		while(numSteps-- > 0){
			if(curNode == null || random.nextDouble() <= restartProbability){
				restart();
			}else{
				int sum = 0;
				final Collection<Edge<K>> allCandidates = g.getNeighbors(curNode);
				
				//Go anywhere version
				for(Edge<K> edge : allCandidates)
					sum += edge.getWeight();
				final Collection<Edge<K>> candidates = allCandidates;
				
				//Don't ever backtrack version
				/*final ArrayList<Edge<K>> properCandidates = new ArrayList<>(allCandidates.size());
				for(Edge<K> edge : allCandidates){
					if(!curPath.contains(edge.getTarget())){
						properCandidates.add(edge);
						sum += edge.getWeight();
					}
				}				
				final Collection<Edge<K>> candidates = properCandidates;*/
				
				if(sum == 0){
					System.out.println("Failed path; restarting...");
					restart();
				}else{
					int r = random.nextInt(sum);
					for(Edge<K> candidate : candidates){
						if(r <= candidate.getWeight()){
							curNode = candidate.getTarget();
							break;
						}else{
							r -= candidate.getWeight();
						}
					}
				}
			}
			if(curPath.add(curNode)){
				nodeCounts.put(curNode, nodeCounts.getOrDefault(curNode, 0) + 1);
				totalSteps++;
			}

			if(curNode != curStartNode){
				boolean successfulPath = false;
				for(K end : startVertices){
					if(curNode.equals(end)){
						successfulPath = true;
						break;
					}
				}
				if(successfulPath){
					successfulPaths.add(curPath);
					curPath = new ArrayList<>();// new HashSet<>();
					curPath.add(curNode);
				}
			}
			
		}
	}

	public Set<ArrayList<K>> getSuccessfulPaths(){
		return successfulPaths;
	}

	public HashMap<K, Double> getDistribution(){
		return getDistribution(0);
	}

	public HashMap<K, Double> getDistribution(double threshold){
		final HashMap<K, Double> distribution = new HashMap<>(nodeCounts.size());
		for(K k : nodeCounts.keySet()){
			final double freq = (double) nodeCounts.get(k) / totalSteps;
			if(freq >= threshold){
				distribution.put(k, freq);
			}
		}
		return distribution;
	}

	/*
    ----CHANGES TO MAKE----
    Random walker:
        2) prevent ``back-stepping" by only considering distribution using nodes that have not been visited
            - if no neighboring node has not been visited, restart
        3) only increment counts upon a successful walk?
        4) continue until x successful walks?
	 */
}
