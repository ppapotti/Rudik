package asu.edu.rule_miner.rudik;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Sets;

import asu.edu.rule_miner.rudik.model.horn_rule.HornRule;
import asu.edu.rule_miner.rudik.model.horn_rule.RuleAtom;
import asu.edu.rule_miner.rudik.rule_generator.DynamicPruningRuleDiscovery;

public class confidence_test {

	public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/spouse");
	// public static final String typeSubject =
	// "http://dbpedia.org/ontology/Person";
	public static final String typeObject = null;
	public static final String typeSubject = null;
	//public static final String typeSubject = null; 

	public static void main(String[] args) throws IOException {

		Map<HornRule, Double> test = new HashMap<HornRule, Double>();

		DynamicPruningRuleDiscovery naive = new DynamicPruningRuleDiscovery();

		
		  BufferedReader in = new BufferedReader(new FileReader("test_rules.txt"));
		  String line = in.readLine(); while (line != null) { HornRule Rule = new
		  HornRule (); String [] rules = line.split("&"); for (String rule: rules) {
		  String [] parts = rule.trim().split("\\("); String [] new_ps =
		  parts[1].split(","); Rule.addRuleAtom(new RuleAtom (new_ps[0], parts[0],
		  new_ps[1].replace(")", "")));
		  
		  } //System.out.println(Rule);
		  
		  double conf = naive.getRuleConfidence(Rule.getRules(), relations,typeSubject, typeObject, false); 
		  test.put(Rule, conf); System.out.println(Rule
		  + "\t" + conf); line = in.readLine(); }
		  
		  
		  for (final HornRule oneRule1 : test.keySet())
		  System.out.println(test.toString() + "\t \t" + test.get(oneRule1));
		  
		 
		//RuleAtom r6 = new RuleAtom("subject", "http://dbpedia.org/ontology/school", "object");
		// RuleAtom r7 = new RuleAtom("subject",
		// "http://dbpedia.org/ontology/foundedBy", "v0");
		// RuleAtom r8 = new RuleAtom("v0", "http://dbpedia.org/ontology/product",
		// "v1");
		// RuleAtom r9 = new RuleAtom("object", "http://dbpedia.org/ontology/product",
		// "v1");
		// RuleAtom r10 = new le (RuleAtom("v1", ">", "v0");
		// RuleAtom r11 = new RuleAtom("subject",
		// "http://dbpedia.org/ontology/production", "v0");

		// RuleAtom r7 = new RuleAtom("v0", "!=", "v1");
		// RuleAtom r8 = new RuleAtom("subject", "http://dbpedia.org/ontology/spouse",
		// "v1");
		//HornRule Rule4 = new HornRule();
		//Rule4.addRuleAtom(r6);
		// Rule4.addRuleAtom(r7);
		// Rule4.addRuleAtom(r8);
		// Rule4.addRuleAtom(r9);
		// Rule4.addRuleAtom(r10);
		// Rule4.addRuleAtom(r11);

		//double conf4 = naive.getRuleConfidence(Rule4.getRules(), relations, typeSubject, typeObject, false);
		//System.out.println(Rule4 + "\t \t " + conf4);

		/*
		 * RuleAtom r2 = new RuleAtom("subject", "http://dbpedia.org/ontology/relative",
		 * "object"); RuleAtom r4 = new RuleAtom("object",
		 * "http://dbpedia.org/ontology/occupation", "v0"); HornRule Rule2 = new
		 * HornRule (); Rule2.addRuleAtom(r2); Rule2.addRuleAtom(r4); double conf2 =
		 * naive.getRuleConfidence(Rule2.getRules(), relations, typeSubject, typeObject,
		 * false); System.out.println(Rule2 + "\t \t " + conf2);
		 * 
		 * 
		 * 
		 * 
		 * 
		 * //negative rules RuleAtom r1 = new RuleAtom("subject",
		 * "http://dbpedia.org/ontology/parent", "object"); HornRule Rule1 = new
		 * HornRule (); Rule1.addRuleAtom(r1); double conf1 =
		 * naive.getRuleConfidence(Rule1.getRules(), relations, typeSubject, typeObject,
		 * false);
		 * 
		 * 
		 * 
		 * RuleAtom r2 = new RuleAtom("subject",
		 * "http://dbpedia.org/ontology/birthPlace", "v0"); RuleAtom r3 = new
		 * RuleAtom("v1", "http://dbpedia.org/ontology/country", "v0"); RuleAtom r4 =
		 * new RuleAtom("object", "http://dbpedia.org/ontology/child", "v1"); HornRule
		 * Rule2 = new HornRule (); Rule2.addRuleAtom(r2); Rule2.addRuleAtom(r3);
		 * Rule2.addRuleAtom(r4); double conf2 =
		 * naive.getRuleConfidence(Rule2.getRules(), relations, typeSubject, typeObject,
		 * false);
		 * 
		 * 
		 * RuleAtom r5 = new RuleAtom("subject",
		 * "http://dbpedia.org/ontology/successor", "object"); HornRule Rule3 = new
		 * HornRule (); Rule3.addRuleAtom(r5); double conf3 =
		 * naive.getRuleConfidence(Rule3.getRules(), relations, typeSubject, typeObject,
		 * false);
		 * 
		 * 
		 * RuleAtom r6 = new RuleAtom("v0", "http://dbpedia.org/ontology/spouse",
		 * "object"); RuleAtom r7 = new RuleAtom("v0", "!=", "v1"); RuleAtom r8 = new
		 * RuleAtom("subject", "http://dbpedia.org/ontology/spouse", "v1"); HornRule
		 * Rule4 = new HornRule (); Rule4.addRuleAtom(r6); Rule4.addRuleAtom(r7);
		 * Rule4.addRuleAtom(r8); double conf4 =
		 * naive.getRuleConfidence(Rule4.getRules(), relations, typeSubject, typeObject,
		 * false);
		 * 
		 * RuleAtom r9 = new RuleAtom("object", "http://dbpedia.org/ontology/spouse",
		 * "v0"); RuleAtom r10 = new RuleAtom("subject",
		 * "http://dbpedia.org/ontology/parent", "v0"); HornRule Rule5 = new HornRule
		 * (); Rule5.addRuleAtom(r9); Rule5.addRuleAtom(r10); double conf5 =
		 * naive.getRuleConfidence(Rule5.getRules(), relations, typeSubject, typeObject,
		 * false);
		 * 
		 * 
		 * 
		 * 
		 * //positive rules RuleAtom p1 = new RuleAtom("v0",
		 * "http://dbpedia.org/ontology/parent", "subject"); RuleAtom p2 = new
		 * RuleAtom("v0", "http://dbpedia.org/ontology/parent", "object"); HornRule
		 * Rule6 = new HornRule (); Rule6.addRuleAtom(p1); Rule6.addRuleAtom(p2); double
		 * conf6 = naive.getRuleConfidence(Rule6.getRules(), relations, typeSubject,
		 * typeObject, true);
		 * 
		 * 
		 * RuleAtom p3 = new RuleAtom("object", "http://dbpedia.org/ontology/spouse",
		 * "subject"); HornRule Rule7 = new HornRule (); Rule7.addRuleAtom(p3); double
		 * conf7 = naive.getRuleConfidence(Rule7.getRules(), relations, typeSubject,
		 * typeObject, true);
		 * 
		 * 
		 * RuleAtom p4 = new RuleAtom("v0", "http://dbpedia.org/ontology/spouse",
		 * "object"); RuleAtom p5 = new RuleAtom("v0",
		 * "http://dbpedia.org/ontology/birthDate", "v1"); RuleAtom p6 = new
		 * RuleAtom("subject", "http://dbpedia.org/ontology/birthDate", "v1"); HornRule
		 * Rule8 = new HornRule (); Rule8.addRuleAtom(p4); Rule8.addRuleAtom(p5);
		 * Rule8.addRuleAtom(p6); double conf8 =
		 * naive.getRuleConfidence(Rule8.getRules(), relations, typeSubject, typeObject,
		 * true);
		 * 
		 * 
		 * RuleAtom p7 = new RuleAtom("subject",
		 * "http://dbpedia.org/ontology/deathPlace", "v0"); RuleAtom p8 = new
		 * RuleAtom("v1", "http://dbpedia.org/ontology/birthPlace", "v0"); RuleAtom p9 =
		 * new RuleAtom("v1", "http://dbpedia.org/ontology/parent", "object"); HornRule
		 * Rule9 = new HornRule (); Rule9.addRuleAtom(p7); Rule9.addRuleAtom(p8);
		 * Rule9.addRuleAtom(p9); double conf9 =
		 * naive.getRuleConfidence(Rule9.getRules(), relations, typeSubject, typeObject,
		 * true);
		 * 
		 * 
		 * 
		 * System.out.println(Rule1 + "\t\t" + conf1); System.out.println(Rule2 + "\t\t"
		 * + conf2); System.out.println(Rule3 + "\t\t" + conf3);
		 * System.out.println(Rule4 + "\t\t" + conf4); System.out.println(Rule5 + "\t\t"
		 * + conf5); System.out.println(Rule6 + "\t\t" + conf6);
		 * System.out.println(Rule7 + "\t\t" + conf7); System.out.println(Rule8 + "\t\t"
		 * + conf8); System.out.println(Rule9 + "\t\t" + conf9);
		 */

	}

}
