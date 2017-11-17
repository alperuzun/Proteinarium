package org.armanious.network.analysis;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

import org.armanious.Tuple;
import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.graph.LayeredGraph;
import org.armanious.network.visualization.ForceDirectedLayout;
import org.armanious.network.visualization.Renderer;

public class Entry {

	private final ProteinInteractionGraph pig;

	private final InputStream proteinInteractionGraphInputStream;
	private final int proteinInteractionGraphThreshold;

	private final Map<String, Collection<Gene>> casesGeneSets;
	private final Map<String, Collection<Gene>> controlsGeneSets;

	private final int pathUncofidenceThreshold;
	private final int pathLengthThreshold;
	
	private final double layoutRepulsionConstant;
	private final double layoutAttractionConstant;
	private final double layoutDeltaThreshold;


	private final Set<Protein> casesProteinSet = new HashSet<>();
	private final Set<Protein> controlsProteinSet = new HashSet<>();

	public Entry(Map<String, String> parsedArgs, Map<String, Collection<Gene>> cases, Map<String, Collection<Gene>> controls) throws IOException {
		final String stringDatabaseFilename = parsedArgs.getOrDefault("interactionfile", "/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.links.v10.5.txt");
		proteinInteractionGraphInputStream = new FileInputStream(stringDatabaseFilename);
		proteinInteractionGraphThreshold = 400;
		pig = new ProteinInteractionGraph(proteinInteractionGraphThreshold, proteinInteractionGraphInputStream);

		//casesGeneSets = new Gene[1][];
		//casesGeneSets[0] = Gene.getGenes(CASES_GENE_SET_IDS[0]);
		//casesProteinSet.addAll(getProteinsFromGeneSet(casesGeneSets[0]));
		//controlsGeneSets = null;
		this.casesGeneSets = cases;
		this.controlsGeneSets = controls;

		for(Collection<Gene> casePatient : this.casesGeneSets.values())
			casesProteinSet.addAll(getProteinsFromGeneSet(casePatient));
		for(Collection<Gene> controlPatient : this.controlsGeneSets.values())
			controlsProteinSet.addAll(getProteinsFromGeneSet(controlPatient));

		pathUncofidenceThreshold = 200;
		pathLengthThreshold = 5;

		layoutRepulsionConstant = 0.15;
		layoutAttractionConstant = 0.01;
		layoutDeltaThreshold = 1E-3;
	}

	private static final Scanner in = new Scanner(System.in);
	private LayeredGraph<Protein> computeLayeredGraph(Map<String, Collection<Gene>> geneSets, String name){
		final File f = new File(name + ".txt");
		if(f.exists()){
			System.out.print(name + " already computed; would you like to recompute? [Y/N]: ");
			
			final String s = in.nextLine();
			if(s.trim().equals("Y")) f.delete();
		}

		final LayeredGraph<Protein> layeredGraph = new LayeredGraph<>();
		if(f.exists()){
			final HashMap<Protein, Integer> counts = new HashMap<>();
			try(final BufferedReader br = new BufferedReader(new FileReader(f))){
				String s;
				while((s = br.readLine()) != null){
					final String[] parts = s.split("\t");
					switch(parts.length){
					case 2:
						counts.put(Protein.getProtein(parts[0]), Integer.parseInt(parts[1]));
						break;
					case 3:
						layeredGraph.forceAddEdge(Protein.getProtein(parts[0]), Protein.getProtein(parts[1]), Integer.parseInt(parts[2]), false);
						break;
					default:
						throw new RuntimeException();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			for(Protein p : counts.keySet()) layeredGraph.setCount(p, counts.get(p));
		}else{
			for(Collection<Gene> geneSet : geneSets.values()){
				final Set<Protein> proteinsFromGeneSet = getProteinsFromGeneSet(geneSet);
				System.out.println("Finding all-pairs paths between " + proteinsFromGeneSet.size() + " proteins from " + geneSet);
				final ArrayList<Tuple<ArrayList<Protein>, Integer>> allPairwisePaths = pairwisePaths(proteinsFromGeneSet.toArray(new Protein[proteinsFromGeneSet.size()]));
				final Map<Protein, Integer> filterCountsByProtein = getPathNodeCounts(allPairwisePaths);
				layeredGraph.addGraph(pig.subgraphWithNodes(filterCountsByProtein.keySet()));
			}
		}
		return layeredGraph;
	}

	public void doYourThang(String filePrefix){
		try {
			final Map<LayeredGraph<Protein>, String> toRender = new HashMap<>();

			System.out.println("Computing layered graph for cases gene sets");
			final LayeredGraph<Protein> casesLayeredGraph = computeLayeredGraph(casesGeneSets, filePrefix  + "Cases");
			casesLayeredGraph.saveTo(filePrefix + "Cases.txt");
			toRender.put(casesLayeredGraph, filePrefix + "Cases.png");

			if(controlsGeneSets != null){
				System.out.println("Computing layered graph for controls gene sets");
				final LayeredGraph<Protein> controlsLayeredGraph = computeLayeredGraph(controlsGeneSets, filePrefix + "Controls");
				if(controlsLayeredGraph.getNodes().size() > 0){
					controlsLayeredGraph.saveTo(filePrefix + "Controls.txt");
					toRender.put(controlsLayeredGraph, filePrefix + "Controls.png");
					final LayeredGraph<Protein> casesMinusControls = casesLayeredGraph.subtract(controlsLayeredGraph).subtract(controlsLayeredGraph);
					if(casesMinusControls.getNodes().size() > 0){
						toRender.put(casesMinusControls, filePrefix + "CasesMinusControls.png");
						final HashSet<Protein> seeds = new HashSet<>();
						for(Protein p : casesMinusControls.getNodes()){
							if(casesMinusControls.getCount(p) == casesMinusControls.getMaxCount() && seeds.add(p)){
								for(Edge<Protein> neighbor : casesMinusControls.getNeighbors(p)){
									seeds.add(neighbor.getTarget());
								}
							}
						}
						final LayeredGraph<Protein> furtherInvestigation = casesMinusControls.subgraphWithNodes(new LayeredGraph<>(), seeds);
						toRender.put(furtherInvestigation, filePrefix + "CasesMinusControlsMostImportant.png");
					}
					
					
					final LayeredGraph<Protein> doubledControls = controlsLayeredGraph.subgraphWithNodes(new LayeredGraph<>(), controlsLayeredGraph.getNodes());
					for(Protein p : doubledControls.getNodes()) doubledControls.setCount(p, doubledControls.getCount(p) * 2);
					final LayeredGraph<Protein> controlsMinusCases = doubledControls.subtract(casesLayeredGraph);
					if(controlsMinusCases.getNodes().size() > 0)
						toRender.put(controlsMinusCases, filePrefix + "ControlsMinusCases.png");
				}
			}

			for(LayeredGraph<Protein> lg : toRender.keySet()){
				layoutAndRender(lg, toRender.get(lg));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void layoutAndRender(LayeredGraph<Protein> graph, String filename) throws IOException {
		assert(graph.getNodes().size() > 0);
		
		final double PERCENTAGE_KEPT = 0.1;
		
		final LayeredGraph<Protein> reducedGraph = graph.subgraphWithNodes(new LayeredGraph<>(),
				Arrays.asList(
						graph.getNodes().stream()
						.sorted((p1, p2) -> graph.getCount(p2) - graph.getCount(p1))
						.limit((long)Math.ceil(PERCENTAGE_KEPT * graph.getNodes().size()))
						.toArray(i -> new Protein[i])
				));
		System.out.println(reducedGraph.getNodes().size());
		
		final Function<Graph<Protein>, Function<Protein, Double>> sizeFunctionGenerator = g -> {
			int maxEdgeCount = 0;
			for(Protein p : reducedGraph.getNodes())
				maxEdgeCount = Math.max(maxEdgeCount, reducedGraph.getNeighbors(p).size());
			
			final int MIN_SIZE = 15;
			final int MAX_SIZE = Math.min(Math.max(g.getNodes().size(), 30), 100);
			final double sizePerEdge = (double) (MAX_SIZE - MIN_SIZE) / maxEdgeCount;
			/*final double a = 0;
			return p -> {
				final double ax = a * g.getNeighbors(p).size();
				return 50 * ax / Math.sqrt(1 + ax * ax);
			};*/
			return p -> MIN_SIZE + sizePerEdge * g.getNeighbors(p).size();
		};

		final Function<Protein, Double> nodeSizeFunction = sizeFunctionGenerator.apply(reducedGraph);


		final ForceDirectedLayout<Protein> fdl = new ForceDirectedLayout<>(
				reducedGraph,
				layoutRepulsionConstant,
				layoutAttractionConstant,
				layoutDeltaThreshold,
				nodeSizeFunction
				);
		fdl.layout();


		final Renderer<Protein> renderer = new Renderer<>(fdl);
		renderer.setLabelFunction(p -> p.getGene() == null ? p.getId() : p.getGene().getSymbol());
		final Color yellowGreen = new Color(180, 255, 0);

		final int LOWER_ALPHA_BOUND = 10;
		final int UPPER_ALPHA_BOUND = 255;
		final int MAXIMUM_NUM = reducedGraph.getMaxCount();
		renderer.setNodeColorFunction(p -> {
			final boolean isCase = casesProteinSet.contains(p);
			final boolean isControl = controlsProteinSet.contains(p);
			final Color c;
			if(isCase && isControl) c = yellowGreen;
			else if(isCase) c = Color.YELLOW;
			else if(isControl) c = Color.GREEN;
			else c = Color.RED;
			final int alpha = (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double) reducedGraph.getCount(p) / MAXIMUM_NUM);
			return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
		});
		renderer.saveTo(new File(filename));
	}

	private Map<Protein, Integer> getPathNodeCounts(ArrayList<Tuple<ArrayList<Protein>, Integer>> paths){
		final Map<Protein, Integer> map = new HashMap<>();
		for(Tuple<ArrayList<Protein>, Integer> path : paths)
			for(Protein protein : path.val1())
				map.put(protein, map.getOrDefault(protein, 0) + 1);
		return map;
	}

	private static Set<Protein> getProteinsFromGeneSet(Collection<Gene> geneSet) {
		final Set<Protein> proteins = new HashSet<>();
		for(Gene gene : geneSet)
			for(Protein protein : gene.getProteins())
				proteins.add(protein);
		return proteins;
	}

	private final HashMap<Protein, HashMap<Protein, Tuple<ArrayList<Protein>, Integer>>> cache = new HashMap<>();
	private final ArrayList<Tuple<ArrayList<Protein>, Integer>> pairwisePaths(Protein[] endpoints){
		final ArrayList<Tuple<ArrayList<Protein>, Integer>> paths = new ArrayList<>();
		for(int i = 0; i < endpoints.length - 1; i++){
			System.out.println("Dijkstra's starting at..." + endpoints[i].getGene().getSymbol());
			for(int j = i + 1; j < endpoints.length; j++){
				if(!cache.containsKey(endpoints[i]))
					cache.put(endpoints[i], new HashMap<>());
				if(!cache.containsKey(endpoints[j]))
					cache.put(endpoints[j], new HashMap<>());
				if(!cache.get(endpoints[i]).containsKey(endpoints[j])){
					final Tuple<ArrayList<Protein>, Integer> path =  pig.dijkstras(endpoints[i], endpoints[j], e -> (1000 - e.getWeight()), pathUncofidenceThreshold, pathLengthThreshold);
					cache.get(endpoints[i]).put(endpoints[j], path);
					cache.get(endpoints[j]).put(endpoints[i], path);
				}
				paths.add(cache.get(endpoints[i]).get(endpoints[j]));
			}
		}
		return paths;
	}




	//from TP53 network on ptbdb
	//target CASE gene set: MCL1, TERT, VDR, BCL2
	/*private static final String[][] CASES_GENE_SET_IDS = {
			{"MCL1", "TERT", "BCL2", "TAF1"},
			{"TERT", "VDR", "BCL2"},
			{"VDR", "MCL1", "TERT", "BCL2"},
	};*/
	//target CONTROL gene set: VHL, HSPA4, TERT
	/*private static final String[][] CONTROLS_GENE_SET_IDS = {
			{"VHL", "HSPA4", "TERT"},
			{"VHL", "TERT", "TAF1"},
			{"TERT", "BCL2", "VHL"}
	};*/
	/*public static void main(String...args) throws IOException {
		Gene.initializeGeneDatabase(new File("/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.aliases.v10.5.hgnc_with_symbol.txt")); //TODO FIXME

		Map<String, Collection<Gene>> casesGeneSets = new HashMap<>();
		Map<String, Collection<Gene>> controlsGeneSets = new HashMap<>();
		for(int i = 0; i < 3; i++){
			casesGeneSets.put(String.valueOf(i), Arrays.asList(Gene.getGenes(CASES_GENE_SET_IDS[i])));
			controlsGeneSets.put(String.valueOf(i), Arrays.asList(Gene.getGenes(CONTROLS_GENE_SET_IDS[i])));
		}

		final Entry e = new Entry(parseArguments(args), casesGeneSets, controlsGeneSets);
		e.doYourThang("test");

		/*final LayeredGraph<String> lg = new LayeredGraph<>();
		MutableGraph<String> g = new MutableGraph<>();
		g.addNode("A");
		g.addNode("B");
		g.addEdge("A", "B");
		lg.addGraph(g);
		g.addNode("C");
		g.addNode("D");
		g.addEdge("C", "A");
		g.addEdge("A", "D");
		g.addEdge("B", "D");
		lg.addGraph(g);
		g = new MutableGraph<>();
		g.addNode("C");
		g.addNode("D");
		g.addEdge("C", "D");
		//lg.addGraph(g);

		final ForceDirectedLayout<String> fdl = new ForceDirectedLayout<>(
				lg,
				0.003,
				0.5,
				1E-3,
				p -> 10D + 5 * lg.getNeighbors(p).size()
				);
		fdl.layout();


		final Renderer<String> renderer = new Renderer<>(fdl);
		final int LOWER_ALPHA_BOUND = 10;
		final int UPPER_ALPHA_BOUND = 255;
		final int MAXIMUM_NUM = lg.getNumberOfGraphs();
		renderer.setNodeColorFunction(s -> {
			final int alpha = (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double) lg.getCount(s) / MAXIMUM_NUM);
			System.out.println(lg.getCount(s));
			System.out.println(alpha);
			return new Color(255, 0, 0, alpha);
		});

		renderer.saveTo(new File("test.png"));
		Desktop.getDesktop().open(new File("test.png"));*/
	//}

	private static Map<String, String> parseArguments(String[] args){
		final HashMap<String, String> argMap = new HashMap<>();
		for(String arg : args){
			arg = arg.toLowerCase();
			if(arg.equals("help")) printUsage(0);
			final int idx = arg.indexOf('=');
			if(idx == -1) printUsage(1);
			argMap.put(arg.substring(0, idx), arg.substring(idx + 1));
		}
		return argMap;
	}

	private static void printUsage(int status){
		System.out.println("USAGE: " + "TODO");
		System.exit(status);
	}

}
