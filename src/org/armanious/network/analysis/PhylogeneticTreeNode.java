package org.armanious.network.analysis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public final class PhylogeneticTreeNode implements Comparable<PhylogeneticTreeNode> {
	
	private final List<PhylogeneticTreeNode> leaves = new LinkedList<>();
	
	private final String label;
	private final double weight;
	private double height;
	
	private PhylogeneticTreeNode parent;
	private PhylogeneticTreeNode left;
	private PhylogeneticTreeNode right;
	
	private int bootstrappingHits;
	private int bootstrappingRounds;
		
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

	public double getBootstrappingConfidence() {
		return this.bootstrappingRounds == 0 ? Double.NaN: 
			(double) this.bootstrappingHits / this.bootstrappingRounds;
	}
	
	private final String flattenCluster(PhylogeneticTreeNode ptn) {
		final String[] leaves = ptn.leaves.stream().map(leaf -> leaf.getLabel()).toArray(String[]::new);
		Arrays.sort(leaves);
		return String.join(",", leaves);
	}

	public void updateWithBootstrapRound(PhylogeneticTreeNode bootstrapTree) {		
		final Set<String> flattenedClusters = new HashSet<>();
		
		final Stack<PhylogeneticTreeNode> bootstrapTreeDfs = new Stack<>();
		if (bootstrapTree != null) bootstrapTreeDfs.push(bootstrapTree);
		
		while(!bootstrapTreeDfs.isEmpty()) {
			final PhylogeneticTreeNode ptn = bootstrapTreeDfs.pop();
			flattenedClusters.add(flattenCluster(ptn));
			if(ptn.left != null) bootstrapTreeDfs.push(ptn.left);
			if(ptn.right != null) bootstrapTreeDfs.push(ptn.right);
		}
		
		final Stack<PhylogeneticTreeNode> realTreeDfs = new Stack<>();
		realTreeDfs.push(this);
		
		while(!realTreeDfs.isEmpty()) {
			final PhylogeneticTreeNode ptn = realTreeDfs.pop();
			if(flattenedClusters.contains(flattenCluster(ptn))) ptn.bootstrappingHits++;
			ptn.bootstrappingRounds++;
			if(ptn.left != null) realTreeDfs.push(ptn.left);
			if(ptn.right != null) realTreeDfs.push(ptn.right);
		}
		
	}
	
	public PhylogeneticTreeNode clone() {
		if(left != null || right != null) throw new IllegalArgumentException("Can only clone leaves!");
		return new PhylogeneticTreeNode(label, weight);
	}
	
}
