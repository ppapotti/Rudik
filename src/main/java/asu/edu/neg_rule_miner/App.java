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
//				relations.add("http://dbpedia.org/ontology/founder");
//				relations.add("http://dbpedia.org/ontology/foundedBy");

		relations.add("http://dbpedia.org/ontology/presidentOf");

		//		relations.add("http://dbpedia.org/ontology/director");

		//		relations.add("http://yago-knowledge.org/resource/hasWonPrize");

		//relations.add("http://yago-knowledge.org/resource/worksAt");

		//		relations.add("http://yago-knowledge.org/resource/isMarriedTo");
//		relations.add("http://yago-knowledge.org/resource/dealsWith");




		SequentialNaiveRuleDiscovery naive = new SequentialNaiveRuleDiscovery();

		File negativeExampleFile = new File("dbpedia_founder_neg_examples");


//		String typeSubject = "http://dbpedia.org/ontology/Organisation";
//		String typeObject ="http://dbpedia.org/ontology/Person";

		//		String typeSubject = "http://schema.org/Movie";
		//		String typeObject = "http://dbpedia.org/ontology/Person";

				String typeSubject = "http://xmlns.com/foaf/0.1/Person";
				String typeObject = "http://schema.org/Country";

		//		String typeSubject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
		//		String typeObject ="http://yago-knowledge.org/resource/wordnet_award_106696483";

		//		String typeSubject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
		//		String typeObject = "http://yago-knowledge.org/resource/wordnet_organization_108008335";

		//		String typeSubject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
		//		String typeObject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
		
//		String typeSubject = "http://yago-knowledge.org/resource/wordnet_country_108544813";
//		String typeObject = "http://yago-knowledge.org/resource/wordnet_country_108544813";

		//		Set<Pair<RDFNode,RDFNode>> negativeExamples =naive.generateNegativeExamples(relations,typeSubject,
		//				typeObject);

		//		Set<Pair<RDFNode,RDFNode>> negativeExamples =
		//				naive.generateNegativeExamples(negativeExampleFile);

		String prefix = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> prefix dbo: <http://dbpedia.org/ontology/> ";

		String queryDirectorRestrict = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://schema.org/Movie>. ?object rdf:type <http://dbpedia.org/ontology/Person>. ?subject ?relTarget ?realObject. ?realSubject ?relTarget ?object. ?subject ?relation ?object. FILTER (?relTarget = <http://dbpedia.org/ontology/director>) FILTER (?relation != <http://dbpedia.org/ontology/director>) FILTER NOT EXISTS {?subject <http://dbpedia.org/ontology/director> ?object.} }";
		String queryDirectorUnion = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://schema.org/Movie>. ?object rdf:type <http://dbpedia.org/ontology/Person>. { {?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.} } ?subject ?relation ?object. FILTER (?relTarget = <http://dbpedia.org/ontology/director>) FILTER (?relation != <http://dbpedia.org/ontology/director>) FILTER NOT EXISTS {?subject <http://dbpedia.org/ontology/director> ?object.} }";

		String queryFounderUnion = prefix+"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> prefix dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type dbo:Organisation. ?object rdf:type dbo:Person. {{?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.}} ?subject ?relation ?object. FILTER (?relTarget = dbo:founder || ?relTarget = dbo:foundedBy) FILTER (?relation != dbo:founder && ?relation != dbo:foundedBy) FILTER NOT EXISTS {?subject dbo:founder ?object.} FILTER NOT EXISTS {?subject dbo:foundedBy ?object.} FILTER NOT EXISTS {?subject rdf:type dbo:Group.}}";

		String queryWorksAtUnion = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_organization_108008335>. { {?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.} } ?subject ?relation ?object. FILTER (?relTarget = <http://yago-knowledge.org/resource/worksAt>) FILTER (?relation != <http://yago-knowledge.org/resource/worksAt>) FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/worksAt> ?object.} }";
		String queryWorksAtRestrict = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_organization_108008335>. ?subject ?relTarget ?realObject. ?realSubject ?relTarget ?object. ?subject ?relation ?object. FILTER (?relTarget = <http://yago-knowledge.org/resource/worksAt>) FILTER (?relation != <http://yago-knowledge.org/resource/worksAt>) FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/worksAt> ?object.} }";

		String queryIsMarriedToRestrict = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?subject ?relTarget ?realObject. ?realSubject ?relTarget ?object. ?subject ?relation ?object. FILTER (?relTarget = <http://yago-knowledge.org/resource/isMarriedTo>) FILTER (?relation != <http://yago-knowledge.org/resource/isMarriedTo>) FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/isMarriedTo> ?object.} }";
		String queryIsMarriedToUnion = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. {{?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.}} ?subject ?relation ?object. FILTER (?relTarget = <http://yago-knowledge.org/resource/isMarriedTo>) FILTER (?relation != <http://yago-knowledge.org/resource/isMarriedTo>) FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/isMarriedTo> ?object.} } ";

		String queryPresidentOf = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type dbo:Person. ?subject ?relation ?object. FILTER NOT EXISTS {?subject dbo:presidentOf ?object.} FILTER (?object = <http://dbpedia.org/resource/United_States>)} LIMIT 8000";
		

		String queryDealsWith = prefix+"select distinct ?subject ?object where { ?subject ?rel ?object. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_country_108544813>. FILTER NOT EXISTS {?subjcet <http://yago-knowledge.org/resource/dealsWith> ?object} FILTER (?subject=<http://yago-knowledge.org/resource/China>)}";
		String queryDealsWithPositive = prefix+"select distinct ?subject ?object where { ?subject <http://yago-knowledge.org/resource/dealsWith> ?object. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_country_108544813>. FILTER (?subject=<http://yago-knowledge.org/resource/China>)}";

		Set<Pair<RDFNode,RDFNode>> negativeExamples = 
				naive.getKBExamples(queryPresidentOf, "subject", "object");
		
//		Set<Pair<RDFNode,RDFNode>> positiveExamples =
//				naive.getKBExamples(queryDealsWithPositive, "subject", "object");
		
		Set<Pair<RDFNode,RDFNode>> positiveExamples =
				naive.generatePositiveExamples(relations, typeSubject, typeObject);
		
		Map<Set<RuleAtom>,Set<Pair<RDFNode,RDFNode>>> finalRules = 
				naive.discoverHornRules(negativeExamples,positiveExamples);

		//int positiveExample = 9925;//founder
		//int positiveExample = 93325;//director
		//int positiveExample = 10257;//yago:worksAt
		//int positiveExample = 120529;//yago:hasWonPrize
		//int positiveExample = 34229;//yago:isMarriedTo


		String subjectConstant = null;
		String objectConstant = "http://dbpedia.org/resource/United_States";
		File outputFile = new File("outputRules.csv");
		naive.printRulesStatistics(finalRules, relations, typeObject, typeSubject,
				negativeExamples.size(),positiveExamples.size(),outputFile,subjectConstant,objectConstant);

	}
}
