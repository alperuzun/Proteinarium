package org.armanious.network.analysis;

import java.awt.Color;
import java.awt.Desktop;
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
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

import org.armanious.Tuple;
import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.graph.LayeredGraph;
import org.armanious.graph.LayeredGraph.Type;
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
		Tuple<Map<String, Gene>, Map<String, Protein>> maps = Gene.loadGenes(c.generalConfig.proteinAliasesFile);		
		final Map<String, Gene> geneMap = maps.val1();
		final Map<String, Protein> proteinMap = maps.val2();
		Function<String, Gene> database = (symbol) -> geneMap.get(symbol);

		if(c.generalConfig.group1GeneSetFile == null){
			System.err.println("group1GeneSetFile must be specified; exiting...");
			System.exit(1);;
		}
		GeneSetMap group1 = GeneSetMap.loadFromFile(c.generalConfig.group1GeneSetFile, database, LayeredGraph.Type.GROUP1);
		GeneSetMap group2;
		GeneSetMap combined;
		if(c.generalConfig.group2GeneSetFile != null) {
			group2 = GeneSetMap.loadFromFile(c.generalConfig.group2GeneSetFile, database, LayeredGraph.Type.GROUP2);
			final Map<String, GeneSet> combinedMap = new HashMap<>();
			combinedMap.putAll(group1.getGeneSetMap());
			combinedMap.putAll(group2.getGeneSetMap());
			combined = GeneSetMap.fromExistingMap(combinedMap, LayeredGraph.Type.COMBINED);
		}else {
			group2 = new GeneSetMap(LayeredGraph.Type.GROUP2);
			combined = new GeneSetMap(LayeredGraph.Type.COMBINED);
		}

		run(c, group1, group2, combined, proteinMap);
	}

	public static void run(Configuration c, GeneSetMap group1) throws IOException {
		run(c, group1, new GeneSetMap(LayeredGraph.Type.GROUP2));
	}

	public static void run(Configuration c, GeneSetMap group1, GeneSetMap group2) throws IOException {
		final Tuple<Map<String, Gene>, Map<String, Protein>> maps = Gene.loadGenes(c.generalConfig.proteinAliasesFile);		
		final Map<String, Protein> proteinMap = maps.val2();
		final Map<String, GeneSet> combinedMap = new HashMap<>();
		combinedMap.putAll(group1.getGeneSetMap());
		combinedMap.putAll(group2.getGeneSetMap());
		final GeneSetMap combined = GeneSetMap.fromExistingMap(combinedMap, LayeredGraph.Type.COMBINED);
		run(c, group1, group2, combined, proteinMap);
	}

	public static void run(Configuration c, GeneSetMap group1, GeneSetMap group2, GeneSetMap combined, Map<String, Protein> proteinMap) throws IOException {
		if(group2 == null)
			group2 = new GeneSetMap(LayeredGraph.Type.GROUP2);
		if(combined == null)
			combined = new GeneSetMap(LayeredGraph.Type.COMBINED);

		for(String group1Key : group1.getGeneSetMap().keySet()){
			if(group2.getGeneSetMap().keySet().contains(group1Key)){
				System.err.println("Cannot have duplicate patient identifier: " + group1Key);
				System.exit(1);
			}
		}

		final GeneSetMap[] bootstrapGeneSetMaps;
		if(c.analysisConfig.bootstrappingRounds != 0 && c.analysisConfig.bootstrappingRounds < 100) {
			System.out.println("If bootstrapping, must specify at least 100 rounds. Skipping bootstrapping...");
			bootstrapGeneSetMaps = new GeneSetMap[0];
		} else {
			bootstrapGeneSetMaps = new GeneSetMap[c.analysisConfig.bootstrappingRounds];
			sampleForBootstrapping(bootstrapGeneSetMaps, combined);
		}
		
		// load/compute pairwise shortest paths
		computeAndSaveSetGraphs(c, group1, group2, combined, bootstrapGeneSetMaps, proteinMap);


		// UPGMA
		performClusterAnalysis(c, group1, group2, combined, bootstrapGeneSetMaps);
	}


	/*private static void addPathToMapHelper_(Protein source, Protein target, Path<Protein> path, Map<Protein, Map<Protein, Path<Protein>>> map, boolean bidirectional){
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
	}*/

	private static void addPathToMapHelper(Protein source, Protein target, Path<Protein> path, Map<Protein, Map<Protein, Path<Protein>>> map){
		assert(source.getId().compareTo(target.getId()) <= 0);
		Map<Protein, Path<Protein>> byTargets = map.get(source);
		if(byTargets == null)
			map.put(source, byTargets = new HashMap<>());
		byTargets.put(target, path);
	}

	private static void savePaths(Configuration c, File file, Map<Protein, Map<Protein, Path<Protein>>> map) throws IOException {
		int saved = 0;
		//final Set<Tuple<Protein, Protein>> checked = new HashSet<>();
		try(final BufferedWriter bw = new BufferedWriter(new FileWriter(file))){

			bw.write(String.valueOf(c.analysisConfig.minInteractomeConfidence)); bw.newLine();
			bw.write(String.valueOf(c.analysisConfig.maxPathLength)); bw.newLine();
			bw.write(String.valueOf(c.analysisConfig.maxPathCost)); bw.newLine();

			for(Protein src : map.keySet()){
				final Map<Protein, Path<Protein>> byTarget = map.get(src);
				for(Protein target : byTarget.keySet()){
					assert(src.getId().compareTo(target.getId()) <= 0);
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


	//TODO FIXME
	private static void loadPaths(Configuration c, File file, Map<Protein, Map<Protein, Path<Protein>>> map, Map<String, Protein> proteinMap) throws IOException {
		try(final BufferedReader br = new BufferedReader(new FileReader(file))){
			int count = 0;
			String s;

			final double minConfidence = Double.parseDouble(br.readLine());
			final int maxPathLength = Integer.parseInt(br.readLine());
			final double maxPathUnconfidence = Double.parseDouble(br.readLine());

			if(minConfidence != c.analysisConfig.minInteractomeConfidence
					|| maxPathLength != c.analysisConfig.maxPathLength
					|| maxPathUnconfidence != c.analysisConfig.maxPathCost){
				System.out.println("Old data outdated: need to recompute everything.");
				return;
			}

			while((s = br.readLine()) != null){
				count++;
				final String[] parts = s.split(",");
				if(parts.length < 3) continue;
				if(parts.length == 3 && parts[0].equalsIgnoreCase("nopath")){
					final Protein source = proteinMap.get(parts[1]); //Protein.getProtein(parts[1], true);
					final Protein target = proteinMap.get(parts[2]); //Protein.getProtein(parts[2], true);
					//System.out.println("L," + source.getId() + "," + target.getId());
					assert(source.getId().compareTo(target.getId()) <= 0);
					addPathToMapHelper(source, target, new Path<>(), map);
				}else{
					final Protein source = proteinMap.get(parts[0]); //Protein.getProtein(parts[0], true);
					Protein prev = source;
					Protein target = null;

					final ArrayList<Edge<Protein>> pathEdges = new ArrayList<>();
					for(int i = 1; i < parts.length - 1; i += 2){
						final int weight = Integer.parseInt(parts[i]);
						target = proteinMap.get(parts[i+1]); //Protein.getProtein(parts[i+1], true);
						pathEdges.add(new Edge<>(prev, target, weight));
						prev = target;
					}
					assert(source.getId().compareTo(target.getId()) <= 0);
					addPathToMapHelper(source, target, new Path<>(pathEdges), map);
				}
			}
			System.out.println("Loaded " + count + " paths.");
		}
	}

	private static void sampleForBootstrapping(GeneSetMap[] bootstrappedMaps, GeneSetMap combined) {
		final Set<Gene> uniqueGenes = combined.getUniqueGenes();
		final int n = uniqueGenes.size();
		final double probability = 1 - Math.pow(((double) n - 1) / n, n);
		final Random random = new Random();
		
		final Map<String, GeneSet> combinedMap = combined.getGeneSetMap();
		for(int i = 0; i < bootstrappedMaps.length; i++) {
			final Map<String, Set<Gene>> gsm = new HashMap<>();
			for(String patient : combinedMap.keySet()) {
				gsm.put(patient, new HashSet<>());
			}
			for(Gene gene : uniqueGenes) {
				if(random.nextDouble() <= probability) {
					for(String patient : combinedMap.keySet()) {
						if(combinedMap.get(patient).getGenes().contains(gene)) {
							gsm.get(patient).add(gene);
						}
					}
				}
			}
			bootstrappedMaps[i] = new GeneSetMap(gsm, Type.BOOTSTRAP);
		}
	}

	//private static int hits;
	//private static int misses;
	private static void computeAndSaveSetGraphs(Configuration c, GeneSetMap group1, GeneSetMap group2, GeneSetMap combined, GeneSetMap[] bootstrapGeneSetMaps, Map<String, Protein> proteinMap) throws IOException {
		final File dataFile = new File(c.generalConfig.activeDirectory + c.generalConfig.projectName + PROJECT_DATA_SUFFIX);
		final Map<Protein, Map<Protein, Path<Protein>>> precomputedPaths = new HashMap<>();
		if(c.analysisConfig.reusePreviousData && dataFile.exists())
			loadPaths(c, dataFile, precomputedPaths, proteinMap);

		// load protein interactome TODO load lazily
		ProteinInteractionGraph pig = new ProteinInteractionGraph(
				Math.max(1000D - c.analysisConfig.maxPathCost, c.analysisConfig.minInteractomeConfidence),
				c.generalConfig.proteinInteractomeFile,
				proteinMap);
		// TODO double check Dijkstra' returning null
		final Function<Tuple<Protein, Protein>, Path<Protein>> pathfinder = t -> {
			final Protein start, end;
			if(t.val1().getId().compareTo(t.val2().getId()) > 0){
				start = t.val2();
				end = t.val1();
			}else{
				start = t.val1();
				end = t.val2();
			}
			assert(start.getId().compareTo(end.getId()) <= 0);
			Path<Protein> path = precomputedPaths.getOrDefault(start, Collections.emptyMap()).get(end);
			if(path == null){
				path = pig.dijkstras(start, end, e -> 1000D - e.getWeight(),
						c.analysisConfig.maxPathCost,
						c.analysisConfig.maxPathLength);
				addPathToMapHelper(start, end, path, precomputedPaths);
			}
			return path;
		};
		System.out.println("Computing pairwise paths...");
		group1.computePairwisePathsAndGraph(pathfinder);
		group2.computePairwisePathsAndGraph(pathfinder);
		combined.computePairwisePathsAndGraph(pathfinder);
		for(int i = 0; i < bootstrapGeneSetMaps.length; i++) {
			System.out.println("Bootstrap round " + i);
			bootstrapGeneSetMaps[i].computePairwisePathsAndGraph(pathfinder);
		}


		//MOD12-2=EPHB2,P4HA2,ARHGEF10L,MYLK,ANGPTL4,SPTA1,ALK,LPA,HCLS1,PLA2G4C,MAP4K1,PRKCA,TBXAS1,ADH6,IQGAP2
		//System.out.println(group1.getGeneSetMap().get("MOD12-2").getGraph().get);
		//System.exit(0);;
		//GeneSet gs = (group1.getGeneSetMap().get("MOD1-4"));
		//System.out.println("Number of genes in MOD1-4 graph.: " + gs.getGenes().size());
		//System.exit(0);;

		//System.out.println(hits + " path cache hits.");
		//System.out.println(misses + " path cache misses.");
		//System.out.println((double) hits / (hits + misses) + " proportion of paths cached.");
		savePaths(c, dataFile, precomputedPaths);
	}

	public static <K> LayeredGraph<K> reduceLayeredGraph(LayeredGraph<K> g, Configuration c){
		//System.out.println("Original graph size: " + g.getNodes().size());
		final ArrayList<Tuple<K, Integer>> degree = new ArrayList<>();
		for(K k : g.getNodes()) degree.add(new Tuple<>(k, g.getNeighbors(k).size()));
		degree.sort(Comparator.comparingInt(t -> -t.val2()));
		final int toRetainCount = (int) Math.ceil(Math.min(c.analysisConfig.fractionOfNodesToRender * degree.size(), c.analysisConfig.maxNodesToRender));
		final Set<K> toRetain = new HashSet<>();
		for(int i = 0; i < toRetainCount; i++) toRetain.add(degree.get(i).val1());
		//System.out.println("Reduced graph size: " + toRetain.size());
		return g.subgraphWithNodes(new LayeredGraph<>(g.getType()), toRetain);
	}

	public static Color parseColorOrDefault(String s, Color defaultColor){
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

	private static Function<Protein, Color> createNodeColorFunction(Configuration c, GeneSetMap group1, GeneSetMap group2, LayeredGraph<Protein> graph){
		final Color defaultColor = parseColorOrDefault(c.rendererConfig.defaultNodeColor, null);
		if(defaultColor == null)
			throw new RuntimeException("At least the default color must be specified");
		final Color group1Color = parseColorOrDefault(c.rendererConfig.group1NodeColor, defaultColor);

		final Color group2Color = parseColorOrDefault(c.rendererConfig.group2NodeColor, defaultColor);
		final Color mixedColor = parseColorOrDefault(c.rendererConfig.bothGroupsNodeColor, mixColors(group1Color, group2Color));

		final Function<Protein, Color> f = p -> {
			final boolean isGroup1 = group1.getUniqueProteins().contains(p);
			final boolean isGroup2 = group2.getUniqueProteins().contains(p);
			if(isGroup1 && isGroup2) return mixedColor;
			if(getMultiColoredState(p, group1, group2, graph) != 0) return defaultColor;
			if(isGroup1) return group1Color;
			else if(isGroup2) return group2Color;
			return defaultColor;
		};
		final int LOWER_ALPHA_BOUND = c.rendererConfig.minNodeAlpha;
		final int UPPER_ALPHA_BOUND = 255;
		final double MAXIMUM_NUM = graph.getMaxCount();

		return p -> {
			final Color color = f.apply(p);
			final int alpha = (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double) graph.getCount(p) / MAXIMUM_NUM);
			return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
		};
	}

	private static int getMultiColoredState(Protein p, GeneSetMap group1, GeneSetMap group2, LayeredGraph<Protein> graph){
		final boolean isGroup1 = group1.getUniqueProteins().contains(p);
		final boolean isGroup2 = group2.getUniqueProteins().contains(p);
		if(isGroup1 == isGroup2) return 0;
		switch(graph.getType()){
		//case GROUP1:
		//case GROUP2:
		//	return 0;
		case GROUP1_MINUS_GROUP2: case GROUP1:
			return isGroup2 ? 2 : 0;
		case GROUP2_MINUS_GROUP1: case GROUP2:
			return isGroup1 ? 1 : 0;
		default:
			return 0;
		}
	}

	private static Function<Edge<Protein>, Color> createEdgeColorFunction(Configuration c, GeneSetMap group1, GeneSetMap group2, LayeredGraph<Protein> graph){
		final int LOWER_ALPHA_BOUND = c.rendererConfig.minEdgeAlpha;
		final int UPPER_ALPHA_BOUND = 255;
		final double MAXIMUM_NUM = graph.getMaxCount();
		return e -> new Color(0, 0, 0, (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * 
				Math.min(graph.getCount(e.getSource()), graph.getCount(e.getTarget())) / MAXIMUM_NUM));
	}

	private static Function<Protein, Color> createNodeBorderColorFunction(Configuration c, GeneSetMap group1, GeneSetMap group2, LayeredGraph<Protein> graph){
		final Color group1Color = parseColorOrDefault(c.rendererConfig.group1NodeColor, Color.BLACK);
		final Color group2Color = parseColorOrDefault(c.rendererConfig.group2NodeColor, Color.BLACK);
		final Color[] colors = new Color[]{Color.BLACK, group1Color, group2Color};
		return p -> {
			return colors[getMultiColoredState(p, group1, group2, graph)];
		};
	}

	private static Function<Protein, Float> createNodeBorderThicknessFunction(Configuration c, GeneSetMap group1, GeneSetMap group2, LayeredGraph<Protein> graph){
		return p -> {
			return getMultiColoredState(p, group1, group2, graph) == 0 ? 1f : 3f;
		};
	}

	private static Renderer<Protein> createRenderer(Configuration c, GeneSetMap group1, GeneSetMap group2, LayeredGraph<Protein> graph, String clusterId){
		final Renderer<Protein> renderer = c.rendererConfig.displayRendering
				? new GUIRenderer<>(c.rendererConfig, new File(c.generalConfig.outputDirectory, clusterId))
						: new Renderer<>(c.rendererConfig, new File(c.generalConfig.outputDirectory));
				renderer.setLabelFunction(p -> p.getGene() == null ? p.getId() : p.getGene().getSymbol());
				renderer.setNodeColorFunction(createNodeColorFunction(c, group1, group2, graph));
				renderer.setEdgeColorFunction(createEdgeColorFunction(c, group1, group2, graph));
				renderer.setNodeBorderColorFunction(createNodeBorderColorFunction(c, group1, group2, graph));
				renderer.setNodeBorderThicknessFunction(createNodeBorderThicknessFunction(c, group1, group2, graph));
				return renderer;
	}

	private static void layoutAndRender(Configuration c, GeneSetMap group1, GeneSetMap group2, GeneSetMap combined, String clusterId) throws IOException {
		// Render group1 (group2, group1 - group2, group2 - group1)
		final List<Tuple<LayeredGraph<Protein>, String>> toRender = new LinkedList<>();

		final LayeredGraph<Protein> group1Graph = group1.getLayeredGraph();		
		final LayeredGraph<Protein> group1GraphReduced = reduceLayeredGraph(group1Graph, c);
		if(group1GraphReduced.getNodes().size() > 0)
			toRender.add(new Tuple<>(group1GraphReduced, "Group1"));

		//TODO refactor code
		if(group2.getGeneSetMap().size() > 0){

			final LayeredGraph<Protein> group2Graph = group2.getLayeredGraph();
			final LayeredGraph<Protein> group2GraphReduced = reduceLayeredGraph(group2Graph, c);
			if(group2GraphReduced.getNodes().size() != 0){
				toRender.add(new Tuple<>(group2GraphReduced, "Group2"));

				final LayeredGraph<Protein> combinedGraph = combined.getLayeredGraph();
				final LayeredGraph<Protein> combinedGraphReduced = reduceLayeredGraph(combinedGraph, c);
				toRender.add(new Tuple<>(combinedGraphReduced, "Group1AndGroup2"));

				if(group1.getLayeredGraph().getNodes().size() > 0 &&
						group2.getLayeredGraph().getNodes().size() > 0 &&
						c.analysisConfig.calculateGraphDifferences){
					final double numGroup1 = group1.getGeneSetMap().size();
					final double numGroup2 = group2.getGeneSetMap().size();

					final double group1ScalingFactor = numGroup1 < numGroup2 ? numGroup2 / numGroup1 : 1;
					final double group2ScalingFactor = numGroup2 < numGroup1 ? numGroup1 / numGroup2 : 1;

					final LayeredGraph<Protein> group1MinusGroup2 = reduceLayeredGraph(group1Graph.subtract(group2Graph, group1ScalingFactor, group2ScalingFactor), c);
					if(group1MinusGroup2.getNodes().size() > 0)
						toRender.add(new Tuple<>(group1MinusGroup2, "Group1MinusGroup2"));
					final LayeredGraph<Protein> group2MinusGroup1 = reduceLayeredGraph(group2Graph.subtract(group1Graph, group2ScalingFactor, group1ScalingFactor), c);
					if(group2MinusGroup1.getNodes().size() > 0)
						toRender.add(new Tuple<>(group2MinusGroup1, "Group2MinusGroup1"));
				}
			}
		}

		for(Tuple<LayeredGraph<Protein>, String> pair : toRender){
			final LayeredGraph<Protein> graph = pair.val1();

			final ForceDirectedLayout<Protein> fdl = new ForceDirectedLayout<>(
					c.forceDirectedLayoutConfig,
					graph,
					createRenderer(c, group1, group2, pair.val1(), clusterId),
					c.generalConfig.projectName + clusterId + "_" + pair.val2());
			final Thread t = new Thread(fdl::start);
			t.setDaemon(false);
			t.start();

			saveGraphInformation(
					c,
					group1,
					group2,
					pair.val1(),
					c.generalConfig.projectName + clusterId + "_" + pair.val2(),
					clusterId);
			// 143Patients, C43, _, Group1, _, Interactions
			// 143Patients, C43, _, Group1, _, GeneSet
		}

	}

	private static void saveGraphInformation(Configuration c, GeneSetMap group1, GeneSetMap group2, LayeredGraph<Protein> graph, String name, String clusterId) throws IOException {
		final File clusterOutputDirectory = new File(c.generalConfig.outputDirectory, clusterId);
		if(!clusterOutputDirectory.exists()) clusterOutputDirectory.mkdirs();
		final File interactionsOutputFile = new File(clusterOutputDirectory, name + "_Interactions.txt");
		final File geneListOutputFile = new File(clusterOutputDirectory, name + "_GeneSet.txt");

		BufferedWriter out = new BufferedWriter(new FileWriter(interactionsOutputFile));
		out.write("#source\ttarget\tweight (STRING score)");
		out.newLine();
		for(Protein p : graph.getNodes()){
			for(Edge<Protein> e : graph.getNeighbors(p)){
				if(e.getSource().getId().compareTo(e.getTarget().getId()) > 0) continue;
				out.write(e.getSource().getGene() == null ? e.getSource().getId() : e.getSource().getGene().getSymbol());
				out.write('\t');
				out.write(e.getTarget().getGene() == null ? e.getTarget().getId() : e.getTarget().getGene().getSymbol());
				out.write('\t');
				out.write(String.valueOf(e.getWeight()));
				out.newLine();
			}
		}
		out.flush();
		out.close();

		out = new BufferedWriter(new FileWriter(geneListOutputFile));
		out.write("#gene\torigin\tcount");
		out.newLine();
		for(Protein p : graph.getNodes()){
			out.write(p.getGene() == null ? p.getId() : p.getGene().getSymbol());
			out.write('\t');
			final boolean isGroup1 = group1.getUniqueProteins().contains(p);
			final boolean isGroup2 = group2.getUniqueProteins().contains(p);
			final String s;
			if(isGroup1 && isGroup2)
				s = "both";
			else if(isGroup1 && !isGroup2)
				s = "group1";
			else if(!isGroup1 && isGroup2)
				s = "group2";
			else
				s = "imputed";
			out.write(s);
			out.write('\t');
			out.write(String.valueOf(graph.getCount(p)));
			out.newLine();
		}
		out.flush();
		out.close();

	}

	private static <K> double calculateIntersectionOverUnionSimilarity(Graph<K> x, Graph<K> y){
		final Set<K> union = new HashSet<>(x.getNodes());
		union.addAll(y.getNodes());
		if(union.size() == 0) return 0;
		final Set<K> intersection = new HashSet<>(x.getNodes());
		intersection.retainAll(y.getNodes());
		return (double) intersection.size() / union.size();
	}

	public static double calculateGraphDistance(Graph<Protein> x, Graph<Protein> y){
		return (1 - calculateIntersectionOverUnionSimilarity(x, y));
	}

	public static void renderAndDisplayDendrogram(Configuration c, GeneSetMap group1, GeneSetMap group2, Map<String, ClusterAnalysis> clusters) throws IOException {
		final DendrogramRenderer dr = new DendrogramRenderer(c.rendererConfig, new File(c.generalConfig.outputDirectory));

		final Map<PhylogeneticTreeNode, ClusterAnalysis> remapping = new HashMap<>();
		clusters.forEach((k, v) -> remapping.put(v.getNode(), v));

		final Color defaultColor = parseColorOrDefault(c.rendererConfig.defaultNodeColor, null);
		if(defaultColor == null)
			throw new RuntimeException("At least the default color must be specified");
		final Color group1color = parseColorOrDefault(c.rendererConfig.group1NodeColor, defaultColor);
		final Color group2color = parseColorOrDefault(c.rendererConfig.group2NodeColor, defaultColor);

		final Function<PhylogeneticTreeNode, Color> clusterEdgeColorFunction = ptn -> {
			double group1Weight = 0;
			final PhylogeneticTreeNode[] nodeLeafs = ptn.getLeaves();
			if(nodeLeafs.length > 0){
				for(PhylogeneticTreeNode ptnLeaf : nodeLeafs)
					if(group1.getGeneSetMap().containsKey(ptnLeaf.getLabel()))
						group1Weight += ptnLeaf.getWeight();
			}else{
				group1Weight = group1.getGeneSetMap().containsKey(ptn.getLabel()) ? ptn.getWeight() : 0;
			}
			final double percentageGroup1 = group1Weight / ptn.getWeight();
			if(Math.abs(percentageGroup1 - 0.5) <= 0.1) return Color.BLACK;
			else if(percentageGroup1 > 0.5) return group1color;
			else if(percentageGroup1 < 0.5) return group2color;
			else return Color.BLACK; //never reach here
		};
		dr.setClusterEdgeColorFunction(clusterEdgeColorFunction);

		if(c.rendererConfig.colorSignificantBranchLabels){
			final Function<PhylogeneticTreeNode, Color> clusterLabelColorFunction = ptn -> {
				return remapping.get(ptn).getpValue() <= c.rendererConfig.significanceThreshold ? Color.RED : Color.BLACK;
			};
			dr.setClusterLabelColorFunction(clusterLabelColorFunction);
		}

		File imageFile = dr.render(clusters, c.generalConfig.projectName + "_Dendrogram");
		if(c.rendererConfig.displayRendering)
			Desktop.getDesktop().open(imageFile);
	}

	private static void performClusterAnalysis(Configuration c, GeneSetMap group1, GeneSetMap group2, GeneSetMap combined, GeneSetMap[] bootstrapGeneSetMaps) {
		final Map<String, GeneSet> allPatients = new HashMap<>();
		final Map<String, PhylogeneticTreeNode> leaves = new HashMap<>();

		final double group1GeneSetSize = group1.getGeneSetMap().size();
		final double group2GeneSetSize = group2.getGeneSetMap().size();

		double group1InitialWeight = group1GeneSetSize < group2GeneSetSize ? group2GeneSetSize / group1GeneSetSize : 1;
		double group2InitialWeight = group2GeneSetSize < group1GeneSetSize ? group1GeneSetSize / group2GeneSetSize : 1;

		for(String patient : group1.getGeneSetMap().keySet()){
			allPatients.put(patient, group1.getGeneSetMap().get(patient));
			leaves.put(patient, new PhylogeneticTreeNode(patient, group1InitialWeight));
		}
		for(String patient : group2.getGeneSetMap().keySet()){
			allPatients.put(patient, group2.getGeneSetMap().get(patient));
			leaves.put(patient, new PhylogeneticTreeNode(patient, group2InitialWeight));
		}
		final String[] allPatientKeys = allPatients.keySet().toArray(new String[allPatients.size()]);

		if(allPatients.size() == 0){
			System.out.println("No patient graphs available...exiting.");
			return;
		}

		if(allPatients.size() == 1){
			//only 1 patient has graphs
			try {
				System.out.println("Only 1 patient graph...will render its graph and exit.");
				layoutAndRender(c, group1, group2, combined, allPatientKeys[0]);
				if(c.rendererConfig.displayRendering){
					System.out.println("Press enter to exit...");
					@SuppressWarnings("resource")
					final Scanner s = new Scanner(System.in);
					s.nextLine();
				}


			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		final DistanceMatrix<PhylogeneticTreeNode> dissimilarityMatrix = new DistanceMatrix<>();

		for(int i = 0; i < allPatientKeys.length - 1; i++){
			for(int j = i + 1; j < allPatientKeys.length; j++){
				final GeneSet x =  allPatients.get(allPatientKeys[i]);
				final GeneSet y =  allPatients.get(allPatientKeys[j]);
				
				dissimilarityMatrix.setDistance(
						leaves.get(allPatientKeys[i]),
						leaves.get(allPatientKeys[j]),
						calculateGraphDistance(x.getGraph(), y.getGraph()));
			}
		}
		
		final PhylogeneticTreeNode treeRoot = PhylogeneticTree.createTreeFromMatrix(dissimilarityMatrix);
		
		int r = 0;
		for(GeneSetMap gsm : bootstrapGeneSetMaps) {
			final HashMap<String, PhylogeneticTreeNode> cloned = new HashMap<>();
			leaves.forEach((k, v) -> cloned.put(k, v.clone()));
			
			System.out.println("Bootstrap analysis round " + r++);
			final DistanceMatrix<PhylogeneticTreeNode> bootstrapDissimilarityMatrix = new DistanceMatrix<>();
			final Map<String, GeneSet> bootstrapPatients = gsm.getGeneSetMap();
			for(int i = 0; i < allPatientKeys.length - 1; i++) {
				for(int j = i + 1; j < allPatientKeys.length; j++) {
					
					final GeneSet x =  bootstrapPatients.get(allPatientKeys[i]);
					final GeneSet y =  bootstrapPatients.get(allPatientKeys[j]);

					// implementation choice: if allPatients.get(~) == null, we simply continue instead
					// of setting the distance to be 1 (the maximum)
					if(x == null || y == null) continue;
					bootstrapDissimilarityMatrix.setDistance(
							cloned.get(allPatientKeys[i]),
							cloned.get(allPatientKeys[j]),
							calculateGraphDistance(x.getGraph(), y.getGraph()));
				}
			}
			final PhylogeneticTreeNode bootstrapTree = PhylogeneticTree.createTreeFromMatrix(bootstrapDissimilarityMatrix);
			treeRoot.updateWithBootstrapRound(bootstrapTree);
		}
		
		assert(treeRoot != null);

		try {
			Map<String, ClusterAnalysis> clusterAnalysisMapping = PhylogeneticTree.recursivelyAnalyzeClusters(c, treeRoot, dissimilarityMatrix, group1, group2, combined);
			renderAndDisplayDendrogram(c, group1, group2, clusterAnalysisMapping);
			saveClusterAnalysesSummary(clusterAnalysisMapping, new File(c.generalConfig.outputDirectory, c.generalConfig.projectName + "_ClusterAnalyses.csv"));
			handleInput(c, group1, group2, combined, clusterAnalysisMapping);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String getClusterAnalysesSummaryText(Map<String, ClusterAnalysis> clusterAnalysisMapping){
		final StringBuilder sb = new StringBuilder(ClusterAnalysis.getCompactStringHeaders()).append('\n');
		clusterAnalysisMapping.values().stream()
		.filter(ca -> !ca.isLeaf())
		.sorted(Comparator.comparing(ca -> Integer.parseInt(ca.getClusterId().substring(1))))
		.forEach(ca -> sb.append(ca.getCompactString()).append('\n'));
		return sb.toString();
	}

	public static void saveClusterAnalysesSummary(Map<String, ClusterAnalysis> clusterAnalysisMapping, File file) throws IOException {
		try(final BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
			for(String line : getClusterAnalysesSummaryText(clusterAnalysisMapping).split("\n")){
				final String[] parts = line.replace(" | ", "\t").split("\t");
				for(String part : parts)
					bw.write('"' + part + "\",");
				bw.newLine();
			}
		}

	}

	private static void handleInput(Configuration c, GeneSetMap group1, GeneSetMap group2,
			GeneSetMap combined, Map<String, ClusterAnalysis> clusterAnalysisMapping){
		final String patientName = group1.getGeneSetMap().keySet().iterator().next();
		System.out.println("\n\n\n---INPUT REQUIRED---\nCommand examples:\n"
				+  patientName + ": Layout and render the pairwise paths graph of patient " + patientName + "\n"
				+ "C5: Layout and render the summary graph of all nodes belonging to C5\n"
				+ "info: Displays information for every cluster\n"
				+ "info C17: Displays information for cluster C17\n"
				+ "q or quit: Terminates the application\n");

		@SuppressWarnings("resource")
		final Scanner in = new Scanner(System.in);
		while(true){
			final String s = in.nextLine().trim();
			if(s.equalsIgnoreCase("q") || s.equalsIgnoreCase("quit"))
				break;

			if(s.equalsIgnoreCase("info")){
				System.out.println(getClusterAnalysesSummaryText(clusterAnalysisMapping));

			}else if(s.toLowerCase().startsWith("info")){
				final String id = s.substring(4).trim();

				final ClusterAnalysis analysis = clusterAnalysisMapping.get(id);
				if(analysis == null){
					System.out.println(id + " is an invalid cluster identifier...\n");
					continue;
				}
				System.out.println(analysis.getPrintableString());
				System.out.println();
			}else{
				handleRenderInput(c, group1, group2, combined, clusterAnalysisMapping, s);
			}

		}
	}

	private static void handleRenderInput(Configuration c, GeneSetMap group1, GeneSetMap group2, GeneSetMap combined, Map<String, ClusterAnalysis> clusterMapping, String id){
		final Set<String> patientsToInclude = new HashSet<>();
		final ClusterAnalysis ca = clusterMapping.get(id);
		if(ca != null){
			final PhylogeneticTreeNode ptn = ca.getNode();
			System.out.println("Laying out and rendering graphs from patients in " + id + "...");
			final PhylogeneticTreeNode[] leaves = ptn.getLeaves();
			if(leaves.length > 0)
				for(PhylogeneticTreeNode leaf : leaves)
					patientsToInclude.add(leaf.getLabel());
			else
				patientsToInclude.add(ptn.getLabel());
		}else{
			System.out.println(id + " is an invalid cluster or patient identifier...\n");
			return;
		}

		final Set<String> group1PatientsToInclude = new HashSet<>(patientsToInclude);
		group1PatientsToInclude.retainAll(group1.getGeneSetMap().keySet());
		final Set<String> group2PatientsToInclude = new HashSet<>(patientsToInclude);
		group2PatientsToInclude.retainAll(group2.getGeneSetMap().keySet());
		System.out.println("Patients in Group 1: " + group1PatientsToInclude);
		System.out.println("Patients in Group 2: " + group2PatientsToInclude);

		final GeneSetMap subsetGroup1 = group1.subset(group1PatientsToInclude);
		final GeneSetMap subsetGroup2 = group2.subset(group2PatientsToInclude);
		final GeneSetMap subsetCombined = combined.subset(patientsToInclude);

		try {
			layoutAndRender(c, subsetGroup1, subsetGroup2, subsetCombined, id);
			System.out.println("\n");
		} catch (IOException e) {
			System.err.println("Error in attempt to layout and render " + id);
			e.printStackTrace();
		}
	}

	/*
	 * group count
	 * 
	 * min width consistent DONE
	 * test group1 or group2 null DONE
	 * easier handling of input files (allow no file or empty file --> empty genesetmap) DONE
	 * FisherExact test ArrayIndexOutOfBounds exception; only FE test when have both cases or controls
	 * if a patient has no graph (or gene not found), print a warning and exclude that patient from all analyses
	 * 			explain errors, such as increase path length for a patient with 2 genes but no path between
	 * 
	 * TODO: beautify output
	 * 
	 * controls11.txt   empty file
	 */

}
