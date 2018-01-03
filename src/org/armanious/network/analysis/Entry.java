package org.armanious.network.analysis;

import java.io.File;
import java.io.IOException;

import org.armanious.network.Configuration;

public class Entry {
	
	public static void main(String...args){
		if(args == null || args.length == 0)
			printUsage();
		final Configuration c;
		if(args[0].startsWith("config=")){
			try {
				c = Configuration.fromFile(new File(args[0].substring("config=".length())));
			} catch (IOException e) {
				System.err.println("Error reading configuration file");
				System.exit(1);
				return;
			}
		}else{
			c = Configuration.fromArgs(args);
		}
		try {
			NetworkAnalysis.run(c);
		} catch (IOException e) {
			System.err.println("Error running NetworkAnaylsis");
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	private static void printUsage(){
		System.err.println("Incorrect usage");
		System.exit(1);
	}

}
