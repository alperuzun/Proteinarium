package org.armanious.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Path<K> {
	
	private final ArrayList<Edge<K>> path;
	
	public Path(){
		this(Collections.emptyList());
	}
	
	public Path(Collection<Edge<K>> path){
		this.path = new ArrayList<>(path);
	}
	
	public void addEdge(Edge<K> edge){
		assert(!path.contains(edge));
		path.add(edge);
	}
	
	public List<Edge<K>> getEdges() {
		return new ArrayList<>(path);
	}
	
	public List<K> getNodes(){
		if(path.size() == 0) return Collections.emptyList();
		final ArrayList<K> nodes = new ArrayList<>(path.size() > 0 ? path.size() + 1 : 0);
		nodes.add(path.get(0).getSource());
		for(Edge<K> edge : path) nodes.add(edge.getTarget());
		return nodes;
	}
	
	public Set<K> getUniqueNodes(){
		if(path.size() == 0) return Collections.emptySet();
		return new HashSet<>(getNodes());
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		return (o instanceof Path) && ((Path<K>)o).path.equals(path);
	}

}
