package org.armanious.graph;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

public class AnnotatedGraph<K, T extends Comparable<T>> extends SimpleGraph<K> {
	
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
	
	@Override
	public void addEdge(K src, K target, int weight, boolean bidirectional) {
		super.addEdge(src, target, weight, bidirectional);
		if(!annotations.containsKey(src)) annotations.put(src, defaultAnnotation);
		if(!annotations.containsKey(target)) annotations.put(target, defaultAnnotation);
	}
	
	@Override
	void saveNodeState(BufferedWriter bw, K node) throws IOException {
		bw.write(String.valueOf(node) + "\t" + getAnnotation(node));
	}

}
