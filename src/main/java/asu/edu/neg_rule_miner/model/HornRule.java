package asu.edu.neg_rule_miner.model;

import java.util.Map;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Maps;

import com.google.common.collect.Sets;

/**
 * A HornRule is made of a set of RuleAtom
 * A RuleAtom contains two variables, each variable can be either START_NODE, END_NODE or a LOOSE_VARIABLES
 * A HornRule is valid if contains START_NODE and END_NODE at least once and each other variable at least twice
 * @author sortona
 *
 */
public class HornRule {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((atoms == null) ? 0 : atoms.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HornRule other = (HornRule) obj;
		if (atoms == null) {
			if (other.atoms != null)
				return false;
		} else if (!atoms.equals(other.atoms))
			return false;
		return true;
	}

	private Set<RuleAtom>  atoms;

	public Map<String,Integer> variables2count;


	public static final String START_NODE = "subject";
	public static final String END_NODE = "object";

	public static final String LOOSE_VARIABLE_NAME = "v";

	public HornRule(){
		this.atoms = Sets.newHashSet();
		this.variables2count = Maps.newHashMap();
	}

	public Set<RuleAtom> getRuleAtoms(){
		return this.atoms;
	}

	public HornRule duplicateRule(){
		HornRule duplicateRule = new HornRule();
		duplicateRule.atoms.addAll(this.atoms);
		duplicateRule.variables2count.putAll(this.variables2count);
		return duplicateRule;
	}

	public boolean addAtom(RuleAtom atom){

		boolean newAtom = atoms.add(atom);
		if(!newAtom)
			return newAtom;
		Integer previousCount = variables2count.get(atom.getSubject());
		if(previousCount==null)
			previousCount=0;
		previousCount++;
		variables2count.put(atom.getSubject(), previousCount);

		previousCount = variables2count.get(atom.getObject());
		if(previousCount==null)
			previousCount=0;
		previousCount++;
		variables2count.put(atom.getObject(), previousCount);

		return true;
	}
	
	

	/**
	 * A HornRule is valid iff each variable appear at least twice and start and end at least once
	 * @return
	 */
	public boolean isValid(){

		boolean containsStart = false;
		boolean containsEnd = false;
		for(String variable:this.variables2count.keySet()){
			if(variable.equals(START_NODE)){
				containsStart = true;
				continue;
			}
			if(variable.equals(END_NODE)){
				containsEnd = true;
				continue;
			}
			if(this.variables2count.get(variable)<2)
				return false;
		}
		return containsStart&&containsEnd;

	}

	/**
	 * Return set of variables to make the rule valid
	 * @return
	 */
	public Set<String> getObligedVariables(){
		Set<String> obligedVariables = Sets.newHashSet();
		obligedVariables.add(START_NODE);
		obligedVariables.add(END_NODE);
		for(String variable:variables2count.keySet()){
			if(variable.equals(START_NODE)||variable.equals(END_NODE)){
				obligedVariables.remove(variable);
				continue;
			}
			if(variables2count.get(variable)<2)
				obligedVariables.add(variable);
		}
		return obligedVariables;

	}

	@Override
	public String toString(){
		if(this.atoms.size()==0)
			return "Empty Rule";
		String hornRule = "";
		for(RuleAtom rule:atoms){
			hornRule+=rule+" ^ ";
		}
		return hornRule.substring(0,hornRule.length()-3);
	}

	public static HornRule readHornRule(String hornRuleString){

		HornRule currentRule = new HornRule();
		String lineSplit[] = hornRuleString.split(" ^ ");
		for(String oneSplit:lineSplit){
			currentRule.addAtom(RuleAtom.readRuleAtom(oneSplit));
		}

		return currentRule;
	}

}
