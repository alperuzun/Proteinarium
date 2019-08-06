package org.armanious.graph;

import java.util.Collection;
import java.util.HashMap;

public class AnnotatedGraph<K extends Comparable<K>, T extends Comparable<T>> extends Graph<K> {
	
	private final HashMap<K, T> annotations = new HashMap<>();
	private final T defaultAnnotation;
	
	public AnnotatedGraph(T defaultAnnotation, double maxPathCost, int maxPathLength){
		super(maxPathCost, maxPathLength);
		this.defaultAnnotation = defaultAnnotation;
	}
	
	public void setAnnotation(K k, T t){
		annotations.put(k, t);
	}
	
	public T getAnnotation(K k){
		return annotations.get(k);
	}
	
	public void removeVertex(K k){
		super.removeVertex(k);
		annotations.remove(k);
	}
	
	public void addEdge(Edge<K> edge) {
		super.addEdge(edge);
		if(!annotations.containsKey(edge.getSource())) annotations.put(edge.getSource(), defaultAnnotation);
		if(!annotations.containsKey(edge.getTarget())) annotations.put(edge.getTarget(), defaultAnnotation);
	}
	
	@Override
	Graph<K> emptyGraph() {
		return new AnnotatedGraph<>(defaultAnnotation, maxPathCost, maxPathLength);
	}
	
	@Override
	public Graph<K> reduceByPaths(Collection<K> endpoints, int maxVertices, boolean bidirectional) {
		final Graph<K> g = super.reduceByPaths(endpoints, maxVertices, bidirectional);
		assert(g instanceof AnnotatedGraph);
		@SuppressWarnings("unchecked")
		final AnnotatedGraph<K, T> ag = (AnnotatedGraph<K, T>) g;
		for (K vertex : ag.getVertices()) {
			ag.setAnnotation(vertex, this.getAnnotation(vertex));
		}
		return g;
	}

	@Override
	public void clear(){
		super.clear();
		annotations.clear();
	}

}
