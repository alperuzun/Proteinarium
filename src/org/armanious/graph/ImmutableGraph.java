package org.armanious.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ImmutableGraph<K> extends Graph<K> {
	
	private boolean isImmutable = false;
	private ArrayList<K> nodes;
	private Map<K, ArrayList<Edge<K>>> neighbors;
	
	public void addNode(K node){
		if(isImmutable)
			throw new IllegalStateException("Graph is immutable");
		if(node == null)
			throw new IllegalArgumentException("Node cannot be null");
		super.addNode(node);
	}
	
	public final void makeImmutable(){
		if(isImmutable)
			throw new IllegalStateException("Graph is already immutable");
		nodes = new ArrayList<>(super.neighbors.keySet());
		
		neighbors = new HashMap<>();
		for(K node : this.nodes){
			neighbors.put(node, new ArrayList<>(super.neighbors.get(node)));
		}
		super.neighbors.clear();
		isImmutable = true;
	}
	
	public final void addEdge(K src, K target){
		if(isImmutable)
			throw new IllegalStateException("Graph is immutable");
		super.addEdge(src, target);
	}
	
	public final void addEdge(K src, K target, int weight){
		if(isImmutable)
			throw new IllegalStateException("Graph is immutable");
		if(src == null || target == null)
			throw new IllegalArgumentException("Neither source nor target can be null");
		super.addEdge(src, target, weight);
	}
	
	public final void addEdge(K src, K target, int weight, boolean bidirectional){
		if(isImmutable)
			throw new IllegalStateException("Graph is immutable");
		super.addEdge(src, target, weight, bidirectional);
	}
	
	public final Collection<K> getNodes(){
		return isImmutable ? nodes : super.getNodes();
	}
	
	public final Collection<Edge<K>> getNeighbors(K n){
		return isImmutable ? neighbors.get(n) : super.getNeighbors(n);
	}

}
