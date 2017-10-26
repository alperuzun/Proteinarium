package org.armanious.network.analysis;

import java.util.HashMap;

public class Protein {

	private static final HashMap<String, Protein> proteins = new HashMap<>();

	private final String id;
	private final Gene gene;

	protected Protein(String id, Gene gene){
		if(proteins.containsKey(id))
			throw new IllegalStateException("Protein " + id + " already loaded");
		this.id = id;
		this.gene = gene;
		proteins.put(id, this);
	}

	public String getId(){
		return id;
	}

	public Gene getGene(){
		return gene;
	}

	public String toString(){
		return id;
	}

	public int hashCode(){
		return id.hashCode();
	}

	public boolean equals(Object o){
		return o instanceof Protein && o.toString().equals(this.toString());
	}

	public static Protein[] getProteins(){
		return proteins.values().toArray(new Protein[proteins.size()]);
	}

	public static Protein getProtein(String id){
		return getProtein(id, false);
	}
	
	public static Protein getProtein(String id, boolean forceReturn){
		Protein p = proteins.get(id);
		if(p == null && forceReturn){
			p = new Protein(id, null);
		}
		return p;
	}

	public Protein[] getProteins(String...ids){
		if(ids == null) return null;
		final Protein[] arr = new Protein[ids.length];
		for(int i = 0; i < ids.length; i++){
			arr[i] = Protein.getProtein(ids[i]);
		}
		return arr;
	}

}
