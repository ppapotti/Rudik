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

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.model.horn_rule.MultipleGraphHornRule;
import asu.edu.neg_rule_miner.model.horn_rule.RuleAtom;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public abstract class RuleEvaluation {
	private final static Logger LOGGER = LoggerFactory.getLogger(RuleEvaluationRestricted.class.getName());

	//silver positive
	protected static String SILVER_POSITIVE_EX_FOLDER = "silver_positive_examples";
	//sivler positive examples known to be wrong
	protected static String UNKNOWN_SILVER_POSITIVE_EX_FOLDER = "unkown_silver_positive_examples";
	//silver negative rules to generate unknown examples
	protected static String SILVER_NEGATIVE_RULES_FOLDER = "silver_negative_rules";
	//silver negative examples, restricted shows the restricted version
	protected static String SILVER_NEGATIVE_EX_FOLDER= "silver_negative_examples";
	//induced rules folder
	protected static String INDUCED_RULES_FOLDER= "induced_rules";
	//induced rules examples, restricted shows the restricted version
	protected static String INDUCED_RULES_EX_FOLDER= "induced_rules_examples";

	protected String inputFolder;

	protected SparqlExecutor executor;

	/**
	 * Switch relation between subject and object when generating negative examples
	 */
	protected boolean switchSubjectObject = false;

	public RuleEvaluation(String inputFolder){
		this.inputFolder = inputFolder;
		executor = ConfigurationFacility.getSparqlExecutor();

	}

	public void setSwitchSubjectObject(boolean switchSubjectObject){
		this.switchSubjectObject = switchSubjectObject;
	}


	public void generatePositiveExample(String relationName, Set<String> relations, String typeSubject, String typeObject) throws IOException{
		if(relationName.contains("_"))
			relationName = relationName.substring(0,relationName.indexOf("_"));
		//check examples have already been created
		File silverPositiveFile = new File(inputFolder+"/"+SILVER_POSITIVE_EX_FOLDER+"/"+relationName);
		if(silverPositiveFile.exists())
			return;
		LOGGER.debug("Generating silver positive examples for {} relation...",relationName);

		Set<Pair<String,String>> positiveExamples = executor.generatePositiveExamples(relations, typeSubject, typeObject);
		BufferedWriter writer = new BufferedWriter(new FileWriter(silverPositiveFile));

		for(Pair<String,String> oneExample:positiveExamples){
			writer.write(oneExample.getLeft()+"\t"+oneExample.getRight()+"\n");
		}
		writer.close();
		LOGGER.debug("Positive examples generated and saved on file {}.",silverPositiveFile.getAbsolutePath());
	}

	public void generateUnknownPositiveExamples(String relationName,Set<String> relations, String typeSubject, String typeObject) throws IOException{
		if(relationName.contains("_"))
			relationName = relationName.substring(0,relationName.indexOf("_"));
		//check examples have already been created
		File unknownSilverPositiveFile = new File(inputFolder+"/"+UNKNOWN_SILVER_POSITIVE_EX_FOLDER+"/"+relationName);
		if(unknownSilverPositiveFile.exists())
			return;
		LOGGER.debug("Generating unknown silver positive examples for {} relation...",relationName);

		//get positive examples query
		String positiveExampleQuery = executor.generatePositiveExampleQuery(relations, typeSubject, typeObject);

		Set<Pair<String,String>> unknownPositiveExamples = Sets.newHashSet();
		//read silver negative rules
		BufferedReader rulesReader = new BufferedReader(new FileReader(new File(inputFolder+"/"+SILVER_NEGATIVE_RULES_FOLDER+"/"+relationName)));
		String line;
		while((line=rulesReader.readLine())!=null){
			//each line is a rule
			Set<RuleAtom> currentRule = MultipleGraphHornRule.readHornRule(line);
			//add curren rule filters
			unknownPositiveExamples.addAll(executor.getKBExamples(positiveExampleQuery.substring(0, positiveExampleQuery.length()-1)
					+" "+executor.getHornRuleAtomQuery(currentRule)+"}","subject","object",true));
		}
		rulesReader.close();

		BufferedWriter writer = new BufferedWriter(new FileWriter(unknownSilverPositiveFile));

		for(Pair<String,String> oneExample:unknownPositiveExamples){
			writer.write(oneExample.getLeft()+"\t"+oneExample.getRight()+"\n");
		}
		writer.close();
		LOGGER.debug("Unknown positive examples generated and saved on file {}.",unknownSilverPositiveFile.getAbsolutePath());
	}

	/**
	 * Generate silver positive examples, silver negative examples, silver unknonw positive examples and induced rules examples on the restricted settings
	 */
	public abstract void generateSilverExamples(String relationName,
			Set<String> relations, String typeSubject, String typeObject) throws IOException;

	public abstract void updatePrecisionRecallCounter(String relationName, PrecisionRecallCountContainer container) throws IOException;



	protected Set<Pair<String,String>> readExamples(String inputFileName) throws IOException{
		BufferedReader examplesReader = new BufferedReader(new FileReader(new File(inputFileName)));

		String line;
		Set<Pair<String,String>> examples = Sets.newHashSet();
		while((line = examplesReader.readLine())!=null){
			if(line.length()>0&&line.contains("\t"))
				examples.add(Pair.of(line.split("\t")[0],line.split("\t")[1]));
		}
		examplesReader.close();
		return examples;
	}

}
