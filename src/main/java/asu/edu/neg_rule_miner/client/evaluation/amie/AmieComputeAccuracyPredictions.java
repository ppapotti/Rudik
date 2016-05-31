package asu.edu.neg_rule_miner.client.evaluation.amie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.model.horn_rule.MultipleGraphHornRule;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

/**
 * Give a set of rules mined from an ontology A, this class apply rules to A and validate
 * the correctness of new inferred facts on a second ontology B, which is an expanded version of A that contains more facts
 * @author sortona
 *
 */
public class AmieComputeAccuracyPredictions {

	private final static Logger LOGGER = LoggerFactory.getLogger(AmieComputeAccuracyPredictions.class.getName());


	/**
	 * 
	 * @param ruleFile
	 * contains rule to evaluate. Each line contains relation \t hornRule \t {true|false}
	 * relation is the relation implied by the horn rule, hornRule is the string representation of a horn rule, true indicates that a rule is generally correct
	 * @param relationFile
	 * contains information about relations in a KB. Each line contains 4 elements separated by tab. The first element is the name of the relaion, the second and
	 * the third are the type of the domain and the type of the codomain, the fourt element is {true|false} where true indicated that the relation is a function
	 * @param secondKBFile
	 * file containing facts about the second KB. Each line contains '<subject>\t<relation>\t<object> .'
	 * @throws IOException 
	 */
	public static void computePredictionsAccuracy(File ruleFile, File relationFile, File secondKBFile, File outputStatistics,
			File inducedUnknownFacts) throws IOException{
		LOGGER.debug("Reading rules and relations information...");
		//read rules
		Map<String,Map<String,Boolean>> relation2rules2correct = Maps.newHashMap();
		BufferedReader reader = new BufferedReader(new FileReader(ruleFile));
		String line;
		int totRule = 0;
		while((line = reader.readLine())!=null){
			String []lineSplit = line.split("\t");
			if(lineSplit.length!=3)
				continue;
			totRule++;
			Map<String,Boolean> currentRelationRules = relation2rules2correct.get(lineSplit[0]);
			if(currentRelationRules==null){
				currentRelationRules = Maps.newHashMap();
				relation2rules2correct.put(lineSplit[0], currentRelationRules);
			}
			currentRelationRules.put(lineSplit[1], Boolean.parseBoolean(lineSplit[2]));
		}
		reader.close();

		//read types and functionality for each relation
		Map<String,Pair<String,String>> relation2types = Maps.newHashMap();
		Map<String,Boolean> relation2isFunction = Maps.newHashMap();
		reader = new BufferedReader(new FileReader(relationFile));
		while((line = reader.readLine())!=null){
			String []lineSplit = line.split("\t"); 
			if(lineSplit.length!=4)
				continue;
			String typeSubject = lineSplit[1];
			typeSubject = typeSubject.equals("any") ? null : typeSubject;
			String typeObject = lineSplit[2];
			typeObject = typeObject.equals("any") ? null : typeObject;
			relation2types.put(lineSplit[0], Pair.of(typeSubject, typeObject));
			relation2isFunction.put(lineSplit[0], Boolean.parseBoolean(lineSplit[3]));
		}
		reader.close();
		LOGGER.debug("Rules and relation information read");

		LOGGER.debug("Reading the new KnowledgeBase file...");
		//read the knowledge base and save triples, and also for each relation that is a function a map '<subject>\t<relation>-><object> .'
		Set<String> secondKBFacts = Sets.newHashSet();
		Map<String,String> functionRelation2object = Maps.newHashMap();
		reader = new BufferedReader(new FileReader(secondKBFile));
		while((line=reader.readLine())!=null){
			String []lineSplit = line.split("\t"); 
			if(lineSplit.length!=3)
				continue;
			secondKBFacts.add(line);
			//check the relation is a function
			String relation = lineSplit[1].substring(1, lineSplit[1].length()-1);
			if(relation2isFunction.containsKey(relation) && relation2isFunction.get(relation)){
				functionRelation2object.put(lineSplit[0]+"\t"+lineSplit[1], lineSplit[2]);
			}
		}
		LOGGER.debug("New KB facts read.");

		//now compute for each relation new induced facts
		//if a fact is induced by mutliples rules, it is counted only once
		if(outputStatistics.exists())
			outputStatistics.delete();
		if(inducedUnknownFacts.exists())
			inducedUnknownFacts.delete();
		int count=0;
		for(String oneRelation:relation2rules2correct.keySet()){

			LOGGER.debug("Analyzing numbers for relation {}...",oneRelation);
			Set<String> correctFacts = Sets.newHashSet();
			Set<String> incorrectFacts = Sets.newHashSet();
			Map<String,Set<String>> rule2unknownFacts = Maps.newHashMap();
			Set<String> totalUnknownFacts = Sets.newHashSet();
			Pair<String,String> typeSubjectObject = relation2types.get(oneRelation);

			for(String oneRule : relation2rules2correct.get(oneRelation).keySet()){
				count++;
				LOGGER.debug("Computing numbers on rule {} ({} out of {})",oneRule,count,totRule);
				//compute the new facts induced by the rule
				Set<String> newFacts = computeNewFacts(oneRelation, typeSubjectObject.getLeft(), typeSubjectObject.getRight(), oneRule);
				LOGGER.debug("Rule {} predicted new {} facts.",oneRule,newFacts.size());
				//if the rule is correct, all newFacts are correct
				if(relation2rules2correct.get(oneRelation).get(oneRule)){
					correctFacts.addAll(newFacts);
					continue;
				}

				//otherwise check the new facts are contained in the new KB or if they violate an existing function relation
				for(String oneNewFact:newFacts){
					if(secondKBFacts.contains(oneNewFact)){
						correctFacts.add(oneNewFact);
						continue;
					}
					//check it does not violate a function relation
					if(relation2isFunction.get(oneRelation)){
						String []factSplit = oneNewFact.split("\t");
						String objectValue = functionRelation2object.get(factSplit[0]+"\t"+factSplit[1]);
						if(objectValue!=null && !objectValue.equals(factSplit[2])){
							incorrectFacts.add(oneNewFact);
							continue;
						}

					}
					//fact has to be manually check
					if(!totalUnknownFacts.contains(oneNewFact)){
						totalUnknownFacts.add(oneNewFact);
						Set<String> currentRuleUnkownFacts = rule2unknownFacts.get(oneRule);
						if(currentRuleUnkownFacts==null){
							currentRuleUnkownFacts = Sets.newHashSet();
							rule2unknownFacts.put(oneRule, currentRuleUnkownFacts);
						}
						currentRuleUnkownFacts.add(oneNewFact);
					}
				}

			}

			incorrectFacts.removeAll(correctFacts);
			totalUnknownFacts.removeAll(correctFacts);

			//remove from the unknown the incorrecr facts
			totalUnknownFacts.removeAll(incorrectFacts);

			LOGGER.debug("Numbers for relation {} computed. Correct: {}, incorrect: {}, unknown: {}",oneRelation,correctFacts.size(),incorrectFacts.size(),
					totalUnknownFacts.size());
			//write statistic about the rule
			BufferedWriter writer = new BufferedWriter(new FileWriter(inducedUnknownFacts,true));
			if(rule2unknownFacts.size()>0)
				writer.write("Unknown facts for relation '"+oneRelation+"':\n");

			for(String oneRule : rule2unknownFacts.keySet()){
				Set<String> currentUnknownFacts = rule2unknownFacts.get(oneRule);
				currentUnknownFacts.removeAll(correctFacts);
				currentUnknownFacts.removeAll(incorrectFacts);
				writer.write("Rule '"+oneRule+"' produced:\n");
				for(String oneUnknownFact:currentUnknownFacts){
					writer.write(oneUnknownFact+"\n");
				}
			}
			writer.close();

			//writer statistic about correct and incorrect values
			writer = new BufferedWriter(new FileWriter(outputStatistics,true));
			writer.write(oneRelation+"\t"+correctFacts.size()+"\t"+incorrectFacts.size()+"\n");
			writer.close();
		}

	}


	/**
	 * THis is for Hasso Plank Institute strategy, that does not compute rules but the output is already a file with facts.
	 * The input file inducedFacts contains facts (tab separated) to be verified
	 * @param inducedFacts
	 * @param relationFile
	 * @param secondKBFile
	 * @param outputStatistics
	 * @param inducedUnknownFacts
	 * @throws IOException
	 */
	public static void computePredictionsAccuracyFactsInduced(File inducedFacts, File relationFile, File secondKBFile, File outputStatistics,
			File inducedUnknownFacts) throws IOException{
		LOGGER.debug("Reading rules and relations information...");
		//read rules
		Map<String,Set<String>> relation2facts = Maps.newHashMap();
		BufferedReader reader = new BufferedReader(new FileReader(inducedFacts));
		String line;
		Set<String> currentRelationFacts = null;
		while((line = reader.readLine())!=null){
			if(line.startsWith("Unknown facts for relation '")){
				currentRelationFacts = Sets.newHashSet();
				relation2facts.put(line.replaceAll("Unknown facts for relation '", "").replaceAll("':", ""), currentRelationFacts);

			}
			String []lineSplit = line.split("\t");
			if(lineSplit.length!=3)
				continue;
			currentRelationFacts.add(line);
		}
		reader.close();

		//read types and functionality for each relation
		Map<String,Boolean> relation2isFunction = Maps.newHashMap();
		reader = new BufferedReader(new FileReader(relationFile));
		while((line = reader.readLine())!=null){
			String []lineSplit = line.split("\t"); 
			if(lineSplit.length!=4)
				continue;
			relation2isFunction.put(lineSplit[0], Boolean.parseBoolean(lineSplit[3]));
		}
		reader.close();
		LOGGER.debug("Rules and relation information read");

		LOGGER.debug("Reading the new KnowledgeBase file...");
		//read the knowledge base and save triples, and also for each relation that is a function a map '<subject>\t<relation>-><object> .'
		Set<String> secondKBFacts = Sets.newHashSet();
		Map<String,String> functionRelation2object = Maps.newHashMap();
		reader = new BufferedReader(new FileReader(secondKBFile));
		while((line=reader.readLine())!=null){
			String []lineSplit = line.split("\t"); 
			if(lineSplit.length!=3)
				continue;
			secondKBFacts.add(line);
			//check the relation is a function
			String relation = lineSplit[1].substring(1, lineSplit[1].length()-1);
			if(relation2isFunction.containsKey(relation) && relation2isFunction.get(relation)){
				functionRelation2object.put(lineSplit[0]+"\t"+lineSplit[1], lineSplit[2]);
			}
		}
		LOGGER.debug("New KB facts read.");

		//now compute for each relation new induced facts
		//if a fact is induced by mutliples rules, it is counted only once
		if(outputStatistics.exists())
			outputStatistics.delete();
		if(inducedUnknownFacts.exists())
			inducedUnknownFacts.delete();
		for(String oneRelation:relation2facts.keySet()){

			LOGGER.debug("Analyzing numbers for relation {}...",oneRelation);
			Set<String> correctFacts = Sets.newHashSet();
			Set<String> incorrectFacts = Sets.newHashSet();
			Set<String> totalUnknownFacts = Sets.newHashSet();

			Set<String> newFacts = relation2facts.get(oneRelation);


			//otherwise check the new facts are contained in the new KB or if they violate an existing function relation
			for(String oneNewFact:newFacts){
				if(secondKBFacts.contains(oneNewFact)){
					correctFacts.add(oneNewFact);
					continue;
				}
				//check it does not violate a function relation
				if(relation2isFunction.get(oneRelation)){
					String []factSplit = oneNewFact.split("\t");
					String objectValue = functionRelation2object.get(factSplit[0]+"\t"+factSplit[1]);
					if(objectValue!=null && !objectValue.equals(factSplit[2])){
						incorrectFacts.add(oneNewFact);
						continue;
					}

				}
				//fact has to be manually check
				totalUnknownFacts.add(oneNewFact);
			}



			incorrectFacts.removeAll(correctFacts);
			totalUnknownFacts.removeAll(correctFacts);

			//remove from the unknown the incorrecr facts
			totalUnknownFacts.removeAll(incorrectFacts);

			LOGGER.debug("Numbers for relation {} computed. Correct: {}, incorrect: {}, unknown: {}",oneRelation,correctFacts.size(),incorrectFacts.size(),
					totalUnknownFacts.size());
			//write statistic about the rule
			BufferedWriter writer = new BufferedWriter(new FileWriter(inducedUnknownFacts,true));
			if(totalUnknownFacts.size()>0){
				writer.write("Unknown facts for relation '"+oneRelation+"':\n");
				writer.write("Rule 'unknown' produced:\n");

				for(String oneUnknownFact:totalUnknownFacts){
					writer.write(oneUnknownFact+"\n");
				}

			}
			writer.close();

			//writer statistic about correct and incorrect values
			writer = new BufferedWriter(new FileWriter(outputStatistics,true));
			writer.write(oneRelation+"\t"+correctFacts.size()+"\t"+incorrectFacts.size()+"\n");
			writer.close();
		}

	}


	/**
	 * Utility method that given a horn rule apply the rule on A and retrieve all the facts from A that are not bound by the input relation
	 * Return a set of pairs, where each pair is <subject,object> that respect the input rule and are not in bounded by the input relation 
	 * @param relation
	 * @param typeSubject
	 * @param typeObject
	 * @param hornRule
	 * @return
	 */
	private static Set<String> computeNewFacts(String relation, String typeSubject, String typeObject, String hornRule){

		SparqlExecutor executor = ConfigurationFacility.getSparqlExecutor();
		String query = "SELECT DISTINCT ?subject ?object ";
		if(executor.getGraphIri()!=null && executor.getGraphIri().length()>0)
			query+="FROM "+executor.getGraphIri()+" ";
		query+="WHERE {";
		if(typeSubject != null)
			query+= "?subject <"+executor.getTypePrefix()+"> <"+typeSubject+">. ";
		if(typeObject!=null)
			query+="?object <"+executor.getTypePrefix()+"> <"+typeObject+">. ";
		query+=executor.getHornRuleAtomQuery(MultipleGraphHornRule.readHornRule(hornRule))+
				" FILTER NOT EXISTS {?subject <"+relation+"> ?object. }"+
				" FILTER (?subject != ?object) }";

		//execute the query
		Set<Pair<String,String>> retrievedExamples = executor.getKBExamples(query, "subject", "object", true);
		Set<String> outputFacts = Sets.newHashSet();
		for(Pair<String,String> oneExample:retrievedExamples){
			outputFacts.add("<"+oneExample.getLeft()+">\t<"+relation+">\t<"+oneExample.getRight()+"> .");
		}
		return outputFacts;
	}

}
