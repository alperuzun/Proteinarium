package org.armanious.network.analysis;

import java.util.List;
import java.util.Set;

import org.armanious.Tuple;

public class PhylogeneticTree {
	
	//private final PhylogeneticTreeNode root;
	//private final Set<PhylogeneticTreeNode> leaves;
	
	private PhylogeneticTree(){}
	
	public static PhylogeneticTreeNode createTreeFromMatrix(DistanceMatrix<PhylogeneticTreeNode> d){
		d = d.clone();
		PhylogeneticTreeNode root = null;
		//leaves = d.getAllEntities();
		while(!d.isEmpty()){
			
			final Tuple<PhylogeneticTreeNode, PhylogeneticTreeNode> min = d.getMinimumDistanceEntry();
			final double dist = d.getDistance(min.val1(), min.val2());
			root = new PhylogeneticTreeNode("(" + min.val1() + "," + min.val2() + ")",
					min.val1().getWeight() + min.val2().getWeight(),
					dist / 2);
			
			root.setChildren(min.val1(), min.val2());
			
			final Set<PhylogeneticTreeNode> allNodes = d.getAllEntities();
			allNodes.remove(min.val1());
			allNodes.remove(min.val2());
			
			for(PhylogeneticTreeNode ptn : allNodes){
				d.setDistance(root, ptn,
						(d.getDistance(min.val1(), ptn)*min.val1().getWeight() + d.getDistance(min.val2(), ptn)*min.val2().getWeight())
							/ (min.val1().getWeight() + min.val2().getWeight())
						);
			}
			
			d.removeAllAssociated(min.val1());
			d.removeAllAssociated(min.val2());
		}
		return root;
	}

	/*public String getStringRepresentation() {
		return root == null ? "" : root.getLabel();
	}*/

}
