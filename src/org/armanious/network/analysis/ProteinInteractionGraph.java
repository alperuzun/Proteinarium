package org.armanious.network.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.armanious.graph.Graph;
import org.armanious.network.Configuration.ProteinInteractomeConfig;

public class ProteinInteractionGraph extends Graph<Protein> {

	public ProteinInteractionGraph(ProteinInteractomeConfig pic) throws IOException {
		this(pic, Double.MAX_VALUE);
	}
	
	public ProteinInteractionGraph(ProteinInteractomeConfig pic, double maxPathUnconfidence) throws IOException {
		load(Math.max(1000 - maxPathUnconfidence, pic.minConfidence), new BufferedReader(new FileReader(pic.interactomeFile)));
	}

	private void load(double threshold, BufferedReader in) throws IOException {
		System.out.println("Loading protein interaction graph...");
		String s;
		while((s = in.readLine()) != null){
			final String[] parts = s.split(" ");
			if(parts[0].length() == 0 || parts[0].charAt(0) != '9') //only 9606 i.e. humans
				continue;
			final int weight = Integer.parseInt(parts[2]);
			if(weight >= threshold){
				final Protein a = Protein.getProtein(parts[0], true);
				final Protein b = Protein.getProtein(parts[1], true);
				addEdge(a, b, weight);
			}
		}
		in.close();
		System.out.println("Loaded protein interaction graph");
	}

}
