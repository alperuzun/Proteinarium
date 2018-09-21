package org.armanious.network.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.armanious.Tuple;
import org.armanious.network.Configuration;

public class PhylogeneticTree {
	
	//private final PhylogeneticTreeNode root;
	//private final Set<PhylogeneticTreeNode> leaves;
	
	private PhylogeneticTree(){}
	
	public static PhylogeneticTreeNode createTreeFromMatrix(DistanceMatrix<PhylogeneticTreeNode> distanceMatrix){
		final DistanceMatrix<PhylogeneticTreeNode> d = distanceMatrix.clone(x -> x.clone());
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
	
	public static Map<String, ClusterAnalysis> recursivelyAnalyzeClusters(Configuration c, PhylogeneticTreeNode root, DistanceMatrix<PhylogeneticTreeNode> distances, GeneSetMap group1, GeneSetMap group2, GeneSetMap combined){
		final Map<String, ClusterAnalysis> map = new HashMap<>();
		final FisherExact fe = new FisherExact(group1.getGeneSetMap().size() + group2.getGeneSetMap().size());
		recursivelyAnalyzeClusters(c, root, map, distances, group1, group2, combined, fe, root.getHeight());
		for(PhylogeneticTreeNode ptn : root.getLeaves())
			map.put(ptn.getLabel(), new ClusterAnalysis(c, ptn.getLabel(), ptn, distances, group1, group2, combined, fe, root.getHeight()));		
		return map;
	}
	
	private static void recursivelyAnalyzeClusters(Configuration c, PhylogeneticTreeNode root, Map<String, ClusterAnalysis> map, DistanceMatrix<PhylogeneticTreeNode> distances, GeneSetMap group1, GeneSetMap group2, GeneSetMap combined, FisherExact fe, double maxHeight){
		if(root.getLeaves().length == 0) return;
		final ClusterAnalysis ca = new ClusterAnalysis(c, "C" + (map.size() + 1), root, distances, group1, group2, combined, fe, maxHeight);
		map.put(ca.getClusterId(), ca);
		recursivelyAnalyzeClusters(c, root.getLeftChild(), map, distances, group1, group2, combined, fe, maxHeight);
		recursivelyAnalyzeClusters(c, root.getRightChild(), map, distances, group1, group2, combined, fe, maxHeight);
	}

}
