package asu.edu.neg_rule_miner.client.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.model.horn_rule.HornRule;
import asu.edu.neg_rule_miner.model.horn_rule.RuleAtom;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;
import asu.edu.neg_rule_miner.sparql.jena.QueryJenaLibrary;

public class FinalEvaluation {

	private final static Logger LOGGER = LoggerFactory.getLogger(FinalEvaluation.class.getName());

	private static final String EVALUATION_FOLDER = "/Users/sortona/Documents/KDR/evaluation";



	/**
	 * ruleFile contains output negative rule in the following format:
	 * targetRelation <tab> rule <tab> true|false 
	 * True or False means the semantic correcteness of the rule
	 * 
	 * It outputs a file per relation called with the relation name, where each file contains errors produced by the rule
	 * @param rulesFile
	 * @throws IOException 
	 */
	public static void computeNegativeErrors(File rulesFile) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(rulesFile));
		Map<String,Map<Set<RuleAtom>,Boolean>> relation2rules = Maps.newHashMap();

		String line;
		while((line = reader.readLine()) != null){
			String []lineSplit = line.split("\t");
			if(lineSplit.length!=3)
				continue;
			String targetRelation = lineSplit[0];
			Map<Set<RuleAtom>,Boolean> currentMap = relation2rules.get(targetRelation);
			if(currentMap==null){
				currentMap = Maps.newHashMap();
				relation2rules.put(targetRelation, currentMap);
			}
			currentMap.put(HornRule.readHornRule(lineSplit[1]), Boolean.parseBoolean(lineSplit[2]));
		}
		reader.close();


		SparqlExecutor executor = ConfigurationFacility.getSparqlExecutor();

		for(String targetRelation:relation2rules.keySet()){
			LOGGER.debug("Evaluating numbers for relation {}...",targetRelation);
			//first execute true rules
			Set<Pair<String,String>> seenExamples = Sets.newHashSet();
			String head = "Rule ";

			for(Set<RuleAtom> oneRule:relation2rules.get(targetRelation).keySet()){
				if(!relation2rules.get(targetRelation).get(oneRule))
					continue;

				String query ="SELECT DISTINCT ?subject ?object ";
				if(executor.getGraphIri()!=null)
					query+="FROM "+executor.getGraphIri()+" ";
				query+="WHERE {?subject <"+targetRelation+"> ?object. "+executor.getHornRuleAtomQuery(oneRule)+" }";


				Set<Pair<String,String>> currentExamples = executor.getKBExamples(query, "?subject", "?object", true);
				LOGGER.debug("Rule {} retrieved {} examples.",oneRule,currentExamples.size());
				seenExamples.addAll(currentExamples);
				head+="'"+oneRule+"' AND ";

			}
			if(head.equals("Rule "))
				head="";
			else
				head=head.substring(0,head.length()-4)+"produced:\n";

			LOGGER.debug("Retrieved a total of {} valid errors.",seenExamples.size());
			//write the examples
			String fileName = targetRelation.substring(targetRelation.lastIndexOf("/")+1);
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(rulesFile.getAbsolutePath().replace(rulesFile.getName(), 
					fileName))));
			writer.write(head);
			for(Pair<String,String> oneExample:seenExamples)
				writer.write(oneExample.getLeft()+"\t"+oneExample.getRight()+"\ttrue\n");

			//execute unknown rules

			for(Set<RuleAtom> oneRule:relation2rules.get(targetRelation).keySet()){
				if(relation2rules.get(targetRelation).get(oneRule))
					continue;

				writer.write("Rule '"+oneRule+"' produced:\n");

				//get the variables
				Set<String> seenVariables = Sets.newHashSet();
				List<String> variables = Lists.newArrayList();
				for(RuleAtom oneAtom:oneRule){
					if(!seenVariables.contains(oneAtom.getSubject())){
						variables.add(oneAtom.getSubject());
						seenVariables.add(oneAtom.getSubject());
						writer.write(oneAtom.getSubject()+" ");
					}
					if(!seenVariables.contains(oneAtom.getObject())){
						variables.add(oneAtom.getObject());
						seenVariables.add(oneAtom.getObject());
						writer.write(oneAtom.getObject()+" ");
					}

				}
				writer.write("\n");

				String query ="SELECT DISTINCT * ";
				if(executor.getGraphIri()!=null)
					query+="FROM "+executor.getGraphIri()+" ";
				query+="WHERE {?subject <"+targetRelation+"> ?object. "+executor.getHornRuleAtomQuery(oneRule)+" }";

				ResultSet results = ((QueryJenaLibrary)executor).executeQuery(query);

				int totRes = 0;
				while(results.hasNext()){
					QuerySolution oneResult = results.next();
					String object = oneResult.get("object").toString();
					//check object is not a literal
					if(oneResult.get("object").isLiteral())
						object = oneResult.get("object").asLiteral().getLexicalForm();

					String subject = oneResult.get("subject").toString();
					if(seenExamples.contains(Pair.of(subject, object)))
						continue;
					seenExamples.add(Pair.of(subject, object));
					for(String oneVariable:variables){
						writer.write(oneResult.get(oneVariable).toString()+"\t");
					}
					writer.write("\n");
					totRes++;

				}
				LOGGER.debug("Rule {} retrieved {} examples.",oneRule,totRes);

				((QueryJenaLibrary)executor).closeResources();


			}
			writer.close();

			LOGGER.debug("Relation {} produced a total number {} of potential errors.",targetRelation,seenExamples.size());

		}

	}

	public static void computeNegativeErrorsWikidata() throws IOException{
		File rulesFile = new File(EVALUATION_FOLDER+"/wikidata/final_results/negative/output_rules");
		computeNegativeErrors(rulesFile);
	}

	public static void computeNegativeErrorsDBPedia() throws IOException{
		File rulesFile = new File(EVALUATION_FOLDER+"/dbpedia/final_results/negative/output_rules");
		computeNegativeErrors(rulesFile);
	}

	public static void computeNegativeErrorsYago() throws IOException{
		File rulesFile = new File(EVALUATION_FOLDER+"/yago/final_results/negative/output_rules");
		computeNegativeErrors(rulesFile);
	}

	public static void main(String[] args) throws Exception{
		computeNegativeErrorsYago();
	}

}
