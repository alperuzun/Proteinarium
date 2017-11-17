package org.armanious.network.analysis.testunits;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;

import org.armanious.Tuple;
import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.network.analysis.Gene;
import org.armanious.network.analysis.Protein;
import org.armanious.network.analysis.ProteinInteractionGraph;
import org.armanious.network.visualization.ForceDirectedLayout;
import org.armanious.network.visualization.Renderer;

public class DijkstrasTester {
	
	private DijkstrasTester(){}
	
	private static <K> ArrayList<K> nodesFromPath(ArrayList<Edge<K>> path){
		final ArrayList<K> list = new ArrayList<>(path.size() + 1);
		if(path.size() > 0){
			list.add(path.get(0).getSource());
			for(Edge<K> edge : path)
				list.add(edge.getTarget());
		}
		return list;
	}
	
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
				final Tuple<ArrayList<Edge<Protein>>, Integer> path = pig.dijkstras(
						proteins[i], proteins[j], (e) -> 1000 - e.getWeight());

				final StringBuilder sb = new StringBuilder();
				sb.append(path.val2()).append(": ");
				for(Protein p : nodesFromPath(path.val1())){
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

		final int maxSubgraphSize = 30;
		final ArrayList<Protein> nodes = new ArrayList<>();
		final Iterator<Protein> iter = entries.iterator();
		for(int i = 0; i < maxSubgraphSize && iter.hasNext(); i++){
			nodes.add(iter.next());
		}
		final Graph<Protein> subgraph = pig.subgraphWithNodes(nodes);
		System.out.println("Attempting to layout " + subgraph.getNodes().size() + " nodes.");
		final ForceDirectedLayout<Protein> fdl = new ForceDirectedLayout<>(subgraph, .01, .5, 1);
		fdl.layout();
		for(int i = 0; i < fdl.positions.length; i++){
			System.out.println(fdl.nodes[i] + "\t" + fdl.positions[i]);
		}
		final Renderer<Protein> r = new Renderer<>(fdl);
		r.setLabelFunction(p -> p.getGene() == null ? p.getId() : p.getGene().getSymbol());
		r.setNodeColorFunction(p -> proteinSet.contains(p) ? Color.YELLOW : Color.RED);
		final File file = new File("dijkstras.png");
		r.saveTo(file);
		Desktop.getDesktop().open(file);
	}
	
	public static void performTest(){
		try {
			DijkstrasTester.test(
					"HSPA5", "TP53");
		} catch (IOException e) {
			System.err.println("Dijkstra's test failed:");
			e.printStackTrace();
		}
	}
	
	public static void main(String...args){
		performTest();
	}

}
