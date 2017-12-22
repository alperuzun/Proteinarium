package org.armanious.network.analysis.testunits;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.armanious.network.Configuration;
import org.armanious.network.analysis.Gene;
import org.armanious.network.analysis.GeneSetMap;
import org.armanious.network.analysis.NetworkAnalysis;

public class RNKFilesTest {
	
	private static final int NUM_GENES_PER_PATIENT = 20;
	
	public static void main(String...args) throws Throwable {
		//Gene.initializeGeneDatabase(new File("/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.aliases.v10.5.hgnc_with_symbol.txt"));
		
		final File directory = new File("/Users/david/OneDrive/Documents/Brown/Comp Bio Research/NetworkAnalysis/143Patients/");
		final Configuration c = Configuration.fromArgs(
				"activeDirectory=" + directory.getPath(),
				"projectName=143Patients",
				"maxPathUnconfidence=200",
				"maxPathLength=5",
				"maxNodesInGraphToRender=100",
				"repulsionConstant=1.5"
			);
		final Map<String, Set<String>> cases = new HashMap<>();
		final Map<String, Set<String>> controls = new HashMap<>();
		
		final File rnkFilesDirectory = new File(directory, "RNKfiles");
		try(final BufferedReader br = new BufferedReader(new FileReader(new File(rnkFilesDirectory, "143Patient_Samples2.txt")))){
			String s;
			while((s = br.readLine()) != null){
				final String[] parts = s.split("\t");
				assert(parts[1].equals("0") || parts[1].equals("1"));
				final String patientId = parts[0];
				final Map<String, Set<String>> mapToAdd = parts[1].equals("0") ? controls : cases;
				assert(!mapToAdd.containsKey(patientId));
				mapToAdd.put(patientId, new HashSet<>());
			}
		}
		
		final Consumer<Map<String, Set<String>>> patientLoader = (map) -> {
			for(String id : map.keySet()){
				try(final BufferedReader br = new BufferedReader(new FileReader(new File(rnkFilesDirectory, 
						id.replace('-', '.') + "_Genes.rnk")))){
					map.get(id).addAll(
							br.lines()
							.filter(g -> Gene.loadedGene(g))
							.limit(NUM_GENES_PER_PATIENT)
							.collect(Collectors.toSet())
							);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		};
		patientLoader.accept(cases);
		patientLoader.accept(controls); 
		
		NetworkAnalysis.run(c, new GeneSetMap(cases), new GeneSetMap(controls));
		System.exit(0);
	}

}
