package org.armanious.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.function.Function;

public class Graph<K> {
	
	final HashMap<K, HashSet<Edge<K>>> neighbors = new HashMap<>();
	
	public void addEdge(Edge<K> edge){
		if(!neighbors.containsKey(edge.getSource())) neighbors.put(edge.getSource(), new HashSet<>());
		if(!neighbors.containsKey(edge.getTarget())) neighbors.put(edge.getTarget(), new HashSet<>());
		neighbors.get(edge.getSource()).add(edge);
	}
	
	public Collection<K> getNodes(){
		return neighbors.keySet();
	}
	
	public Collection<Edge<K>> getNeighbors(K n){
		assert(neighbors.containsKey(n));
		return neighbors.get(n);
	}

	public void removeNode(K k) {
		neighbors.remove(k);
		for(HashSet<Edge<K>> edges : neighbors.values()){
			final Iterator<Edge<K>> iter = edges.iterator();
			while(iter.hasNext()){
				if(iter.next().getTarget().equals(k)){
					iter.remove();
				}
			}
		}
	}
	
	public void clear(){
		neighbors.clear();
	}
	
	public final void addEdge(K src, K target){
		addEdge(src, target, 1);
	}
	
	public final void addEdge(K src, K target, int weight){
		addEdge(src, target, weight, true);
	}
	
	public final void addEdge(K src, K target, int weight, boolean bidirectional){
		addEdge(new Edge<>(src, target, weight));
		if(bidirectional) addEdge(new Edge<>(target, src, weight));
	}

	public final Path<K> dijkstras(K source, K target){
		return dijkstras(source, target, e -> (double) e.getWeight());
	}

	public final Path<K> dijkstras(K source, K target, Function<Edge<K>, Double> cost){
		return dijkstras(source, target, cost, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}
	
	public final Path<K> dijkstras(K source, K target, Function<Edge<K>, Double> cost, double maxPathCost, int maxPathLength){
		if(!neighbors.containsKey(source) || !neighbors.containsKey(target))
			return new Path<>();
		
		final HashMap<K, Double> distances = new HashMap<>();
		final HashMap<K, Integer> lengths = new HashMap<>();
		final HashMap<K, Edge<K>> prev = new HashMap<>();

		distances.put(source, 0d);
		lengths.put(source, 1);
		prev.put(source, null);

		final PriorityQueue<K> queue = new PriorityQueue<>(Comparator.comparing(k -> distances.get(k)));
		queue.add(source);

		while(!queue.isEmpty()){
			final K cur = queue.poll();
			final double currentCost = distances.get(cur);
			final int currentLength = lengths.get(cur);
			if(currentLength == maxPathLength) continue;
			if(getNeighbors(cur) == null){
				System.out.println("getNeighbors() for " + cur + " returns null!! :(");
			}
			for(Edge<K> edge : getNeighbors(cur)){
				final K next = edge.getTarget();
				final double edgeCost = cost.apply(edge);
				if(currentCost + edgeCost < distances.getOrDefault(next, Double.MAX_VALUE)
						&& currentCost + edgeCost <= maxPathCost){
					distances.put(next, currentCost + edgeCost);
					lengths.put(next, currentLength + 1);
					prev.put(next, edge);
					queue.remove(next);
					queue.add(next);
				}
			}
		}
		
		ArrayList<Edge<K>> path = new ArrayList<>();
		Edge<K> cur = prev.get(target);
		
		while(cur != null){
			path.add(cur);
			cur = prev.get(cur.getSource());
		}
		Collections.reverse(path);
		return new Path<>(path);
	}

	@Deprecated
	public Graph<K> subgraphWithNodes(Collection<K> nodes){
		return subgraphWithNodes(new Graph<>(), nodes);
	}

	@Deprecated
	public <G extends Graph<K>> G subgraphWithNodes(G g, Collection<K> nodes) {
		for(K k : nodes)
			for(Edge<K> e : getNeighbors(k))
				if(nodes.contains(e.getTarget()))
					g.addEdge(k, e.getTarget(), e.getWeight(), false);
		return g;
	}
	
	public Graph<K> subgraphWithEdges(Collection<Edge<K>> edges){
		return subgraphWithEdges(new Graph<>(), edges);
	}

	public <G extends Graph<K>> G subgraphWithEdges(G g, Collection<Edge<K>> edges) {
		for(Edge<K> e : edges)
			g.addEdge(e);
		return g;
	}

}
