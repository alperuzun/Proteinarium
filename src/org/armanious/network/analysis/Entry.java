package org.armanious.network.analysis;

public class Entry {

//	private final ProteinInteractionGraph pig;
//
//	private final Map<String, Collection<Gene>> casesGeneSets;
//	private final Map<String, Collection<Gene>> controlsGeneSets;
//
//	private final Set<Protein> casesProteinSet = new HashSet<>();
//	private final Set<Protein> controlsProteinSet = new HashSet<>();
//
//	private final Map<String, ArrayList<Path<Protein>>> casePaths = new HashMap<>();
//	private final Map<String, ArrayList<Path<Protein>>> controlPaths = new HashMap<>();
//
//	private final Map<String, Graph<Protein>> caseGraphs = new HashMap<>();
//	private final Map<String, Graph<Protein>> controlGraphs = new HashMap<>();
//
//	private final LayeredGraph<Protein> caseSummaryGraph = new LayeredGraph<>();
//	private final LayeredGraph<Protein> controlSummaryGraph = new LayeredGraph<>();
//	
//	public Entry(Configuration config) throws IOException {
//		pig = new ProteinInteractionGraph(config.proteinInteractomeConfig);
//
//		//casesGeneSets = new Gene[1][];
//		//casesGeneSets[0] = Gene.getGenes(CASES_GENE_SET_IDS[0]);
//		//casesProteinSet.addAll(getProteinsFromGeneSet(casesGeneSets[0]));
//		//controlsGeneSets = null;
//		this.casesGeneSets = cases;
//		this.controlsGeneSets = controls;
//
//		for(Collection<Gene> casePatient : this.casesGeneSets.values())
//			casesProteinSet.addAll(getProteinsFromGeneSet(casePatient));
//		for(Collection<Gene> controlPatient : this.controlsGeneSets.values())
//			controlsProteinSet.addAll(getProteinsFromGeneSet(controlPatient));
//	}
//
//	private static final Scanner in = new Scanner(System.in);
//
//	private static String prompt(String s){
//		System.out.print(s);
//		System.out.flush();
//		return in.nextLine().trim();
//	}
//
//	private void computePaths(){
//		System.out.println("Computing paths for " + casesGeneSets.size() + " cases...");
//		for(String casePatient : casesGeneSets.keySet()){
//			final Collection<Gene> geneSet = casesGeneSets.get(casePatient);
//			final Set<Protein> proteinsFromGeneSet = getProteinsFromGeneSet(geneSet);
//			System.out.println("Finding all-pairs paths between " + proteinsFromGeneSet.size() + " proteins from " + geneSet);
//			final ArrayList<Path<Protein>> allPairwisePaths = pairwisePaths(proteinsFromGeneSet.toArray(new Protein[proteinsFromGeneSet.size()]));
//			casePaths.put(casePatient, allPairwisePaths);
//		}
//		System.out.println("Computing paths for " + controlsGeneSets.size() + " controls...");
//		for(String controlPatient : controlsGeneSets.keySet()){
//			final Collection<Gene> geneSet = controlsGeneSets.get(controlPatient);
//			final Set<Protein> proteinsFromGeneSet = getProteinsFromGeneSet(geneSet);
//			System.out.println("Finding all-pairs paths between " + proteinsFromGeneSet.size() + " proteins from " + geneSet);
//			final ArrayList<Path<Protein>> allPairwisePaths = pairwisePaths(proteinsFromGeneSet.toArray(new Protein[proteinsFromGeneSet.size()]));
//			controlPaths.put(controlPatient, allPairwisePaths);
//		}
//	}
//
//	private void readPaths(String file){
//		try(BufferedReader br = new BufferedReader(new FileReader(file))){
//			//TODO check STRING version
//			assert(br.readLine().equals("CASE PATIENT PATHS"));
//			readPaths(br, casePaths);
//			assert(br.readLine().equals("CONTROL PATIENT PATHS"));
//			readPaths(br, controlPaths);
//		}catch(IOException e){
//			e.printStackTrace();
//		}
//	}
//
//	private void readPaths(BufferedReader br, Map<String, ArrayList<Path<Protein>>> map) throws IOException {
//		int count = Integer.parseInt(br.readLine());
//		while(count-- > 0){
//			final String s = br.readLine();
//			String[] parts = s.split("=");
//			final String identifier = parts[0];
//			if(parts[1].length() == 0) continue;
//			final String[] paths = parts[1].split(";");
//
//			final ArrayList<Path<Protein>> patientPaths = new ArrayList<>(paths.length);
//			map.put(identifier, patientPaths);
//
//			for(String path : paths){
//				final String[] edgeComponents = path.split(",");
//				final Path<Protein> pathToAdd = new Path<>();
//				Protein prev = Protein.getProtein(edgeComponents[0]);
//				for(int i = 1; i < edgeComponents.length - 1; i += 2){
//					final int weight = Integer.parseInt(edgeComponents[i]);
//					final Protein next = Protein.getProtein(edgeComponents[i+1]);
//					pathToAdd.addEdge(new Edge<>(prev, next, weight));
//					prev = next;
//				}
//				patientPaths.add(pathToAdd);
//			}
//		}
//	}
//
//	private void savePaths(BufferedWriter bw, Map<String, ArrayList<Path<Protein>>> pathsMap) throws IOException {
//		bw.write(String.valueOf(pathsMap.size())); bw.newLine();
//		for(String identifier : pathsMap.keySet()){
//			bw.write(identifier);
//			bw.write("=");
//			final ArrayList<Path<Protein>> paths = pathsMap.get(identifier);
//			for(Path<Protein> path : paths){
//				final List<Edge<Protein>> edges = path.getEdges();
//				if(edges.size() > 0){
//					bw.write(edges.get(0).getSource().getId());
//					bw.write(',');
//				}
//				for(Edge<Protein> edge : edges){
//					bw.write(String.valueOf(edge.getWeight()));
//					bw.write(',');
//					bw.write(edge.getTarget().getId());
//					bw.write(',');
//				}
//				bw.write(';');
//			}
//			bw.newLine();
//		}
//	}
//
//	private void writePaths(String file){
//		try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))){
//			//TODO write version of STRING
//			//write patient paths
//			bw.write("CASE PATIENT PATHS"); bw.newLine();
//			savePaths(bw, casePaths);
//			bw.write("CONTROL PATIENT PATHS"); bw.newLine();
//			savePaths(bw, controlPaths);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private void computeGraphs(){
//		// paths already computed
//		for(String patient : casePaths.keySet()){
//			final Set<Edge<Protein>> edges = new HashSet<>();
//			for(Path<Protein> path : casePaths.get(patient))
//				edges.addAll(path.getEdges());
//			final Graph<Protein> graph = pig.subgraphWithEdges(edges);
//			caseGraphs.put(patient, graph);
//			caseSummaryGraph.addGraph(graph);
//		}
//		for(String patient : controlPaths.keySet()){
//			final Set<Edge<Protein>> edges = new HashSet<>();
//			for(Path<Protein> path : controlPaths.get(patient))
//				edges.addAll(path.getEdges());
//			final Graph<Protein> graph = pig.subgraphWithEdges(edges);
//			controlGraphs.put(patient, graph);
//			controlSummaryGraph.addGraph(graph);
//		}
//	}
//
//	public void doYourThang(String file){
//		if(new File(file + ".txt").exists() && prompt(file + " data exists; load existing data? [Y/N]: ").equalsIgnoreCase("y")){
//			readPaths(file + ".txt");
//		}else{
//			computePaths();
//			writePaths(file + ".txt");
//		}
//		computeGraphs();
//
//		final Map<LayeredGraph<Protein>, String> toRender = new HashMap<>();
//
//		toRender.put(caseSummaryGraph, file + "Cases.png");
//		System.out.println("Cases: " + caseSummaryGraph.getNodes().size() + " nodes.");
//		
//		if(controlsGeneSets != null){
//			System.out.println("Controls: " + controlSummaryGraph.getNodes().size() + " nodes.");
//
//			if(controlSummaryGraph.getNodes().size() > 0){
//				toRender.put(controlSummaryGraph, file + "Controls.png");
//
//				final LayeredGraph<Protein> casesMinusControls = caseSummaryGraph.subtract(controlSummaryGraph);
//				if(casesMinusControls.getNodes().size() > 0){
//					toRender.put(casesMinusControls, file + "CasesMinusControls.png");
//					final HashSet<Protein> seeds = new HashSet<>();
//					for(Protein p : casesMinusControls.getNodes())
//						if(casesMinusControls.getCount(p) == casesMinusControls.getMaxCount())
//							seeds.add(p);
//					for(Protein seed : seeds){
//						final Set<Protein> seedAndNeighbors = new HashSet<>();
//						seedAndNeighbors.add(seed);
//						for(Edge<Protein> edge : casesMinusControls.getNeighbors(seed))
//							seedAndNeighbors.add(edge.getTarget());
//						System.out.println("Seed " + seed.getGene().getSymbol() + " graph has a total of " + seedAndNeighbors.size() + " nodes.");
//						final LayeredGraph<Protein> furtherInvestigation = casesMinusControls.subgraphWithNodes(new LayeredGraph<>(), seedAndNeighbors);
//						assert(furtherInvestigation.getNodes().size() == seedAndNeighbors.size());
//						assert(furtherInvestigation.getNodes().size() > 1);
//						toRender.put(furtherInvestigation, file + seed.getGene().getSymbol() + ".png");
//					}
//				}
//
//				final LayeredGraph<Protein> controlsMinusCases = controlSummaryGraph.subtract(caseSummaryGraph);
//				if(controlsMinusCases.getNodes().size() > 0)
//					toRender.put(controlsMinusCases, file + "ControlsMinusCases.png");
//			}
//		}
//		
//		for(LayeredGraph<Protein> lg : toRender.keySet()){
//			try {
//				layoutAndRender(lg, toRender.get(lg));
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		
//		final Map<String, Graph<Protein>> allPatients = new HashMap<>();
//		for(String patient : caseGraphs.keySet())
//			allPatients.put("<CASE>" + patient, caseGraphs.get(patient));
//		for(String patient : controlGraphs.keySet())
//			allPatients.put("<CONTROL>" + patient, controlGraphs.get(patient));
//		final String[] allPatientKeys = allPatients.keySet().toArray(new String[allPatients.size()]);
//		for(int i = 0; i < allPatientKeys.length - 1; i++){
//			for(int j = i + 1; j < allPatientKeys.length; j++){
//				final Graph<Protein> x = allPatients.get(allPatientKeys[i]);
//				final Graph<Protein> y = allPatients.get(allPatientKeys[j]);
//				System.out.println(i + ", " + j + ": " + 
//						calculateGraphDistance(x, y));
//			}
//		}
//
//	}
//	
//	private static <K> double calculateIntersectionOverUnionSimilarity(Graph<K> x, Graph<K> y){
//		final Set<K> union = new HashSet<>(x.getNodes());
//		union.addAll(y.getNodes());
//		final Set<K> intersection = new HashSet<>(x.getNodes());
//		intersection.retainAll(y.getNodes());
//		return (double) intersection.size() / union.size();
//	}
//	
//	private static double calculateGraphDistance(Graph<Protein> x, Graph<Protein> y){
//		return 1 - calculateIntersectionOverUnionSimilarity(x, y);
//	}
//
//	
//	public void layoutAndRender(LayeredGraph<Protein> graph, String filename) throws IOException {
//		final LayeredGraph<Protein> reducedGraph = graph.subgraphWithNodes(new LayeredGraph<>(),
//				Arrays.asList(
//						graph.getNodes().stream()
//						.sorted((p1, p2) -> Double.compare(graph.getCount(p2), graph.getCount(p1)))
//						.limit((long)Math.ceil(percentageKept * graph.getNodes().size()))
//						.toArray(i -> new Protein[i])
//						));
//		System.out.println("Keeping " + reducedGraph.getNodes().size() + " nodes from the initial " + graph.getNodes().size());
//
//		final Function<Graph<Protein>, Function<Protein, Double>> sizeFunctionGenerator = g -> {
//			int maxEdgeCount = 0;
//			for(Protein p : reducedGraph.getNodes())
//				maxEdgeCount = Math.max(maxEdgeCount, reducedGraph.getNeighbors(p).size());
//
//			final int MIN_SIZE = 15;
//			final int MAX_SIZE = Math.min(Math.max(g.getNodes().size(), 30), 100);
//			final double sizePerEdge = (double) (MAX_SIZE - MIN_SIZE) / maxEdgeCount;
//			/*final double a = 0;
//			return p -> {
//				final double ax = a * g.getNeighbors(p).size();
//				return 50 * ax / Math.sqrt(1 + ax * ax);
//			};*/
//			return p -> MIN_SIZE + sizePerEdge * g.getNeighbors(p).size();
//		};
//
//		final Function<Protein, Double> nodeSizeFunction = sizeFunctionGenerator.apply(reducedGraph);
//
//
//		final ForceDirectedLayout<Protein> fdl = new ForceDirectedLayout<>(
//				reducedGraph,
//				layoutRepulsionConstant,
//				layoutAttractionConstant,
//				layoutDeltaThreshold,
//				nodeSizeFunction
//				);
//		fdl.layout();
//
//
//		final Renderer<Protein> renderer = new Renderer<>(fdl);
//		renderer.setLabelFunction(p -> p.getGene() == null ? p.getId() : p.getGene().getSymbol());
//		final Color yellowGreen = new Color(180, 255, 0);
//
//		final int LOWER_ALPHA_BOUND = 50;
//		final int UPPER_ALPHA_BOUND = 255;
//		final double MAXIMUM_NUM = reducedGraph.getMaxCount();
//		final Function<Protein, Color> nodeColorFunction = p -> {
//			final boolean isCase = casesProteinSet.contains(p);
//			final boolean isControl = controlsProteinSet.contains(p);
//			final Color c;
//			if(isCase && isControl) c = yellowGreen;
//			else if(isCase) c = Color.YELLOW;
//			else if(isControl) c = Color.GREEN;
//			else c = Color.RED;
//			final int alpha = (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double) reducedGraph.getCount(p) / MAXIMUM_NUM);
//			if(alpha > 255 || alpha < 0)
//				System.out.println(alpha);
//			return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
//		};
//		renderer.setNodeColorFunction(nodeColorFunction);
//		renderer.setEdgeColorFunction(e -> {
//			final int alpha = (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double)reducedGraph.getCount(e.getSource()) / MAXIMUM_NUM);
//			return new Color(0, 0, 0, alpha);
//		});
//		renderer.saveTo(new File(filename));
//	}
//
//	/*private Map<Protein, Integer> getPathNodeCounts(ArrayList<Tuple<ArrayList<Protein>, Integer>> paths){
//		final Map<Protein, Integer> map = new HashMap<>();
//		for(Tuple<ArrayList<Protein>, Integer> path : paths)
//			for(Protein protein : path.val1())
//				map.put(protein, map.getOrDefault(protein, 0) + 1);
//		return map;
//	}*/
//
//	private static Set<Protein> getProteinsFromGeneSet(Collection<Gene> geneSet) {
//		final Set<Protein> proteins = new HashSet<>();
//		for(Gene gene : geneSet)
//			for(Protein protein : gene.getProteins())
//				proteins.add(protein);
//		return proteins;
//	}
//
//	private final HashMap<Protein, HashMap<Protein, Path<Protein>>> cache = new HashMap<>();
//	private final ArrayList<Path<Protein>> pairwisePaths(Protein[] endpoints){
//		final ArrayList<Path<Protein>> paths = new ArrayList<>();
//		for(int i = 0; i < endpoints.length - 1; i++){
//			System.out.println("Dijkstra's starting at..." + endpoints[i].getGene().getSymbol());
//			for(int j = i + 1; j < endpoints.length; j++){
//				if(!cache.containsKey(endpoints[i]))
//					cache.put(endpoints[i], new HashMap<>());
//				if(!cache.containsKey(endpoints[j]))
//					cache.put(endpoints[j], new HashMap<>());
//				if(!cache.get(endpoints[i]).containsKey(endpoints[j])){
//					final Path<Protein> path =  pig.dijkstras(endpoints[i], endpoints[j], e -> (1000 - e.getWeight()), pathUncofidenceThreshold, pathLengthThreshold);
//					cache.get(endpoints[i]).put(endpoints[j], path);
//					cache.get(endpoints[j]).put(endpoints[i], path);
//				}
//				paths.add(cache.get(endpoints[i]).get(endpoints[j]));
//			}
//		}
//		return paths;
//	}
//
//
//
//
//	//from TP53 network on ptbdb
//	//target CASE gene set: MCL1, TERT, VDR, BCL2
//	private static final String[][] CASES_GENE_SET_IDS = {
//			{"MCL1", "TERT", "BCL2", "TAF1"},
//			{"TERT", "VDR", "BCL2"},
//			{"VDR", "MCL1", "TERT", "BCL2"},
//	};
//	//target CONTROL gene set: VHL, HSPA4, TERT
//	private static final String[][] CONTROLS_GENE_SET_IDS = {
//			{"VHL", "HSPA4", "TERT"},
//			{"VHL", "TERT", "TAF1"},
//			{"TERT", "BCL2", "VHL"}
//	};
//	public static void main(String...args) throws Throwable {
//		PTBLiveTest.main(args);
//		System.exit(0);
//
//
//		Gene.initializeGeneDatabase(new File("/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.aliases.v10.5.hgnc_with_symbol.txt")); //TODO FIXME
//
//		Map<String, Collection<Gene>> casesGeneSets = new HashMap<>();
//		Map<String, Collection<Gene>> controlsGeneSets = new HashMap<>();
//		for(int i = 0; i < 3; i++){
//			casesGeneSets.put(String.valueOf(i), Arrays.asList(Gene.getGenes(CASES_GENE_SET_IDS[i])));
//			controlsGeneSets.put(String.valueOf(i), Arrays.asList(Gene.getGenes(CONTROLS_GENE_SET_IDS[i])));
//		}
//
//		final Entry e = new Entry(parseArguments(args), casesGeneSets, controlsGeneSets);
//		e.doYourThang("test");
//
//		/*final LayeredGraph<String> lg = new LayeredGraph<>();
//		MutableGraph<String> g = new MutableGraph<>();
//		g.addNode("A");
//		g.addNode("B");
//		g.addEdge("A", "B");
//		lg.addGraph(g);
//		g.addNode("C");
//		g.addNode("D");
//		g.addEdge("C", "A");
//		g.addEdge("A", "D");
//		g.addEdge("B", "D");
//		lg.addGraph(g);
//		g = new MutableGraph<>();
//		g.addNode("C");
//		g.addNode("D");
//		g.addEdge("C", "D");
//		//lg.addGraph(g);
//
//		final ForceDirectedLayout<String> fdl = new ForceDirectedLayout<>(
//				lg,
//				0.003,
//				0.5,
//				1E-3,
//				p -> 10D + 5 * lg.getNeighbors(p).size()
//				);
//		fdl.layout();
//
//
//		final Renderer<String> renderer = new Renderer<>(fdl);
//		final int LOWER_ALPHA_BOUND = 10;
//		final int UPPER_ALPHA_BOUND = 255;
//		final int MAXIMUM_NUM = lg.getNumberOfGraphs();
//		renderer.setNodeColorFunction(s -> {
//			final int alpha = (int)(LOWER_ALPHA_BOUND + (UPPER_ALPHA_BOUND - LOWER_ALPHA_BOUND) * (double) lg.getCount(s) / MAXIMUM_NUM);
//			System.out.println(lg.getCount(s));
//			System.out.println(alpha);
//			return new Color(255, 0, 0, alpha);
//		});
//
//		renderer.saveTo(new File("test.png"));
//		Desktop.getDesktop().open(new File("test.png"));*/
//	}
//
//	private static Map<String, String> parseArguments(String[] args){
//		final HashMap<String, String> argMap = new HashMap<>();
//		for(String arg : args){
//			arg = arg.toLowerCase();
//			if(arg.equals("help")) printUsage(0);
//			final int idx = arg.indexOf('=');
//			if(idx == -1) printUsage(1);
//			argMap.put(arg.substring(0, idx), arg.substring(idx + 1));
//		}
//		return argMap;
//	}
//
//	private static void printUsage(int status){
//		System.out.println("USAGE: " + "TODO");
//		System.exit(status);
//	}

}
