package org.armanious.network.analysis;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.armanious.graph.LayeredGraph;
import org.armanious.network.Configuration;
import org.armanious.network.visualization.DendrogramRenderer;

public class BMatrixAnalysis {
	
	private BMatrixAnalysis(){}
	
	private static Color colorGradient(Color start, Color end, double percentage){
		return new Color((int)(start.getRed() * (1 - percentage) + end.getRed() * percentage),
				(int)(start.getGreen() * (1 - percentage) + end.getGreen() * percentage),
				(int)(start.getBlue() * (1 - percentage) + end.getBlue() * percentage));
	}
	
	private static void printUsage(){
		System.err.println("Usage:\njava -jar bmatrix_dendrogram.jar binary_matrix_file\nOR\njava -jar bmatrix_dendrogram.jar -c configuration_file binary_matrix_file");
		System.exit(1);
	}
	
	public static void main(String...args) throws IOException {
		if(args == null ||
				(args.length != 1 && args.length != 3) ||
				(args.length == 3 && !args[0].equals("-c")))
			printUsage();
		File binaryMatrixFile = new File(args.length == 1 ? args[0] : args[2]);
		String projectName = binaryMatrixFile.getName();
		projectName = projectName.substring(projectName.lastIndexOf('.') + 1);
		Configuration c = args.length == 3 ? Configuration.fromFile(new File(args[1])) : Configuration.fromArgs("projectName=" + binaryMatrixFile.getName());
		if(!binaryMatrixFile.isAbsolute() && args.length == 3) binaryMatrixFile = new File(c.generalConfig.activeDirectory + args[2]);
		//if(binaryMatrixFile.exists()){
		//	System.err.println("Cannot find binary matrix file: " + binaryMatrixFile.getAbsolutePath());
		//	System.exit(1);
		//}
		renderAndDisplayDendrogramForBinaryMatrix(c, parseBinaryMatrix(binaryMatrixFile));
	}
	
	public static void main_(String...args) throws IOException {
		final File directory = new File("/Users/david/OneDrive/Documents/Brown/Comp Bio Research/NetworkAnalysis/191Patients/");
		final String[] matrices = new String[]{"binary_matrix_for_iBBiG_pphen", "binary_matrix_for_iBBiG_pt05", "binary_matrix_for_iBBiG"};
		
		for(String matrix : matrices){
			Configuration c = Configuration.fromArgs(
					"activeDirectory=" + directory,
					"outputDirectory=" + directory,
					"projectName=" + matrix);
			renderAndDisplayDendrogramForBinaryMatrix(c, parseBinaryMatrix(new File(directory, matrix + ".txt")));
		}
	}
	
	private static void renderAndDisplayDendrogramForBinaryMatrix(Configuration c, Map<String, Set<String>> matrix) throws IOException {
		
		
		final Map<String, Set<String>> cases = new HashMap<>();
		final Map<String, Set<String>> controls = new HashMap<>();
		matrix.forEach((k, v) -> (k.contains("cas") ? cases : controls).put(k, v));
		
		double group1initialWeight = cases.size() < controls.size() ? (float) controls.size() / cases.size() : 1;
		double group2initialWeight = controls.size() < cases.size() ? (float) cases.size() / controls.size() : 1;
		
		final DistanceMatrix<PhylogeneticTreeNode> dissimilarityMatrix = new DistanceMatrix<>();
		
		final String[] allPatientKeys = matrix.keySet().toArray(new String[matrix.size()]);
		
		final Map<String, PhylogeneticTreeNode> leaves = new HashMap<>();
		for(String patientKey : allPatientKeys){
			leaves.put(patientKey, new PhylogeneticTreeNode(patientKey,
					cases.containsKey(patientKey) ? group1initialWeight : group2initialWeight));
			assert(cases.containsKey(patientKey) || controls.containsKey(patientKey));
		}
		
		for(int i = 0; i < allPatientKeys.length - 1; i++){
			for(int j = i + 1; j < allPatientKeys.length; j++){
				final Set<String> x = matrix.get(allPatientKeys[i]);
				final Set<String> y = matrix.get(allPatientKeys[j]);

				dissimilarityMatrix.setDistance(
						leaves.get(allPatientKeys[i]),
						leaves.get(allPatientKeys[j]),
						calculateJaccardDissimilarity(x, y));
			}
		}
		final PhylogeneticTreeNode treeRoot = PhylogeneticTree.createTreeFromMatrix(dissimilarityMatrix);
		final DendrogramRenderer dr = new DendrogramRenderer(c.rendererConfig, new File(c.generalConfig.outputDirectory));
		final Function<PhylogeneticTreeNode, Color> clusterEdgeColorFunction = ptn -> {
			double group1weight = 0;
			final PhylogeneticTreeNode[] nodeLeafs = ptn.getLeaves();
			if(nodeLeafs.length > 0){
				for(PhylogeneticTreeNode ptnLeaf : nodeLeafs)
					if(cases.containsKey(ptnLeaf.getLabel()))
						group1weight += ptnLeaf.getWeight();
			}else{
				//System.out.println("!: " + ptn.getLabel() + ": " + cases.containsKey(ptn.getLabel()));
				group1weight = cases.containsKey(ptn.getLabel()) ? ptn.getWeight() : 0;
			}
			final double percentageGroup1 = group1weight / ptn.getWeight();
			if(percentageGroup1 >= 0.5){
				return colorGradient(Color.BLACK, Color.ORANGE, (percentageGroup1 - 0.5) * 2);
			}else{
				final double percentageSecondary = 1 - percentageGroup1;
				return colorGradient(Color.BLACK, Color.BLUE, (percentageSecondary - 0.5) * 2);
			}
		};
		dr.setClusterEdgeColorFunction(clusterEdgeColorFunction);
		
		final Map<String, Set<Gene>> group1map = new HashMap<>();
		cases.forEach((k, v) -> group1map.put(k, (Set<Gene>)v.stream().map(s -> new Gene(s)).collect(Collectors.toSet())));
		final GeneSetMap group1 = new GeneSetMap(group1map, LayeredGraph.Type.GROUP1);
		
		final Map<String, Set<Gene>> group2map = new HashMap<>();
		controls.forEach((k, v) -> group2map.put(k, (Set<Gene>)v.stream().map(s -> new Gene(s)).collect(Collectors.toSet())));
		final GeneSetMap group2 = new GeneSetMap(group2map, LayeredGraph.Type.GROUP2);
		
		final Map<String, Set<Gene>> combinedMap = new HashMap<>();
		combinedMap.putAll(group1map);
		combinedMap.putAll(group2map);
		final GeneSetMap combinedGroup = new GeneSetMap(combinedMap, LayeredGraph.Type.COMBINED);
		
		Set<String> group1patients = new HashSet<>(group1map.keySet());
		group1patients.retainAll(group2map.keySet());
		
		final Map<String, ClusterAnalysis> clusters = PhylogeneticTree.recursivelyAnalyzeClusters(c, treeRoot, dissimilarityMatrix, group1, group2, combinedGroup);

		NetworkAnalysis.renderAndDisplayDendrogram(c, group1, group2, clusters);
		NetworkAnalysis.saveClusterAnalysesSummary(clusters, new File(c.generalConfig.outputDirectory, c.generalConfig.projectName + "_ClusterAnalyses.csv"));
	}
	
	
	private static <K> double calculateJaccardDissimilarity(Set<K> x, Set<K> y){
		final Set<K> union = new HashSet<>(x);
		union.addAll(y);
		if(union.size() == 0) return 0;
		final Set<K> intersection = new HashSet<>(x);
		intersection.retainAll(y);
		return 1D - (double) intersection.size() / union.size();
	}
	
	private static Map<String, Set<String>> parseBinaryMatrix(File f) throws IOException {
		final Map<String, Set<String>> patients = new HashMap<>();
		try(final BufferedReader br = new BufferedReader(new FileReader(f))){
			String s = br.readLine();
			assert(s != null);
			final String[] patientKeys = s.split("\t");
			for(int i = 1; i < patientKeys.length; i++)
				patients.put(patientKeys[i], new HashSet<>());
			while((s = br.readLine()) != null){
				if(s.isEmpty()) continue;
				final String[] parts = s.split("\t");
				for(int i = 1; i < parts.length; i++)
					if(parts[i].trim().equals("1"))
						patients.get(patientKeys[i]).add(parts[0]);
			}
		}
		return patients;
	}

}
