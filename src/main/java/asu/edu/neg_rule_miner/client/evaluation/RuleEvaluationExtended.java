package asu.edu.neg_rule_miner.client.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.configuration.Constant;
import asu.edu.neg_rule_miner.model.horn_rule.MultipleGraphHornRule;
import asu.edu.neg_rule_miner.model.horn_rule.RuleAtom;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public class RuleEvaluationExtended extends RuleEvaluation{

	private final static Logger LOGGER = LoggerFactory.getLogger(RuleEvaluationExtended.class.getName());

	//induced rules examples, restricted shows the restricted version
	private static String EXTENDED_EVALUATION_NUMBERS= "extended_evaluation_numbers";

	private static String PREFIX_TOT_SILVER_NEGATIVE = "Tot. silver negative examples:";
	private static String PREFIX_RULE_NEGATIVE_COVERED = "Tot. output rules negative examples:";

	public RuleEvaluationExtended(String inputFolder) {
		super(inputFolder);
	}

	@Override
	public void generateSilverExamples(String relationName, Set<String> relations, String typeSubject,
			String typeObject) throws IOException {

		LOGGER.debug("Executing silver examples queries against the KB for the relation {}.",relationName);
		//check the existence of induced rules file and silver negative rules file
		File inducedRulesFile = new File(inputFolder+"/"+INDUCED_RULES_FOLDER+"/"+relationName);
		if(!inducedRulesFile.exists())
			throw new RuleMinerException("Cannot locate the induced rules file: "+inducedRulesFile.getAbsolutePath(), LOGGER);

		File silverNegativeRulesFile = new File(inputFolder+"/"+SILVER_NEGATIVE_RULES_FOLDER+"/"+relationName);
		if(relationName.contains("_"))
			silverNegativeRulesFile = new File(inputFolder+"/"+SILVER_NEGATIVE_RULES_FOLDER+"/"+relationName.substring(0,relationName.indexOf("_")));
		if(!silverNegativeRulesFile.exists())
			throw new RuleMinerException("Cannot locate the silver negative rules file: "+silverNegativeRulesFile.getAbsolutePath(), LOGGER);

		//generate all examples
		this.generatePositiveExample(relationName, relations, typeSubject, typeObject);
		this.generateUnknownPositiveExamples(relationName, relations, typeSubject, typeObject);
		this.generateOutputRulesPositiveExamples(relationName, relations, typeSubject, typeObject);
		//check the file has not been already created
		File extendedStatisticsFile = new File(inputFolder+"/"+EXTENDED_EVALUATION_NUMBERS+"/"+relationName);
		if(!extendedStatisticsFile.exists()){
			this.generateSilverNegativeExamples(relationName, relations, typeSubject, typeObject);
			this.generateOutputRulesExamples(relationName, relations, typeSubject, typeObject);
		}
		LOGGER.debug("Queries executed and files saved for relation {}.",relationName);

	}

	private void generateOutputRulesPositiveExamples(String relationName, Set<String> relations, String typeSubject, String typeObject) 
			throws IOException{
		File outputRulesPositiveFile = new File(inputFolder+"/"+INDUCED_RULES_EX_FOLDER+"/"+relationName+"_positive_extended");
		if(outputRulesPositiveFile.exists())
			return;
		LOGGER.debug("Generating output rules positive extended examples for {} relation...",relationName);
		Set<Pair<String,String>> outputRulesPositiveExtendedExamples = Sets.newHashSet();

		String positiveExampleQuery = executor.generatePositiveExampleQuery(relations, typeSubject, typeObject);

		//read input rules
		BufferedReader rulesReader = new BufferedReader(new FileReader(new File(inputFolder+"/"+INDUCED_RULES_FOLDER+"/"+relationName)));
		String line;
		while((line = rulesReader.readLine())!=null){
			//each line is on the format: 'type\trule'. Type is not used in the restricted settings
			Set<RuleAtom> currentRule = MultipleGraphHornRule.readHornRule(line.split("\t")[1]);
			String currentRuleFilter = executor.getHornRuleAtomQuery(currentRule);
			//excute the rule over restricted negative examples
			outputRulesPositiveExtendedExamples.addAll(executor.getKBExamples(positiveExampleQuery.substring(0, positiveExampleQuery.length()-1)+" "
					+currentRuleFilter+"}","subject","object",true));
		}
		rulesReader.close();

		BufferedWriter writer = new BufferedWriter(new FileWriter(outputRulesPositiveFile));

		for(Pair<String,String> oneExample:outputRulesPositiveExtendedExamples){
			writer.write(oneExample.getLeft()+"\t"+oneExample.getRight()+"\n");
		}
		writer.close();
		LOGGER.debug("Output rules positive extended examples generated and saved on file {}.",outputRulesPositiveFile.getAbsolutePath());
	}

	private void generateOutputRulesExamples(String relationName, Set<String> relations, String typeSubject, String typeObject) 
			throws IOException{

		//read rules and save them in normal and expensive
		LOGGER.debug("Generating output rules negative extended examples for {} relation...",relationName);

		//read input rules
		BufferedReader rulesReader = new BufferedReader(new FileReader(new File(inputFolder+"/"+INDUCED_RULES_FOLDER+"/"+relationName)));
		String line;
		Map<String,Set<String>> type2rule = Maps.newHashMap();
		type2rule.put("normal", Sets.newHashSet());
		type2rule.put("expensive", Sets.newHashSet());

		while((line = rulesReader.readLine())!=null){

			if(!line.contains("\t"))
				continue;
			//a rule that connects subject to the object will always return an empty result
			if(line.contains("(subject,object)"))
				continue;
			Set<String> currentRules = type2rule.get(line.split("\t")[0]);
			currentRules.add(line.split("\t")[1]);
		}
		rulesReader.close();

		//builder filter for expensive rules
		String expensiveRulesFilter = "";

		int count=0;
		long totExamplesCount = 0;
		for(String expensiveRule : type2rule.get("expensive")){
			//first execute the expensive rule
			if(expensiveRule.contains(Constant.GREATER_EQUAL_REL+"(") || expensiveRule.contains(Constant.GREATER_REL+"(") || 
					expensiveRule.contains(Constant.LESS_REL+"(") || expensiveRule.contains(Constant.LESS_EQUAL_REL+"("))
				totExamplesCount+=this.executeCountInequalityQuery(relations, typeSubject, typeObject, MultipleGraphHornRule.readHornRule(expensiveRule));

			else{
				//inequality query
				if(expensiveRule.contains("!=(")){
					//TO DO: != query
				}
				else{
					//expensive normal query with 2 atoms
					totExamplesCount+=this.executeEqualityExpensiveQuery(relations, typeSubject, typeObject, 
							MultipleGraphHornRule.readHornRule(expensiveRule));
				}
			}

			Set<RuleAtom> expensiveAtoms = MultipleGraphHornRule.readHornRule(expensiveRule);
			modifyVariablesRuleAtoms(expensiveAtoms,"other"+count);
			count++;
			expensiveRulesFilter+=" FILTER NOT EXISTS {"+ executor.getHornRuleAtomQuery(expensiveAtoms)+"}";
		}

		//execute the non expensive rules
		Set<Pair<String,String>> outputNormalRulesNegativeExtendedExamples = Sets.newHashSet();
		String negativeExampleQuery = executor.generateNegativeExtendedExampleQuery(relations, typeSubject, typeObject);

		for(String normalRule : type2rule.get("normal")){
			String currentRuleFilter = executor.getHornRuleAtomQuery(MultipleGraphHornRule.readHornRule(normalRule));

			outputNormalRulesNegativeExtendedExamples.addAll(executor.getKBExamples(negativeExampleQuery.substring(0, negativeExampleQuery.length()-1)+" "
					+currentRuleFilter+expensiveRulesFilter+"}","subject","object",true));
		}

		//add normal rules examples to the total count
		totExamplesCount+=outputNormalRulesNegativeExtendedExamples.size();

		//write result on the output file
		File outputFile = new File(inputFolder+"/"+EXTENDED_EVALUATION_NUMBERS+"/"+relationName);
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true));
		writer.write(PREFIX_RULE_NEGATIVE_COVERED+"\t"+totExamplesCount);
		writer.close();

		LOGGER.debug("Output rules negative extended examples generated and saved on file {}.",outputFile.getAbsolutePath());

	}

	/**
	 * Generate count of all the pairs subject,object where both subject and object are in the target relation, minus those pairs where subject and object
	 * have a relation connecting them
	 * @param relationName
	 * @param relations
	 * @param typeSubject
	 * @param typeObject
	 * @throws IOException 
	 */
	private void generateSilverNegativeExamples(String relationName, Set<String> relations, String typeSubject, String typeObject) 
			throws IOException{
		LOGGER.debug("Counting extended silver negative examples for {} relation...",relationName);
		//create left extended query
		String leftExtendedQuery = executor.generateOneSideExampleQuery(relations, typeSubject, "subject");
		//add count condition
		leftExtendedQuery = leftExtendedQuery.replaceAll("SELECT DISTINCT", "SELECT (COUNT(*) AS ?count) {SELECT DISTINCT")+"}";
		//execute the query
		long leftSideValues = executor.executeCountQuery(leftExtendedQuery);

		//create rright extended query
		String rightExtendedQuery = executor.generateOneSideExampleQuery(relations, typeObject, "object");
		//add count condition
		rightExtendedQuery = rightExtendedQuery.replaceAll("SELECT DISTINCT", "SELECT (COUNT(*) AS ?count) {SELECT DISTINCT")+"}";
		//execute the query
		long rightSideValues = executor.executeCountQuery(rightExtendedQuery);

		//get all the extended examples that are in a relation
		String examplsConnectedWithRelation = executor.generateExampleWithRelationsQuery(relations, typeSubject, typeObject);
		examplsConnectedWithRelation = examplsConnectedWithRelation.replaceAll("SELECT DISTINCT", "SELECT (COUNT(*) AS ?count) {SELECT DISTINCT")+"}";
		int examplesRelationCount = executor.executeCountQuery(examplsConnectedWithRelation);

		long totNegativeExamples = leftSideValues*rightSideValues - examplesRelationCount;

		if(totNegativeExamples <= 0)
			throw new RuleMinerException("The count of total silver negative examples is negative! (tot. left side="+leftSideValues+", tot. right side="+rightSideValues+","
					+" tot. examples connected by a relation="+examplesRelationCount+", tot. count="+totNegativeExamples+")", LOGGER);

		//write it on a file
		File outputFile = new File(inputFolder+"/"+EXTENDED_EVALUATION_NUMBERS+"/"+relationName);
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		writer.write(PREFIX_TOT_SILVER_NEGATIVE+"\t"+totNegativeExamples+"\n");
		writer.close();
		LOGGER.debug("Silver negative examples counted and saved to file {}.",outputFile.getAbsolutePath());
	}
	@Override
	public void updatePrecisionRecallCounter(String relationName, PrecisionRecallCountContainer container)
			throws IOException {
		LOGGER.debug("Reading input silver examples file and updating precision and recall container for relation {} extended.",relationName);
		
		String originalRelationName = relationName;
		if(relationName.contains("_"))
			originalRelationName = relationName.substring(0,relationName.indexOf("_"));
		//positive examples
		Set<Pair<String,String>> positiveExamples = readExamples(inputFolder+"/"+SILVER_POSITIVE_EX_FOLDER+"/"+originalRelationName);

		//silver positive unknown
		Set<Pair<String,String>> silverPositiveUnknonwExamples = readExamples(inputFolder+"/"+UNKNOWN_SILVER_POSITIVE_EX_FOLDER+"/"+originalRelationName);

		//output rules examples
		Set<Pair<String,String>> outputRulesPositiveExamples = readExamples(inputFolder+"/"+INDUCED_RULES_EX_FOLDER+"/"+relationName+"_restricted");

		//unknown induced examples
		Set<Pair<String,String>> unknownOutputRulesExamples = Sets.newHashSet();
		unknownOutputRulesExamples.addAll(outputRulesPositiveExamples);
		unknownOutputRulesExamples.retainAll(silverPositiveUnknonwExamples);

		//wrong induced examples
		Set<Pair<String,String>> wrongOutputRulesExamples = Sets.newHashSet();
		wrongOutputRulesExamples.addAll(outputRulesPositiveExamples);
		wrongOutputRulesExamples.retainAll(positiveExamples);
		wrongOutputRulesExamples.removeAll(silverPositiveUnknonwExamples);
		LOGGER.debug("Wrong extracted examples: "+wrongOutputRulesExamples);

		//read correct negative induced and total negative
		File extendedNegativeStatistics = new File(inputFolder+"/"+EXTENDED_EVALUATION_NUMBERS+"/"+relationName);
		BufferedReader reader = new BufferedReader(new FileReader(extendedNegativeStatistics));
		String line;
		long totRulesNegative = 0;
		while((line=reader.readLine())!=null){
			if(!line.contains("\t"))
				continue;
			if(line.startsWith(PREFIX_TOT_SILVER_NEGATIVE))
				container.totalSivlerNegativeCount += Long.parseLong(line.split("\t")[1]);
			if(line.startsWith(PREFIX_RULE_NEGATIVE_COVERED))
				totRulesNegative = Long.parseLong(line.split("\t")[1]);
		}
		reader.close();

		//check both numbers have been specified
		if(totRulesNegative==0 || container.totalSivlerNegativeCount==0)
			throw new RuleMinerException("File with extended statistics does not contain either tot. siver negative examples or "
					+ "tot. output rules negative examples. File located at: "+extendedNegativeStatistics.getAbsolutePath(), LOGGER);

		container.ruleCorrectExamplesCount+=totRulesNegative;
		container.ruleExamplesCount+=totRulesNegative+unknownOutputRulesExamples.size()+wrongOutputRulesExamples.size();
		container.ruleUnknownCount+=unknownOutputRulesExamples.size();
		container.ruleWrongExamplesRuleCount+=wrongOutputRulesExamples.size();
		LOGGER.debug("Relation {} extended processed and precision and recall container updated.",relationName);

	}

	private void modifyVariablesRuleAtoms(Set<RuleAtom> atoms, String prefixToAdd){
		//modify not subject object variables
		for(RuleAtom oneAtom:atoms){
			if(!oneAtom.getSubject().equals(MultipleGraphHornRule.START_NODE)&&!oneAtom.getSubject().equals(MultipleGraphHornRule.END_NODE))
				oneAtom.setSubject(prefixToAdd+oneAtom.getSubject());
			if(!oneAtom.getObject().equals(MultipleGraphHornRule.START_NODE)&&!oneAtom.getObject().equals(MultipleGraphHornRule.END_NODE))
				oneAtom.setObject(prefixToAdd+oneAtom.getObject());
		}
	}

	/**
	 * Execute query of type r_1(subject,v0) r_2(object,v1) <(v0,v1).
	 * We do not deal with equality, no queries with <= or >=
	 * @param relations
	 * @param typeSubject
	 * @param typeObject
	 * @return
	 */
	private long executeCountInequalityQuery(Set<String> relations, String typeSubject, String typeObject,Set<RuleAtom> expensiveRule){
		LOGGER.debug("Executing expensive inequality query: {}.",expensiveRule);
		//frst count the number of examples that respect the expesniveRule, they are to be removed
		String removedExamplesQuery = executor.generateExampleWithRelationsQuery(relations, typeSubject, typeObject);
		removedExamplesQuery = removedExamplesQuery.substring(0,removedExamplesQuery.length()-1) + " "+executor.getHornRuleAtomQuery(expensiveRule)+"}";
		//create count query
		removedExamplesQuery = removedExamplesQuery.replaceAll("SELECT DISTINCT", "SELECT (COUNT(*) AS ?count) {SELECT DISTINCT")+"}";
		int toBeRemoved = executor.executeCountQuery(removedExamplesQuery);

		String oneSideSubjectQuery = executor.generateOneSideExampleQuery(relations, typeSubject, "subject");
		String oneSideObjectQuery = executor.generateOneSideExampleQuery(relations, typeObject, "object");
		String variableSubject = null;
		RuleAtom inequalityAtom = null;
		//get the atom with subject
		Set<Pair<String,String>> subjectExamples = null;
		Set<Pair<String,String>> objectExamples = null;
		for(RuleAtom atom:expensiveRule){
			String otherVariable=null;
			if(atom.getSubject().equals(MultipleGraphHornRule.START_NODE) || atom.getSubject().equals(MultipleGraphHornRule.END_NODE))
				otherVariable = atom.getObject();
			if(atom.getObject().equals(MultipleGraphHornRule.START_NODE) || atom.getObject().equals(MultipleGraphHornRule.END_NODE))
				otherVariable = atom.getSubject();
			if(atom.getSubject().equals(MultipleGraphHornRule.START_NODE) || atom.getObject().equals(MultipleGraphHornRule.START_NODE)){
				Set<RuleAtom> currentAtoms = Sets.newHashSet();
				currentAtoms.add(atom);
				oneSideSubjectQuery = oneSideSubjectQuery.replaceAll("SELECT DISTINCT ", "SELECT DISTINCT ?"+otherVariable+" ");
				oneSideSubjectQuery = oneSideSubjectQuery.substring(0, oneSideSubjectQuery.length()-1)+executor.getHornRuleAtomQuery(currentAtoms)+"}";
				subjectExamples = executor.getKBExamples(oneSideSubjectQuery, "subject", otherVariable,true);
				variableSubject = otherVariable;
				continue;
			}

			if(atom.getSubject().equals(MultipleGraphHornRule.END_NODE) || atom.getObject().equals(MultipleGraphHornRule.END_NODE)){
				Set<RuleAtom> currentAtoms = Sets.newHashSet();
				currentAtoms.add(atom);
				oneSideObjectQuery = oneSideObjectQuery.replaceAll("SELECT DISTINCT ", "SELECT DISTINCT ?"+otherVariable+" ");
				oneSideObjectQuery = oneSideObjectQuery.substring(0, oneSideObjectQuery.length()-1)+executor.getHornRuleAtomQuery(currentAtoms)+"}";
				objectExamples = executor.getKBExamples(oneSideObjectQuery, "object", otherVariable,true);
				continue;
			}
			inequalityAtom = atom;
		}

		//iterate over object examples and create a list of objects without duolicates
		Set<String> seenEntities = Sets.newHashSet();
		List<Entity2ValueContainer> objectList = Lists.newLinkedList();
		for(Pair<String,String> oneObjectExample:objectExamples){
			if(seenEntities.contains(oneObjectExample.getLeft()))
				continue;
			seenEntities.add(oneObjectExample.getLeft());
			//check the value can be parsed
			if(executor.isLiteralNumber(oneObjectExample.getRight()))
				objectList.add(new Entity2ValueContainer(oneObjectExample.getLeft(), oneObjectExample.getRight()));
		}

		//sort the object lists
		Collections.sort(objectList);

		seenEntities.clear();

		String relation = inequalityAtom.getSubject().equals(variableSubject) ? inequalityAtom.getRelation() : 
			SparqlExecutor.getInverseRelation(inequalityAtom.getRelation());
		if(!relation.equals(Constant.GREATER_REL) && !relation.equals(Constant.LESS_EQUAL_REL)&&
				!relation.equals(Constant.LESS_REL) && !relation.equals(Constant.GREATER_EQUAL_REL)){
			LOGGER.warn("Inequality expensive relation is something different from what expected "
					+ "(can be one of the following values: {},{},{}, and {}).",Constant.GREATER_EQUAL_REL,
					Constant.GREATER_REL,Constant.LESS_REL,Constant.LESS_EQUAL_REL);
			return 0;
		}

		int objectSize = objectList.size();
		//iterate over all subject entities
		long totCount = 0;
		for(Pair<String,String> oneExampleSubject:subjectExamples){
			if(seenEntities.contains(oneExampleSubject.getLeft()))
				continue;
			seenEntities.add(oneExampleSubject.getLeft());
			if(executor.isLiteralNumber(oneExampleSubject.getRight())){

				//get the current index
				int index = getListIndex(objectList, oneExampleSubject.getRight(), 0, objectSize);
				if(index >=0){
					if(relation.equals(Constant.GREATER_REL))
						totCount+=index;
					if(relation.equals(Constant.GREATER_EQUAL_REL)){
						//advance the counter until it encounter the first not equal element
						while(index<objectList.size() &&
								SparqlExecutor.compareLiteral(oneExampleSubject.getRight(), objectList.get(index).value)==0){
							index++;
						}

						totCount+=index;
					}
					if(relation.equals(Constant.LESS_EQUAL_REL)){
						totCount+=objectSize-index;
					}
					if(relation.equals(Constant.LESS_REL)){
						//advance the counter until it finds the first not equal element
						while(index<objectList.size() &&
								SparqlExecutor.compareLiteral(oneExampleSubject.getRight(), objectList.get(index).value)==0){
							index++;
						}
						totCount+=objectSize-index;
					}
				}
			}
		}
		totCount-=toBeRemoved;
		if(totCount<0){
			throw new RuleMinerException("Total count of expensive query examples is negative, query: "
					+expensiveRule, LOGGER);
		}

		LOGGER.debug("Query executed and count results updated with {} results.",totCount);
		return totCount;

	}


	/**
	 * Return the index of the first value in the list that is greater or equal than currentValue
	 * In other words return the index j such that the value at j is greater or equal than current value,
	 * and the value at j-1 is less than currentValue
	 * 
	 * It can be solved with a binary search, and then move the pointer to the first occurence of the element (this is necessary only if the collection contains
	 * the searched element)
	 * @param inputList
	 * @param currentValue
	 * @param relation
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	private int getListIndex(List<Entity2ValueContainer> inputList, String currentValue, int startIndex, int endIndex){

		//edge case: start index is on the correct element
		if(endIndex<=startIndex){
			if(SparqlExecutor.compareLiteral(currentValue, inputList.get(startIndex).value)>0)
				return startIndex+1;
			return startIndex;

		}

		//edge cases: first element greater than current value
		if(SparqlExecutor.compareLiteral(currentValue, inputList.get(startIndex).value)<0)
			return startIndex;

		//edge cases: last element smaller than current value
		if(SparqlExecutor.compareLiteral(currentValue, inputList.get(endIndex-1).value)>0)
			return endIndex;

		int middleElement = startIndex+((endIndex-startIndex)/2);

		Integer currentComparison = SparqlExecutor.compareLiteral(currentValue, inputList.get(middleElement).value);
		if(currentComparison==null)
			throw new RuleMinerException("Unable to compare value '"+currentValue+"' with value '"+inputList.get(middleElement).value+"'.", LOGGER);

		//check if middle element is equal
		if(currentComparison==0){
			//need to backtrack until the previous element is not equal
			while(middleElement>0 && SparqlExecutor.compareLiteral(currentValue, inputList.get(middleElement-1).value)==0){
				middleElement--;
			}
			return middleElement;
		}

		//if middle element is greater, inspect the left side of the list
		if(currentComparison<0){
			//special case in which the current middle element is exactly what we want
			if(SparqlExecutor.compareLiteral(currentValue, inputList.get(middleElement-1).value)>=0){	
				while(middleElement>0 && SparqlExecutor.compareLiteral(currentValue, inputList.get(middleElement-1).value)==0)
					middleElement--;
				return middleElement;
			}
			return getListIndex(inputList, currentValue, startIndex, middleElement-1);
		}

		//if middle element is smaller, inspect the right side of the list
		if(currentComparison>0){
			//special case in which the current middle element is exactly what we want
			if(SparqlExecutor.compareLiteral(currentValue, inputList.get(middleElement+1).value)<=0)
				return middleElement+1;
			return getListIndex(inputList, currentValue, middleElement+1, endIndex);
		}

		//case where the two 
		throw new RuleMinerException("Unable to compare value '"+currentValue+"' with value '"+inputList.get(middleElement).value+"'.", LOGGER);


	}


	private class Entity2ValueContainer implements Comparable<Entity2ValueContainer>{
		String entity;
		String value;
		public Entity2ValueContainer(String entity, String value){
			this.entity = entity;
			this.value = value;
		}
		@Override
		public int compareTo(Entity2ValueContainer o) {
			Integer result = SparqlExecutor.compareLiteral(this.value, o.value);
			if(result==null)
				throw new RuleMinerException("Unable to compare value '"+this.value+"' with value '"+o.value+"'.", LOGGER);
			return result;
		}

		public String toString(){
			return entity+"->"+value;
		}
	}

	/**
	 * Execute expensive query of the form r_1(subject,v0), r_2(object,v0).
	 * Only suck=h kind of queries are allowed
	 * @param relations
	 * @param typeSubject
	 * @param typeObject
	 * @param expensiveRule
	 * @return
	 */
	private long executeEqualityExpensiveQuery(Set<String> relations, String typeSubject, String typeObject,Set<RuleAtom> expensiveRule){
		LOGGER.debug("Executing expensive equality query: {}.",expensiveRule);
		if(expensiveRule.size()!=2)
			throw new RuleMinerException("Trying to execute an expensive query that contains more than 2 atoms: "+expensiveRule, LOGGER);


		//frst count the number of examples that respect the expesniveRule, they are to be removed
		String removedExamplesQuery = executor.generateExampleWithRelationsQuery(relations, typeSubject, typeObject);
		removedExamplesQuery = removedExamplesQuery.substring(0,removedExamplesQuery.length()-1) + " "+executor.getHornRuleAtomQuery(expensiveRule)+"}";
		//create count query
		removedExamplesQuery = removedExamplesQuery.replaceAll("SELECT DISTINCT", "SELECT (COUNT(*) AS ?count) {SELECT DISTINCT")+"}";
		int toBeRemoved = executor.executeCountQuery(removedExamplesQuery);

		String oneSideSubjectQuery = executor.generateOneSideExampleQuery(relations, typeSubject, "subject");
		String oneSideObjectQuery = executor.generateOneSideExampleQuery(relations, typeObject, "object");
		//get the atom with subject
		Set<Pair<String,String>> subjectExamples = null;
		Set<Pair<String,String>> objectExamples = null;
		String otherVariable = null;
		for(RuleAtom atom:expensiveRule){
			String currentOtherVariable = null;
			if(atom.getSubject().equals(MultipleGraphHornRule.START_NODE) || atom.getSubject().equals(MultipleGraphHornRule.END_NODE))
				currentOtherVariable = atom.getObject();
			if(atom.getObject().equals(MultipleGraphHornRule.START_NODE) || atom.getObject().equals(MultipleGraphHornRule.END_NODE))
				currentOtherVariable = atom.getSubject();
			if(currentOtherVariable==null)
				throw new RuleMinerException("Trying to execute an unknown format expensive query: "+expensiveRule, LOGGER);
			if(otherVariable!=null && !otherVariable.equals(currentOtherVariable))
				throw new RuleMinerException("Trying to execute an unknown format expensive query: "+expensiveRule, LOGGER);

			otherVariable = currentOtherVariable;
			if(atom.getSubject().equals(MultipleGraphHornRule.START_NODE) || atom.getObject().equals(MultipleGraphHornRule.START_NODE)){
				Set<RuleAtom> currentAtoms = Sets.newHashSet();
				currentAtoms.add(atom);
				oneSideSubjectQuery = oneSideSubjectQuery.replaceAll("SELECT DISTINCT ", "SELECT DISTINCT ?"+otherVariable+" ");
				oneSideSubjectQuery = oneSideSubjectQuery.substring(0, oneSideSubjectQuery.length()-1)+executor.getHornRuleAtomQuery(currentAtoms)+"}";
				subjectExamples = executor.getKBExamples(oneSideSubjectQuery, "subject", otherVariable,true);
				continue;
			}

			if(atom.getSubject().equals(MultipleGraphHornRule.END_NODE) || atom.getObject().equals(MultipleGraphHornRule.END_NODE)){
				Set<RuleAtom> currentAtoms = Sets.newHashSet();
				currentAtoms.add(atom);
				oneSideObjectQuery = oneSideObjectQuery.replaceAll("SELECT DISTINCT ", "SELECT DISTINCT ?"+otherVariable+" ");
				oneSideObjectQuery = oneSideObjectQuery.substring(0, oneSideObjectQuery.length()-1)+executor.getHornRuleAtomQuery(currentAtoms)+"}";
				objectExamples = executor.getKBExamples(oneSideObjectQuery, "object", otherVariable,true);
				continue;
			}
		}

		//iterate over object examples and create a map where key is otherVariable and values are all object having that value
		Map<String,Set<String>> value2object = Maps.newHashMap();
		for(Pair<String,String> oneObjectExample:objectExamples){
			String object = oneObjectExample.getLeft();
			String value = oneObjectExample.getRight();

			Set<String> currentObjects = value2object.get(value);
			if(currentObjects==null){
				currentObjects = Sets.newHashSet();
				value2object.put(value, currentObjects);
			}
			currentObjects.add(object);
		}

		//create map for the subjects
		Map<String,Set<String>> subject2value = Maps.newHashMap();
		for(Pair<String,String> oneSubjectExample:subjectExamples){
			String subject = oneSubjectExample.getLeft();
			String value = oneSubjectExample.getRight();

			Set<String> currentValues = subject2value.get(subject);
			if(currentValues==null){
				currentValues = Sets.newHashSet();
				subject2value.put(subject, currentValues);
			}
			currentValues.add(value);
		}

		long totCount = 0;


		//count for each subject the corresponding objects
		for(String subject:subject2value.keySet()){
			Set<String> currentObjects = Sets.newHashSet();
			for(String value:subject2value.get(subject)){
				if(value2object.get(value)!=null)
					currentObjects.addAll(value2object.get(value));
			}

			//remove the current subject
			currentObjects.remove(subject);
			totCount += currentObjects.size();
		}

		totCount-=toBeRemoved;
		if(totCount<0){
			throw new RuleMinerException("Total count of expensive query examples is negative, query: "
					+expensiveRule, LOGGER);
		}

		LOGGER.debug("Query executed and count results updated with {} results.",totCount);
		return totCount;

	}

}
