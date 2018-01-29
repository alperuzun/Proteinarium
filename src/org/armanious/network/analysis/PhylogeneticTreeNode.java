package org.armanious.network.analysis;

import java.util.LinkedList;
import java.util.List;

public final class PhylogeneticTreeNode implements Comparable<PhylogeneticTreeNode> {
	
	private final List<PhylogeneticTreeNode> leaves = new LinkedList<>();
	
	private final String label;
	private final double weight;
	private double height;
	
	private PhylogeneticTreeNode parent;
	private PhylogeneticTreeNode left;
	private PhylogeneticTreeNode right;
		
	public PhylogeneticTreeNode(String label, double weight){
		this(label, weight, 0);
	}
	
	public PhylogeneticTreeNode(String label, double weight, double height){
		if(label == null) throw new IllegalArgumentException("Label must not be null");
		this.label = label;
		this.weight = weight;
		this.height = height;
	}
	
	private void setParent(PhylogeneticTreeNode parent){
		this.parent = parent;
	}
	
	public String getLabel(){
		return label;
	}
	
	public double getWeight(){
		return weight;
	}
	
	public PhylogeneticTreeNode getParent(){
		return parent;
	}
	
	public PhylogeneticTreeNode getLeftChild(){
		return left;
	}
	
	public PhylogeneticTreeNode getRightChild(){
		return right;
	}
	
	public double getHeight(){
		return height;
	}
	
	public boolean equals(Object o){
		return o != null && o instanceof PhylogeneticTreeNode && ((PhylogeneticTreeNode)o).label.equals(label);
	}
	
	public PhylogeneticTreeNode[] getLeaves(){
		return leaves.toArray(new PhylogeneticTreeNode[leaves.size()]);
	}
	
	public int hashCode(){
		return label.hashCode();
	}
	
	@Override
	public String toString() {
		return label;
	}

	@Override
	public int compareTo(PhylogeneticTreeNode o) {
		return label.compareTo(o.label);
	}

	public void setChildren(PhylogeneticTreeNode left, PhylogeneticTreeNode right){
		if(left.leaves.isEmpty())
			leaves.add(left);
		else
			leaves.addAll(left.leaves);
		if(right.leaves.isEmpty())
			leaves.add(right);
		else
			leaves.addAll(right.leaves);
		this.left = left;
		this.right = right;
		left.setParent(this);
		right.setParent(this);
	}

}
