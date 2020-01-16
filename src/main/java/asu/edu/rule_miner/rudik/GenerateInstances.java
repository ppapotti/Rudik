package asu.edu.rule_miner.rudik;

import asu.edu.rule_miner.rudik.api.RudikApi;
import asu.edu.rule_miner.rudik.api.model.HornRuleInstantiation;
import asu.edu.rule_miner.rudik.api.model.HornRuleResult;
import asu.edu.rule_miner.rudik.api.model.RudikResult;
import asu.edu.rule_miner.rudik.model.horn_rule.HornRule;
import asu.edu.rule_miner.rudik.model.horn_rule.RuleAtom;

import org.bson.Document;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class GenerateInstances {

    public static String computeHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
    	String rudik_config = "src/main/config/DbpediaConfiguration.xml";
        int max_instances = 5;

        //store the atoms to use them to construct the graph
        Map<String, List<RuleAtom>> rulesAtomsDict = new HashMap<>();
        Map<String, String> rules_entities_dict = new HashMap<>();
        List<String> returnResult = new ArrayList<>();
        
        String predicate = "http://dbpedia.org/ontology/spouse(subject,object)";
        Boolean rule_type = true;
        String rule_premise = "http://dbpedia.org/ontology/spouse(object,subject)";
        
        HornRuleResult.RuleType type = null;
        if (rule_type) {
            type = HornRuleResult.RuleType.positive;
        } else {
            type = HornRuleResult.RuleType.negative;
        }

        HornRule horn_rule = HornRule.createHornRule(rule_premise);
        System.out.println(horn_rule);

        RudikApi API = new RudikApi(rudik_config,
                5 * 60,
                true,
                max_instances);

        final RudikResult result = API.instantiateSingleRule(horn_rule, predicate, type, 5);
        if (result != null) {
            for (final HornRuleResult oneResult : result.getResults()) {

                // get all instantiation of the rule over the KB
                final List<HornRuleInstantiation> ruleInst = oneResult.getAllInstantiations();
                // iterate over all instantiation
                int j = 0;
                for (final HornRuleInstantiation instance : ruleInst) {
                	j++;
                    // get <subject,object> of the instantiation - this could be something like <Barack_Obama,Michelle_Obama>
                	Boolean isInList = false;
                    String sep = "";
                    String entitieSep = "";
                    String temp = "";
                    String ruleEntities = "";
                    //store the previous atom to compare with the new one and remove the instantiation if two following atoms are the same
                    List<String> ruleAtoms = new ArrayList<String>();
                    int i = 0;
                    List<RuleAtom> ruleAtomsList = new LinkedList<>();

                    // iterate over all instantiated atoms of the rule
                    Document assignment = new Document();
                    
                    assignment.append("subject", instance.getRuleSubject());
                    assignment.append("object", instance.getRuleObject());
                    for (final RuleAtom atom : instance.getInstantiatedAtoms()) {
                        //list of atoms composing a rule
                        // get <subject,relation,object> of one atom - this could be something like
                        // <Barack_Obama,child,Sasha_Obama> (now the atoms are instantiated, so they contain actual values and not
                        // variables)

                        File relation = new File(atom.getRelation());
                        //if the current atoms is already in the list of atoms set i to 1
                        String constructed_name = relation.getName() + "(" + atom.getSubject() + "," + atom.getObject() + ")";
                        for(String str: ruleAtoms) {
                            if(constructed_name.contains(str.trim()))
                            	isInList = true;
                        }
                        if (isInList) {
                            i++;
                        }
                        temp += sep + relation.getName() + "(" + atom.getSubject() + "," + atom.getObject() + ")";
                        sep = " & ";
                        ruleAtoms.add(relation.getName() + "(" + atom.getSubject() + "," + atom.getObject() + ")");
                        ruleAtomsList.add(atom);
                        //construct the string of entities that compose the instantiated rule
                        if (!ruleEntities.contains(atom.getSubject())) {
                            ruleEntities += entitieSep + atom.getSubject();
                            entitieSep = ";";
                        }
                        if (!ruleEntities.contains(atom.getObject())) {
                            ruleEntities += entitieSep + atom.getObject();
                            entitieSep = ";";

                        }
                    }
                    rulesAtomsDict.put(temp, ruleAtomsList);
                    rules_entities_dict.put(temp, ruleEntities);
                    isInList = false;
                    for(String str: returnResult) {
                        if(temp.contains(str.trim()))
                        	isInList = true;
                    }
                    if (!isInList & i == 0) {
                        returnResult.add(temp);

                        // add instantiation to MongoDB
                        Document inst = new Document("rule_id", "test")
                                .append("predicate", predicate);

                        List<Document> instance_atoms = new LinkedList<>();
                        
                        StringBuilder premise = new StringBuilder();
                        for (RuleAtom atom : ruleAtomsList) {
                            // build rule entities
                            

                            instance_atoms.add(new Document("subject", atom.getSubject())
                                    .append("predicate", atom.getRelation())
                                    .append("object", atom.getObject()));


                            if (!premise.toString().equals("")) {
                                premise.append(" & ").append(atom.getRelation()).append("(").append(atom.getSubject()).append(",").append(atom.getObject()).append(")");
                            } else {
                                premise.append(atom.getRelation()).append("(").append(atom.getSubject()).append(",").append(atom.getObject()).append(")");
                            }
                        }

                        inst.append("premise_triples", instance_atoms)
                                .append("assignment", assignment)
                                .append("premise", premise.toString());

                        // build conclusion
                        StringBuilder conclusion = new StringBuilder();
                        Document conclusion_triple = new Document("subject", assignment.get("subject"))
                                .append("predicate", predicate)
                                .append("object", assignment.get("object"));
                        conclusion.append(predicate).append("(").append(assignment.get("subject")).append(",").append(assignment.get("object")).append(")");

                        inst.append("conclusion", conclusion.toString());
                        inst.append("conclusion_triple", conclusion_triple);

                        // compute an MD5 hash for the instance
                        String joined = String.format("%s %s", premise.toString(), conclusion.toString());
                        String md5_hash = computeHash(String.format("%s %s", premise.toString(), conclusion.toString()));
                        inst.append("hashcode", md5_hash);

                        // add label and details field
                        inst.append("label", -1);
                        inst.append("details", new Document());
                        
                        System.out.printf("Instance %s \n", j);
                        System.out.println(inst);

                    }
                }
            }
        }
        
        System.exit(0);
    }
}