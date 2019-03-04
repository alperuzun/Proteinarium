package org.armanious.network.analysis;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.armanious.graph.LayeredGraph;
import org.armanious.network.Configuration;

public class ClusterAnalysis {

	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat();
	private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat();
	static {
		DECIMAL_FORMAT.setMinimumFractionDigits(6);
		DECIMAL_FORMAT.setMaximumFractionDigits(6);
		DECIMAL_FORMAT.setMinimumIntegerDigits(1);

		PERCENTAGE_FORMAT.setMinimumFractionDigits(4);
		PERCENTAGE_FORMAT.setMaximumFractionDigits(4);
		PERCENTAGE_FORMAT.setMultiplier(100);
	}

	private final String clusterId;
	private final PhylogeneticTreeNode node;
	private final boolean isLeaf;
	private final double normalizedHeight;
	private final int numberCombined;
	private final int numberGroup1;
	private final int numberGroup2;
	private final double weightGroup1;
	private final double weightGroup2;
	private final double pValue;
	private final double bootstrappingConfidence;

	private final double combinedClusteringCoefficient;
	private final double group1ClusteringCoefficient;
	private final double group2ClusteringCoefficient;
	private final double group1minusGroup2ClusteringCoefficient;
	private final double group2minusGroup1ClusteringCoefficient;

	private final int numDigits;
	private final String group1Patients;
	private final String group2Patients;
	
	private final boolean bootstrapped;

	public ClusterAnalysis(Configuration c,
			String clusterId,
			PhylogeneticTreeNode cluster,
			DistanceMatrix<PhylogeneticTreeNode> distances,
			GeneSetMap group1,
			GeneSetMap group2,
			GeneSetMap combined,
			FisherExact fe,
			double maxHeight){
		final PhylogeneticTreeNode[] nodeLeafs = cluster.getLeaves();
		this.isLeaf = nodeLeafs.length == 0;

		double weightGroup1 = 0;
		int numberGroup1 = 0;
		if(isLeaf){
			if(group1.getGeneSetMap().containsKey(clusterId)){
				numberGroup1 = 1;
				weightGroup1 = cluster.getWeight();
			}
		}else{
			for(int i = 0; i < nodeLeafs.length; i++){
				if(group1.getGeneSetMap().containsKey(nodeLeafs[i].getLabel())){
					weightGroup1 += nodeLeafs[i].getWeight();
					numberGroup1++;
				}
			}
		}
		
		this.bootstrapped = cluster.isBootstrapped();
		
		this.clusterId = clusterId;
		this.node = cluster;
		this.normalizedHeight = cluster.getHeight();// / maxHeight;
		this.numberGroup1 = numberGroup1;
		this.numberGroup2 = (isLeaf ? 1 : nodeLeafs.length) - numberGroup1;
		this.numberCombined = this.numberGroup1 + this.numberGroup2;
		this.weightGroup1 = weightGroup1;
		this.weightGroup2 = cluster.getWeight() - weightGroup1;

		this.pValue = fe.getTwoTailedP(
				numberGroup1, group1.getGeneSetMap().size() - numberGroup1,
				numberGroup2, group2.getGeneSetMap().size() - numberGroup2);
		
		this.bootstrappingConfidence = this.node.getBootstrappingConfidence();

		final double[] clusteringCoefficients = calculateClusteringCoefficients(c, cluster, group1, group2, combined);
		combinedClusteringCoefficient = clusteringCoefficients[0];
		group1ClusteringCoefficient = clusteringCoefficients[1];
		group2ClusteringCoefficient = clusteringCoefficients[2];
		group1minusGroup2ClusteringCoefficient = clusteringCoefficients[3];
		group2minusGroup1ClusteringCoefficient = clusteringCoefficients[4];

		this.numDigits = (int) Math.max(Math.ceil(Math.log10(cluster.getLabel().length())), clusterId.length() - 1);

		if(isLeaf){
			this.group1Patients = this.group2Patients = "";
		}else{
			final StringBuilder group1Patients = new StringBuilder();
			final StringBuilder group2Patients = new StringBuilder();
			final String[] patientLabels = new String[nodeLeafs.length];
			for(int i = 0; i < patientLabels.length; i++)
				patientLabels[i] = nodeLeafs[i].getLabel();
			Arrays.sort(patientLabels);
			for(String label : patientLabels)
				(group1.getGeneSetMap().containsKey(label) ? group1Patients : group2Patients).append(label).append(", ");
			this.group1Patients = group1Patients.length() > 0 ? group1Patients.substring(0, group1Patients.length() - 2) : "<none>";
			this.group2Patients = group2Patients.length() > 0 ? group2Patients.substring(0, group2Patients.length() - 2) : "<none>";
		}

	}

	private static String pad(Object o, int numChars){
		final StringBuilder sb = new StringBuilder(String.valueOf(o));
		if(sb.length() > numChars){
			System.err.println(sb.toString());
			System.err.println(numChars);
		}
		assert(sb.length() <= numChars);
		if(sb.length() > numChars) {
			System.out.println("[+] " + o + " " + numChars);
		}
		if(sb.length() == numChars) return sb.toString();
		final char[] padding = new char[numChars - sb.length()];
		Arrays.fill(padding, ' ');
		return sb.insert(0, padding).toString();
	}

	public String getClusterId(){
		return clusterId;
	}

	public boolean isLeaf(){
		return isLeaf;
	}

	public String getPrintableString(){
		final StringBuilder sb = new StringBuilder();
		if(isLeaf){
			sb.append("Information for patient ").append(clusterId).append("\n\t")
			.append("Group = Group ").append(2 - numberGroup1).append("\n\t")
			.append("Weight = ").append(weightGroup1 + weightGroup2).append("\n\t");
		}else{
			sb.append("Cluster analysis of ").append(clusterId).append("\n\t")
			.append("Average Distance (Height) = ").append(DECIMAL_FORMAT.format(normalizedHeight)).append("\n\t")
			.append("Bootstrapping Confidence = ").append(bootstrapped ? DECIMAL_FORMAT.format(bootstrappingConfidence) : "N/A").append("\n\t")
			.append("Total Number of Patients = ").append(numberCombined).append("\n\t")
			.append("Number in Group 1 = ").append(numberGroup1)
			.append(" (").append(DECIMAL_FORMAT.format(weightGroup1 * 100D / (weightGroup1 + weightGroup2))).append("%)\n\t")
			.append("Number in Group 2 = ").append(numberGroup2)
			.append(" (").append(DECIMAL_FORMAT.format(weightGroup2 * 100D / (weightGroup1 + weightGroup2))).append("%)\n\t")
			.append("p-value = ").append(DECIMAL_FORMAT.format(pValue)).append("\n\t")
			.append("Group 1 and Group 2 Clustering Coefficient = ").append(DECIMAL_FORMAT.format(combinedClusteringCoefficient)).append("\n\t")
			.append("Group 1 Clustering Coefficient = ").append(DECIMAL_FORMAT.format(group1ClusteringCoefficient)).append("\n\t")
			.append("Group 2 Clustering Coefficient = ").append(DECIMAL_FORMAT.format(group2ClusteringCoefficient)).append("\n\t")
			.append("Group 1 minus Group 2 Clustering Coefficient = ").append(DECIMAL_FORMAT.format(group1minusGroup2ClusteringCoefficient)).append("\n\t")
			.append("Group 2 minus Group 1 Clustering Coefficient = ").append(DECIMAL_FORMAT.format(group2minusGroup1ClusteringCoefficient)).append("\n\t")
			.append("Group 1 Patients = ").append(group1Patients).append("\n\t")
			.append("Group 2 Patients = ").append(group2Patients);
		}
		return sb.toString();
	}

	public String getCompactString(){
		assert(!isLeaf);
		return new StringBuilder()
				.append(pad(clusterId, numDigits + 1)).append('\t')
				.append(DECIMAL_FORMAT.format(normalizedHeight)).append('\t')
				.append(bootstrapped ? DECIMAL_FORMAT.format(bootstrappingConfidence) : "N/A").append('\t')
				.append(pad(numberCombined, numDigits)).append('\t')
				.append(pad(numberGroup1, numDigits)).append('\t')
				.append(pad(numberGroup2, numDigits)).append('\t')
				.append(pad(PERCENTAGE_FORMAT.format(weightGroup1 / (weightGroup1 + weightGroup2)), 9)).append("%\t")
				.append(pad(PERCENTAGE_FORMAT.format(weightGroup2 / (weightGroup1 + weightGroup2)), 9)).append("%\t")
				.append(DECIMAL_FORMAT.format(pValue)).append('\t')
				.append(DECIMAL_FORMAT.format(combinedClusteringCoefficient)).append('\t')
				.append(DECIMAL_FORMAT.format(group1ClusteringCoefficient)).append('\t')
				.append(DECIMAL_FORMAT.format(group2ClusteringCoefficient)).append('\t')
				.append(DECIMAL_FORMAT.format(group1minusGroup2ClusteringCoefficient)).append('\t')
				.append(DECIMAL_FORMAT.format(group2minusGroup1ClusteringCoefficient)).append('\t')
				.append(group1Patients).append('\t')
				.append(group2Patients)//.append('\t')
				.toString();
	}

	public static String getCompactStringHeaders(){
		final String[] headers = {
				"Cluster Id",
				"Average Distance (Height)",
				"Bootstrapping Confidence Level",
				"Total Number of Patients",
				"Number in Group 1",
				"Number in Group 2",
				"Group 1 Weight Percentage",
				"Group 2 Weight Percentage",
				"p-value",
				"Group 1 and Group 2 Clustering Coefficient",
				"Group 1 Clustering Coefficient",
				"Group 2 Clustering Coefficient",
				"Group 1 minus Group 2 Clustering Coefficient",
				"Group 2 minus Group 1 Clustering Coefficient",
				"Group 1 Patients",
				"Group 2 Patients",
		};
		return String.join(" | ", headers);
	}

	public PhylogeneticTreeNode getNode() {
		return node;
	}

	public double getpValue() {
		return pValue;
	}

	public double getNormalizedHeight() {
		return normalizedHeight;
	}

	/*
	 *   
Cluster Id       Num Group 1 Weight Group 1     Max Dissimilarity
         Height      Num Group 2         Weight Group 2         p-value 
  C1	0.376525	 96	 47	  50.0000%	  50.0000%	0.905882	1.000000	
  C2	0.369184	 95	 46	  50.2759%	  49.7241%	0.905882	0.444401	




Cluster Id            Num Group 2                Max Dissimilarity
         Height            Weight Group 1                       p-value
                Num Group 1             Weight Group 3
  C1	0.376525	 96	 47	  50.0000%	  50.0000%	0.905882	1.000000	
  C2	0.369184	 95	 46	  50.2759%	  49.7241%	0.905882	0.444401	
	 */

	private static double[] calculateClusteringCoefficients(Configuration c, PhylogeneticTreeNode node, GeneSetMap group1, GeneSetMap group2, GeneSetMap combined){
		final double[] coefficients = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};

		final Set<String> patientsToInclude = new HashSet<>();
		final PhylogeneticTreeNode[] leaves = node.getLeaves();
		if(leaves.length > 0)
			for(PhylogeneticTreeNode leaf : leaves)
				patientsToInclude.add(leaf.getLabel());
		else
			patientsToInclude.add(node.getLabel());


		final Set<String> group1PatientsToInclude = new HashSet<>(patientsToInclude);
		group1PatientsToInclude.retainAll(group1.getGeneSetMap().keySet());
		final Set<String> group2PatientsToInclude = new HashSet<>(patientsToInclude);
		group2PatientsToInclude.retainAll(group2.getGeneSetMap().keySet());

		group1 = group1.subset(group1PatientsToInclude);
		group2 = group2.subset(group2PatientsToInclude);

		final double numGroup1 = group1.getGeneSetMap().size();
		final double numGroup2 = group2.getGeneSetMap().size();

		final double group1ScalingFactor = numGroup1 < numGroup2 ? numGroup2 / numGroup1 : 1;
		final double group2ScalingFactor = numGroup2 < numGroup1 ? numGroup1 / numGroup2 : 1;

		final LayeredGraph<Protein> combinedGraph = combined.getLayeredGraph();
		coefficients[0] = combinedGraph.getGlobalClusteringCoefficient();

		final LayeredGraph<Protein> group1Graph = NetworkAnalysis.getReducedGraph(c, group2);
		coefficients[1] = group1Graph.getGlobalClusteringCoefficient();

		final LayeredGraph<Protein> group2Graph = NetworkAnalysis.getReducedGraph(c, group2);
		coefficients[2] = group2Graph.getGlobalClusteringCoefficient();

		final LayeredGraph<Protein> group1minusGroup2 = NetworkAnalysis.getReducedGraph(c, group1.getLayeredGraph().subtract(group2Graph, group1ScalingFactor, group2ScalingFactor), group1.getUniqueProteins());
		coefficients[3] = group1minusGroup2.getGlobalClusteringCoefficient();
		final LayeredGraph<Protein> group2minusGroup1 = NetworkAnalysis.getReducedGraph(c, group2Graph.subtract(group1Graph, group2ScalingFactor, group1ScalingFactor), group2.getUniqueProteins());
		coefficients[4] = group2minusGroup1.getGlobalClusteringCoefficient();

		return coefficients;
	}

	public double getBootstrappingConfidence() {
		return bootstrappingConfidence;
	}

}
