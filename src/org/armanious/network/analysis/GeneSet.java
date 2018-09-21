package org.armanious.network.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.armanious.Tuple;
import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.graph.Path;

public class GeneSet {
	
	private final Set<Gene> geneSet;
	private final Set<Protein> proteinSet;
	
	//private final Set<Path<Protein>> pairwisePathMap;
	
	private final Graph<Protein> graph;
	
	public GeneSet(Collection<String> symbols, Function<String, Gene> geneDatabase){
		assert(symbols.size() > 0);
		geneSet = new HashSet<>(symbols.size());
		proteinSet = new HashSet<>();
		for(String symbol : symbols){
			final Gene gene = geneDatabase.apply(symbol);
			if(gene != null){
				geneSet.add(gene);
				proteinSet.addAll(gene.getProteins());
			}else{
				System.err.println("[WARNING]: Cannot find gene " + symbol + " in STRING's database. " + 
						"Consider using the official HGNC symbol.");
			}
		}
		graph = new Graph<>();
		//pairwisePathMap = new HashSet<>();
	}
	
	public GeneSet(Collection<Gene> genes){		
		geneSet = new HashSet<>(genes);
		proteinSet = new HashSet<>();
		for(Gene gene : geneSet) proteinSet.addAll(gene.getProteins());
		graph = new Graph<>();
		//pairwisePathMap = new HashSet<>();
	}
	
	public Set<Gene> getGenes(){
		return geneSet;
	}
	
	public Set<Protein> getProteins(){
		return proteinSet;
	}
	
	public Graph<Protein> getGraph(){
		return graph;
	}
	
	//public Set<Path<Protein>> getPairwisePaths(){
		//return pairwisePathMap;
	//}
	
	public boolean computePairwisePathsAndGraph(Function<Tuple<Protein, Protein>, Path<Protein>> pairwisePathInitializer){
		graph.clear();
		final Protein[] endpoints = proteinSet.toArray(new Protein[proteinSet.size()]);
		for(int i = 0; i < endpoints.length - 1; i++){
			for(int j = i + 1; j < endpoints.length; j++){
				final Path<Protein> path = pairwisePathInitializer.apply(new Tuple<>(endpoints[i], endpoints[j]));			
				//pairwisePathMap.add(path);
				for(Edge<Protein> edge : path.getEdges()){
					graph.addEdge(edge.getSource(), edge.getTarget(), edge.getWeight(), true);
				}
			}
		}
		return graph.getNodes().size() > 0;
	}
	
}
