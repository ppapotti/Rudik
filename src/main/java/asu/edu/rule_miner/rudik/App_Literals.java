package asu.edu.rule_miner.rudik;


import java.io.IOException;
import java.util.Map;
import java.util.Set;

import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;

import asu.edu.rule_miner.rudik.model.horn_rule.HornRule;
import asu.edu.rule_miner.rudik.rule_generator.DynamicPruningRuleDiscovery;



public class App_Literals
{

 	public static Set<String> relations; 
	public static String typeSubject = null;
	public static String typeObject = null;



	private final static Logger LOGGER = LoggerFactory.getLogger(App.class.getName());
	
	public static void main( String[] args ) throws IOException
	{

		relations = Sets.newHashSet("http://dbpedia.org/ontology/launchDate"); 
   		DynamicPruningRuleDiscovery naive = new DynamicPruningRuleDiscovery();
	   
   		Set<Pair<String,String>> negativeExamples = Sets.newHashSet();
   		Set<Pair<String,String>> positiveExamples = Sets.newHashSet();

   		System.out.println("No dataset specified...Try to generate it using rudik");
   		System.out.println(" Generate Positive Examples ");

   		positiveExamples = naive.generatePositiveExamples(relations, typeSubject, typeObject, 100);
			
   		for (final  Pair<String,String> example  :positiveExamples) 
   			System.out.println("Positive example: " + example.getLeft() + " --- " + example.getRight());
			
   		System.out.println(" Generate Negative Examples ");
			
   		negativeExamples= naive.generateNegativeExamples(relations, typeSubject, typeObject, 200);
			
   		for (final  Pair<String,String> example  :negativeExamples) 
   			System.out.println("Negative example: " + example.getLeft() + " --- " + example.getRight());
  
			
   		Map<HornRule,Double> outputRules_negative = naive.discoverNegativeHornRules(negativeExamples, positiveExamples, relations, typeSubject, typeObject);
   		Map<HornRule,Double> outputRules_positive = naive.discoverPositiveHornRules(negativeExamples, positiveExamples, relations, typeSubject, typeObject);
		    
	    
	    
   		for (final HornRule oneRule : outputRules_negative.keySet() ) 
   			LOGGER.info("neg Rule:{}\tScore:{}", oneRule, outputRules_negative.get(oneRule));

   		for (final HornRule oneRule : outputRules_positive.keySet()) {
   			LOGGER.info("itpos Rule:{}\tScore:{}", oneRule, outputRules_positive.get(oneRule));
		    
		    
   		// write results to a txt file
	    String [] rels = relations.iterator().next().split("/");
	    String name = rels[rels.length-1];
   		PrintWriter writer = new PrintWriter("results_" + name + ".txt", "UTF-8");

   		writer.write("relation: \t" + relations.toString() + "\n");
   		writer.write("Subject: \t" + typeSubject + "\n");
   		writer.write("Object: \t" + typeObject + "\n");
	        
   		writer.write("----------------------------Negative output rules----------------------------" + "\n");

   		for (final HornRule oneRule1 : outputRules_negative.keySet()) 
   			writer.write(oneRule1.toString() + "\t \t" + outputRules_negative.get(oneRule1) + "\n");
	        
   		writer.write("----------------------------Positive output rules----------------------------" + "\n");

   		for (final HornRule oneRule1 : outputRules_positive.keySet()) 
   			writer.write(oneRule1.toString() + "\t \t" + outputRules_positive.get(oneRule1) + "\n");
	        
   		writer.write("---------------------------------End----------------------------------------------" + "\n \n \n");
   		writer.close();

   		}
	}
}
