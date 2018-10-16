package org.armanious.network.analysis;

public class Protein implements Comparable<Protein> {
	
	private final Gene gene;
	private final String id;
	
	protected Protein(Gene gene, String id){
		this.gene = gene;
		this.id = id;
	}
	
	public Gene getGene(){
		return gene;
	}
	
	public String getId(){
		return id;
	}
	
	@Override
	public String toString(){
		return getId();
	}
	
	@Override
	public boolean equals(Object o){
		return o instanceof Protein && equals((Protein)o);
	}
	
	public boolean equals(Protein p){
		return getId().equals(p.getId());
	}
	
	@Override
	public int hashCode(){
		return id.hashCode();
	}

	@Override
	public int compareTo(Protein p) {
		return id.compareTo(p.id);
	}

}
