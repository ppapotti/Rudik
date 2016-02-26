package asu.edu.neg_rule_miner.model;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.RDFNode;

import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
/**
 * Class to model a HornRule representation.
 * A HornRule contains a set of RuleAtom, and it is supported by a set of graphs.
 * Each graph contains a start node and a end node. For each supported graph there exists the path corresponding to the HornRule
 * @author ortona
 *
 * @param <T>
 */
public class HornRule<T> {

	public static final String START_NODE = "subject";
	public static final String END_NODE = "object";

	private Graph<T> graph;

	private Set<T> currentNodes;

	private Set<RuleAtom> rules;

	private Map<T,String> instance2Variable;

	int variableCount = 0;

	private String currentVariable;

	/**
	 * For each supported graph, get the set of current nodes identified by the rule
	 * @return
	 */
	public Set<T> getCurrentNodes(){
		return this.currentNodes;
	}

	/**
	 * Return a new Rule Atom that is supported by at least threshold graphs
	 * @param threshold
	 * @return
	 */
	public Map<RuleAtom,Set<Edge<T>>> nextPlausibleRules(boolean isLast){
		Map<RuleAtom,Set<Edge<T>>> rule2edges = Maps.newHashMap();
		String obligedVariable = null;
		if(isLast){
			boolean containsStart = false;
			boolean containsEnd = false;
			for(RuleAtom atom:this.rules){
				if(!containsStart)
					containsStart = atom.getSubject().equals(START_NODE)||atom.getObject().equals(START_NODE);
				if(!containsEnd)
					containsEnd = atom.getSubject().equals(END_NODE)||atom.getObject().equals(END_NODE);
			}

			if(!containsStart&&!containsEnd)
				return rule2edges;
			if(!containsStart)
				obligedVariable = START_NODE;
			if(!containsEnd)
				obligedVariable = END_NODE;

		}


		for(T currentNode:this.currentNodes){
			String variable = this.instance2Variable.get(currentNode);
			Set<Edge<T>> neighbors = this.graph.getNeighbours(currentNode);
			for(Edge<T> e:neighbors){
				boolean isArtifical = graph.isArtifical(e);
				String newVariable = this.instance2Variable.get(e.getNodeEnd());
				if(newVariable==null){
					if(isLast)
						continue;
					newVariable = "v"+variableCount;
				}
				if(obligedVariable!=null && !newVariable.equals(obligedVariable))
					continue;
				RuleAtom newRule = null;
				if(isArtifical)
					newRule= new RuleAtom(newVariable, e.getLabel(), variable);
				else
					newRule= new RuleAtom(variable, e.getLabel(), newVariable);
				if(rules.contains(newRule))
					continue;

				//TO DO: different check
				if(variable.equals(newVariable)&&!(e.getNodeEnd().equals(e.getNodeSource())))
					continue;


				Set<Edge<T>> currentEdges = rule2edges.get(newRule);
				if(currentEdges==null){
					currentEdges = Sets.newHashSet();
					rule2edges.put(newRule, currentEdges);
				}
				currentEdges.add(e);
			}
		}

		return rule2edges;
	}

	/**
	 * Add a rule atome to the current rule
	 * @param rule
	 * @param graph2EdgeSupport
	 */
	public void addRuleAtom(RuleAtom rule, Set<Edge<T>> edgeSupport){

		if(rules.contains(rule))
			return;
		if(edgeSupport.size()==0)
			return;
		rules.add(rule);
		boolean newVariable = false;

		this.currentNodes.clear();


		this.currentNodes.clear();
		for(Edge<T> supportEdge:edgeSupport){
			T endingNode = supportEdge.getNodeEnd();
			this.currentNodes.add(endingNode);
			String variableEnding = this.instance2Variable.get(endingNode);
			if(variableEnding==null){
				variableEnding = "v"+variableCount;
				this.instance2Variable.put(endingNode, variableEnding);
				newVariable = true;
			}
			currentVariable = variableEnding;
		}

		if(newVariable){
			variableCount++;
		}

	}

	/**
	 * Initialise a Horn rule by giving a map containing a pair start-node as a key, and the corresponding graph as a value
	 * @param example2graph
	 */
	public HornRule(Pair<T,T> startingNode,Graph<T> graph){
		this();

		this.graph=graph;

		this.currentNodes.add(startingNode.getLeft());
		//this.currentNodes.add(startingNode.getRight());

		this.instance2Variable.put(startingNode.getLeft(), START_NODE);
		this.instance2Variable.put(startingNode.getRight(), END_NODE);

		//TO DO: check
		this.currentVariable = START_NODE;
	}

	private HornRule(){
		this.currentNodes = Sets.newHashSet();
		this.rules = Sets.newHashSet();
		this.instance2Variable = Maps.newHashMap();
		this.variableCount = 0;
		this.currentVariable = null;
	}

	/**
	 * Duplicate the current rule and return a new duplicated rule
	 * @return
	 */
	public HornRule<T> duplicateRule(){
		HornRule<T> newRule = new HornRule<T>();
		Set<T> currentNewNodes = Sets.newHashSet(this.currentNodes);
		Map<T,String> newInstance2variable = Maps.newHashMap(this.instance2Variable);

		newRule.currentNodes = currentNewNodes;
		newRule.instance2Variable = newInstance2variable;

		newRule.graph = this.graph;

		Set<RuleAtom> rules = Sets.newHashSet(this.rules);
		newRule.rules = rules;

		newRule.variableCount = variableCount;
		newRule.currentVariable = this.currentVariable;

		return newRule;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((rules == null) ? 0 : rules.hashCode());
		return result;
	}



	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HornRule<T> other = (HornRule<T>) obj;
		if (rules == null) {
			if (other.rules != null)
				return false;
		} else if (!rules.equals(other.rules))
			return false;
		return true;
	}

	/**
	 * Get the number of rule atoms in the rule
	 * @return
	 */
	public int getLen(){
		return this.rules.size();
	}

	@Override
	public String toString(){
		if(this.rules.size()==0)
			return "Empty Rule";
		String hornRule = "";
		for(RuleAtom rule:rules){
			hornRule+=rule+" ^ ";
		}
		return hornRule.substring(0,hornRule.length()-3);
	}

	/**
	 * A HornRule is valid iff each variable appear at least twice and start and end at least once
	 * @return
	 */
	public boolean isValid(){

		Set<String> seenVariables = Sets.newHashSet();
		seenVariables.add(START_NODE);
		seenVariables.add(END_NODE);
		Set<String> countOneVariable = Sets.newHashSet();
		countOneVariable.add(START_NODE);
		countOneVariable.add(END_NODE);
		for(RuleAtom rule:rules){
			String firstVariable = rule.getSubject();
			String secondVariable = rule.getObject();

			if(seenVariables.contains(firstVariable)){
				countOneVariable.remove(firstVariable);
			}
			else{
				seenVariables.add(firstVariable);
				countOneVariable.add(firstVariable);
			}

			if(seenVariables.contains(secondVariable)){
				countOneVariable.remove(secondVariable);
			}
			else{
				seenVariables.add(secondVariable);
				countOneVariable.add(secondVariable);
			}
		}
		return countOneVariable.size()==0;

	}

	/**
	 * Return thes set of rule atoms
	 * @return
	 */
	public Set<RuleAtom> getRules(){
		return this.rules;
	}

	/**
	 * A rule is expandible only if it contains less than maxAtomLen atoms
	 * @return
	 */
	public boolean isExpandible(int maxAtomLen){
		if(this.getLen()<maxAtomLen)
			return true;
		return false;
	}
	public Map<T,String> getInstance2Variable(){
		return this.instance2Variable;
	}
	
	public String getCurrentVariable(){
		return this.currentVariable;
	}
	
	public Set<String> getBoundVariables(String variable){
		Set<String> boundVariables = Sets.newHashSet();
		for(RuleAtom rule:this.rules){
			if(rule.getSubject().equals(variable))
				boundVariables.add(rule.getObject());
			if(rule.getObject().equals(variable))
				boundVariables.add(rule.getSubject());
		}
		return boundVariables;
	}

}
