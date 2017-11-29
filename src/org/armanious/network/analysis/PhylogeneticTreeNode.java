package org.armanious.network.analysis;

import java.util.LinkedList;
import java.util.List;

public final class PhylogeneticTreeNode implements Comparable<PhylogeneticTreeNode> {
	
	private final List<String> childLabels = new LinkedList<>();
	
	private final String label;
	private final int weight;
	private final double depth;
	
	private PhylogeneticTreeNode parent;
	private PhylogeneticTreeNode left;
	private PhylogeneticTreeNode right;
	
	public PhylogeneticTreeNode(String label, int weight){
		this(label, weight, 0);
	}
	
	public PhylogeneticTreeNode(String label, int weight, double depth){
		if(label == null) throw new IllegalArgumentException("Label must not be null");
		this.label = label;
		this.weight = weight;
		this.depth = depth;
	}
	
	public void setParent(PhylogeneticTreeNode parent){
		this.parent = parent;
	}
	
	public String getLabel(){
		return label;
	}
	
	public int getWeight(){
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
	
	public double getDepth(){
		return depth;
	}
	
	public boolean equals(Object o){
		return o != null && o instanceof PhylogeneticTreeNode && ((PhylogeneticTreeNode)o).label.equals(label);
	}
	
	public String[] getChildLabels(){
		return childLabels.toArray(new String[childLabels.size()]);
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
		if(left.getWeight() == 1)
			childLabels.add(left.label);
		else
			childLabels.addAll(left.childLabels);
		if(right.getWeight() == 1)
			childLabels.add(right.label);
		else
			childLabels.addAll(right.childLabels);
		this.left = left;
		this.right = right;
		left.setParent(this);
		right.setParent(this);
	}

}
