package org.armanious.graph;

import java.util.Collection;
import java.util.HashSet;

public final class LayeredGraph<K> extends AnnotatedGraph<K, Double> {
	
	public static enum Type {
		GROUP1,
		GROUP2,
		GROUP1_MINUS_GROUP2,
		GROUP2_MINUS_GROUP1;
	}

	private final Type type;
	private double maxCount = 0;

	public LayeredGraph(Type type){
		super(0D);
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
		for(K k : graph.getNodes())
			for(Edge<K> e : graph.getNeighbors(k))
				addEdgeNoIncrement(e);
		for(K k : graph.getNodes()){
			setCount(k, getCount(k) + 1);
		}
	}

	public double getCount(K node){
		return getAnnotation(node);
	}

	public void setCount(K node, double count){
		setAnnotation(node, count);
		if(count > maxCount) maxCount = count;
	}

	//TODO edge-based layered graph
	public LayeredGraph<K> subtract(LayeredGraph<K> lg, double lhsFactor, double rhsFactor){
		assert(type == Type.GROUP1 || type == Type.GROUP2);
		assert(lg.type == Type.GROUP1 || lg.type == Type.GROUP2);
		assert(type != lg.type);
		final LayeredGraph<K> result = new LayeredGraph<>(type == Type.GROUP1 ? Type.GROUP1_MINUS_GROUP2 : Type.GROUP2_MINUS_GROUP1);
		final HashSet<K> toRetain = new HashSet<>();
		
		for(K node : getNodes())
			if(!lg.getNodes().contains(node) || lhsFactor * getCount(node) > rhsFactor * (lg.getNodes().contains(node) ? lg.getCount(node) : 0))
				toRetain.add(node);
		for(K node : toRetain)
			for(Edge<K> edge : neighbors.get(node))
				if(toRetain.contains(edge.getSource()) && toRetain.contains(edge.getTarget()))
					result.addEdge(edge);
		for(K node : toRetain)
			result.setCount(node, lhsFactor * getCount(node) - rhsFactor * (lg.getNodes().contains(node) ? lg.getCount(node) : 0));
		return result;
	}

	public double getMaxCount() {
		return maxCount;
	}

	@Override
	public <G extends Graph<K>> G subgraphWithNodes(G g, Collection<K> nodes) {
		g = super.subgraphWithNodes(g, nodes);
		if(g instanceof LayeredGraph){
			final LayeredGraph<K> lg = (LayeredGraph<K>) g;
			for(K node : lg.getNodes())
				lg.setCount(node, getCount(node));
		}
		return g;
	}

	@Override
	public <G extends Graph<K>> G subgraphWithEdges(G g, Collection<Edge<K>> edges) {
		g = super.subgraphWithEdges(g, edges);
		if(g instanceof LayeredGraph){
			final LayeredGraph<K> lg = (LayeredGraph<K>) g;
			for(K node : lg.getNodes())
				lg.setCount(node, getCount(node));
		}
		return g;
	}

}
