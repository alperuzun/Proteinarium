package org.armanious.network.analysis.testunits;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.armanious.Tuple;
import org.armanious.network.analysis.Gene;
import org.armanious.network.analysis.Protein;
import org.armanious.network.analysis.ProteinInteractionGraph;

public class DijkstrasTester {
	
	private DijkstrasTester(){}
	
	public static void test(String...geneIds) throws IOException {
		Gene.initializeGeneDatabase(new File("/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.aliases.v10.5.hgnc_with_symbol.txt"));
		
		final ProteinInteractionGraph pig = new ProteinInteractionGraph(400);
		final Gene[] genes = Gene.getGenes(geneIds);
		final Set<Protein> proteinSet = new HashSet<>();
		for(Gene gene : genes){
			for(Protein protein : gene.getProteins()){
				proteinSet.add(protein);
			}
		}
		final Protein[] proteins = proteinSet.toArray(new Protein[proteinSet.size()]);
		
		System.out.println("---SUCCESSFUL PATHS---");
		final HashMap<Protein, Integer> counts = new HashMap<>();
		for(int i = 0; i < proteins.length; i++){
			for(int j = i + 1; j < proteins.length; j++){
				final Tuple<ArrayList<Protein>, Integer> path = pig.dijkstras(
						proteins[i], proteins[j], (e) -> 1000 - e.getWeight());

				final StringBuilder sb = new StringBuilder();
				sb.append(path.val2()).append(": ");
				for(Protein p : path.val1()){
					counts.put(p, counts.getOrDefault(p, 0) + 1);
					sb.append(p.getGene() != null ? p.getGene().getSymbol() : p.getId()).append(',');
				}
				System.out.println(sb.substring(0, sb.length() - 1));
			}
		}
		
		System.out.println("\n\n\n---NODE DISTRIBUTIONS---");
		final PriorityQueue<Protein> entries = new PriorityQueue<>(counts.size(), Comparator.comparing(p -> -counts.get(p)));
		int sum = 0;
		for(Protein p : counts.keySet()){
			entries.add(p);
			sum += counts.get(p);
		}
		
		for(Protein p : entries){
			System.out.println((p.getGene() == null ? p.getId() : p.getGene().getSymbol())
					+ "\t" + counts.get(p) / (double)sum);
		}

		
	}
	
	public static void performTest(){
		try {
			DijkstrasTester.test("ESR1", "HSPA5", "MIF", "MCL1", "HOXA9");
		} catch (IOException e) {
			System.err.println("BiasedWalker test failed:");
			e.printStackTrace();
		}
	}
	
	public static void main(String...args){
		performTest();
	}

}
