package org.armanious.network.analysis;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.armanious.Tuple;
import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.graph.LayeredGraph;
import org.armanious.graph.Path;
import org.armanious.network.Configuration;
import org.armanious.network.visualization.ForceDirectedLayout;
import org.armanious.network.visualization.GUIRenderer;
import org.armanious.network.visualization.Renderer;

public final class NetworkAnalysis {

	private NetworkAnalysis(){}

	private static final String PROJECT_DATA_SUFFIX = "_Data.txt";

	public static void run(Configuration c) throws IOException {
		// load gene sets
		GeneSetMap primary = GeneSetMap.loadFromFile(c.generalConfig.primaryGeneSetGroupFile);
		GeneSetMap secondary = GeneSetMap.loadFromFile(c.generalConfig.secondaryGeneSetGroupFile);

		// load/compute pairwise shortest paths
		computeAndSaveSetGraphs(c, primary, secondary);


		// UPGMA
		performClusterAnalysis(c, primary, secondary);


		// Layout and render
		if(c.analysisConfig.layoutAndRender) layoutAndRender(c, primary, secondary);

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

	private static void savePaths(File file, Map<Protein, Map<Protein, Path<Protein>>> map) throws IOException {
		int saved = 0;
		//final Set<Tuple<Protein, Protein>> checked = new HashSet<>();
		try(final BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
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

	private static void loadPaths(File file, Map<Protein, Map<Protein, Path<Protein>>> map) throws IOException {
		try(final BufferedReader br = new BufferedReader(new FileReader(file))){
			int count = 0;
			String s;
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
		final File dataFile = new File(c.generalConfig.projectName + PROJECT_DATA_SUFFIX);
		final Map<Protein, Map<Protein, Path<Protein>>> precomputedPaths = new HashMap<>();
		if(c.analysisConfig.reusePreviousData && dataFile.exists())
			loadPaths(dataFile, precomputedPaths);
		
		// load protein interactome TODO load lazily
		ProteinInteractionGraph pig = new ProteinInteractionGraph(c.proteinInteractomeConfig, c.analysisConfig.maxPathUnconfidence);
		// TODO double check Dijkstra' returning null
		final Function<Tuple<Protein, Protein>, Path<Protein>> pathfinder = t -> {
			Path<Protein> path = precomputedPaths.getOrDefault(t.val1(), Collections.emptyMap()).get(t.val2());
			if(path == null){
				misses++;
				System.out.println(t.val1().getId() + "," + t.val2().getId());
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
		secondary.computePairwisePathsAndGraph(pathfinder);
		System.out.println(hits + " path cache hits.");
		System.out.println(misses + " path cache misses.");
		System.out.println((double) hits / (hits + misses) + " proportion of paths cached.");
		savePaths(dataFile, precomputedPaths);
	}

	private static <K> LayeredGraph<K> reduceLayeredGraph(LayeredGraph<K> g, Configuration c){
		final ArrayList<Tuple<K, Integer>> degree = new ArrayList<>();
		for(K k : g.getNodes()) degree.add(new Tuple<>(k, g.getNeighbors(k).size()));
		degree.sort(Comparator.comparingInt(t -> -t.val2()));
		final int toRetainCount = (int) Math.ceil(Math.min(c.analysisConfig.percentageOfNodesToRender * degree.size(), c.analysisConfig.maxNodesInGraphToRender));
		final Set<K> toRetain = new HashSet<>();
		for(int i = 0; i < toRetainCount; i++) toRetain.add(degree.get(i).val1());
		return g.subgraphWithNodes(new LayeredGraph<>(), toRetain);
	}

	private static Color parseColorOrDefault(String s, Color defaultColor){
		if(s == null) return defaultColor;
		assert(s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')');
		final String[] parts = s.substring(1, s.length() - 1).split(",");
		return new Color(Integer.parseInt(parts[0]),
				Integer.parseInt(parts[1]),
				Integer.parseInt(parts[2]));
	}

	private static Color mixColors(Color x, Color y){
		return new Color((x.getRed() + y.getRed()) / 2,
				(x.getGreen() + y.getGreen()) / 2,
				(x.getBlue() + y.getBlue()) / 2);
	}

	private static Function<Protein, Color> createNodeColorFunction(Configuration c, GeneSetMap primary, GeneSetMap secondary, LayeredGraph<Protein> graph){
		final Color defaultColor = parseColorOrDefault(c.rendererConfig.defaultNodeColor, null);
		if(defaultColor == null)
			throw new RuntimeException("At least the default color must be specified");
		final Color primaryColor = parseColorOrDefault(c.rendererConfig.primaryGroupNodeColor, defaultColor);
		final Color secondaryColor = parseColorOrDefault(c.rendererConfig.secondaryGroupNodeColor, defaultColor);
		final Color mixedColor = parseColorOrDefault(c.rendererConfig.bothGroupsNodeColor, mixColors(primaryColor, secondaryColor));
		
		if(c.rendererConfig.varyNodeAlphaValues){
			final int LOWER_ALPHA_BOUND = 50;
			final int UPPER_ALPHA_BOUND = 255;
			final double MAXIMUM_NUM = graph.getMaxCount();
			return p -> {
				final boolean isPrimary = primary.getUniqueProteins().contains(p);
				final boolean isSecondary = secondary != null && secondary.getUniqueProteins().contains(p);
				final Color color;
				if(isPrimary && isSecondary) color = mixedColor;
				else if(isPrimary) color = primaryColor;
				else if(isSecondary) color = secondaryColor;
				else color = defaultColor;
				final int alpha = (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double) graph.getCount(p) / MAXIMUM_NUM);
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
		final int LOWER_ALPHA_BOUND = 50;
		final int UPPER_ALPHA_BOUND = 255;
		final double MAXIMUM_NUM = graph.getMaxCount();
		if(c.rendererConfig.varyEdgeAlphaValues)
			return e -> new Color(0, 0, 0, (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double) graph.getCount(e.getSource()) / MAXIMUM_NUM));
		return e -> Color.BLACK;
	}

	private static Renderer<Protein> createRenderer(Configuration c, GeneSetMap primary, GeneSetMap secondary, LayeredGraph<Protein> graph){
		final Renderer<Protein> renderer = new GUIRenderer<>(c.rendererConfig);
		renderer.setLabelFunction(p -> p.getGene() == null ? p.getId() : p.getGene().getSymbol());
		renderer.setNodeColorFunction(createNodeColorFunction(c, primary, secondary, graph));
		renderer.setEdgeColorFunction(createEdgeColorFunction(c, primary, secondary, graph));
		return renderer;
	}

	private static void layoutAndRender(Configuration c, GeneSetMap primary, GeneSetMap secondary) throws IOException {
		// Render primary (secondary, primary - secondary, secondary - primary)
		final List<Tuple<LayeredGraph<Protein>, String>> toRender = new LinkedList<>();
		
		final LayeredGraph<Protein> primaryGraph = reduceLayeredGraph(primary.getLayeredGraph(), c);
		if(primaryGraph.getNodes().size() > 0)
			toRender.add(new Tuple<>(primaryGraph, "Primary"));
		
		if(secondary != null){
			final LayeredGraph<Protein> secondaryGraph = reduceLayeredGraph(secondary.getLayeredGraph(), c);
			if(secondaryGraph.getNodes().size() != 0){
				toRender.add(new Tuple<>(primaryGraph, "Secondary"));
				
				if(c.analysisConfig.calculateGraphDifferences){
					final LayeredGraph<Protein> primaryMinusSecondary = reduceLayeredGraph(primary.getLayeredGraph().subtract(secondary.getLayeredGraph()), c);
					if(primaryMinusSecondary.getNodes().size() > 0)
						toRender.add(new Tuple<>(primaryMinusSecondary, "PrimaryMinusSecondary"));
					final LayeredGraph<Protein> secondaryMinusPrimary = reduceLayeredGraph(secondary.getLayeredGraph().subtract(primary.getLayeredGraph()), c);
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
					c.generalConfig.projectName + "_" + pair.val2())
			.layoutAndRender();
		}

	}
	
	private static <K> double calculateIntersectionOverUnionSimilarity(Graph<K> x, Graph<K> y){
		final Set<K> union = new HashSet<>(x.getNodes());
		union.addAll(y.getNodes());
		final Set<K> intersection = new HashSet<>(x.getNodes());
		intersection.retainAll(y.getNodes());
		return (double) intersection.size() / union.size();
	}
	
	private static double calculateGraphDistance(Graph<Protein> x, Graph<Protein> y){
		return (1 - calculateIntersectionOverUnionSimilarity(x, y));
	}

	private static void performClusterAnalysis(Configuration c, GeneSetMap primary, GeneSetMap secondary) {
		// TODO UPGMA
		final Map<String, GeneSet> allPatients = new HashMap<>();
		final Map<String, PhylogeneticTreeNode> leaves = new HashMap<>();
		
		for(String patient : primary.getGeneSetMap().keySet()){
			allPatients.put("<P>" + patient, primary.getGeneSetMap().get(patient));
			leaves.put("<P>" + patient, new PhylogeneticTreeNode("<P>" + patient, 1));
		}
		for(String patient : secondary.getGeneSetMap().keySet()){
			allPatients.put("<S>" + patient, secondary.getGeneSetMap().get(patient));
			leaves.put("<S>" + patient, new PhylogeneticTreeNode("<S>" + patient, 1));
		}
		final String[] allPatientKeys = allPatients.keySet().toArray(new String[allPatients.size()]);
		
		
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
		//System.out.println(dissimilarityMatrix);
		
		System.out.println("\n\nCLUSTER ANALYSIS");
		final LinkedList<PhylogeneticTreeNode> clustersToAnalyze = new LinkedList<>();
		clustersToAnalyze.add(PhylogeneticTree.createTreeFromMatrix(dissimilarityMatrix));
		while(!clustersToAnalyze.isEmpty()){
			final PhylogeneticTreeNode ptn = clustersToAnalyze.removeFirst();
			if(ptn.getWeight() == 1) continue; //ignore leaves
			int primaryCount = 0;
			final String[] childLabels = ptn.getChildLabels();
			Arrays.sort(childLabels);
			for(String label : childLabels)
				if(label.startsWith("<P>"))
					primaryCount++;
			double maxDissimilarity = 0;
			for(int i = 0; i < childLabels.length - 1; i++){
				for(int j = i + 1; j < childLabels.length; j++){
					
					final Graph<Protein> x = allPatients.get(childLabels[i]).getGraph();
					final Graph<Protein> y = allPatients.get(childLabels[j]).getGraph();
					final double distance = calculateGraphDistance(x, y);
					
					if(distance > maxDissimilarity)
						maxDissimilarity = distance;
					
				}
			}
			System.out.println(Arrays.toString(childLabels));
			System.out.println("\tdepth = " + ptn.getDepth() + "; total = " + ptn.getWeight() + 
					"; primary = " + primaryCount + " (" + ((int)(primaryCount * 10000.0 / ptn.getWeight()))/100.0 + "%)" + 
					"; secondary = " + (ptn.getWeight() - primaryCount) + " (" + ((int)((ptn.getWeight() - primaryCount) * 10000.0 / ptn.getWeight()))/100.0 + "%)" +
					"; max dissimilarity = " + maxDissimilarity + 
					"; max dissimilarity/2 - depth = " + (maxDissimilarity*0.5-ptn.getDepth())
					);
			//System.out.println("\t" + ptn.getLabel());
			if(ptn.getLeftChild() != null) clustersToAnalyze.add(ptn.getLeftChild());
			if(ptn.getRightChild() != null) clustersToAnalyze.add(ptn.getRightChild());
		}
		
		System.out.println("\n\n");
		
	}
	
}
