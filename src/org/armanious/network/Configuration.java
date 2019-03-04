package org.armanious.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Configuration {
	
	public static boolean GETTING_DEFAULT_OPTIONS = false;

	public static final class GeneralConfig {

		public final String activeDirectory;
		public final String outputDirectory;

		public final String group1GeneSetFile;
		public final String group2GeneSetFile;
		public final String projectName;

		public final String proteinInteractomeFile;
		public final String proteinAliasesFile;
		
		public final String stringDatabaseVersion;

		// public final boolean multiThreaded = false;

		private static void downloadURLToFile(String urlPath, String file) throws IOException {
			System.out.println("Downloading " + urlPath + " to file " + file);
			final URL url = new URL(urlPath);
			final URLConnection c = url.openConnection();
			c.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
			
			final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			final BufferedInputStream bis = new BufferedInputStream(c.getInputStream());
			final byte[] buffer = new byte[4096];
			int read;
			while((read = bis.read(buffer, 0, buffer.length)) != -1){
				bos.write(buffer, 0, read);
			}
			bis.close();
			bos.close();
		}

		public GeneralConfig(Map<String, String> map){
			String activeDirectory = map.getOrDefault("activeDirectory", "");
			if(!activeDirectory.isEmpty() && !activeDirectory.endsWith(File.separator))
				activeDirectory += File.separator;
			this.activeDirectory = activeDirectory;
			
			String outputDirectory = map.getOrDefault("outputDirectory", activeDirectory + "output");
			if(!outputDirectory.isEmpty() && !outputDirectory.endsWith(File.separator))
				outputDirectory += File.separator;
			if(!new File(outputDirectory).isAbsolute())
				outputDirectory = activeDirectory + outputDirectory;
			this.outputDirectory = outputDirectory;
			
			
			String group1GeneSetGroupFile = map.get("group1GeneSetFile");
			if(group1GeneSetGroupFile == null || group1GeneSetGroupFile.isEmpty()) {
				throw new RuntimeException("must specify group1GeneSetGroupFile");
			}
			if(!new File(group1GeneSetGroupFile).isAbsolute())
				group1GeneSetGroupFile = activeDirectory + group1GeneSetGroupFile;
			this.group1GeneSetFile = group1GeneSetGroupFile;
			
			String group2GeneSetGroupFile = map.get("group2GeneSetFile");
			if(group2GeneSetGroupFile != null && group2GeneSetGroupFile.isEmpty())
				group2GeneSetGroupFile = null;
			if(group2GeneSetGroupFile != null)
				if(!new File(group2GeneSetGroupFile).isAbsolute())
					group2GeneSetGroupFile = activeDirectory + group2GeneSetGroupFile;
			this.group2GeneSetFile = group2GeneSetGroupFile;
			
			
			String projectName = map.get("projectName");
			if(projectName == null){
				if(!map.getOrDefault("activeDirectory", "").isEmpty()){
					projectName = new File(activeDirectory).getName();
				}else if(group1GeneSetGroupFile != null){
					projectName = group1GeneSetGroupFile.contains(".") ? group1GeneSetGroupFile.substring(0, group1GeneSetGroupFile.indexOf('.')) : group1GeneSetGroupFile;
				}
			}
			this.projectName = projectName;
			
			
			this.stringDatabaseVersion = map.getOrDefault("stringDatabaseVersion", "11.0");
			
			proteinInteractomeFile = map.getOrDefault("proteinInteractomeFile",
					new File(System.getProperty("user.dir"), "9606.protein.links.v" + stringDatabaseVersion + ".txt.gz").getPath());
			proteinAliasesFile = map.getOrDefault("proteinAliasesFile",
					new File(System.getProperty("user.dir"), "9606.protein.aliases.v" + stringDatabaseVersion + ".txt.gz").getPath());
			
			if(GETTING_DEFAULT_OPTIONS) return;
			try {
				if(!new File(proteinInteractomeFile).exists())
					downloadURLToFile("https://stringdb-static.org/download/protein.links.v" + stringDatabaseVersion + "/9606.protein.links.v" + stringDatabaseVersion + ".txt.gz", proteinInteractomeFile);
				if(!new File(proteinAliasesFile).exists())
					downloadURLToFile("https://stringdb-static.org/download/protein.aliases.v" + stringDatabaseVersion + "/9606.protein.aliases.v" + stringDatabaseVersion + ".txt.gz", proteinAliasesFile);
			} catch (IOException e) {
				System.err.println("Error downloading STRING database files. Please download 9606.protein.links.v" 
							+ stringDatabaseVersion + ".txt.gz and 9606.protein.aliases.v" + stringDatabaseVersion 
							+ ".txt.gz from their website and specify the file paths in the configuration file under"
							+ " proteinInteractomeFile and proteinAliasesFile, respectively.");
				throw new RuntimeException(e);
			}
		}

	}

	public static final class AnalysisConfig {

		public final boolean reusePreviousData;
		public final boolean calculateGraphDifferences;

		public final double minInteractomeConfidence;

		public final double maxPathCost;
		public final int maxPathLength;

		//public final boolean layoutAndRender;
		public final double fractionOfVerticesToRender;
		public final int maxVerticesToRender;
		
		public final int bootstrappingRounds;

		public AnalysisConfig(Map<String, String> map){
			reusePreviousData = Boolean.parseBoolean(map.getOrDefault("reusePreviousData", "true"));
			calculateGraphDifferences = Boolean.parseBoolean(map.getOrDefault("calculateGraphDifferences", "true"));

			minInteractomeConfidence = Double.parseDouble(map.getOrDefault("minInteractomeConfidence", "0"));

			maxPathCost = Double.parseDouble(map.getOrDefault("maxPathCost", "200"));
			maxPathLength = Integer.parseInt(map.getOrDefault("maxPathLength", "5"));

			//layoutAndRender = Boolean.parseBoolean(map.getOrDefault("layoutAndRender", "true"));
			fractionOfVerticesToRender = Double.parseDouble(map.getOrDefault("fractionOfVerticesToRender", "1"));
			maxVerticesToRender = Integer.parseInt(map.getOrDefault("maxVerticesToRender", String.valueOf(Integer.MAX_VALUE)));
			
			bootstrappingRounds = Integer.parseInt(map.getOrDefault("bootstrappingRounds", "1000"));
		}

	}

	public static final class ForceDirectedLayoutConfig {

		public final double repulsionConstant;
		public final double attractionConstant;
		public final double deltaThreshold;

		public final long maxIterations;
		public final long maxTime;

		public final double minVertexRadius;
		public final double maxVertexRadius;

		public ForceDirectedLayoutConfig(Map<String, String> map){
			repulsionConstant = Double.parseDouble(map.getOrDefault("repulsionConstant", "0.2"));
			attractionConstant = Double.parseDouble(map.getOrDefault("attractionConstant", "0.0003"));
			deltaThreshold = Double.parseDouble(map.getOrDefault("deltaThreshold", "0.001"));
			maxIterations = Long.parseLong(map.getOrDefault("maxIterations", "10000"));
			maxTime = Long.parseLong(map.getOrDefault("maxTime", String.valueOf(Long.MAX_VALUE)));

			minVertexRadius = Double.parseDouble(map.getOrDefault("minVertexRadius", "15"));
			maxVertexRadius = Double.parseDouble(map.getOrDefault("maxVertexRadius", "-1"));
		}

	}

	public static final class RendererConfig {
		
		public final boolean displayRendering;
		
		public final int minVertexAlpha;
		public final int minEdgeAlpha;
		
		public final boolean drawGeneSymbols;
		
		public final String defaultVertexColor;
		public final String group1VertexColor;
		public final String group2VertexColor;
		public final String bothGroupsVertexColor;

		public final boolean colorSignificantBranchLabels;
		public final double significanceThreshold;

		public final double metaClusterThreshold;

		public RendererConfig(Map<String, String> map){
			displayRendering = Boolean.parseBoolean(map.getOrDefault("performRendering", "true"));

			minVertexAlpha = Integer.parseInt(map.getOrDefault("minVertexAlpha", "50"));
			minEdgeAlpha = Integer.parseInt(map.getOrDefault("minEdgeAlpha", "50"));
			
			drawGeneSymbols = Boolean.parseBoolean(map.getOrDefault("drawGeneSymbols", "true"));
			
			defaultVertexColor = map.getOrDefault("defaultVertexColor", "(255,0,0)"); //red
			group1VertexColor = map.getOrDefault("group1VertexColor", "(255,200,0)"); //orange
			group2VertexColor = map.getOrDefault("group2VertexColor", "(0,0,255)"); //blue
			bothGroupsVertexColor = map.getOrDefault("bothGroupsVertexColor", "(0,255,0)"); //green

			colorSignificantBranchLabels = Boolean.parseBoolean(map.getOrDefault("colorSignificantBranchLabels", "true"));
			significanceThreshold = Double.parseDouble(map.getOrDefault("significanceThreshold", "0.05"));
			
			metaClusterThreshold = Double.parseDouble(map.getOrDefault("metaClusterThreshold", "0.3333"));
		}

	}

	public final GeneralConfig generalConfig;
	public final AnalysisConfig analysisConfig;
	public final ForceDirectedLayoutConfig forceDirectedLayoutConfig;
	public final RendererConfig rendererConfig;

	private Configuration(Map<String, String> map){
		generalConfig = new GeneralConfig(map);
		analysisConfig = new AnalysisConfig(map);
		forceDirectedLayoutConfig = new ForceDirectedLayoutConfig(map);
		rendererConfig = new RendererConfig(map);
		
		final Set<String> keySetCopy = new HashSet<>(map.keySet());
		for(Class<?> clazz : this.getClass().getDeclaredClasses()){
			for(Field field : clazz.getDeclaredFields()){
				keySetCopy.remove(field.getName());
			}
		}
		for(String key : keySetCopy)
			System.err.println("Warning: unknown parameter " + key + " provided.");
	}

	public static Configuration defaultConfiguration(String group1GeneSetFile){
		return fromArgs("group1GeneSetFile=" + group1GeneSetFile);
	}

	public static Configuration fromFile(File file) throws IOException {
		try(final BufferedReader br = new BufferedReader(new FileReader(file))){
			final Map<String, String> map = new HashMap<>();
			String s;
			while((s = br.readLine()) != null){
				if(s.startsWith("#") || s.startsWith("//") || s.trim().isEmpty()) continue;
				final int idx = s.indexOf('=');
				if(idx == -1)
					throw new RuntimeException("Configuration file " + file + " is invalid: \"" + s + "\"\n"
							+ "\n\tMust have the form <configOption>=<value>\n"
							+ "\tExample: maxPathLength=4");

				final String prevVal = map.put(s.substring(0, idx).trim(), s.substring(idx + 1).trim());
				if(prevVal != null)
					throw new RuntimeException("Configuration file " + file + " is invalid: option \"" + s.substring(0, idx) + "\" appears more than once");
			}
			return Configuration.fromMap(map);
		}
	}

	public static Configuration fromArgs(String...args){
		final Map<String, String> map = new HashMap<>();
		for(String arg : args){
			final int idx = arg.indexOf('=');
			if(idx == -1)
				throw new RuntimeException("Configuration option \"" + arg + "\" is invalid\n"
						+ "\tMust have the form <configOption>=<value>\n"
						+ "\tExample: maxPathLength=4");
			map.put(arg.substring(0, idx).trim(), arg.substring(idx + 1).trim());
		}
		return Configuration.fromMap(map);
	}

	public static Configuration fromMap(Map<String, String> map){
		return new Configuration(map);
	}

}
