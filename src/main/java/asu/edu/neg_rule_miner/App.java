package asu.edu.neg_rule_miner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.RDFNode;

import asu.edu.neg_rule_miner.model.HornRule;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.naive.SequentialNaiveRuleDiscovery;
/**
 * Hello world!
 *
 */
public class App 
{
	public static void main( String[] args ) throws IOException
	{
		Set<String> relations = Sets.newHashSet();
//		relations.add("http://dbpedia.org/ontology/founder");
//		relations.add("http://dbpedia.org/ontology/foundedBy");
				relations.add("http://yago-knowledge.org/resource/hasWonPrize");


		SequentialNaiveRuleDiscovery naive = new SequentialNaiveRuleDiscovery();

		File negativeExampleFile = new File("dbpedia_founder_neg_examples");


//		String typeSubject = "http://dbpedia.org/ontology/Organisation";
//		String typeObject ="http://dbpedia.org/ontology/Person";
				String typeSubject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
				String typeObject ="http://yago-knowledge.org/resource/wordnet_award_106696483";
		Set<Pair<RDFNode,RDFNode>> negativeExamples =naive.generateNegativeExamples(relations,typeSubject,
				typeObject);

		//		Set<Pair<RDFNode,RDFNode>> negativeExamples =
		//				naive.generateNegativeExamples(negativeExampleFile);


		Map<Set<RuleAtom>,Set<Pair<RDFNode,RDFNode>>> finalRules = 
				naive.discoverHornRules(negativeExamples);

		File outputFile = new File("outputRules.csv");
		naive.printRulesStatistics(finalRules, relations, typeObject, typeSubject,outputFile);

	}
}
