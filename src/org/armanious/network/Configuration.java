package org.armanious.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Configuration {
	
	public static final class GeneralConfig {
		
		public final String caseGeneSetsFile;
		public final String controlGeneSetsFile;
		public final String projectName;
		
		public final boolean verboseOutput;
		
		public final boolean multiThreaded = false;
		
		public GeneralConfig(Map<String, String> map){
			caseGeneSetsFile = map.get("casesGeneSetsFile");
			if(caseGeneSetsFile == null)
				throw new RuntimeException("Must at least supply cases gene set file in configuration.");
			controlGeneSetsFile = map.get("controlsGeneSetsFile");
			projectName = map.getOrDefault("projectName", caseGeneSetsFile.contains(".") ? caseGeneSetsFile.substring(0, caseGeneSetsFile.indexOf('.')) : caseGeneSetsFile);
			verboseOutput = Boolean.parseBoolean(map.getOrDefault("verboseOutput", "false"));
			
			//multiThreaded = Boolean.parseBoolean(map.getOrDefault("multiThreaded", "false"));
		}

	}
	
	public static final class AnalysisConfig {
		
		public final boolean reusePreviousData;
		public final boolean calculateGraphDifferences;
		
		public final double maximumPathUnconfidence;
		public final int maximumPathLength;

		public final boolean layoutAndRender;
		public final double percentageOfNodesToRender;
		public final int maximumGraphSizeToRender;
		
		public AnalysisConfig(Map<String, String> map){
			reusePreviousData = Boolean.parseBoolean(map.getOrDefault("reusePreviousData", "true"));
			calculateGraphDifferences = Boolean.parseBoolean(map.getOrDefault("calculateGraphDifferences", "true"));
			
			maximumPathUnconfidence = Double.parseDouble(map.getOrDefault("maximumPathUnconfidence", "400"));
			maximumPathLength = Integer.parseInt(map.getOrDefault("maximumPathLength", "5"));
			
			layoutAndRender = Boolean.parseBoolean(map.getOrDefault("layoutAndRender", "true"));
			percentageOfNodesToRender = Double.parseDouble(map.getOrDefault("percentageOfNodesToRender", "1"));
			maximumGraphSizeToRender = Integer.parseInt(map.getOrDefault("maximumGraphSizeToRender", "50"));
		}
		
	}
	
	public static final class ForceDirectedLayoutConfig {
		
		public final double repulsionConstant;
		public final double attractionConstant;
		public final double deltaThreshold;
		public final long maxIterations;
		public final long maxTime;
		
		public ForceDirectedLayoutConfig(Map<String, String> map){
			repulsionConstant = Double.parseDouble(map.getOrDefault("repulsionConstant", "0.15"));
			attractionConstant = Double.parseDouble(map.getOrDefault("attractionConstant", "0.01"));
			deltaThreshold = Double.parseDouble(map.getOrDefault("deltaThreshold", "0.001"));
			maxIterations = Long.parseLong(map.getOrDefault("maxIterations", "5000"));
			maxTime = Long.parseLong(map.getOrDefault("maxTime", "-1"));
		}
		
	}
	
	public static final class RendererConfig {
		
		public final boolean transparentBackground;
		public final String imageExtension;
		public final boolean drawGeneSymbols;
		public final String fontName;
		public final int fontSize;
		
		public RendererConfig(Map<String, String> map){
			transparentBackground = Boolean.parseBoolean(map.getOrDefault("transparentBackground", "true"));
			imageExtension = map.getOrDefault("imageExtension", "png");
			drawGeneSymbols = Boolean.parseBoolean(map.getOrDefault("drawGeneSymbols", "true"));
			fontName = map.getOrDefault("fontName", "Dialog");
			fontSize = Integer.parseInt(map.getOrDefault("fontSize", "12"));
		}
		
	}
	
	public static final class ProteinInteractomeConfig {
		
		public final double minimumConfidence;
		
		public ProteinInteractomeConfig(Map<String, String> map){
			minimumConfidence = Double.parseDouble(map.getOrDefault("minimumConfidence", "400"));
		}
		
	}
	
	public final GeneralConfig GeneralConfig;
	public final AnalysisConfig AnalysisConfig;
	public final ForceDirectedLayoutConfig ForceDirectedLayoutConfig;
	public final RendererConfig RendererConfig;
	public final ProteinInteractomeConfig ProteinInteractomeConfig;
	
	private Configuration(Map<String, String> map){
		GeneralConfig = new GeneralConfig(map);
		AnalysisConfig = new AnalysisConfig(map);
		ForceDirectedLayoutConfig = new ForceDirectedLayoutConfig(map);
		RendererConfig = new RendererConfig(map);
		ProteinInteractomeConfig = new ProteinInteractomeConfig(map);
	}
	
	public static Configuration defaultConfiguration(){
		return fromMap(Collections.emptyMap());
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
	
	public static Configuration fromArgs(String[] args){
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
