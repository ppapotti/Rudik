package asu.edu.neg_rule_miner.client.evaluation;

import java.io.IOException;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;

public class RuleEvaluationClient {


	public static void evaluateDBPediaSingleFile() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/DbpediaConfiguration.xml");

		String inputFolder = "/Users/sortona/Documents/KDR/evaluation/dbpedia";
		RuleEvaluation restrictedEvaluation = new RuleEvaluationRestricted(inputFolder);
		PrecisionRecallCountContainer containerRestricted = new PrecisionRecallCountContainer();
		RuleEvaluation extendedEvaluation = new RuleEvaluationExtended(inputFolder);
		PrecisionRecallCountContainer containerExtended = new PrecisionRecallCountContainer();


		String []founder = new String[]{"founder_manual_5","http://dbpedia.org/ontology/Organisation","http://dbpedia.org/ontology/Person"};
		Set<String> founderRelations = Sets.newHashSet();
		founderRelations.add("http://dbpedia.org/ontology/foundedBy");
		founderRelations.add("http://dbpedia.org/ontology/founder");

		String []spouse = new String[]{"spouse_best_10","http://dbpedia.org/ontology/Person","http://dbpedia.org/ontology/Person"};
		Set<String> spouseRelations = Sets.newHashSet();
		spouseRelations.add("http://dbpedia.org/ontology/spouse");

		String []academicAdvisor = new String[]{"academicAdvisor","http://dbpedia.org/ontology/Person","http://dbpedia.org/ontology/Person"};
		Set<String> academicAdvisorRelations = Sets.newHashSet();
		academicAdvisorRelations.add("http://dbpedia.org/ontology/academicAdvisor");




		containerRestricted = new PrecisionRecallCountContainer();
		restrictedEvaluation.generateSilverExamples(founder[0], founderRelations, founder[1], founder[2]);
		restrictedEvaluation.updatePrecisionRecallCounter(founder[0], containerRestricted);
		containerExtended = new PrecisionRecallCountContainer();
		extendedEvaluation.generateSilverExamples(founder[0], founderRelations, founder[1], founder[2]);
		extendedEvaluation.updatePrecisionRecallCounter(founder[0], containerExtended);

		//		containerRestricted = new PrecisionRecallCountContainer();
		//		restrictedEvaluation.generateSilverExamples(spouse[0], spouseRelations, spouse[1], spouse[2]);
		//		restrictedEvaluation.updatePrecisionRecallCounter(spouse[0], containerRestricted);
		//		containerExtended = new PrecisionRecallCountContainer();
		//		extendedEvaluation.generateSilverExamples(spouse[0], spouseRelations, spouse[1], spouse[2]);
		//		extendedEvaluation.updatePrecisionRecallCounter(spouse[0], containerExtended);

		//		containerRestricted = new PrecisionRecallCountContainer();
		//		restrictedEvaluation.generateSilverExamples(academicAdvisor[0], academicAdvisorRelations, academicAdvisor[1], academicAdvisor[2]);
		//		restrictedEvaluation.updatePrecisionRecallCounter(academicAdvisor[0], containerRestricted);
		//		containerExtended = new PrecisionRecallCountContainer();
		//		extendedEvaluation.generateSilverExamples(academicAdvisor[0], academicAdvisorRelations, academicAdvisor[1], academicAdvisor[2]);
		//		extendedEvaluation.updatePrecisionRecallCounter(academicAdvisor[0], containerExtended);


		System.out.println("Precision\tRecall\tUnknown\tTot_Induced_Rules\tTot_Negative_Silver");
		System.out.println(containerRestricted.getPrecision()+"\t"+containerRestricted.getRecall()+"\t"+containerRestricted.getUnknown()+"\t"+
				containerRestricted.getTotalRuleExamples()+"\t"+containerRestricted.totalSivlerNegativeCount);
		System.out.println(containerExtended.getPrecision()+"\t"+containerExtended.getRecall()+"\t"+containerExtended.getUnknown()+"\t"+
				containerExtended.getTotalRuleExamples()+"\t"+containerExtended.totalSivlerNegativeCount);
	}

	public static void evaluateDBPedia(){

	}

	public static void evaluateYagoSingleFile() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/YagoConfiguration.xml");

		String inputFolder = "/Users/sortona/Documents/KDR/evaluation/yago";
		RuleEvaluation restrictedEvaluation = new RuleEvaluationRestricted(inputFolder);
		PrecisionRecallCountContainer restrictedContainer = new PrecisionRecallCountContainer();
		RuleEvaluation extendedEvaluation = new RuleEvaluationExtended(inputFolder);
		PrecisionRecallCountContainer containerExtended = new PrecisionRecallCountContainer();


		String []created = new String[]{"created_manual_1","http://yago-knowledge.org/resource/wordnet_person_100007846",
		"http://yago-knowledge.org/resource/wordnet_organization_108008335"};
		Set<String> createdRelations = Sets.newHashSet();
		createdRelations.add("http://yago-knowledge.org/resource/created");

		String []marriedTo = new String[]{"marriedTo_best_10","http://yago-knowledge.org/resource/wordnet_person_100007846",
		"http://yago-knowledge.org/resource/wordnet_person_100007846"};
		Set<String> marriedToRelations = Sets.newHashSet();
		marriedToRelations.add("http://yago-knowledge.org/resource/isMarriedTo");




		restrictedContainer = new PrecisionRecallCountContainer();
		restrictedEvaluation.generateSilverExamples(created[0], createdRelations, created[1], created[2]);
		restrictedEvaluation.updatePrecisionRecallCounter(created[0], restrictedContainer);
		containerExtended = new PrecisionRecallCountContainer();
		extendedEvaluation.generateSilverExamples(created[0], createdRelations, created[1], created[2]);
		extendedEvaluation.updatePrecisionRecallCounter(created[0], containerExtended);

		//		restrictedContainer = new PrecisionRecallCountContainer();
		//		restrictedEvaluation.generateSilverExamples(marriedTo[0], marriedToRelations, marriedTo[1], marriedTo[2]);
		//		restrictedEvaluation.updatePrecisionRecallCounter(marriedTo[0], restrictedContainer);
		//		containerExtended = new PrecisionRecallCountContainer();
		//		extendedEvaluation.generateSilverExamples(marriedTo[0], marriedToRelations, marriedTo[1], marriedTo[2]);
		//		extendedEvaluation.updatePrecisionRecallCounter(marriedTo[0], containerExtended);


		System.out.println("Precision\tRecall\tUnknown\tTot_Induced_Rules\tTot_Negative_Silver");
		System.out.println(restrictedContainer.getPrecision()+"\t"+restrictedContainer.getRecall()+"\t"+restrictedContainer.getUnknown()+"\t"+
				restrictedContainer.getTotalRuleExamples()+"\t"+restrictedContainer.totalSivlerNegativeCount);
		System.out.println(containerExtended.getPrecision()+"\t"+containerExtended.getRecall()+"\t"+containerExtended.getUnknown()+"\t"+
				containerExtended.getTotalRuleExamples()+"\t"+containerExtended.totalSivlerNegativeCount);
	}

	public static void evaluateYago(){

	}

	public static void evaluateWikidataSingleFile() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/WikidataConfiguration.xml");

		String inputFolder = "/Users/sortona/Documents/KDR/evaluation/wikidata";
		RuleEvaluation restrictedEvaluation = new RuleEvaluationRestricted(inputFolder);
		PrecisionRecallCountContainer containerRestricted = new PrecisionRecallCountContainer();
		RuleEvaluation extendedEvaluation = new RuleEvaluationExtended(inputFolder);
		PrecisionRecallCountContainer containerExtended = new PrecisionRecallCountContainer();


		String []founder = new String[]{"founder_best_10","http://www.wikidata.org/entity/Q4830453","http://www.wikidata.org/entity/Q5"};
		Set<String> founderRelations = Sets.newHashSet();
		founderRelations.add("http://www.wikidata.org/prop/direct/P112");

		String []spouse = new String[]{"spouse_best_10","http://www.wikidata.org/entity/Q5","http://www.wikidata.org/entity/Q5"};
		Set<String> spouseRelations = Sets.newHashSet();
		spouseRelations.add("http://www.wikidata.org/prop/direct/P26");

		String []oathGiven = new String[]{"oathGiven","http://www.wikidata.org/entity/Q159821","http://www.wikidata.org/entity/Q5"};
		Set<String> oathGivenRelations = Sets.newHashSet();
		oathGivenRelations.add("http://www.wikidata.org/prop/direct/P543");




		//		containerRestricted = new PrecisionRecallCountContainer();
		//		restrictedEvaluation.generateSilverExamples(founder[0], founderRelations, founder[1], founder[2]);
		//		restrictedEvaluation.updatePrecisionRecallCounter(founder[0], containerRestricted);
		//		containerExtended = new PrecisionRecallCountContainer();
		//		extendedEvaluation.generateSilverExamples(founder[0], founderRelations, founder[1], founder[2]);
		//		extendedEvaluation.updatePrecisionRecallCounter(founder[0], containerExtended);

		containerRestricted = new PrecisionRecallCountContainer();
		restrictedEvaluation.generateSilverExamples(spouse[0], spouseRelations, spouse[1], spouse[2]);
		restrictedEvaluation.updatePrecisionRecallCounter(spouse[0], containerRestricted);
		containerExtended = new PrecisionRecallCountContainer();
		extendedEvaluation.generateSilverExamples(spouse[0], spouseRelations, spouse[1], spouse[2]);
		extendedEvaluation.updatePrecisionRecallCounter(spouse[0], containerExtended);

		//		containerRestricted = new PrecisionRecallCountContainer();
		//		restrictedEvaluation.setSwitchSubjectObject(true);
		//		restrictedEvaluation.generateSilverExamples(oathGiven[0], oathGivenRelations, oathGiven[1], oathGiven[2]);
		//		restrictedEvaluation.updatePrecisionRecallCounter(oathGiven[0], containerRestricted);
		//		containerExtended = new PrecisionRecallCountContainer();
		//		extendedEvaluation.generateSilverExamples(oathGiven[0], oathGivenRelations, oathGiven[1], oathGiven[2]);
		//		extendedEvaluation.updatePrecisionRecallCounter(oathGiven[0], containerExtended);


		System.out.println("Precision\tRecall\tUnknown\tTot_Induced_Rules\tTot_Negative_Silver");
		System.out.println(containerRestricted.getPrecision()+"\t"+containerRestricted.getRecall()+"\t"+containerRestricted.getUnknown()+"\t"+
				containerRestricted.getTotalRuleExamples()+"\t"+containerRestricted.totalSivlerNegativeCount);
		System.out.println(containerExtended.getPrecision()+"\t"+containerExtended.getRecall()+"\t"+containerExtended.getUnknown()+"\t"+
				containerExtended.getTotalRuleExamples()+"\t"+containerExtended.totalSivlerNegativeCount);
	}

	public static void evaluateWikidata(){

	}

	public static void main(String[] args) throws IOException{
		//		evaluateDBPediaSingleFile();

		evaluateYagoSingleFile();

		//		evaluateWikidataSingleFile();
	}

}
