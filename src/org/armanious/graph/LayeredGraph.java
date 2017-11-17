package org.armanious.graph;

import java.util.Collection;
import java.util.HashSet;

public final class LayeredGraph<K> extends AnnotatedGraph<K, Integer> {
	
	private int maxCount = 0;
	
	public LayeredGraph(){
		super(0);
	}

	@Override
	public void addEdge(K src, K target, int weight, boolean bidirectional) {
		throw new UnsupportedOperationException("Can only add graphs to LayeredGraph");
	}

	public void forceAddEdge(K src, K target, int weight, boolean bidirectional){
		super.addEdge(src, target, weight, bidirectional);
	}

	public void addGraph(Graph<K> graph){
		if(graph instanceof LayeredGraph)
			throw new UnsupportedOperationException("Cannot layer LayeredGraphs");
		for(K k : graph.getNodes()){
			for(Edge<K> e : graph.getNeighbors(k)){
				forceAddEdge(k, e.getTarget(), e.getWeight(), false);
				setCount(k, getCount(k) + 1);
				setCount(e.getTarget(), getCount(e.getTarget()) + 1);
				if(getCount(k) > maxCount) maxCount = getCount(k);
				if(getCount(e.getTarget()) > maxCount) maxCount = getCount(e.getTarget());
			}
		}
	}

	public int getCount(K node){
		return getAnnotation(node);
	}
	
	public void setCount(K node, int count){
		setAnnotation(node, count);
		if(count > maxCount) maxCount = count;
	}

	public LayeredGraph<K> subtract(LayeredGraph<K> lg){
		final LayeredGraph<K> result = new LayeredGraph<>();
		final HashSet<K> toRetain = new HashSet<>();
		for(K node : getNodes())
			if(!lg.getNodes().contains(node) || getCount(node) > lg.getCount(node)) toRetain.add(node);
		for(K node : toRetain){
			for(Edge<K> neighbor : neighbors.get(node)){
				if(toRetain.contains(neighbor.getTarget())){
					result.forceAddEdge(node, neighbor.getTarget(), neighbor.getWeight(), false);
				}
			}
		}
		for(K node : toRetain){
			result.setCount(node, getCount(node) - (lg.getNodes().contains(node) ? lg.getCount(node) : 0));
			if(result.getCount(node) > result.maxCount)
				result.maxCount = result.getCount(node);
		}
		return result;
	}

	public int getMaxCount() {
		return maxCount;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <G extends Graph<K>> G subgraphWithNodes(G g, Collection<K> nodes) {
		if(!(g instanceof LayeredGraph)) return super.subgraphWithNodes(g, nodes);
		final LayeredGraph<K> lg = (LayeredGraph<K>) g;
		for(K node : nodes){
			for(Edge<K> edge : neighbors.get(node)){
				if(nodes.contains(edge.getTarget())){
					lg.forceAddEdge(node, edge.getTarget(), edge.getWeight(), false);
				}
			}
		}
		for(K node : lg.getNodes()){
			lg.setCount(node, getCount(node));
			if(lg.getCount(node) > lg.maxCount) lg.maxCount = lg.getCount(node);
		}
		return (G) lg;
	}

	//TODO
	//idea: when subtracting, have negatives literally become inverted in color
	//that way we can detect SNP's / whatever that is associated with healthiness?
	//or separate into two visual results: positive correlations and negative correlation

}
