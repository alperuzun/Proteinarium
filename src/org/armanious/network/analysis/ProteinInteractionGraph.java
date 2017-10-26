package org.armanious.network.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.armanious.graph.ImmutableGraph;

public class ProteinInteractionGraph extends ImmutableGraph<Protein> {

	public ProteinInteractionGraph(int threshold) throws IOException {
		this(threshold, "/Users/david/PycharmProjects/NetworkAnalysis/9606.protein.links.v10.5.txt");
	}

	public ProteinInteractionGraph(int threshold, String file) throws IOException {
		this(threshold, new FileInputStream(file));
	}

	public ProteinInteractionGraph(int threshold, InputStream in) throws IOException {
		load(threshold, new BufferedReader(new InputStreamReader(in)));
		makeImmutable();
	}

	private void load(int threshold, BufferedReader in) throws IOException {
		String s;
		while((s = in.readLine()) != null){
			final String[] parts = s.split(" ");
			if(parts[0].length() == 0 || parts[0].charAt(0) != '9'){
				continue;
			}
			final int weight = Integer.parseInt(parts[2]);
			if(weight >= threshold){
				final Protein a = Protein.getProtein(parts[0], true);
				final Protein b = Protein.getProtein(parts[1], true);
				addNode(a);
				addNode(b);
				addEdge(a, b, Integer.parseInt(parts[2]));
			}
		}
		in.close();
	}

}
