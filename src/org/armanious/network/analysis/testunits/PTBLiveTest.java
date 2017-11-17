package org.armanious.network.analysis.testunits;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.armanious.graph.Edge;
import org.armanious.graph.Graph;
import org.armanious.graph.LayeredGraph;
import org.armanious.io.IOUtils;
import org.armanious.network.analysis.Entry;
import org.armanious.network.analysis.Gene;
import org.armanious.network.analysis.Protein;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PTBLiveTest {

	public PTBLiveTest(String vcfFile) throws IOException {
		this(new FileReader(vcfFile));
	}

	private static final double GMAF_THRESHOLD = 0.01;
	private static final String[] CONTROLS = {"1-2", "1-4", "2-2", "2-3", "2-4", "3-1", "3-3", "4-4", "7-1", "8-2", "9-2", "9-3", "10-2", "11-2", "11-3", "12-4"};
	private static final String[] CASES = {"1-1", "1-3", "2-1", "3-2", "3-4", "4-1", "4-2", "4-3", "5-1", "5-2", "5-3", "5-4", "6-1", "6-2", "6-3", "6-4",
			"7-2", "7-3", "7-4", "8-1", "8-3", "8-4", "9-1", "9-4", "10-1", "10-3", "10-4", "11-1", "11-4", "12-1", "12-2", "12-3"};
	static {
		for(String[] arr : new String[][]{CONTROLS, CASES})
			for(int i = 0; i < arr.length; i++)
				arr[i] = "MOD" + arr[i];
	}

	private final Map<String, Integer> columnIndexMap;
	private final List<String[]> table;

	private final Map<String, Collection<Gene>> caseGenes;
	private final Map<String, Collection<Gene>> controlGenes;

	public PTBLiveTest(Reader reader) throws IOException {
		System.out.println("Parsing VCF file and loading all related SNP data");
		final BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
		String s;
		while((s = br.readLine()) != null && s.startsWith("##"));
		columnIndexMap = new HashMap<>();
		table = new ArrayList<String[]>();
		for(String column : s.split("\t"))
			columnIndexMap.put(column, columnIndexMap.size());
		final int filterIndex = columnIndexMap.get("FILTER");
		final int snpIdIndex = columnIndexMap.get("ID");
		while((s = br.readLine()) != null){
			final String[] row = s.split("\t");
			if(row[filterIndex].equals("PASS") && !row[snpIdIndex].equals(".")){
				table.add(s.split("\t"));
			}
		}

		caseGenes = parseGenes(CASES);
		controlGenes = parseGenes(CONTROLS);
		
		final Set<Gene> uniqueGenes = new HashSet<>();
		int sum = 0;
		for(Collection<Gene> set : caseGenes.values()){
			sum += set.size();
			uniqueGenes.addAll(set);
		}
		for(Collection<Gene> set : controlGenes.values()){
			sum += set.size();
			uniqueGenes.addAll(set);
		}
		System.out.println(uniqueGenes.size() + " unique genes with an average of " + (double) sum / (caseGenes.size() + controlGenes.size()) + " genes per patient.");

		br.close();
		System.out.println("Finished parsing VCF file");
	}

	private static Node getChildNode(Node node, String...subElements){
		outer:
			for(String next : subElements){
				final NodeList nl = node.getChildNodes();
				for(int i = 0; i < nl.getLength(); i++){
					if(nl.item(i).getNodeName().equals(next)){
						node = nl.item(i);
						continue outer;
					}
				}
				return null;
			}
	return node;
	}
	private static double getGMAFFromSnpId(String rsId){
		final Node n = getChildNode(getSnpData(rsId).getDocumentElement(), "Rs", "Frequency");
		return n == null ? Double.MAX_VALUE : Double.parseDouble(n.getAttributes().getNamedItem("freq").getTextContent());
	}
	private static int getGeneIdFromSnpId(String rsId){
		final Node n = getChildNode(getSnpData(rsId).getDocumentElement(), "Rs", "Assembly", "Component", "MapLoc", "FxnSet");
		return n == null ? -1 : Integer.parseInt(n.getAttributes().getNamedItem("geneId").getTextContent());
	}
	private static String getGeneSymbolFromSnpId(String rsId){
		final Node n = getChildNode(getSnpData(rsId).getDocumentElement(), "Rs", "Assembly", "Component", "MapLoc", "FxnSet");
		return n == null ? null : n.getAttributes().getNamedItem("symbol").getTextContent();
	}
	private static boolean isNonSynonymousSnp(String rsId){
		final Node n = getChildNode(getSnpData(rsId).getDocumentElement(), "Rs");
		final NodeList nl = n.getChildNodes();
		for(int i = 0; i < nl.getLength(); i++){
			final Node candidate = nl.item(i);
			if(candidate.getNodeName().equals("Ss")){
				final Node locSnpId = candidate.getAttributes().getNamedItem("locSnpId");
				if(locSnpId != null && locSnpId.getTextContent().startsWith("nonsyn"))
					return true;
			}
		}
		return false;
	}

	private Map<String, Collection<Gene>> parseGenes(String[] cohort){
		final Map<String, Collection<Gene>> snpsMapping = new HashMap<>();
		final int[] cohortIndices = new int[cohort.length];
		for(int i = 0; i < cohortIndices.length; i++){
			cohortIndices[i] = columnIndexMap.get(cohort[i]);
			snpsMapping.put(cohort[i], new HashSet<>());
		}
		final int snpIdIndex = columnIndexMap.get("ID");
		for(String[] row : table){
			final String rsSNP = row[snpIdIndex];
			if(!rsSNP.startsWith("rs") || getGMAFFromSnpId(rsSNP) > GMAF_THRESHOLD
					|| !isNonSynonymousSnp(rsSNP)){
				continue;
			}
			for(int i = 0; i < cohort.length; i++){
				if(row[cohortIndices[i]].substring(0, 3).contains("1")){
					try{
						final Gene gene = Gene.getGene(getGeneSymbolFromSnpId(rsSNP));
						if(gene != null) snpsMapping.get(cohort[i]).add(gene);
					}catch(AssertionError ignored){}
				}
			}
		}
		return snpsMapping;
	}

	private static final File SNP_DATA_DIR = new File("/Users/david/OneDrive/Documents/Brown/Comp Bio Research/SNP Data 11-06-2017/");
	private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private static final HashMap<String, Document> snpDataCache = new HashMap<>();
	static {
		if(!SNP_DATA_DIR.exists()) SNP_DATA_DIR.mkdirs();
	}
	private static Document getSnpData(String rsId){
		if(!snpDataCache.containsKey(rsId)){
			final File target = new File(SNP_DATA_DIR, rsId + ".xml");
			if(!target.exists()){
				final String url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=SNP&id=" + rsId + "&rettype=xml&retmode=xml&version=2.0";
				try {
					IOUtils.writeToFile(IOUtils.readURL(url), target);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
					return null;
				}
			}
			try {
				snpDataCache.put(rsId, dbf.newDocumentBuilder().parse(target));
			} catch (SAXException | IOException | ParserConfigurationException e) {
				throw new RuntimeException(e);
			}
		}
		return snpDataCache.get(rsId);
	}

	public void run(){
		try {
			Entry entry = new Entry(new HashMap<>(), caseGenes, controlGenes);
			entry.doYourThang("realPtbData");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String...args) throws Throwable {
		Gene.initializeGeneDatabase(new File("/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.aliases.v10.5.hgnc_with_symbol.txt"));
		if(args == null || args.length == 0){
			args = new String[]{
					"/Users/david/OneDrive/Documents/Brown/Comp Bio Research/Preterm Birth SNPs Analysis/Whole Analysis v2/Original Input/",
			"/Users/david/OneDrive/Documents/Brown/Comp Bio Research/Preterm Birth SNPs Analysis/Whole Analysis v2/"};
			args[0] += "variants.filtered.snp48pat.vcf";
		}
		new PTBLiveTest(args[0]).run();	
	}


	public static void main_(String...args) throws Throwable {
		Gene.initializeGeneDatabase(new File("/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.aliases.v10.5.hgnc_with_symbol.txt"));

		final Function<String, LayeredGraph<Protein>> graphReader = filename -> {
			final LayeredGraph<Protein> lg = new LayeredGraph<>();
			final HashMap<Protein, Double> counts = new HashMap<>();
			try(final BufferedReader br = new BufferedReader(new FileReader(filename))){
				String s;
				while((s = br.readLine()) != null){
					final String[] parts = s.split("\t");
					switch(parts.length){
					case 2:
						counts.put(Protein.getProtein(parts[0]), Double.parseDouble(parts[1]));
						break;
					case 3:
						lg.addEdge(new Edge<>(Protein.getProtein(parts[0]), Protein.getProtein(parts[1]), Integer.parseInt(parts[2])));
						break;
					default:
						throw new RuntimeException();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			for(Protein p : counts.keySet()) lg.setCount(p, counts.get(p));
			return lg;
		};
		String s = "realPtbDataCases";
		final Entry e = new Entry(new HashMap<>(), new HashMap<>(), new HashMap<>());
		final LayeredGraph<Protein> graph = graphReader.apply(s + ".txt");
		//final Collection<Protein> reducedProteinSet = new HashSet<>(Arrays.asList((graph.getNodes().stream().sorted((c1, c2) -> graph.getCount(c2) - graph.getCount(c1)).limit(50).toArray(i -> new Protein[i]))));
		
		final LayeredGraph<Protein> reducedGraph = graph;//graph.subgraphWithNodes(new LayeredGraph<>(), reducedProteinSet);
		
		
		final Function<Graph<Protein>, Function<Protein, Double>> sigmoidFunctionGenerator = g -> {
			final double a = 20.0 / g.getNodes().size();
			return p -> {
				final int x = g.getNeighbors(p).size();
				return 80 * a * x / Math.sqrt(1 + a * a * x * x);
			};
		};
		
		final Function<Protein, Double> nodeSize = sigmoidFunctionGenerator.apply(reducedGraph);
		
		for(Protein protein : reducedGraph.getNodes()){
			System.out.println((protein.getGene() == null ? protein.getId() : protein.getGene().getSymbol()) + 
					"\t" + reducedGraph.getNeighbors(protein).size() +
					"\t" + nodeSize.apply(protein));
		}
		e.layoutAndRender(reducedGraph, s + ".png");
		
	}

}
