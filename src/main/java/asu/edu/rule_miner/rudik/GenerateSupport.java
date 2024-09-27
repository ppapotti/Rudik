package asu.edu.rule_miner.rudik;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Sets;

import asu.edu.rule_miner.rudik.configuration.ConfigurationFacility;
import asu.edu.rule_miner.rudik.model.horn_rule.HornRule;
import asu.edu.rule_miner.rudik.model.horn_rule.RuleAtom;
import asu.edu.rule_miner.rudik.predicate.analysis.KBPredicateSelector;
import asu.edu.rule_miner.rudik.predicate.analysis.SparqlKBPredicateSelector;
import asu.edu.rule_miner.rudik.rule_generator.DynamicPruningRuleDiscovery;

public class GenerateSupport {

    public static void main(String[] args) {
    	String rudik_config = "src/main/config/DbPediaConfiguration.xml";
		
		if (args.length == 1) {
			rudik_config = args[0];
			System.out.println("Reading configuration from: " + rudik_config);
		}
		
		ConfigurationFacility.setConfigurationFile(rudik_config);
		
		KBPredicateSelector kbAnalysis = new SparqlKBPredicateSelector();
    	DynamicPruningRuleDiscovery naive = new DynamicPruningRuleDiscovery();
		
    	String predicate = "http://dbpedia.org/ontology/spouse";
    	String premise = "http://dbpedia.org/ontology/spouse(object,subject)";
    	Boolean rule_type = true;
         
    	Pair<String, String> subjectObjectType = kbAnalysis.getPredicateTypes(predicate);
        String typeSubject = subjectObjectType.getLeft();
        String typeObject = subjectObjectType.getRight();
        Set<String> set_relations = Sets.newHashSet(predicate);
        Set<RuleAtom> rule_atom = HornRule.readHornRule(premise);
        
        Double support = naive.getRuleConfidence(rule_atom, set_relations, typeSubject, typeObject, rule_type);
        
        System.out.println("predicate: " + predicate);
        System.out.println("premise: " + premise);
        System.out.println("score: " + support);
    }
}
