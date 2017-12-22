package org.armanious.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Configuration {
	
	public static final class GeneralConfig {

		public final String activeDirectory;
		public final String imageDirectory;
		
		public final String primaryGeneSetGroupFile;
		public final String secondaryGeneSetGroupFile;
		public final String projectName;
		
		public final boolean verboseOutput;
		
		// public final boolean multiThreaded = false;
		
		public GeneralConfig(Map<String, String> map){
			primaryGeneSetGroupFile = map.get("primaryGeneSetGroupFile");
			//if(primaryGeneSetGroupFile == null)
				//throw new RuntimeException("Must at least supply cases gene set file in configuration.");
			
			String activeDirectory = map.getOrDefault("activeDirectory", "");
			if(!activeDirectory.isEmpty() && !activeDirectory.endsWith(File.separator))
				activeDirectory += File.separator;
			this.activeDirectory = activeDirectory;
			
			String imageDirectory = map.getOrDefault("imageDirectory", activeDirectory + "images");
			if(!imageDirectory.isEmpty() && !imageDirectory.endsWith(File.separator))
				imageDirectory += File.separator;
			this.imageDirectory = imageDirectory;
			
			
			secondaryGeneSetGroupFile = map.get("secondaryGeneSetGroupFile");
			String projectName = map.get("projectName");
			if(projectName == null){
				if(!activeDirectory.isEmpty()){
					projectName = activeDirectory;
				}else if(primaryGeneSetGroupFile != null){
					projectName = primaryGeneSetGroupFile.contains(".") ? primaryGeneSetGroupFile.substring(0, primaryGeneSetGroupFile.indexOf('.')) : primaryGeneSetGroupFile;
				}
			}
			this.projectName = projectName;
			
			verboseOutput = Boolean.parseBoolean(map.getOrDefault("verboseOutput", "false"));

			//multiThreaded = Boolean.parseBoolean(map.getOrDefault("multiThreaded", "false"));
		}

	}
	
	public static final class AnalysisConfig {
		
		public final boolean reusePreviousData;
		public final boolean calculateGraphDifferences;
		
		public final double maxPathUnconfidence;
		public final int maxPathLength;

		public final boolean layoutAndRender;
		public final double percentageOfNodesToRender;
		public final int maxNodesInGraphToRender;
		
		public AnalysisConfig(Map<String, String> map){
			reusePreviousData = Boolean.parseBoolean(map.getOrDefault("reusePreviousData", "true"));
			calculateGraphDifferences = Boolean.parseBoolean(map.getOrDefault("calculateGraphDifferences", "true"));
			
			maxPathUnconfidence = Double.parseDouble(map.getOrDefault("maxPathUnconfidence", "400"));
			maxPathLength = Integer.parseInt(map.getOrDefault("maxPathLength", "5"));
			
			layoutAndRender = Boolean.parseBoolean(map.getOrDefault("layoutAndRender", "true"));
			percentageOfNodesToRender = Double.parseDouble(map.getOrDefault("percentageOfNodesToRender", "1"));
			maxNodesInGraphToRender = Integer.parseInt(map.getOrDefault("maxNodesInGraphToRender", String.valueOf(Integer.MAX_VALUE)));
		}
		
	}
	
	public static final class ForceDirectedLayoutConfig {
		
		public final double repulsionConstant;
		public final double attractionConstant;
		public final double deltaThreshold;
		
		public final long maxIterations;
		public final long maxTime;
		
		public final double minNodeSize;
		public final double maxNodeSize;
		
		public ForceDirectedLayoutConfig(Map<String, String> map){
			repulsionConstant = Double.parseDouble(map.getOrDefault("repulsionConstant", "0.5"));
			attractionConstant = Double.parseDouble(map.getOrDefault("attractionConstant", "0.001"));
			deltaThreshold = Double.parseDouble(map.getOrDefault("deltaThreshold", "0.001"));
			maxIterations = Long.parseLong(map.getOrDefault("maxIterations", "10000"));
			maxTime = Long.parseLong(map.getOrDefault("maxTime", String.valueOf(Long.MAX_VALUE)));

			minNodeSize = Double.parseDouble(map.getOrDefault("minNodeSize", "15"));
			maxNodeSize = Double.parseDouble(map.getOrDefault("maxNodeSize", "-1"));
		}
		
	}
	
	public static final class RendererConfig {
		
		public final boolean transparentBackground;
		public final String imageExtension;
		public final boolean drawGeneSymbols;
		public final String fontName;
		public final int fontSize;
		public final boolean dynamicallySizedFont = false;
		
		public final int minNodeAlpha;
		public final int minEdgeAlpha;

		public final String defaultNodeColor;
		public final String primaryGroupNodeColor;
		public final String secondaryGroupNodeColor;
		public final String bothGroupsNodeColor;
		public final boolean varyNodeAlphaValues;
		public final boolean varyEdgeAlphaValues;
		
		public RendererConfig(Map<String, String> map){
			transparentBackground = Boolean.parseBoolean(map.getOrDefault("transparentBackground", "true"));
			imageExtension = map.getOrDefault("imageExtension", "png");
			drawGeneSymbols = Boolean.parseBoolean(map.getOrDefault("drawGeneSymbols", "true"));
			fontName = map.getOrDefault("fontName", "Dialog");
			fontSize = Integer.parseInt(map.getOrDefault("fontSize", "12"));
			//dynamicallySizedFont = Boolean.parseBoolean(map.getOrDefault("dynamicallySizedFont", "false"));
			

			minNodeAlpha = Integer.parseInt(map.getOrDefault("minNodeAlpha", "50"));
			minEdgeAlpha = Integer.parseInt(map.getOrDefault("minEdgeAlpha", "50"));
			
			defaultNodeColor = map.getOrDefault("primaryGroupNodeColor", "(255,0,0)");
			primaryGroupNodeColor = map.getOrDefault("primaryGroupNodeColor", "(255,255,0)");
			secondaryGroupNodeColor = map.getOrDefault("secondaryGroupNodeColor", "(0,255,0)");
			bothGroupsNodeColor = map.getOrDefault("", null);
			varyNodeAlphaValues = Boolean.parseBoolean(map.getOrDefault("varyNodeAlphaValues", "true"));
			varyEdgeAlphaValues = Boolean.parseBoolean(map.getOrDefault("varyEdgeAlphaValues", "true"));
			
		}
		
	}
	
	public static final class ProteinInteractomeConfig {
		
		public final String STRINGversion = "10.5";
		public final String interactomeFile = "9606.protein.links.v" + STRINGversion + ".txt";
		public final double minConfidence;
		
		public ProteinInteractomeConfig(Map<String, String> map){
			minConfidence = Double.parseDouble(map.getOrDefault("minConfidence", "400"));
		}
		
	}
	
	public final GeneralConfig generalConfig;
	public final AnalysisConfig analysisConfig;
	public final ForceDirectedLayoutConfig forceDirectedLayoutConfig;
	public final RendererConfig rendererConfig;
	public final ProteinInteractomeConfig proteinInteractomeConfig;
	
	private Configuration(Map<String, String> map){
		generalConfig = new GeneralConfig(map);
		analysisConfig = new AnalysisConfig(map);
		forceDirectedLayoutConfig = new ForceDirectedLayoutConfig(map);
		rendererConfig = new RendererConfig(map);
		proteinInteractomeConfig = new ProteinInteractomeConfig(map);
		
		final Set<String> keySetCopy = new HashSet<>(map.keySet());
		for(Class<?> clazz : this.getClass().getDeclaredClasses()){
			for(Field field : clazz.getDeclaredFields()){
				keySetCopy.remove(field.getName());
			}
		}
		for(String key : keySetCopy)
			System.err.println("Warning: unknown parameter " + key + " provided.");
	}
	
	public static Configuration defaultConfiguration(String primaryGeneSetGroupFile){
		return fromArgs("primaryGeneSetGroupFile=" + primaryGeneSetGroupFile);
	}
	
	public static Configuration fromFile(File file) throws IOException {
		try(final BufferedReader br = new BufferedReader(new FileReader(file))){
			final Map<String, String> map = new HashMap<>();
			String s;
			while((s = br.readLine()) != null){
				if(s.startsWith("#") || s.startsWith("//")) continue;
				final int idx = s.indexOf('=');
				if(idx == -1)
					throw new RuntimeException("Configuration file " + file + " is invalid: \"" + s + "\"");
				
				final String prevVal = map.put(s.substring(0, idx), s.substring(idx + 1));
				if(prevVal != null)
					throw new RuntimeException("Configuration file " + file + " is invalid: key \"" + s.substring(0, idx) + "\" appears more than once");
			}
			return Configuration.fromMap(map);
		}
	}
	
	public static Configuration fromArgs(String...args){
		final Map<String, String> map = new HashMap<>();
		for(String arg : args){
			final int idx = arg.indexOf('=');
			if(idx == -1)
				throw new RuntimeException("Argument \"" + arg + "\" is invalid");
			map.put(arg.substring(0, idx), arg.substring(idx + 1));
		}
		return Configuration.fromMap(map);
	}
	
	public static Configuration fromMap(Map<String, String> map){
		return new Configuration(map);
	}

}
