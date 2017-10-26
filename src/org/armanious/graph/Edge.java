package org.armanious.graph;

public class Edge<K> {
	
	private final K src;
	private final K target;
	private int weight;
	
	public Edge(K src, K target){
		this(src, target, 1);
	}
	
	public Edge(K src, K target, int weight){
		this.src = src;
		this.target = target;
		this.weight = weight;
	}
	
	public K getSource(){
		return src;
	}
	
	public K getTarget(){
		return target;
	}
	
	public int getWeight(){
		return weight;
	}
	
	protected void setWeight(int weight){
		this.weight = weight;
	}
	
	public String toString(){
		return "(" + src.toString() + ", " + weight + ", " + target.toString() + ")";
	}
	
	@SuppressWarnings("rawtypes")
	public boolean equals(Object o){
		return o instanceof Edge && 
				((Edge)o).src.equals(src) && 
				((Edge)o).target.equals(target) &&
				((Edge)o).weight == weight;
	}
	
	public int hashCode(){
		return toString().hashCode();
	}

}
