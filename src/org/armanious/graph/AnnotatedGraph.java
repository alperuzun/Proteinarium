package org.armanious.graph;

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
	public void clear(){
		super.clear();
		annotations.clear();
	}

}
