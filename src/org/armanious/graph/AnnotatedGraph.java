package org.armanious.graph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

public class AnnotatedGraph<K, T extends Comparable<T>> extends Graph<K> {
	
	private final HashMap<K, T> annotations = new HashMap<>();
	private final T defaultAnnotation;
	
	public AnnotatedGraph(T defaultAnnotation){
		this.defaultAnnotation = defaultAnnotation;
	}
	
	public void setAnnotation(K k, T t){
		annotations.put(k, t);
	}
	
	public T getAnnotation(K k){
		return annotations.get(k);
	}
	
	public void removeNode(K k){
		super.removeNode(k);
		annotations.remove(k);
	}
	
	public void addEdge(Edge<K> edge) {
		super.addEdge(edge);
		if(!annotations.containsKey(edge.getSource())) annotations.put(edge.getSource(), defaultAnnotation);
		if(!annotations.containsKey(edge.getTarget())) annotations.put(edge.getTarget(), defaultAnnotation);
	}
	
	@Override
	void saveNodeState(BufferedWriter bw, K node) throws IOException {
		bw.write(String.valueOf(node) + "\t" + getAnnotation(node));
	}

}
