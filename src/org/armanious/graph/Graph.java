package org.armanious.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.function.Function;

import org.armanious.Tuple;

public class Graph<K> {
	
	final HashMap<K, HashSet<Edge<K>>> neighbors = new HashMap<>();
	private int edgeCount = 0;
	
	public void addNode(K node){
		if(!neighbors.containsKey(node)){
			neighbors.put(node, new HashSet<>());
		}
	}
	
	public void addEdge(K src, K target){
		addEdge(src, target, 1, true);
	}
	
	public void addEdge(K src, K target, int weight){
		addEdge(src, target, weight, true);
	}
	
	public void addEdge(K src, K target, int weight, boolean bidirectional){
		assert(neighbors.containsKey(src) && neighbors.containsKey(target));
		neighbors.get(src).add(new Edge<>(src, target, weight));
		edgeCount++;
		if(bidirectional){
			neighbors.get(target).add(new Edge<>(target, src, weight));
			edgeCount++;
		}
	}
	
	public Collection<K> getNodes(){
		return neighbors.keySet();
	}
	
	public Collection<Edge<K>> getNeighbors(K n){
		return neighbors.get(n);
	}
	
	public int getEdgeCount(){
		return edgeCount;
	}
	
	public Tuple<ArrayList<K>, Integer> dijkstras(K source, K target){
		return dijkstras(source, target, e -> e.getWeight());
	}
	
	public Tuple<ArrayList<K>, Integer> dijkstras(K source, K target, Function<Edge<K>,Integer> cost){
		final HashMap<K, Integer> distances = new HashMap<>();
		final HashMap<K, K> prev = new HashMap<>();
		
		distances.put(source, 0);
		prev.put(source, null);
		
		final PriorityQueue<K> queue = new PriorityQueue<>(Comparator.comparing(k -> distances.get(k)));
		queue.add(source);
		
		while(!queue.isEmpty()){
			final K cur = queue.poll();
			final int currentCost = distances.get(cur);
			for(Edge<K> edge : getNeighbors(cur)){
				final K next = edge.getTarget();
				final int edgeCost = cost.apply(edge);
				if(currentCost + edgeCost < distances.getOrDefault(next, Integer.MAX_VALUE)){
					distances.put(next, currentCost + edgeCost);
					prev.put(next, cur);
					queue.remove(next);
					queue.add(next);
				}
			}
		}
		
		ArrayList<K> path = new ArrayList<>();
		K cur = target;
		while(cur != null){
			path.add(cur);
			cur = prev.get(cur);
		}
		Collections.reverse(path);
		return new Tuple<>(path, distances.getOrDefault(target, Integer.MAX_VALUE));
		
	}

	public Graph<K> subgraphWithNodes(Collection<K> nodes) {
		final Graph<K> g = new Graph<>();
		for(K k : nodes) g.addNode(k);
		for(K k : nodes)
			for(Edge<K> e : getNeighbors(k))
				if(g.neighbors.containsKey(e.getTarget()))
					g.addEdge(k, e.getTarget(), e.getWeight());
		return g;
	}

}
