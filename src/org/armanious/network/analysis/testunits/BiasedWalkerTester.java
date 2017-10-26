package org.armanious.network.analysis.testunits;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.armanious.Tuple;
import org.armanious.graph.Graph;
import org.armanious.network.analysis.BiasedWalker;
import org.armanious.network.analysis.Gene;
import org.armanious.network.analysis.Protein;
import org.armanious.network.analysis.ProteinInteractionGraph;
import org.armanious.network.visualization.ForceDirectedLayout;
import org.armanious.network.visualization.Renderer;

public class BiasedWalkerTester {

	private BiasedWalkerTester(){}

	public static void test(int numIters, int stepsPerIter, double restartProbability, int maxSubgraphSize, String...geneIds) throws IOException {
		Gene.initializeGeneDatabase(new File("/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.aliases.v10.5.hgnc_with_symbol.txt"));

		final ProteinInteractionGraph pig = new ProteinInteractionGraph(400);
		final Gene[] genes = Gene.getGenes(geneIds);
		final Set<Protein> proteins = new HashSet<>();
		for(Gene gene : genes){
			for(Protein protein : gene.getProteins()){
				proteins.add(protein);
			}
		}
		final BiasedWalker<Protein> bw = new BiasedWalker<>(pig,
				proteins.toArray(new Protein[proteins.size()]), restartProbability);
		for(int i = 0; i < numIters; i++){
			bw.run(stepsPerIter);
			System.out.println("BiasedWalker has now run " + ((i+1) * stepsPerIter) + " steps...");
		}

		System.out.println("---SUCCESSFUL PATHS---");
		final HashMap<Protein, Integer> counts = new HashMap<>();
		final StringBuilder sb = new StringBuilder();
		for(ArrayList<Protein> path : bw.getSuccessfulPaths()){
			for(Protein p : path){
				counts.put(p, counts.getOrDefault(p, 0) + 1);
				sb.append(p.getGene() != null ? p.getGene().getSymbol() : p.getId()).append(',');
			}
			sb.replace(sb.length() - 1, sb.length(), "\n");
		}
		System.out.print(sb.toString());

		System.out.println("\n\n\n---NODE DISTRIBUTIONS BY WALKER---");
		final HashMap<Protein, Double> unorderedDistribution = bw.getDistribution(0);
		final ArrayList<Tuple<Protein, Double>> orderedDistribution = new ArrayList<>(unorderedDistribution.size());
		for(Protein p : unorderedDistribution.keySet())
			orderedDistribution.add(new Tuple<>(p, unorderedDistribution.get(p)));
		Collections.sort(orderedDistribution, Comparator.comparing((Tuple<Protein,Double> t) -> -t.val2()));


		for(int i = 0; i < maxSubgraphSize && i < orderedDistribution.size(); i++){
			final Protein p = orderedDistribution.get(i).val1();
			System.out.println((p.getGene() != null ? p.getGene().getSymbol() : p.getId()) + "\t" + unorderedDistribution.get(p));
		}

		System.out.println("\n\n\n---NODE DISTRIBUTIONS BY SUCCESSFUL PATHS---");
		final PriorityQueue<Protein> entries = new PriorityQueue<>(counts.size() > 0 ? counts.size() : 1, Comparator.comparing(p -> -counts.get(p)));
		if(counts.size() > 0){
			int sum = 0;
			for(Protein p : counts.keySet()){
				entries.add(p);
				sum += counts.get(p);
			}

			for(Protein p : entries){
				System.out.println((p.getGene() == null ? p.getId() : p.getGene().getSymbol())
						+ "\t" + counts.get(p) / (double)sum);
			}
		}else{
			System.out.println("NONE");
		}

		final ArrayList<Protein> nodes = new ArrayList<>();
		for(int i = 0; i < maxSubgraphSize && i < orderedDistribution.size(); i++){
			nodes.add(orderedDistribution.get(i).val1());
		}
		final Graph<Protein> subgraph = pig.subgraphWithNodes(nodes);
		System.out.println("Attempting to layout " + subgraph.getNodes().size() + " nodes.");
		final ForceDirectedLayout<Protein> fdl = new ForceDirectedLayout<>(subgraph, .01, .5, 1);
		fdl.layout();
		for(int i = 0; i < fdl.positions.length; i++){
			System.out.println(fdl.nodes[i] + "\t" + fdl.positions[i]);
		}
		final Renderer<Protein> r = new Renderer<>(
				fdl,
				p -> p.getGene() == null ? p.getId() : p.getGene().getSymbol(),
				p -> proteins.contains(p) ? Color.YELLOW : Color.RED,
				e -> entries.contains(e.getSource()) || entries.contains(e.getTarget()) ? Color.BLACK : Color.YELLOW);
		final File file = new File("test.png");
		r.saveTo(file);
		Desktop.getDesktop().open(file);

	}

	public static void performTest(){
		try {
			//BiasedWalkerTester.test(10, 10000, 0.2, 30, "ESR1", "HSPA5", "MIF", "MCL1", "HOXA9");
			BiasedWalkerTester.test(10, 10000, 0.2, 30, "VEGFA", "MYC", "STAT3");
		} catch (IOException e) {
			System.err.println("BiasedWalker test failed:");
			e.printStackTrace();
		}
	}

	public static void main(String...args){
		performTest();
	}

}
