package org.armanious.graph;

import java.util.Collection;
import java.util.HashSet;

public final class LayeredGraph<K extends Comparable<K>> extends AnnotatedGraph<K, Double> {
	
	public static enum Type {
		GROUP1,
		GROUP2,
		COMBINED,
		GROUP1_MINUS_GROUP2,
		GROUP2_MINUS_GROUP1,
		BOOTSTRAP;
	}

	private final Type type;
	private double maxCount = 0;

	public LayeredGraph(Type type, double maxPathCost, int maxPathLength){
		super(0D, maxPathCost, maxPathLength);
		this.type = type;
	}
	
	public Type getType(){
		return type;
	}

	@Override
	public void addEdge(Edge<K> edge) {
		super.addEdge(edge);
		setCount(edge.getSource(), getCount(edge.getSource()) + 1);
		setCount(edge.getTarget(), getCount(edge.getTarget()) + 1);
	}

	public void addEdgeNoIncrement(Edge<K> edge){
		super.addEdge(edge);
	}

	public void addGraph(Graph<K> graph){
		if(graph instanceof LayeredGraph)
			throw new UnsupportedOperationException("Cannot layer LayeredGraphs");
		for(K k : graph.getVertices())
			for(Edge<K> e : graph.getNeighbors(k))
				addEdgeNoIncrement(e);
		for(K k : graph.getVertices()){
			setCount(k, getCount(k) + 1);
		}
	}

	public double getCount(K vertex){
		return getAnnotation(vertex);
	}

	public void setCount(K vertex, double count){
		setAnnotation(vertex, count);
		if(count > maxCount) maxCount = count;
	}

	//TODO edge-based layered graph
	public LayeredGraph<K> subtract(LayeredGraph<K> lg, double lhsFactor, double rhsFactor){
		assert(type == Type.GROUP1 || type == Type.GROUP2);
		assert(lg.type == Type.GROUP1 || lg.type == Type.GROUP2);
		assert(type != lg.type);
		final LayeredGraph<K> result = new LayeredGraph<>(type == Type.GROUP1 ? Type.GROUP1_MINUS_GROUP2 : Type.GROUP2_MINUS_GROUP1, maxPathCost, maxPathLength);
		final HashSet<K> toRetain = new HashSet<>();
		
		for(K vertex : getVertices())
			if(!lg.getVertices().contains(vertex) || lhsFactor * getCount(vertex) > rhsFactor * lg.getCount(vertex))
				toRetain.add(vertex);
		for(K vertex : toRetain)
			for(Edge<K> edge : neighbors.get(vertex))
				if(toRetain.contains(edge.getSource()) && toRetain.contains(edge.getTarget()))
					result.addEdge(edge);
		for(K vertex : toRetain)
			result.setCount(vertex, lhsFactor * getCount(vertex) - rhsFactor * (lg.getVertices().contains(vertex) ? lg.getCount(vertex) : 0));
		return result;
	}

	public double getMaxCount() {
		return maxCount;
	}

	@Override
	public <G extends Graph<K>> G subgraphWithEdges(G g, Collection<Edge<K>> edges) {
		g = super.subgraphWithEdges(g, edges);
		if(g instanceof LayeredGraph){
			final LayeredGraph<K> lg = (LayeredGraph<K>) g;
			for(K vertex : lg.getVertices())
				lg.setCount(vertex, getCount(vertex));
		}
		return g;
	}
	
	private void resetMaxCount() {
		double maxCount = 0;
		for (K vertex : getVertices()) {
			if (getCount(vertex) > maxCount) {
				maxCount = getCount(vertex);
			}
		}
		this.maxCount = maxCount;
	}
	
	@Override
	public Graph<K> reduceByPaths(Collection<K> endpoints, int maxVertices, boolean bidirectional) {
		final Graph<K> g = super.reduceByPaths(endpoints, maxVertices, bidirectional);
		assert(g instanceof LayeredGraph);
		((LayeredGraph<K>) g).resetMaxCount();
		return g;
	}
	
	@Override
	Graph<K> emptyGraph() {
		return new LayeredGraph<>(type, maxCount, maxPathLength);
	}

}
