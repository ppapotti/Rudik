package asu.edu.neg_rule_miner.client.evaluation.amie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.model.horn_rule.HornRule;
import asu.edu.neg_rule_miner.model.horn_rule.RuleAtom;
import asu.edu.neg_rule_miner.rule_generator.DynamicPruningRuleDiscovery;

public class AmieEvaluationClient {

	private final static Logger LOGGER = LoggerFactory.getLogger(AmieEvaluationClient.class.getName());


	public static void computeDBPediaTypes() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/DBPedia2Configuration.xml");
		Set<String> topTypes = Sets.newHashSet();
		topTypes.add("http://www.w3.org/2002/07/owl#Thing");
		topTypes.add("http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#NaturalPerson");	
		topTypes.add("dbo:Agent");
		topTypes.add("http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Agent");
		topTypes.add("http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#SocialPerson");

		String relationsFile = "dbpedia_3.8_whole_relations";

		computeKBTypes(topTypes, relationsFile);

	}

	public static void computePositiveDBPediaOneRelation(){
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/DBPedia2Configuration.xml");


		String relation = "dbo:child";

		String typeSubject = "http://www.wikidata.org/entity/Q34770";
		String typeObject = "http://www.wikidata.org/entity/Q34770";

		computePositiveKBOneRelation(relation, typeSubject, typeObject,false,false,null,null);

	}

	public static void computeNegativeDBPediaOneRelation(){
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/DBPedia2Configuration.xml");


		String relation = "dbo:founder";
		String typeSubject = "http://www.wikidata.org/entity/Q43229";
		String typeObject = "http://www.wikidata.org/entity/Q215627";

		computeNegativeKBOneRelation(relation, typeSubject, typeObject,true,true);
	}

	public static void computePositiveDBPediaWhole() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/DBPedia2Configuration.xml");
		//first read the file with relations
		String inputRelationFile = "rules";
		computePositiveKBWhole(inputRelationFile);
	}

	public static void computeYagoTypes() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/YagoConfiguration.xml");
		//TO DO: compute top types for yago
		Set<String> topTypes = Sets.newHashSet();
		topTypes.add("http://www.w3.org/2002/07/owl#Thing");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_living_thing_100004258");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_physical_entity_100001930");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_whole_100003553");
		topTypes.add("http://yago-knowledge.org/resource/yagoGeoEntity");
		topTypes.add("http://yago-knowledge.org/resource/yagoLegalActor");
		topTypes.add("http://yago-knowledge.org/resource/yagoLegalActorGeo");
		topTypes.add("http://yago-knowledge.org/resource/yagoPermanentlyLocatedEntity");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_abstraction_100002137");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_causal_agent_100007347");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_object_100002684");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_organism_100004475");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_social_group_107950920");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_psychological_feature_100023100");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_instrumentality_103575240");
		topTypes.add("http://yago-knowledge.org/resource/wordnet_communicator_109610660");

		String relationsFile = "yago_complete_whole_relations";

		computeKBTypes(topTypes, relationsFile);

	}

	public static void computePositiveYagoOneRelation(){
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/Yago2Configuration.xml");

		String relation = "hasChild";
		String typeSubject = "wordnet_person_100007846";
		String typeObject = "wordnet_person_100007846";

		File posExFile = new File("posExamples");
		File negExFile = new File("negExamples");
		computePositiveKBOneRelation(relation, typeSubject, typeObject,false,false,negExFile,posExFile);

		//		computePositiveKBOneRelation(relation, typeSubject, typeObject,false,false,null,null);
	}

	public static void computeNegativeYagoOneRelation(){
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/Yago2Configuration.xml");

		String relation = "directed";
		String typeSubject = "wordnet_person_100007846";
		String typeObject = "wordnet_movie_106613686";

		computeNegativeKBOneRelation(relation, typeSubject, typeObject,true,true);
	}

	public static void computePositiveYagoWhole() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/Yago2Configuration.xml");
		//first read the file with relations
		String inputRelationFile = "yago_whole_relations";
		computePositiveKBWhole(inputRelationFile);
	}

	public static void computeWikidataTypes() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/WikidataConfiguration.xml");
		Set<String> topTypes = Sets.newHashSet();

		String relationsFile = "wikidata_whole_relations";

		computeKBTypes(topTypes, relationsFile);

	}

	public static void computePositiveWikidataWhole() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/WikidataConfiguration.xml");
		//first read the file with relations
		String inputRelationFile = "wikidata_whole_relations";
		computePositiveKBWhole(inputRelationFile);
	}

	public static void computeNegativeWikidataOneRelation(){
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/WikidataConfiguration.xml");

		String relation = "creator_P170";
		String typeSubject = "painting_Q3305213";
		String typeObject = "human_Q5";

		computeNegativeKBOneRelation(relation, typeSubject, typeObject,true,true);
	}

	private static void computeKBTypes(Set<String> topTypes, String relationsFile) throws IOException{

		OntologyTypesInference inference = new OntologyTypesInference(topTypes);

		Set<String> relations = Sets.newHashSet();
		//read relations from input file
		BufferedReader reader = new BufferedReader(new FileReader(new File(relationsFile)));

		String line;
		while((line = reader.readLine())!=null){
			relations.add(line);
		}
		reader.close();

		LOGGER.debug("Read a total of {} relation, starting types inference...",relations.size());
		long startTime = System.currentTimeMillis();
		Map<String,Pair<String,String>> relation2types = inference.computeTypes(relations);
		LOGGER.debug("Types inference finished in {} seconds",(System.currentTimeMillis()-startTime)/1000.);

		for(String relation:relation2types.keySet()){
			Pair<String,String> types = relation2types.get(relation);
			String typeSubject = types.getLeft()!=null ? types.getLeft() : "any";
			String typeObject = types.getRight()!=null ? types.getRight() : "any";
			System.out.println(relation+"\t"+typeSubject+"\t"+typeObject);

		}

	}

	/**
	 * If negativeExFile (positiveExFile != null) the negative examples (positive examples) are read from file
	 * @param relation
	 * @param typeSubject
	 * @param typeObject
	 * @param subjectFunction
	 * @param objectFunction
	 * @param negativeExFile
	 * @param posExFile
	 */
	private static void computePositiveKBOneRelation(String relation, String typeSubject, String typeObject,boolean subjectFunction, boolean objectFunction,
			File negativeExFile, File positiveExFile){
		Set<String> relations = Sets.newHashSet();
		relations.add(relation);

		String graphIri = ConfigurationFacility.getSparqlExecutor().getGraphIri();

		String positiveQuery = "select distinct ?subject ?object from "+graphIri+" where { ?subject <"+relation+"> ?object. }";


		DynamicPruningRuleDiscovery ruleDiscovery = new DynamicPruningRuleDiscovery();
		Set<Pair<String,String>>  positiveExamples = null;
		if(positiveExFile==null)
			positiveExamples = ruleDiscovery.getKBExamples(positiveQuery, "subject", "object");
		else
			positiveExamples = ruleDiscovery.readExamples(positiveExFile);

		Set<Pair<String,String>> negativeExamples = null;
		if(negativeExFile==null)
			negativeExamples = ruleDiscovery.generateNegativeExamples(relations, typeSubject, typeObject,subjectFunction,objectFunction);
		else
			negativeExamples = ruleDiscovery.readExamples(negativeExFile);



		System.out.println(ruleDiscovery.discoverPositiveHornRules(negativeExamples,positiveExamples,relations,typeSubject,typeObject,subjectFunction,objectFunction));
	}

	private static void computeNegativeKBOneRelation(String relation, String typeSubject, String typeObject, boolean subjectFunction, boolean objectFunction){
		Set<String> relations = Sets.newHashSet();
		relations.add(relation);

		String graphIri = ConfigurationFacility.getSparqlExecutor().getGraphIri();

		String positiveQuery = "select distinct ?subject ?object from "+graphIri+" where { ?subject <"+relation+"> ?object. }";


		DynamicPruningRuleDiscovery ruleDiscovery = new DynamicPruningRuleDiscovery();
		Set<Pair<String,String>>  positiveExamples = ruleDiscovery.getKBExamples(positiveQuery, "subject", "object");

		Set<Pair<String,String>> negativeExamples =
				ruleDiscovery.generateNegativeExamples(relations, typeSubject, typeObject,subjectFunction,objectFunction);


		System.out.println(ruleDiscovery.discoverNegativeHornRules(negativeExamples,positiveExamples,relations,typeSubject,typeObject));
	}

	private static void computePositiveKBWhole(String inputRelationFile) throws IOException{
		//first read the file with relations
		BufferedReader reader = new BufferedReader(new FileReader(new File(inputRelationFile)));
		Map<String,Pair<String,String>> relation2subjectObjectTypes = Maps.newHashMap();
		String line;
		Map<String,Pair<Boolean,Boolean>> relation2subjectObjectFunction = Maps.newHashMap();

		while((line=reader.readLine())!=null){
			if(line.split("\t").length!=3 && line.split("\t").length!=5){
				LOGGER.error("Line {} does not contains three or five elements separated by tab!",line);
				continue;
			}
			String relation = line.split("\t")[0];
			String typeSubject = line.split("\t")[1];
			String typeObject = line.split("\t")[2];

			typeSubject = typeSubject.equals("any") ? null : typeSubject;
			typeObject = typeObject.equals("any") ? null : typeObject;
			relation2subjectObjectTypes.put(relation, Pair.of(typeSubject, typeObject));

			//check if subjectFunction and objectFunction information are specified
			if(line.split("\t").length==5){
				relation2subjectObjectFunction.put(relation,Pair.of(Boolean.parseBoolean(line.split("\t")[3]), 
						Boolean.parseBoolean(line.split("\t")[4])));
			}
		}

		reader.close();

		Map<String,List<HornRule>> relation2rules = Maps.newHashMap();
		int totalRelation = relation2subjectObjectTypes.size();
		int count=0;
		String graphIri = ConfigurationFacility.getSparqlExecutor().getGraphIri();
		long startTime = System.currentTimeMillis();
		for(String relation:relation2subjectObjectTypes.keySet()){
			count++;
			LOGGER.info("Computing rules for relation {} ({} out of {})",relation,count,totalRelation);

			Pair<String,String> subjectObject = relation2subjectObjectTypes.get(relation);
			boolean subjectFunction = true;
			boolean objectFunction = true;
			if(relation2subjectObjectFunction.containsKey(relation)){
				subjectFunction = relation2subjectObjectFunction.get(relation).getLeft();
				objectFunction = relation2subjectObjectFunction.get(relation).getRight();
			}

			Set<String> relations = Sets.newHashSet();

			relations.add(relation);
			String positiveQuery = "select distinct ?subject ?object "
					+ "from "+graphIri+" where { ?subject <"+relation+"> ?object. }";

			try{
				DynamicPruningRuleDiscovery ruleDiscovery = new DynamicPruningRuleDiscovery();
				Set<Pair<String,String>>  positiveExamples = ruleDiscovery.getKBExamples(positiveQuery, "subject", "object");

				Set<Pair<String,String>> negativeExamples =
						ruleDiscovery.generateNegativeExamples(relations, subjectObject.getLeft(), subjectObject.getRight(),subjectFunction,objectFunction);


				List<HornRule> currentRules = ruleDiscovery.discoverPositiveHornRules(
						negativeExamples,positiveExamples,relations,subjectObject.getLeft(),
						subjectObject.getRight(),subjectFunction,objectFunction);
				relation2rules.put(relation, currentRules);
				LOGGER.info("Set of found valid rules for relation '{}': {}",relation,currentRules);
			}
			catch(Exception e){
				LOGGER.error("Error while computing rules for relation {}.",relation,e);
			}
		}
		System.out.println("Total computation time (seconds):"+(System.currentTimeMillis()-startTime)/1000.);

		for(String relation:relation2rules.keySet()){
			System.out.println(relation+"\t"+relation2rules.get(relation));
		}
	} 

	public static void computeNewPredictionsYago() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/Yago2Configuration.xml");
		File yagoRelationFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/yago_2_relations.txt");
		File yagoNewKB = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/amie_yago_2s.tsv");

		String systemToEvaluate = "amie";
		File outputRules = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/"+systemToEvaluate+"/output_rules.txt");
		File outputStatistics = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/"+systemToEvaluate+"/statistics.txt");
		File unknowInducedFacts = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/"+systemToEvaluate+"/unknown_induced_facts.txt");

		AmieComputeAccuracyPredictions.computePredictionsAccuracy(outputRules, yagoRelationFile, yagoNewKB, outputStatistics, unknowInducedFacts);
	}

	public static void computeNewPredictionsYagoHassPlank() throws IOException{
		File yagoRelationFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/yago_2_relations.txt");
		File yagoNewKB = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/amie_yago_2s.tsv");

		String systemToEvaluate = "hass_plank";
		File outputFacts = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/"+systemToEvaluate+"/total_facts_produced.txt");
		File outputStatistics = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/"+systemToEvaluate+"/statistics.txt");
		File unknowInducedFacts = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/"+systemToEvaluate+"/unknown_induced_facts.txt");

		AmieComputeAccuracyPredictions.computePredictionsAccuracyFactsInduced(outputFacts, yagoRelationFile, yagoNewKB, outputStatistics, unknowInducedFacts);
	}

	public static void computeNewPredictionsDBPedia() throws IOException{
		ConfigurationFacility.setConfigurationFile("src/main/config/amie/DBPedia2Configuration.xml");
		File yagoRelationFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/dbpedia_2.0_relations.txt");
		File yagoNewKB = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/amie_dbpedia_3.8.tsv");

		String systemToEvaluate = "us";
		File outputRules = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/results/"+systemToEvaluate+"/output_rules.txt");
		File outputStatistics = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/results/"+systemToEvaluate+"/statistics.txt");
		File unknowInducedFacts = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/results/"+systemToEvaluate+"/unknown_induced_facts.txt");

		AmieComputeAccuracyPredictions.computePredictionsAccuracy(outputRules, yagoRelationFile, yagoNewKB, outputStatistics, unknowInducedFacts);
	}

	public static void computeNewPredictionsYagoAggregatedPrecision() throws IOException{
		File amieStatisticFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/amie/statistics_best_30.txt");
		File amieUnknown = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/amie/unknown_induced_facts_best_30.txt");
		computeNewPredictionsAggregatedPrecision(amieStatisticFile,amieUnknown);

		File stasticFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/us/statistics.txt");
		File unknownFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/yago/results/us/unknown_induced_facts.txt");
		//		computeNewPredictionsAggregatedPrecision(stasticFile,unknownFile);
	}

	public static void computeNewPredictionsDBPediaAggregatedPrecision() throws IOException{
		File amieStatisticFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/results/amie/statistics.txt");
		File amieUnknown = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/results/amie/unknown_induced_facts.txt");
		computeNewPredictionsAggregatedPrecision(amieStatisticFile,amieUnknown);

		File stasticFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/results/us/statistics.txt");
		File unknownFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/dbpedia/results/us/unknown_induced_facts.txt");
		computeNewPredictionsAggregatedPrecision(stasticFile,unknownFile);
	}

	private static void computeNewPredictionsAggregatedPrecision(File statisticFile, 
			File unknowFile) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(statisticFile));
		String line;
		Map<String,Long> relation2positive = Maps.newHashMap();
		Map<String,Long> relation2negative = Maps.newHashMap();
		Map<String,Long> relation2total = Maps.newHashMap();
		while((line = reader.readLine())!=null){
			String []lineSplit = line.split("\t");
			if(lineSplit.length!=3)
				continue;
			String relation = lineSplit[0];
			long positive = Long.parseLong(lineSplit[1]);
			long negative = Long.parseLong(lineSplit[2]);
			relation2positive.put(relation, positive);
			relation2negative.put(relation, negative);
			relation2total.put(relation, positive+negative);

		}
		reader.close();

		//read unknown values
		String unknownRelation = null;
		String previousRelation = null;
		reader = new BufferedReader(new FileReader(unknowFile));
		int positive=-1;
		int negative=-1;
		int totCount=-1;
		while((line = reader.readLine()) != null){
			if(line.contains("Unknown facts for relation '")){
				unknownRelation = line.replace("Unknown facts for relation '", "");
				unknownRelation = unknownRelation.substring(0, unknownRelation.length()-2);
				if(positive==-1)
					previousRelation = unknownRelation;
				continue;
			}
			if(line.startsWith("Rule '")&&line.endsWith("' produced:")){
				//adjust weights
				if(positive!=-1){

					if(positive==0 && negative==0){
						LOGGER.warn("0 positive and negative labelled manually examples for relation {}.",previousRelation);
					}
					else{
						double negFraction = (negative+0.)/(negative+positive);
						double posFraction = (positive+0.)/(negative+positive);

						long totPos = Math.round(totCount*posFraction);
						long totNeg = Math.round(totCount*negFraction);

						if(totPos+totNeg!=totCount){
							LOGGER.warn("Computing percentages for {} manually labelled positive and {} manually labelled negative over a total of {} examples "
									+ "returned {} total positive and {} total negative",positive,negative,totCount,totPos,totNeg);
						}
						relation2positive.put(previousRelation, relation2positive.get(previousRelation)+totPos);
						relation2negative.put(previousRelation, relation2negative.get(previousRelation)+totNeg);
						relation2total.put(previousRelation, relation2total.get(previousRelation)+totPos+totNeg);

					}
				}

				previousRelation = unknownRelation;
				positive=0;
				negative=0;
				totCount=0;
				continue;
			}
			if(line.split("\t").length<3)
				continue;
			totCount++;
			if(line.split("\t").length==4){
				if(line.split("\t")[3].equals("true"))
					positive++;
				if(line.split("\t")[3].equals("false"))
					negative++;
			}
		}
		//last count
		if(positive==0 && negative==0){
			LOGGER.warn("0 positive and negative labelled manually examples for relation {}.",unknownRelation);
		}
		else{
			double negFraction = (negative+0.)/(negative+positive);
			double posFraction = (positive+0.)/(negative+positive);

			long totPos = Math.round(totCount*posFraction);
			long totNeg = Math.round(totCount*negFraction);

			if(totPos+totNeg!=totCount){
				LOGGER.warn("Computing percentages for {} manually labelled positive and {} manually labelled negative over a total of {} examples "
						+ "returned {} total positive and {} total negative",positive,negative,totCount,totPos,totNeg);
			}
			else{
				relation2positive.put(previousRelation, relation2positive.get(previousRelation)+totPos);
				relation2negative.put(previousRelation, relation2negative.get(previousRelation)+totNeg);
				relation2total.put(previousRelation, relation2total.get(previousRelation)+totPos+totNeg);
			}
		}

		//print by descending order of (positive - negative)

		StringBuilder aggregatedPrecisionBuilder = new StringBuilder();
		StringBuilder relationBuilder = new StringBuilder();
		StringBuilder factsCounBuilder = new StringBuilder();
		StringBuilder averagePrecisionBuilder = new StringBuilder();
		double totPos = 0;
		double totNeg = 0;
		int totPredictions = 0;
		double averagePrecisionCount =0; 
		int relCount =0;
		while(relation2positive.size()>0){
			relCount++;
			String bestRelation = null;
			long bestScore = Long.MIN_VALUE;
			for(String relation:relation2positive.keySet()){
				long currentScore = relation2positive.get(relation)-relation2negative.get(relation);
				if(currentScore>bestScore){
					bestScore = currentScore;
					bestRelation = relation;
				}
			}
			relationBuilder.append(bestRelation+"\t");
			long currentTotal = relation2total.get(bestRelation);
			totPredictions += currentTotal;
			factsCounBuilder.append(totPredictions+"\t");
			relation2total.remove(bestRelation);
			long currPos = relation2positive.get(bestRelation);
			totPos += currPos;
			relation2positive.remove(bestRelation);
			long currNeg = relation2negative.get(bestRelation);
			totNeg += currNeg;
			relation2negative.remove(bestRelation);
			aggregatedPrecisionBuilder.append((totPos/(totPos+totNeg))+"\t");

			if(currPos == 0 && currNeg == 0){
				LOGGER.warn("No new facts induced for relation {}.",bestRelation);
				relCount--;
			}
			else{
				averagePrecisionCount += ((currPos+0.)/(currPos+currNeg));
			}
			averagePrecisionBuilder.append((averagePrecisionCount/relCount)+"\t");
		}
		System.out.println("Relations:\t"+relationBuilder.toString());
		System.out.println("Facts count:\t"+factsCounBuilder.toString());
		System.out.println("Aggregated Precision:\t"+aggregatedPrecisionBuilder.toString());
		System.out.println("Average Precision:\t"+averagePrecisionBuilder.toString());
		System.out.println();

	}


	public static void main(String[] args) throws Exception{

		ConfigurationFacility.getConfiguration();

		//DBPEDIA 2.0
		//		computeDBPediaTypes();
		//		computePositiveDBPediaOneRelation();
		//		computeNegativeDBPediaOneRelation();
		//computePositiveDBPediaWhole();

		//		computeNewPredictionsDBPedia();
		computeNewPredictionsDBPediaAggregatedPrecision();

		//YAGO2
		//		computeYagoTypes();
		//				computePositiveYagoOneRelation();
		//		computeNegativeYagoOneRelation();
		//		computePositiveYagoWhole();	

		//		computeNewPredictionsYago();
		//		computeNewPredictionsYagoAggregatedPrecision();

		//		computeNewPredictionsYagoHassPlank();

		//WIKIDATA
		//		computeWikidataTypes();
		//		computePositiveWikidataWhole();
		//		computeNegativeWikidataOneRelation();
	}

}
