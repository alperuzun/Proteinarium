package org.armanious.network.analysis.testunits;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.armanious.network.Configuration.RendererConfig;
import org.armanious.network.analysis.DistanceMatrix;
import org.armanious.network.analysis.PhylogeneticTree;
import org.armanious.network.analysis.PhylogeneticTreeNode;
import org.armanious.network.visualization.DendrogramRenderer;

public class DendrogramTest {
	
	private DendrogramTest(){}
	
	private static Color colorGradient(Color start, Color end, double percentage){
		return new Color((int)(start.getRed() * (1 - percentage) + end.getRed() * percentage),
				(int)(start.getGreen() * (1 - percentage) + end.getGreen() * percentage),
				(int)(start.getBlue() * (1 - percentage) + end.getBlue() * percentage));
	}
	
	public static void main(String...args) throws IOException {
		System.err.println("This code is broken due to the frequent requested code changes...");
		System.exit(1);
		
		final Map<String, Set<String>> geneSetMap = parseBinaryMatrix(new File("matrix_143samples.txt"));
		
		final File directory = new File("/Users/david/OneDrive/Documents/Brown/Comp Bio Research/NetworkAnalysis/143Patients/");
		
		final Map<String, Set<String>> cases = new HashMap<>();
		final Map<String, Set<String>> controls = new HashMap<>();
		
		final File rnkFilesDirectory = new File(directory, "RNKfiles");
		try(final BufferedReader br = new BufferedReader(new FileReader(new File(rnkFilesDirectory, "143Patient_Samples2.txt")))){
			String s;
			while((s = br.readLine()) != null){
				final String[] parts = s.split("\t");
				assert(parts[1].equals("0") || parts[1].equals("1"));
				final String patientId = parts[0].replace('-', '.');
				//System.out.print(patientId);
				final Map<String, Set<String>> mapToAdd = parts[1].equals("0") ? controls : cases;
				//System.out.println(": " + parts[1]);
				assert(!mapToAdd.containsKey(patientId));
				mapToAdd.put(patientId, new HashSet<>());
			}
		}
		double group1initialWeight = cases.size() < controls.size() ? (float) controls.size() / cases.size() : 1;
		double group2initialWeight = controls.size() < cases.size() ? (float) cases.size() / controls.size() : 1;
		
		final DistanceMatrix<PhylogeneticTreeNode> dissimilarityMatrix = new DistanceMatrix<>();
		
		final String[] allPatientKeys = geneSetMap.keySet().toArray(new String[geneSetMap.size()]);
		final Map<String, PhylogeneticTreeNode> leaves = new HashMap<>();
		for(String patientKey : allPatientKeys){
			leaves.put(patientKey, new PhylogeneticTreeNode(patientKey,
					cases.containsKey(patientKey) ? group1initialWeight : group2initialWeight));
			assert(cases.containsKey(patientKey) || controls.containsKey(patientKey));
		}
		
		for(int i = 0; i < allPatientKeys.length - 1; i++){
			for(int j = i + 1; j < allPatientKeys.length; j++){
				final Set<String> x = geneSetMap.get(allPatientKeys[i]);
				final Set<String> y = geneSetMap.get(allPatientKeys[j]);

				dissimilarityMatrix.setDistance(
						leaves.get(allPatientKeys[i]),
						leaves.get(allPatientKeys[j]),
						calculateJaccardDissimilarity(x, y));
			}
		}
		final PhylogeneticTreeNode treeRoot = PhylogeneticTree.createTreeFromMatrix(dissimilarityMatrix);
		final DendrogramRenderer dr = new DendrogramRenderer(new RendererConfig(Collections.emptyMap()), new File(System.getProperty("user.dir")));
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
		
		//dr.render(treeRoot, "matrix_143samples_Dendrogram");
		
		System.out.println(allPatientKeys.length + " patients in the dendrogram.");
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
					if(parts[i].equals("1"))
						patients.get(patientKeys[i]).add(parts[0]);
			}
		}
		return patients;
	}

}
