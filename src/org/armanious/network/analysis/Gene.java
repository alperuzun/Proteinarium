package org.armanious.network.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Gene {

	private static HashMap<Integer, Gene> genesById;
	private static HashMap<String, Gene> genesBySymbol;
	
	private final int hgncId;
	private final String symbol;
	private final ArrayList<Protein> proteins = new ArrayList<>();
	
	private Gene(int hgncId, String symbol){
		if(genesById.containsKey(hgncId) || genesBySymbol.containsKey(symbol))
			throw new IllegalStateException("Gene " + hgncId + " already loaded");
		this.hgncId = hgncId;
		this.symbol = symbol;
		genesById.put(hgncId, this);
		genesBySymbol.put(symbol, this);
	}
	
	public int getHgncId(){
		return hgncId;
	}
	
	public String getSymbol(){
		return symbol;
	}
	
	public void addProtein(Protein protein){
		proteins.add(protein);
	}
	
	public Collection<Protein> getProteins(){
		return proteins;
	}
	
	public String toString(){
		return getSymbol();
	}
	
	public int hashCode(){
		return hgncId;
	}
	
	public boolean equals(Object o){
		return o instanceof Gene && o.toString().equals(this.toString());
	}
	
	public static Gene[] getGenes(){
		return genesById.values().toArray(new Gene[genesById.size()]);
	}
	
	public static Gene getGene(String symbol){
		if(genesBySymbol == null)
			throw new IllegalStateException("Gene database not yet initialized");
		assert(genesBySymbol.containsKey(symbol));
		return genesBySymbol.get(symbol);
	}
	
	public static Gene getGene(int id){
		if(genesById == null)
			throw new IllegalStateException("Gene database not yet initialized");
		assert(genesById.containsKey(id));
		return genesById.get(id);
	}
	
	public static Gene[] getGenes(String...ids){
		if(ids == null) return null;
		final Gene[] arr = new Gene[ids.length];
		for(int i = 0; i < ids.length; i++){
			try{
				arr[i] = Gene.getGene(Integer.parseInt(ids[i]));
			}catch(NumberFormatException e){
				arr[i] = Gene.getGene(ids[i]);
			}
		}
		return arr;
	}
	
	public static void initializeGeneDatabase(File f) throws IOException {
		initializeGeneDatabase(new FileReader(f));
	}
	
	public static void initializeGeneDatabase(Reader r) throws IOException {
		if(genesById != null)
			throw new IllegalStateException("Gene database already initialized");
		genesById = new HashMap<>();
		genesBySymbol = new HashMap<>();
		System.out.print("Initializing gene database...");
		System.out.flush();
		if(!(r instanceof BufferedReader))
			r = new BufferedReader(r);
		BufferedReader br = (BufferedReader) r;
		String s;
		while((s = br.readLine()) != null){
			final String[] parts = s.split("\t");
			if(Protein.getProtein(parts[0]) != null) continue;
			final int id = Integer.parseInt(parts[1].substring(5));
			Gene gene = genesById.get(id);
			if(gene == null) gene = new Gene(id, parts[2]);
			gene.addProtein(new Protein(parts[0], gene));
		}
		System.out.println("finished");
	}

}
