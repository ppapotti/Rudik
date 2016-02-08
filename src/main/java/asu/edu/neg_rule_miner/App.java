package asu.edu.neg_rule_miner;

import java.io.File;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.RDFNode;

import asu.edu.neg_rule_miner.naive.NaiveRuleDiscovery;
/**
 * Hello world!
 *
 */
public class App 
{
	public static void main( String[] args )
	{
		Set<String> relations = Sets.newHashSet();
		relations.add("http://dbpedia.org/ontology/founder");
		relations.add("http://dbpedia.org/ontology/foundedBy");

		Set<String> subjectFilters = Sets.newHashSet();
		//		subjectFilters.add("http://dbpedia.org/resource/United_World_Colleges");
		//		subjectFilters.add("http://dbpedia.org/resource/BBC");
		//		subjectFilters.add("http://dbpedia.org/resource/The_Lego_Group");
		//		subjectFilters.add("http://dbpedia.org/resource/Georgia_Tech_Research_Institute");
		//		subjectFilters.add("http://dbpedia.org/resource/Barnes_family");

		Set<String> objectFilters = Sets.newHashSet();
		//		objectFilters.add("http://dbpedia.org/resource/James_M._McGarrah");
		//		objectFilters.add("http://dbpedia.org/resource/Rona_Fairhead");
		//		objectFilters.add("http://dbpedia.org/resource/Jørgen_Vig_Knudstorp");
		//		objectFilters.add("http://dbpedia.org/resource/Charles,_Prince_of_Wales");
		//		objectFilters.add("http://dbpedia.org/resource/John_Ross_Ewing_III");


		NaiveRuleDiscovery naive = new NaiveRuleDiscovery();

		File negativeExampleFile = new File("dbpedia_founder_neg_examples");


		Set<Pair<RDFNode,RDFNode>> negativeExamples =naive.generateNegativeExamples(relations,"http://dbpedia.org/ontology/Organisation",
				"http://dbpedia.org/ontology/Person",subjectFilters,objectFilters,false);

		//		Set<Pair<RDFNode,RDFNode>> negativeExamples =
		//				naive.generateNegativeExamples(negativeExampleFile);


		naive.discoverHornRules(negativeExamples);
		
	}
}
