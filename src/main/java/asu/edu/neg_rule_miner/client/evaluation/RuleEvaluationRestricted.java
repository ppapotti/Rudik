package asu.edu.neg_rule_miner.client.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.model.horn_rule.MultipleGraphHornRule;
import asu.edu.neg_rule_miner.model.horn_rule.RuleAtom;

public class RuleEvaluationRestricted extends RuleEvaluation{

	private final static Logger LOGGER = LoggerFactory.getLogger(RuleEvaluationRestricted.class.getName());

	public RuleEvaluationRestricted(String inputFolder){
		super(inputFolder);
	}

	private void generateSilverNegativeExamples(String relationName,Set<String> relations, String typeSubject, String typeObject) throws IOException{
		//check examples have already been created
		if(relationName.contains("_"))
			relationName = relationName.substring(0,relationName.indexOf("_"));
		File silverNegativeRestrictedFile = new File(inputFolder+"/"+SILVER_NEGATIVE_EX_FOLDER+"/"+relationName+"_restricted");
		if(silverNegativeRestrictedFile.exists())
			return;
		LOGGER.debug("Generating silver negative restricted examples for {} relation...",relationName);
		Set<Pair<String,String>> negativeExamples = executor.getKBExamples(
				executor.generateNegativeExampleRestrictedQuery(relations, typeSubject, typeObject, switchSubjectObject), "subject", "object",true);
		BufferedWriter writer = new BufferedWriter(new FileWriter(silverNegativeRestrictedFile));

		for(Pair<String,String> oneExample:negativeExamples){
			writer.write(oneExample.getLeft()+"\t"+oneExample.getRight()+"\n");
		}
		writer.close();
		LOGGER.debug("Restricted negative examples generated and saved on file {}.",silverNegativeRestrictedFile.getAbsolutePath());

	}

	private void generateOutputRulesExamples(String relationName,
			Set<String> relations, String typeSubject, String typeObject) throws IOException{
		//check examples have already been created
		File outputRulesRestrictedFile = new File(inputFolder+"/"+INDUCED_RULES_EX_FOLDER+"/"+relationName+"_restricted");
		if(outputRulesRestrictedFile.exists())
			return;
		LOGGER.debug("Generating output rules restricted examples for {} relation...",relationName);
		Set<Pair<String,String>> outputRulesRestrictedExamples = Sets.newHashSet();

		String positiveExampleQuery = executor.generatePositiveExampleQuery(relations, typeSubject, typeObject);

		String negativeRestrictedExamplesQuery = executor.generateNegativeExampleRestrictedQuery(relations, typeSubject, typeObject,switchSubjectObject);

		//read input rules
		BufferedReader rulesReader = new BufferedReader(new FileReader(new File(inputFolder+"/"+INDUCED_RULES_FOLDER+"/"+relationName)));
		String line;
		while((line = rulesReader.readLine())!=null){
			//each line is on the format: 'type\trule'. Type is not used in the restricted settings
			Set<RuleAtom> currentRule = MultipleGraphHornRule.readHornRule(line.split("\t")[1]);
			String currentRuleFilter = executor.getHornRuleAtomQuery(currentRule);
			//excute the rule over restricted negative examples
			outputRulesRestrictedExamples.addAll(executor.getKBExamples(negativeRestrictedExamplesQuery.substring(0, negativeRestrictedExamplesQuery.length()-1)+" "
					+currentRuleFilter+"}","subject","object",true));
			//excute the rule over positive examples
			outputRulesRestrictedExamples.addAll(executor.getKBExamples(positiveExampleQuery.substring(0, positiveExampleQuery.length()-1)+" "
					+currentRuleFilter+"}","subject","object",true));
		}
		rulesReader.close();

		BufferedWriter writer = new BufferedWriter(new FileWriter(outputRulesRestrictedFile));

		for(Pair<String,String> oneExample:outputRulesRestrictedExamples){
			writer.write(oneExample.getLeft()+"\t"+oneExample.getRight()+"\n");
		}
		writer.close();
		LOGGER.debug("Restricted output rules examples generated and saved on file {}.",outputRulesRestrictedFile.getAbsolutePath());
	}

	/**
	 * Generate silver positive examples, silver negative examples, silver unknonw positive examples and induced rules examples on the restricted settings
	 */
	@Override
	public void generateSilverExamples(String relationName,
			Set<String> relations, String typeSubject, String typeObject) throws IOException{
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
		this.generateSilverNegativeExamples(relationName, relations, typeSubject, typeObject);
		this.generateOutputRulesExamples(relationName, relations, typeSubject, typeObject);
		LOGGER.debug("Queries executed and files saved for relation {}.",relationName);

	}

	@Override
	public void updatePrecisionRecallCounter(String relationName, PrecisionRecallCountContainer container) throws IOException{
		
		String originalRelationName = relationName;
		if(relationName.contains("_"))
			originalRelationName = relationName.substring(0,relationName.indexOf("_"));
		LOGGER.debug("Reading input silver examples file and updating precision and recall container for relation {} restricted.",relationName);
		//positive examples
		Set<Pair<String,String>> positiveExamples = readExamples(inputFolder+"/"+SILVER_POSITIVE_EX_FOLDER+"/"+originalRelationName);

		//negative examples
		Set<Pair<String,String>> negativeExamples = readExamples(inputFolder+"/"+SILVER_NEGATIVE_EX_FOLDER+"/"+originalRelationName+"_restricted");

		//silver positive unknown
		Set<Pair<String,String>> silverPositiveUnknonwExamples = readExamples(inputFolder+"/"+UNKNOWN_SILVER_POSITIVE_EX_FOLDER+"/"+originalRelationName);

		//output rules examples
		Set<Pair<String,String>> outputRulesExamples = readExamples(inputFolder+"/"+INDUCED_RULES_EX_FOLDER+"/"+relationName+"_restricted");

		//correct induced examples
		Set<Pair<String,String>> correctOutputRulesExamples = Sets.newHashSet();
		correctOutputRulesExamples.addAll(outputRulesExamples);
		correctOutputRulesExamples.retainAll(negativeExamples);

		//unknown induced examples
		Set<Pair<String,String>> unknownOutputRulesExamples = Sets.newHashSet();
		unknownOutputRulesExamples.addAll(outputRulesExamples);
		unknownOutputRulesExamples.retainAll(silverPositiveUnknonwExamples);

		//wrong induced examples
		Set<Pair<String,String>> wrongOutputRulesExamples = Sets.newHashSet();
		wrongOutputRulesExamples.addAll(outputRulesExamples);
		wrongOutputRulesExamples.retainAll(positiveExamples);
		wrongOutputRulesExamples.removeAll(silverPositiveUnknonwExamples);
		LOGGER.debug("Wrong extracted examples: "+wrongOutputRulesExamples);

		container.ruleCorrectExamplesCount+=correctOutputRulesExamples.size();
		container.ruleExamplesCount+=outputRulesExamples.size();
		container.ruleUnknownCount+=unknownOutputRulesExamples.size();
		container.ruleWrongExamplesRuleCount+=wrongOutputRulesExamples.size();
		container.totalSivlerNegativeCount+=negativeExamples.size();
		LOGGER.debug("Relation {} restricted processed and precision and recall container updated.",relationName);


	}

}
