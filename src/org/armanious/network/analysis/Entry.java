package org.armanious.network.analysis;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.armanious.network.Configuration;

public class Entry {
	
	public static void main(String...args){
		if(args == null || args.length == 0)
			printHelp(1);
		if(args[0].equals("-h") || args[0].equals("--help"))
			printHelp(0);
		if(args[0].equals("-o") || args[0].equals("--options"))
			printOptions();
		if(args[0].equals("-d") || args[0].equals("--default-config"))
			printDefaultConfig();
		final Configuration c;
		try {
			if(args[0].startsWith("config=")){
				c = Configuration.fromFile(new File(args[0].substring("config=".length())));
			}else{
				c = Configuration.fromArgs(args);
			}
		} catch (IOException e) {
			System.err.println("Error reading configuration file");
			System.exit(1);
			return;
		} catch (RuntimeException e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(1);
			return;
		}
		try {
			NetworkAnalysis.run(c);
			System.out.println("Note that all windows must be closed before the program competely terminates.");
		} catch (IOException e) {
			System.err.println("Error running NetworkAnaylsis: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void printHelp(int exitCode) {
		System.out.println("To specify a config file, supply config=<filename> as the first argument.\n"
		+ "Othwerwise, you may specify individual config options in the form <configOption>=<value>");
		System.out.println("\t-h\t--help\t\tPrints out this help message");
		System.out.println("\t-o\t--options\tPrints out all available options");
		System.out.println("\t-d\t--default-config\tPrints out the default configuration in the format expected");
		System.exit(exitCode);
	}
	
	private static String insertSpaces(String s) {
		final StringBuilder sb = new StringBuilder();
		for(char c : s.toCharArray()) {
			if(Character.isUpperCase(c) && sb.length() > 0) {
				sb.append(' ').append(c);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	private static String lowercaseFirstChar(String s) {
		return s.length() <= 1 ? s.toLowerCase() : (Character.toLowerCase(s.charAt(0)) + s.substring(1));
	}

	private static String pad(String s, int length) {
		length -= s.length();
		if(length <= 0) return s;
		final StringBuilder sb = new StringBuilder(length).append(s);
		while(length-- > 0) sb.append(' ');
		return sb.toString();
	}
	
	private static void printOptions() {
		try {
			Configuration.GETTING_DEFAULT_OPTIONS = true;
			
			int longestOption = 0;
			for(Class<?> clazz : Configuration.class.getClasses()){
				for(Field field : clazz.getFields()){
					if(field.getName().length() > longestOption) {
						longestOption = field.getName().length();
					}
				}
			}
			
			final Configuration defaultConfig = Configuration.defaultConfiguration("<no default>");
			for(Class<?> clazz : Configuration.class.getClasses()){
				System.out.println(insertSpaces(clazz.getSimpleName()));
				final Object subConfig = Configuration.class.getField(lowercaseFirstChar(clazz.getSimpleName())).get(defaultConfig);
				for(Field field : clazz.getFields()){
					final Object defaultValue = field.getName().equals("projectName") ? "<group1GeneSetFile>" : (
							field.getName().equals("group2GeneSetFile") ? "" : field.get(subConfig)
						);
					System.out.println("\t" + pad(field.getName(), longestOption) + "\tDefault: " + defaultValue);
				}
				System.out.println();
			}
		} catch (Exception ignored) {}
		System.exit(0);
	}
	
	private static void printDefaultConfig() {
		try {
			Configuration.GETTING_DEFAULT_OPTIONS = true;
			
			int longestOption = 0;
			for(Class<?> clazz : Configuration.class.getClasses()){
				for(Field field : clazz.getFields()){
					if(field.getName().length() > longestOption) {
						longestOption = field.getName().length();
					}
				}
			}
			
			final Configuration defaultConfig = Configuration.defaultConfiguration("defaultConfigExample");
			for(Class<?> clazz : Configuration.class.getClasses()){
				System.out.println("# " + insertSpaces(clazz.getSimpleName()));
				final Object subConfig = Configuration.class.getField(lowercaseFirstChar(clazz.getSimpleName())).get(defaultConfig);
				for(Field field : clazz.getFields()){
					final Object defaultValue = field.get(subConfig);
					System.out.println(pad(field.getName(), longestOption) + " = " + defaultValue);
				}
				System.out.println();
			}
		} catch (Exception ignored) {}
		System.exit(0);
	}

}
