package org.armanious.graph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.function.Function;

import org.armanious.Tuple;

public abstract class Graph<K> {
	
	public abstract void addEdge(K src, K target, int weight, boolean bidirectional);
	
	public abstract Collection<K> getNodes();
	
	public abstract Collection<Edge<K>> getNeighbors(K n);
	
	public Tuple<ArrayList<K>, Integer> dijkstras(K source, K target){
		return dijkstras(source, target, e -> e.getWeight());
	}
	
	public Tuple<ArrayList<K>, Integer> dijkstras(K source, K target, Function<Edge<K>, Integer> cost){
		return dijkstras(source, target, cost, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}
	
	public Tuple<ArrayList<K>, Integer> dijkstras(K source, K target, Function<Edge<K>, Integer> cost, int maxPathCost, int maxPathLength){
		final HashMap<K, Integer> distances = new HashMap<>();
		final HashMap<K, Integer> lengths = new HashMap<>();
		final HashMap<K, K> prev = new HashMap<>();
		
		distances.put(source, 0);
		lengths.put(source, 1);
		prev.put(source, null);
		
		final PriorityQueue<K> queue = new PriorityQueue<>(Comparator.comparing(k -> distances.get(k)));
		queue.add(source);
		
		while(!queue.isEmpty()){
			final K cur = queue.poll();
			final int currentCost = distances.get(cur);
			final int currentLength = lengths.get(cur);
			if(currentLength == maxPathLength) continue;
			for(Edge<K> edge : getNeighbors(cur)){
				final K next = edge.getTarget();
				final int edgeCost = cost.apply(edge);
				if(currentCost + edgeCost < distances.getOrDefault(next, Integer.MAX_VALUE)
						&& currentCost + edgeCost <= maxPathCost){
					distances.put(next, currentCost + edgeCost);
					lengths.put(next, currentLength + 1);
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
	
	public SimpleGraph<K> subgraphWithNodes(Collection<K> nodes){
		return subgraphWithNodes(new SimpleGraph<>(), nodes);
	}
	
	public <G extends Graph<K>> G subgraphWithNodes(G g, Collection<K> nodes) {
		for(K k : nodes)
			for(Edge<K> e : getNeighbors(k))
				if(nodes.contains(e.getTarget()))
					g.addEdge(k, e.getTarget(), e.getWeight(), false);
		for(K node : nodes)
			if(getNeighbors(node).size() == 0)
				getNeighbors(node).remove(node);
		return g;
	}
	
	void saveNodeState(BufferedWriter bw, K node) throws IOException {
		bw.write(String.valueOf(node));
	}
	
	void saveEdgeState(BufferedWriter bw, Edge<K> edge) throws IOException {
		bw.write(String.valueOf(edge.getSource()) + "\t" + String.valueOf(edge.getTarget()) + "\t" + String.valueOf(edge.getWeight()));
	}
	
	public void saveTo(String file) throws IOException {
		saveTo(new FileWriter(file));
	}
	
	public void saveTo(Writer out) throws IOException {
		final BufferedWriter bw = out instanceof BufferedWriter ? (BufferedWriter) out : new BufferedWriter(out);
		for(K node : getNodes()){
			saveNodeState(bw, node);
			bw.newLine();
		}
		for(K node : getNodes()){
			for(Edge<K> neighbor : getNeighbors(node)){
				saveEdgeState(bw, neighbor);
				bw.newLine();
			}
		}
		bw.flush();
		bw.close();
	}


}
