package org.armanious.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class SimpleGraph<K> extends Graph<K> {
	
	final HashMap<K, HashSet<Edge<K>>> neighbors = new HashMap<>();
	
	public void addEdge(K src, K target){
		addEdge(src, target, 1, true);
	}
	
	public void addEdge(K src, K target, int weight){
		addEdge(src, target, weight, true);
	}
	
	public void addEdge(K src, K target, int weight, boolean bidirectional){
		if(!neighbors.containsKey(src)) neighbors.put(src, new HashSet<>());
		neighbors.get(src).add(new Edge<>(src, target, weight));
		if(bidirectional) addEdge(target, src, weight, false);
	}
	
	public Collection<K> getNodes(){
		return neighbors.keySet();
	}
	
	public Collection<Edge<K>> getNeighbors(K n){
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
	
}
