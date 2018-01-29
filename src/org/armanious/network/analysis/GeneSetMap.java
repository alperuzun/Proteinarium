package org.armanious.network.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.armanious.Tuple;
import org.armanious.graph.LayeredGraph;
import org.armanious.graph.Path;

public class GeneSetMap {
	
	private final boolean group1;
	private final Map<String, GeneSet> geneSetMap;
	private final Set<Gene> uniqueGenes;
	private final Set<Protein> uniqueProteins;
	
	public GeneSetMap(boolean group1){
		this(Collections.emptyMap(), group1);
	}
	
	public GeneSetMap(Map<String, ? extends Collection<String>> map, Function<String, Gene> geneDatabase, boolean group1){
		this.group1 = group1;
		geneSetMap = new HashMap<>();
		uniqueGenes = new HashSet<>();
		uniqueProteins = new HashSet<>();
		for(String id : map.keySet()){
			final GeneSet geneSet = new GeneSet(map.get(id), geneDatabase);
			geneSetMap.put(id, geneSet);
			uniqueGenes.addAll(geneSet.getGenes());
			uniqueProteins.addAll(geneSet.getProteins());
		}
	}
	
	public GeneSetMap(Map<String, ? extends Collection<Gene>> map, boolean group1){		
		this.group1 = group1;
		geneSetMap = new HashMap<>();
		uniqueGenes = new HashSet<>();
		uniqueProteins = new HashSet<>();
		for(String id : map.keySet()){
			final GeneSet geneSet = new GeneSet(map.get(id));
			geneSetMap.put(id, geneSet);
			uniqueGenes.addAll(geneSet.getGenes());
			uniqueProteins.addAll(geneSet.getProteins());
		}
	}
	
	public void computePairwisePathsAndGraph(Function<Tuple<Protein, Protein>, Path<Protein>> pairwisePathInitializer){
		for(GeneSet geneSet : geneSetMap.values())
			geneSet.computePairwisePathsAndGraph(pairwisePathInitializer);
	}
	
	public Map<String, GeneSet> getGeneSetMap(){
		return geneSetMap;
	}
	
	public LayeredGraph<Protein> getLayeredGraph(){
		return getLayeredGraph(geneSetMap.keySet());
	}
	
	private static String tmp(Collection<Protein> coll){
		final StringBuilder sb = new StringBuilder();
		for(Protein c : coll){
			sb.append(c.getGene() != null ? c.getGene().getSymbol() : c.getId()).append(", ");
		}
		return sb.toString();
	}
	
	public LayeredGraph<Protein> getLayeredGraph(Collection<String> geneSetIdentifiers){
		final LayeredGraph<Protein> graph = new LayeredGraph<>(group1 ? LayeredGraph.Type.GROUP1 : LayeredGraph.Type.GROUP2);
		for(String id : geneSetIdentifiers)
			graph.addGraph(geneSetMap.get(id).getGraph());
		return graph;
	}
	
	public Set<Gene> getUniqueGenes(){
		return uniqueGenes;
	}
	
	public Set<Protein> getUniqueProteins(){
		return uniqueProteins;
	}
	
	public GeneSetMap subset(Collection<String> keys){
		final GeneSetMap gsm = new GeneSetMap(group1);
		for(String key : keys){
			GeneSet gs = geneSetMap.get(key);
			gsm.uniqueGenes.addAll(gs.getGenes());
			gsm.uniqueProteins.addAll(gs.getProteins());
			gsm.geneSetMap.put(key, gs);
		}
		return gsm;
	}
	
	public static GeneSetMap loadFromFile(String geneSetGroupFile, Function<String, Gene> geneDatabase, boolean group1) throws IOException {
		final Map<String, List<String>> geneSetMap = new HashMap<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(geneSetGroupFile))){
			String s;
			while((s = br.readLine()) != null){
				final int idx = s.indexOf('=');
				if(idx == -1)
					throw new RuntimeException("Input gene set file " + geneSetGroupFile + " is not validly formed.");
				final String[] geneSymbols = s.substring(idx + 1).split(",");
				geneSetMap.put(s.substring(0, idx), Arrays.asList(geneSymbols));
			}
		}
		return new GeneSetMap(geneSetMap, geneDatabase, group1);
	}

}
