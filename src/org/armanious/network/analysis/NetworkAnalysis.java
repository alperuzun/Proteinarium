package org.armanious.network.analysis;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

import org.armanious.Tuple;
import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.graph.LayeredGraph;
import org.armanious.graph.Path;
import org.armanious.network.Configuration;
import org.armanious.network.visualization.DendrogramRenderer;
import org.armanious.network.visualization.ForceDirectedLayout;
import org.armanious.network.visualization.GUIRenderer;
import org.armanious.network.visualization.Renderer;

public final class NetworkAnalysis {

	private NetworkAnalysis(){}

	private static final String PROJECT_DATA_SUFFIX = "_Data.txt";

	public static void run(Configuration c) throws IOException {
		// load gene sets
		GeneSetMap primary = GeneSetMap.loadFromFile(c.generalConfig.primaryGeneSetGroupFile);
		GeneSetMap secondary = c.generalConfig.secondaryGeneSetGroupFile != null ? GeneSetMap.loadFromFile(c.generalConfig.secondaryGeneSetGroupFile) : null;
		run(c, primary, secondary);
	}
	
	public static void run(Configuration c, GeneSetMap primary) throws IOException {
		run(c, primary, null);
	}
	
	public static void run(Configuration c, GeneSetMap primary, GeneSetMap secondary) throws IOException {
		if(secondary != null){
			for(String primaryKey : primary.getGeneSetMap().keySet()){
				if(secondary.getGeneSetMap().keySet().contains(primaryKey)){
					System.err.println("Primary and secondary gene set groups cannot have duplicate identifier: " + primaryKey);
					return;
				}
			}
		}

		// load/compute pairwise shortest paths
		computeAndSaveSetGraphs(c, primary, secondary);


		// UPGMA
		performClusterAnalysis(c, primary, secondary);
	}


	private static void addPathToMapHelper(Protein source, Protein target, Path<Protein> path, Map<Protein, Map<Protein, Path<Protein>>> map, boolean bidirectional){
		Map<Protein, Path<Protein>> byTargets = map.get(source);
		if(byTargets == null)
			map.put(source, byTargets = new HashMap<>());
		byTargets.put(target, path);

		if(!bidirectional) return;


		if(path != null){
			final List<Edge<Protein>> edges = path.getEdges();
			Collections.reverse(edges);
			path = new Path<>(edges);
		}
		Protein tmp = source;
		source = target;
		target = tmp;


		byTargets = map.get(source);
		if(byTargets == null)
			map.put(source, byTargets = new HashMap<>());
		byTargets.put(target, path);
	}

	private static void savePaths(Configuration c, File file, Map<Protein, Map<Protein, Path<Protein>>> map) throws IOException {
		int saved = 0;
		//final Set<Tuple<Protein, Protein>> checked = new HashSet<>();
		try(final BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
			
			bw.write(c.proteinInteractomeConfig.STRINGversion); bw.newLine();
			bw.write(String.valueOf(c.proteinInteractomeConfig.minConfidence)); bw.newLine();
			bw.write(String.valueOf(c.analysisConfig.maxPathLength)); bw.newLine();
			bw.write(String.valueOf(c.analysisConfig.maxPathUnconfidence)); bw.newLine();
			
			for(Protein src : map.keySet()){
				final Map<Protein, Path<Protein>> byTarget = map.get(src);
				for(Protein target : byTarget.keySet()){
					//if(!checked.add(new Tuple<>(src, target)) || !checked.add(new Tuple<>(target, src)))
					//continue;
					final List<Edge<Protein>> pathEdges = byTarget.get(target).getEdges();
					if(pathEdges.size() == 0){
						bw.write("nopath");
						bw.write(',');
						bw.write(src.getId()); 
						bw.write(',');
						bw.write(target.getId());
					}else{
						bw.write(pathEdges.get(0).getSource().getId());
						for(Edge<Protein> edge : pathEdges){
							bw.write(',');
							bw.write(String.valueOf(edge.getWeight()));
							bw.write(',');
							bw.write(edge.getTarget().getId());
						}
					}
					saved++;
					bw.newLine();
				}
			}
		}
		System.out.println("Saved " + saved + " paths");
	}

	private static void loadPaths(Configuration c, File file, Map<Protein, Map<Protein, Path<Protein>>> map) throws IOException {
		try(final BufferedReader br = new BufferedReader(new FileReader(file))){
			int count = 0;
			String s;
			
			final String version = br.readLine();
			final double minConfidence = Double.parseDouble(br.readLine());
			final int maxPathLength = Integer.parseInt(br.readLine());
			final double maxPathUnconfidence = Double.parseDouble(br.readLine());
			
			if(!version.equals(c.proteinInteractomeConfig.STRINGversion)
					|| minConfidence != c.proteinInteractomeConfig.minConfidence
					|| maxPathLength != c.analysisConfig.maxPathLength
					|| maxPathUnconfidence != c.analysisConfig.maxPathUnconfidence){
				System.out.println("Old data outdated: need to recompute everything.");
				return;
			}
			
			while((s = br.readLine()) != null){
				count++;
				final String[] parts = s.split(",");
				if(parts.length < 3) continue;
				if(parts.length == 3 && parts[0].equalsIgnoreCase("nopath")){
					final Protein source = Protein.getProtein(parts[1], true);
					final Protein target = Protein.getProtein(parts[2], true);
					//System.out.println("L," + source.getId() + "," + target.getId());
					addPathToMapHelper(source, target, new Path<>(), map, false);
				}else{
					final Protein source = Protein.getProtein(parts[0], true);
					Protein prev = source;
					Protein target = null;

					final ArrayList<Edge<Protein>> pathEdges = new ArrayList<>();
					for(int i = 1; i < parts.length - 1; i += 2){
						final int weight = Integer.parseInt(parts[i]);
						target = Protein.getProtein(parts[i+1], true);
						pathEdges.add(new Edge<>(prev, target, weight));
						prev = target;
					}

					addPathToMapHelper(source, target, new Path<>(pathEdges), map, false);
					//System.out.println("L," + source.getId() + "," + target.getId());
				}
			}
			System.out.println("Loaded " + count + " paths.");
		}
	}

	private static int hits;
	private static int misses;
	private static void computeAndSaveSetGraphs(Configuration c, GeneSetMap primary, GeneSetMap secondary) throws IOException {
		final File dataFile = new File(c.generalConfig.activeDirectory + c.generalConfig.projectName + PROJECT_DATA_SUFFIX);
		final Map<Protein, Map<Protein, Path<Protein>>> precomputedPaths = new HashMap<>();
		if(c.analysisConfig.reusePreviousData && dataFile.exists())
			loadPaths(c, dataFile, precomputedPaths);

		// load protein interactome TODO load lazily
		ProteinInteractionGraph pig = new ProteinInteractionGraph(c.proteinInteractomeConfig, c.analysisConfig.maxPathUnconfidence);
		// TODO double check Dijkstra' returning null
		final Function<Tuple<Protein, Protein>, Path<Protein>> pathfinder = t -> {
			Path<Protein> path = precomputedPaths.getOrDefault(t.val1(), Collections.emptyMap()).get(t.val2());
			if(path == null){
				misses++;
				//System.out.println(t.val1().getId() + "," + t.val2().getId());
				path = pig.dijkstras(t.val1(), t.val2(), e -> 1000D - e.getWeight(),
						c.analysisConfig.maxPathUnconfidence,
						c.analysisConfig.maxPathLength);
				assert(path != null);
				//System.out.println(path.getEdges());
				//path = new Path<>();
				addPathToMapHelper(t.val1(), t.val2(), path, precomputedPaths, true);
			}else{
				hits++;
				//System.out.println("Found cached path");
			}
			if((hits + misses) % 1000 == 0){
				System.out.println(hits + misses + " pairwise paths checked...");
			}
			return path;
		};

		primary.computePairwisePathsAndGraph(pathfinder);
		if(secondary != null)
			secondary.computePairwisePathsAndGraph(pathfinder);
		
		
		//MOD12-2=EPHB2,P4HA2,ARHGEF10L,MYLK,ANGPTL4,SPTA1,ALK,LPA,HCLS1,PLA2G4C,MAP4K1,PRKCA,TBXAS1,ADH6,IQGAP2
		//System.out.println(primary.getGeneSetMap().get("MOD12-2").getGraph().get);
		//System.exit(0);;
		//GeneSet gs = (primary.getGeneSetMap().get("MOD1-4"));
		//System.out.println("Number of genes in MOD1-4 graph.: " + gs.getGenes().size());
		//System.exit(0);;
		
		
		System.out.println(hits + " path cache hits.");
		System.out.println(misses + " path cache misses.");
		System.out.println((double) hits / (hits + misses) + " proportion of paths cached.");
		savePaths(c, dataFile, precomputedPaths);
	}

	private static <K> LayeredGraph<K> reduceLayeredGraph(LayeredGraph<K> g, Configuration c){
		//System.out.println("Original graph size: " + g.getNodes().size());
		final ArrayList<Tuple<K, Integer>> degree = new ArrayList<>();
		for(K k : g.getNodes()) degree.add(new Tuple<>(k, g.getNeighbors(k).size()));
		degree.sort(Comparator.comparingInt(t -> -t.val2()));
		final int toRetainCount = (int) Math.ceil(Math.min(c.analysisConfig.percentageOfNodesToRender * degree.size(), c.analysisConfig.maxNodesInGraphToRender));
		final Set<K> toRetain = new HashSet<>();
		for(int i = 0; i < toRetainCount; i++) toRetain.add(degree.get(i).val1());
		//System.out.println("Reduced graph size: " + toRetain.size());
		return g.subgraphWithNodes(new LayeredGraph<>(), toRetain);
	}

	private static Color parseColorOrDefault(String s, Color defaultColor){
		if(s == null || s.isEmpty() || s.equalsIgnoreCase("null")) return defaultColor;
		assert(s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')');
		final String[] parts = s.substring(1, s.length() - 1).split(",");
		return new Color(Integer.parseInt(parts[0]),
				Integer.parseInt(parts[1]),
				Integer.parseInt(parts[2]));
	}

	private static Color colorGradient(Color start, Color end, double percentage){
		return new Color((int)(start.getRed() * (1 - percentage) + end.getRed() * percentage),
				(int)(start.getGreen() * (1 - percentage) + end.getGreen() * percentage),
				(int)(start.getBlue() * (1 - percentage) + end.getBlue() * percentage));
	}

	private static Color mixColors(Color x, Color y){
		return colorGradient(x, y, 0.5);
	}

	private static Function<Protein, Color> createNodeColorFunction(Configuration c, GeneSetMap primary, GeneSetMap secondary, LayeredGraph<Protein> graph){
		final Color defaultColor = parseColorOrDefault(c.rendererConfig.defaultNodeColor, null);
		if(defaultColor == null)
			throw new RuntimeException("At least the default color must be specified");
		final Color primaryColor = parseColorOrDefault(c.rendererConfig.primaryGroupNodeColor, defaultColor);
		
		final Color secondaryColor = parseColorOrDefault(c.rendererConfig.secondaryGroupNodeColor, defaultColor);
		final Color mixedColor = parseColorOrDefault(c.rendererConfig.bothGroupsNodeColor, mixColors(primaryColor, secondaryColor));
		
		
		//System.out.println(mixedColor);
		
		if(c.rendererConfig.varyNodeAlphaValues){
			final int LOWER_ALPHA_BOUND = c.rendererConfig.minNodeAlpha;
			final int UPPER_ALPHA_BOUND = 255;
			final double MAXIMUM_NUM = graph.getMaxCount();
			//System.out.println("Max count of graph: " + MAXIMUM_NUM);
			return p -> {
				final boolean isPrimary = primary.getUniqueProteins().contains(p);
				final boolean isSecondary = secondary != null && secondary.getUniqueProteins().contains(p);
				final Color color;
				if(isPrimary && isSecondary) color = mixedColor;
				else if(isPrimary) color = primaryColor;
				else if(isSecondary) color = secondaryColor;
				else color = defaultColor;
				final int alpha = (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double) graph.getCount(p) / MAXIMUM_NUM);
				
				//if(p.getGene() != null)
				//	System.out.println("Node color for " + p.getGene().getSymbol() + ": " + color);
				//System.out.println(alpha);
				
				return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
			};
		}
		return p -> {
			final boolean isPrimary = primary.getUniqueProteins().contains(p);
			final boolean isSecondary = secondary != null && secondary.getUniqueProteins().contains(p);
			if(isPrimary && isSecondary) return mixedColor;
			else if(isPrimary) return primaryColor;
			else if(isSecondary) return secondaryColor;
			return defaultColor;
		};
	}

	private static Function<Edge<Protein>, Color> createEdgeColorFunction(Configuration c, GeneSetMap primary, GeneSetMap secondary, LayeredGraph<Protein> graph){
		final int LOWER_ALPHA_BOUND = c.rendererConfig.minEdgeAlpha;
		final int UPPER_ALPHA_BOUND = 255;
		final double MAXIMUM_NUM = graph.getMaxCount();
		if(c.rendererConfig.varyEdgeAlphaValues)
			return e -> new Color(0, 0, 0, (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * Math.max(graph.getCount(e.getSource()), graph.getCount(e.getTarget())) / MAXIMUM_NUM));
		return e -> Color.BLACK;
	}

	private static Renderer<Protein> createRenderer(Configuration c, GeneSetMap primary, GeneSetMap secondary, LayeredGraph<Protein> graph){
		final Renderer<Protein> renderer = new GUIRenderer<>(c.rendererConfig, new File(c.generalConfig.imageDirectory));
		renderer.setLabelFunction(p -> p.getGene() == null ? p.getId() : p.getGene().getSymbol());
		renderer.setNodeColorFunction(createNodeColorFunction(c, primary, secondary, graph));
		renderer.setEdgeColorFunction(createEdgeColorFunction(c, primary, secondary, graph));
		return renderer;
	}

	private static void layoutAndRender(Configuration c, GeneSetMap primary, GeneSetMap secondary, String projectNameSuffix) throws IOException {
		// Render primary (secondary, primary - secondary, secondary - primary)
		final List<Tuple<LayeredGraph<Protein>, String>> toRender = new LinkedList<>();
		
		final LayeredGraph<Protein> primaryGraph = primary.getLayeredGraph();
		final LayeredGraph<Protein> primaryGraphReduced = reduceLayeredGraph(primaryGraph, c);
		if(primaryGraphReduced.getNodes().size() > 0)
			toRender.add(new Tuple<>(primaryGraphReduced, "Primary"));

		//TODO refactor code
		if(secondary != null){
			
			final LayeredGraph<Protein> secondaryGraph = secondary.getLayeredGraph();
			final LayeredGraph<Protein> secondaryGraphReduced = reduceLayeredGraph(secondaryGraph, c);
			if(secondaryGraphReduced.getNodes().size() != 0){
				toRender.add(new Tuple<>(secondaryGraphReduced, "Secondary"));

				if(c.analysisConfig.calculateGraphDifferences){
					final double numPrimary = primary.getGeneSetMap().size();
					final double numSecondary = secondary.getGeneSetMap().size();
					final double primaryScalingFactor = numPrimary < numSecondary ? numSecondary / numPrimary : 1;
					final double secondaryScalingFactor = numSecondary < numPrimary ? numPrimary / numSecondary : 1;
					
					
					final LayeredGraph<Protein> primaryMinusSecondary = reduceLayeredGraph(primaryGraph.subtract(secondaryGraph, primaryScalingFactor, secondaryScalingFactor), c);
					if(primaryMinusSecondary.getNodes().size() > 0)
						toRender.add(new Tuple<>(primaryMinusSecondary, "PrimaryMinusSecondary"));
					final LayeredGraph<Protein> secondaryMinusPrimary = reduceLayeredGraph(secondaryGraph.subtract(primaryGraph, secondaryScalingFactor, primaryScalingFactor), c);
					if(secondaryMinusPrimary.getNodes().size() > 0)
						toRender.add(new Tuple<>(secondaryMinusPrimary, "SecondaryMinusPrimary"));
				}
			}
		}

		for(Tuple<LayeredGraph<Protein>, String> pair : toRender){
			new ForceDirectedLayout<>(
					c.forceDirectedLayoutConfig,
					pair.val1(),
					createRenderer(c, primary, secondary, pair.val1()),
					c.generalConfig.projectName + projectNameSuffix + "_" + pair.val2())
			.layoutAndRender();
		}

	}

	private static <K> double calculateIntersectionOverUnionSimilarity(Graph<K> x, Graph<K> y){
		final Set<K> union = new HashSet<>(x.getNodes());
		union.addAll(y.getNodes());
		if(union.size() == 0) return 0;
		final Set<K> intersection = new HashSet<>(x.getNodes());
		intersection.retainAll(y.getNodes());
		return (double) intersection.size() / union.size();
	}

	private static double calculateGraphDistance(Graph<Protein> x, Graph<Protein> y){
		return (1 - calculateIntersectionOverUnionSimilarity(x, y));
	}

	private static void performClusterAnalysis(Configuration c, GeneSetMap primary, GeneSetMap secondary) {
		final Map<String, GeneSet> allPatients = new HashMap<>();
		final Map<String, PhylogeneticTreeNode> leaves = new HashMap<>();

		//TODO FIXME which weight ratios to use?
		final double primaryGeneSetSize = primary.getGeneSetMap().size();
		final double secondaryGeneSetSize = secondary.getGeneSetMap().size();

		double primaryInitialWeight = primaryGeneSetSize < secondaryGeneSetSize ? secondaryGeneSetSize / primaryGeneSetSize : 1;
		double secondaryInitialWeight = secondaryGeneSetSize < primaryGeneSetSize ? primaryGeneSetSize / secondaryGeneSetSize : 1;
		
		//primaryInitialWeight = secondaryInitialWeight = 1;

		for(String patient : primary.getGeneSetMap().keySet()){
			allPatients.put(patient, primary.getGeneSetMap().get(patient));
			leaves.put(patient, new PhylogeneticTreeNode(patient, primaryInitialWeight));
		}
		for(String patient : secondary.getGeneSetMap().keySet()){
			allPatients.put(patient, secondary.getGeneSetMap().get(patient));
			leaves.put(patient, new PhylogeneticTreeNode(patient, secondaryInitialWeight));
		}
		final String[] allPatientKeys = allPatients.keySet().toArray(new String[allPatients.size()]);
		
		if(allPatients.size() == 1){
			//only 1 patient gene set provided (assumed to be in primary); layout and render that patient
			try {
				layoutAndRender(c, primary, secondary, allPatientKeys[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		final DistanceMatrix<PhylogeneticTreeNode> dissimilarityMatrix = new DistanceMatrix<>();

		for(int i = 0; i < allPatientKeys.length - 1; i++){
			for(int j = i + 1; j < allPatientKeys.length; j++){
				final Graph<Protein> x = allPatients.get(allPatientKeys[i]).getGraph();
				final Graph<Protein> y = allPatients.get(allPatientKeys[j]).getGraph();

				dissimilarityMatrix.setDistance(
						leaves.get(allPatientKeys[i]),
						leaves.get(allPatientKeys[j]),
						calculateGraphDistance(x, y));
			}
		}
		final PhylogeneticTreeNode treeRoot = PhylogeneticTree.createTreeFromMatrix(dissimilarityMatrix);

		try {
			final DendrogramRenderer dr = new DendrogramRenderer(c.rendererConfig, new File(c.generalConfig.imageDirectory));

			final Color defaultColor = parseColorOrDefault(c.rendererConfig.defaultNodeColor, null);
			if(defaultColor == null)
				throw new RuntimeException("At least the default color must be specified");
			final Color primaryColor = parseColorOrDefault(c.rendererConfig.primaryGroupNodeColor, defaultColor);
			final Color secondaryColor = parseColorOrDefault(c.rendererConfig.secondaryGroupNodeColor, defaultColor);

			final Function<PhylogeneticTreeNode, Color> clusterEdgeColorFunction = ptn -> {
				double primaryWeight = 0;
				final PhylogeneticTreeNode[] nodeLeafs = ptn.getLeaves();
				if(nodeLeafs.length > 0){
					for(PhylogeneticTreeNode ptnLeaf : nodeLeafs)
						if(primary.getGeneSetMap().containsKey(ptnLeaf.getLabel()))
							primaryWeight += ptnLeaf.getWeight();
				}else{
					primaryWeight = primary.getGeneSetMap().containsKey(ptn.getLabel()) ? ptn.getWeight() : 0;
				}
				final double percentagePrimary = primaryWeight / ptn.getWeight();
				if(percentagePrimary >= 0.5){
					return colorGradient(Color.BLACK, primaryColor, (percentagePrimary - 0.5) * 2);
				}else{
					final double percentageSecondary = 1 - percentagePrimary;
					return colorGradient(Color.BLACK, secondaryColor, (percentageSecondary - 0.5) * 2);
				}
			};
			dr.setClusterEdgeColorFunction(clusterEdgeColorFunction);

			final Map<String, PhylogeneticTreeNode> clusterMapping = 
					dr.render(treeRoot, c.generalConfig.projectName + "_Dendrogram");


			System.out.println("\n\n\n---INPUT REQUIRED---\nCommand examples:\n"
					+  allPatientKeys[0] + ": Layout and render the pairwise paths graph of patient " + allPatientKeys[0] + "\n"
					+ "C5: Layout and render the summary graph of all nodes belonging to C5\n"
					+ "info C17: Display the phylogenetic tree information for cluster C17\n"
					+ "q or quit: Terminates the application\n");

			@SuppressWarnings("resource")
			final Scanner in = new Scanner(System.in);
			while(true){
				final String s = in.nextLine().trim();
				if(s.equalsIgnoreCase("q") || s.equalsIgnoreCase("quit"))
					System.exit(0);

				if(s.toLowerCase().startsWith("info")){
					final String id = s.substring(4).trim();

					final PhylogeneticTreeNode ptn = clusterMapping.get(id);

					if(ptn == null){
						System.out.println(id + " is an invalid cluster identifier...\n");
						continue;
					}

					double primaryWeight = 0;
					final PhylogeneticTreeNode[] nodeLeafs = ptn.getLeaves();
					for(PhylogeneticTreeNode ptnLeaf : nodeLeafs)
						if(primary.getGeneSetMap().containsKey(ptnLeaf.getLabel()))
							primaryWeight += ptnLeaf.getWeight();
					double maxDissimilarity = 0;
					for(int i = 0; i < nodeLeafs.length - 1; i++){
						for(int j = i + 1; j < nodeLeafs.length; j++){

							final Graph<Protein> x = allPatients.get(nodeLeafs[i].getLabel()).getGraph();
							final Graph<Protein> y = allPatients.get(nodeLeafs[j].getLabel()).getGraph();
							final double distance = calculateGraphDistance(x, y);

							if(distance > maxDissimilarity)
								maxDissimilarity = distance;

						}
					}
					System.out.println("Information for " + id + ":");
					System.out.println("\tDepth: " + ((int)(ptn.getDepth() * 100.0)) / 100.0);
					System.out.println("\tNumber of leaves: " + nodeLeafs.length);
					//System.out.println("\tTotal weight: " + ptn.getWeight());
					System.out.println("\tNumber primary: " + Math.round((primaryWeight / primaryInitialWeight)) + " (" + ((int)(primaryWeight * 10000.0 / ptn.getWeight()))/100.0 + "%)");
					System.out.println("\tNumber secondary: " + Math.round(((ptn.getWeight() - primaryWeight) / secondaryInitialWeight)) + " (" + ((int)((ptn.getWeight() - primaryWeight) * 10000.0 / ptn.getWeight()))/100.0 + "%)"); 
					System.out.println("\tMax dissimilarity: " + ((int)(maxDissimilarity * 100.0)) / 100.0);
					System.out.println();
				}else{
					final Set<String> patientsToInclude = new HashSet<>();
					final PhylogeneticTreeNode ptn = clusterMapping.get(s);
					if(ptn != null){
						System.out.println("Laying out and rendering graphs from patients in " + s + "...");
						for(PhylogeneticTreeNode leaf : ptn.getLeaves())
							patientsToInclude.add(leaf.getLabel());
					}else{
						if(primary.getGeneSetMap().containsKey(s) || secondary.getGeneSetMap().containsKey(s)){
							patientsToInclude.add(s);
							System.out.println("Laying out and rendering graph for patient " + s + "...");
						}else{
							System.out.println(s + " is an invalid cluster or patient identifier...\n");
							continue;
						}
					}
					
					
					System.out.println("All patients to include: " + patientsToInclude);
					final Set<String> primaryPatientsToInclude = new HashSet<>(patientsToInclude);
					primaryPatientsToInclude.retainAll(primary.getGeneSetMap().keySet());
					final Set<String> secondaryPatientsToInclude = new HashSet<>(patientsToInclude);
					secondaryPatientsToInclude.retainAll(secondary.getGeneSetMap().keySet());
					System.out.println("Primary patients: " + primaryPatientsToInclude);
					System.out.println("Secondary patients: " + secondaryPatientsToInclude);
					
					final GeneSetMap subsetPrimary = primary.subset(primaryPatientsToInclude);
					final GeneSetMap subsetSecondary = secondary.subset(secondaryPatientsToInclude);
					
					layoutAndRender(c, subsetPrimary, subsetSecondary, s);
					System.out.println("Done\n");
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
