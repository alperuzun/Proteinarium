package org.armanious.network.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.armanious.Tuple;

public class Gene {
	
	private final String symbol;
	private final Set<Protein> proteins = new HashSet<>();
	
	public Gene(String symbol){
		this.symbol = symbol;
	}
	
	public String getSymbol(){
		return symbol;
	}
	
	public Set<Protein> getProteins(){
		return Collections.unmodifiableSet(proteins);
	}
	
	public Protein addProtein(String protein){
		final Protein p = new Protein(this, protein);
		proteins.add(p);
		return p;
	}
	
	@Override
	public String toString(){
		return getSymbol();
	}
	
	@Override
	public int hashCode(){
		return getSymbol().hashCode() * 71 + getProteins().hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof Gene && equals((Gene)o);
	}
	
	public boolean equals(Gene gene){
		return getSymbol().equals(gene.getSymbol()) 
				&& getProteins().equals(gene.getProteins());
	}

	public static Tuple<Map<String, Gene>, Map<String, Protein>> loadGenes(String proteinAliasesFile) throws IOException {
		InputStream in = new FileInputStream(proteinAliasesFile);
		if(proteinAliasesFile.endsWith(".gz"))
			in = new GZIPInputStream(in);
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String s;
		final Map<String, Gene> geneMap = new HashMap<>();
		final Map<String, Protein> proteinMap = new HashMap<>();
		while((s = br.readLine()) != null){
			if(s.startsWith("#")) continue;
			final String[] parts = s.split("\t");
			if(parts[2].contains("BLAST_KEGG_NAME")){
				final String symbol = parts[1];
				final String protein = parts[0];
				Gene gene = geneMap.get(symbol);
				if(gene == null)
					geneMap.put(symbol, gene = new Gene(symbol));
				proteinMap.put(protein, gene.addProtein(protein));
			}
		}
		br.close();
		return new Tuple<>(geneMap, proteinMap);
	}

}
