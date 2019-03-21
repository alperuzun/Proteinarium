package org.armanious.network.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.armanious.graph.Graph;
import org.armanious.graph.LayeredGraph;

public class GeneSetMap {
	
	private final LayeredGraph.Type type;
	private final Map<String, GeneSet> geneSetMap;
	private final Set<Gene> uniqueGenes;
	private final Set<Protein> uniqueProteins;
	
	private final double maxPathCost;
	private final int maxPathLength;
	
	private GeneSetMap(Map<String, GeneSet> existingMap, LayeredGraph.Type type, boolean unused, double maxPathCost, int maxPathLength) {
		this.type = type;
		this.geneSetMap = existingMap;
		this.uniqueGenes = new HashSet<>();
		this.uniqueProteins = new HashSet<>();
		this.maxPathCost = maxPathCost;
		this.maxPathLength = maxPathLength;
		
		for(GeneSet geneSet : existingMap.values()) {
			uniqueGenes.addAll(geneSet.getGenes());
			uniqueProteins.addAll(geneSet.getProteins());
		}
	}
	
	public GeneSetMap(LayeredGraph.Type type, double maxPathCost, int maxPathLength){
		this(Collections.emptyMap(), type, maxPathCost, maxPathLength);
	}
	
	public GeneSetMap(Map<String, ? extends Collection<String>> map, Function<String, Gene> geneDatabase, LayeredGraph.Type type,
			double maxPathCost, int maxPathLength){
		this.type = type;
		this.maxPathCost = maxPathCost;
		this.maxPathLength = maxPathLength;
		geneSetMap = new HashMap<>();
		uniqueGenes = new HashSet<>();
		uniqueProteins = new HashSet<>();
		for(String id : map.keySet()){
			final GeneSet geneSet = new GeneSet(map.get(id), geneDatabase, new Graph<>(maxPathCost, maxPathLength));
			geneSetMap.put(id, geneSet);
			uniqueGenes.addAll(geneSet.getGenes());
			uniqueProteins.addAll(geneSet.getProteins());
		}
	}
	
	public GeneSetMap(Map<String, ? extends Collection<Gene>> map, LayeredGraph.Type type,
			double maxPathCost, int maxPathLength){		
		this.type = type;
		this.maxPathCost = maxPathCost;
		this.maxPathLength = maxPathLength;
		geneSetMap = new HashMap<>();
		uniqueGenes = new HashSet<>();
		uniqueProteins = new HashSet<>();
		for(String id : map.keySet()){
			final GeneSet geneSet = new GeneSet(map.get(id), new Graph<>(maxPathCost, maxPathLength));
			geneSetMap.put(id, geneSet);
			uniqueGenes.addAll(geneSet.getGenes());
			uniqueProteins.addAll(geneSet.getProteins());
		}
	}
	
	public void computePairwisePathsAndGraph(Pathfinder<Protein> pathfinder){
		final Set<String> toRemove = new HashSet<>();
		for(String patientKey : geneSetMap.keySet()){
			final GeneSet geneSet = geneSetMap.get(patientKey);
			if(!geneSet.computePairwisePathsAndGraph(pathfinder)){
				System.err.println("[WARNING] Patient " + patientKey + " has an empty graph; removing from analyses..." +
						"\n\tThis may be due to insufficient genes for " + patientKey + " or too restrictive path contraints to find a path between any two genes." +
						"\n\tConsider increasing maxPathLength or maxPathCost options.");
				toRemove.add(patientKey);
			}
		}
		toRemove.stream().forEach(geneSetMap::remove);
	}
	
	public Map<String, GeneSet> getGeneSetMap(){
		return geneSetMap;
	}
	
	public LayeredGraph<Protein> getLayeredGraph(){
		return getLayeredGraph(geneSetMap.keySet());
	}
	
	public LayeredGraph<Protein> getLayeredGraph(Collection<String> geneSetIdentifiers){
		final LayeredGraph<Protein> graph = new LayeredGraph<>(type, maxPathCost, maxPathLength);
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
		final GeneSetMap gsm = new GeneSetMap(type, this.maxPathCost, this.maxPathLength);
		for(String key : keys){
			GeneSet gs = geneSetMap.get(key);
			gsm.uniqueGenes.addAll(gs.getGenes());
			gsm.uniqueProteins.addAll(gs.getProteins());
			gsm.geneSetMap.put(key, gs);
		}
		return gsm;
	}
	
	public static GeneSetMap loadFromFile(String geneSetGroupFile, Function<String, Gene> geneDatabase, LayeredGraph.Type type,
			double maxPathCost, int maxPathLength) throws IOException {
		final Map<String, List<String>> geneSetMap = new HashMap<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(geneSetGroupFile))){
			String s;
			while((s = br.readLine()) != null){
				s = s.trim();
				if(s.isEmpty() || s.startsWith("#") || s.startsWith("//")) continue;
				final int idx = s.indexOf('=');
				final String sampleIdentifier;
				if(idx == -1 || (sampleIdentifier = s.substring(0, idx).trim()).isEmpty())
					throw new RuntimeException("Input gene set file " + geneSetGroupFile + " is not validly formed.");
				final String[] geneSymbols = s.substring(idx + 1).split(",");
				final ArrayList<String> geneSymbolsList = new ArrayList<>();
				for(int i = 0; i < geneSymbols.length; i++) {
					if (!geneSymbols[i].trim().isEmpty()) {
						geneSymbolsList.add(geneSymbols[i].trim());
					}
				}
				if(geneSetMap.put(sampleIdentifier, Arrays.asList(geneSymbols)) != null){
					System.err.println("Cannot have duplicate patient identifier: " + s.substring(0, idx));
					System.exit(1);
				}
			}
		}
		return new GeneSetMap(geneSetMap, geneDatabase, type, maxPathCost, maxPathLength);
	}
	
	public static GeneSetMap fromExistingMap(Map<String, GeneSet> geneSetMap, LayeredGraph.Type type, double maxPathCost, int maxPathLength) {
		return new GeneSetMap(geneSetMap, type, false, maxPathCost, maxPathLength);
	}

}
