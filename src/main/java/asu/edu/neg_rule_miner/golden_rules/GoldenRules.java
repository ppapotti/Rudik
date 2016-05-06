package asu.edu.neg_rule_miner.golden_rules;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;
import asu.edu.neg_rule_miner.sparql.jena.*;
import asu.edu.neg_rule_miner.sparql.jena.remote.*;
import asu.edu.neg_rule_miner.model.MultipleGraphHornRule;
import asu.edu.neg_rule_miner.model.RuleAtom;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.sparql.jena.QueryJenaLibrary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;

public class GoldenRules {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String fileName = "/Users/vmeduri/Documents/rules/yagoIsMarriedTo_restrictive.xlsx";
		//String fileName = "/Users/vmeduri/Documents/rules/dbpediaFounder_union_rules.csv";
		String readFileName = "rulesSample";
		String writeFileName = "negExamples";
		String typeSubject = "http://dbpedia.org/ontology/Organisation";
		String typeObject = "http://dbpedia.org/ontology/Person";
		BufferedWriter bw = null;
		//String fileName = args[0];
		
		//String subjectType = args[1];
		//String objectType = args[2];
		BufferedReader br = null;
		Scanner scan = new Scanner(System.in);
		Set<Pair<String,String>> globalResultPairs = Sets.newHashSet();
		Set<Pair<String,String>> resultPairs;
		int setSize = 0;
		try{
			br = new BufferedReader(new FileReader(readFileName));
			String line;
			bw = new BufferedWriter(new FileWriter(writeFileName));
			while((line=br.readLine())!=null){
				Set<RuleAtom> rules = MultipleGraphHornRule.readHornRule(line);
				System.out.println("Is this query semantically meaningful? Enter yes or no: \n "+rules);
				String response = scan.nextLine();
				if (response.equals("no"))
					continue;
				
				resultPairs = null;
				while(resultPairs == null){
					SparqlExecutor exec = ConfigurationFacility.getSparqlExecutor();
					try{
						resultPairs= exec.executeHornRuleQuery(rules, typeSubject, typeObject);
					}
					catch(Exception e){
						//deal with the exception
					}
				}
				
				
				globalResultPairs.addAll(resultPairs);
				if (globalResultPairs.size() == setSize)
					continue;
				setSize = globalResultPairs.size();
				for(Pair<String,String> onePair:resultPairs){
					String subject = onePair.getLeft();
					String object = onePair.getRight();
					bw.write(subject+"\t"+object+"\n");
				}
				bw.flush();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		scan.close();
		
	}

}

//select ?x ?y where {?x <http://yago-knowledge.org/resource/hasChild> ?y}
//or
//prefix yago:<http://yago-knowledge.org/resource/>
//select ?x ?y where {?x yago:hasChild ?y}

/*
* [http://yago-knowledge.org/resource/wasBornOnDate(object,v0), <=(v0,v1), http://yago-knowledge.org/resource/diedOnDate(subject,v1)]
FILTER (?v0>=?v1)
?subject <http://yago-knowledge.org/resource/diedOnDate> ?v1.
?object <http://yago-knowledge.org/resource/diedOnDate> ?v0.
*/

/*
* [http://yago-knowledge.org/resource/isMarriedTo(v0,subject), http://yago-knowledge.org/resource/hasChild(subject,object), http://yago-knowledge.org/resource/isMarriedTo(subject,v0)]
* ?v0 <http://yago-knowledge.org/resource/isMarriedTo> ?subject.
* ?subject <http://yago-knowledge.org/resource/hasChild> ?object.
* ?subject <http://yago-knowledge.org/resource/isMarriedTo> ?v0.
*
*/