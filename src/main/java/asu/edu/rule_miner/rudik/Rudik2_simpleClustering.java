package asu.edu.rule_miner.rudik;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;

import asu.edu.rule_miner.rudik.model.horn_rule.HornRule;
import asu.edu.rule_miner.rudik.rule_generator.DynamicPruningRuleDiscovery;




public class Rudik2_simpleClustering
{

	public static String toDbpedia_resource(String resource) {
		final String result = "http://dbpedia.org/resource/" + resource;
	    return result;
	} 
	public static String toDbpedia_property(String property) {
	    return "http://dbpedia.org/property/" + property;
	} 
    

 	public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/foundedBy");
	//public static final String typeSubject = "http://dbpedia.org/ontology/Agent";
	//public static final String typeObject = "http://dbpedia.org/ontology/Agent";

      //public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/spouse");
	//public static final String typeSubject = "http://dbpedia.org/ontology/Person";
	//public static final String typeObject = "http://dbpedia.org/ontology/Person";


 // 	public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/vicePresident");
	// public static final String typeSubject = "http://dbpedia.org/ontology/Person";
	// public static final String typeObject = "http://dbpedia.org/ontology/Person";

  	//public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/deathPlace");
	// public static final String typeSubject = "http://dbpedia.org/ontology/Person";
	// public static final String typeObject = "http://dbpedia.org/ontology/Place";

 // 	public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/birthYear");
	// public static final String typeSubject = "http://dbpedia.org/ontology/Person";
	// public static final String typeObject = null;



	private final static Logger LOGGER = LoggerFactory.getLogger(App.class.getName());
	public static void main( String[] args ) throws IOException
	{

   		DynamicPruningRuleDiscovery naive = new DynamicPruningRuleDiscovery();
	
	   	Set<Pair<String,String>> negativeExamples = Sets.newHashSet();
	    Set<Pair<String,String>> positiveExamples = Sets.newHashSet();

	    String relation = relations.iterator().next();
	    Set<String> currentTypes_sub = naive.get_rel_types(relation, true);
	    Set<String> currentTypes_obj = naive.get_rel_types(relation.toString(), false);
	    
	    String[] sub_types = currentTypes_sub.toArray(new String[currentTypes_sub.size()]);
	    String[] obj_types = currentTypes_obj.toArray(new String[currentTypes_obj.size()]);
	    
	    LOGGER.info("Two Main types for Subjects:\t" + currentTypes_sub);
	    LOGGER.info("Two Main types for Objects:\t" + currentTypes_obj);
	    
	    int cluster = 0;
	    String [] rels = relations.iterator().next().split("/");
	    String name = rels[rels.length-1];
    
   		PrintWriter writer = new PrintWriter("simpleClustering_" + name + ".txt", "UTF-8");
   		writer.write("relation: \t" + relations.toString() + "\n");
   		writer.write("Main types for Subjects: \t" + currentTypes_sub + "\n");
   		writer.write("Main types for Objects: \t" + currentTypes_obj+ "\n");

	    for (String subject: sub_types)
	    	for (String object: obj_types) {
	    		positiveExamples = naive.generatePositiveExamples(relations, subject, object, 500);
	    		negativeExamples= naive.generateNegativeExamples(relations, subject, object, 900);
	    		
	    		Map<HornRule,Double> outputRules_negative =  naive.discoverNegativeHornRules(negativeExamples, positiveExamples, relations, subject, object);
	    		Map<HornRule,Double> outputRules_positive = naive.discoverPositiveHornRules(negativeExamples, positiveExamples, relations, subject, object);
	    		cluster +=1;
	    		
	       		// write results to a txt file
	    		writer.write("-----------------------Cluster Number " + cluster + "-----------------------------\n");
	       		writer.write("Subject Type: \t" + subject + "\n");
	       		writer.write("Object Type: \t" + object + "\n");
	    	        
	       		writer.write("----------------------------Negative output rules----------------------------" + "\n");

	       		for (final HornRule oneRule1 : outputRules_negative.keySet()) 
	       			writer.write(oneRule1.toString() + "\t \t" + outputRules_negative.get(oneRule1) + "\n");
	    	        
	       		writer.write("----------------------------Positive output rules----------------------------" + "\n");

	       		for (final HornRule oneRule1 : outputRules_positive.keySet()) 
	       			writer.write(oneRule1.toString() + "\t \t" + outputRules_positive.get(oneRule1) + "\n");
	    	        
	       		writer.write("---------------------------------End----------------------------------------------" + "\n \n \n");

	    	}

	    writer.close();
	    
	            	
	}
		    
	
}
		



