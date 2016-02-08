package asu.edu.neg_rule_miner.model;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;

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

	private static final String START_NODE = "start";
	private static final String END_NODE = "end";

	private Map<Graph<T>,Set<T>> currentNodes;

	private Set<RuleAtom> rules;

	private Map<Graph<T>,Map<T,String>> graph2instanceVariable;

	int variableCount = 0;

	String currentVariable;

	/**
	 * For each supported graph, get the set of current nodes identified by the rule
	 * @return
	 */
	public Map<Graph<T>,Set<T>> getCurrentNodes(){
		return this.currentNodes;
	}

	/**
	 * Return a new Rule Atom that is supported by at least threshold graphs
	 * @param threshold
	 * @return
	 */
	public Map<RuleAtom,Map<Graph<T>,Set<Edge<T>>>> nextPlausibleRules(int threshold,boolean isLast){
		Map<RuleAtom,Map<Graph<T>,Set<Edge<T>>>> thresholdRule2graphEdge = Maps.newHashMap();
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
				return thresholdRule2graphEdge;
			if(!containsStart)
				obligedVariable = START_NODE;
			if(!containsEnd)
				obligedVariable = END_NODE;

		}


		Map<RuleAtom,Map<Graph<T>,Set<Edge<T>>>> rule2graphEdge = Maps.newHashMap();
		for(Graph<T> g:currentNodes.keySet()){
			Map<T,String> currentVariables = this.graph2instanceVariable.get(g);
			for(T currentNode:currentNodes.get(g)){
				String variable = this.graph2instanceVariable.get(g).get(currentNode);
				Set<Edge<T>> neighbors = g.getNeighbours(currentNode);
				for(Edge<T> e:neighbors){
					boolean isArtifical = g.isArtifical(e);
					String newVariable = currentVariables.get(e.getNodeEnd());
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

					Map<Graph<T>,Set<Edge<T>>> currentGraphSupport = rule2graphEdge.get(newRule);
					if(currentGraphSupport==null){
						currentGraphSupport=Maps.newHashMap();
						rule2graphEdge.put(newRule, currentGraphSupport);
					}

					Set<Edge<T>> currentEdges = currentGraphSupport.get(g);
					if(currentEdges==null){
						currentEdges = Sets.newHashSet();
						currentGraphSupport.put(g, currentEdges);
					}
					currentEdges.add(e);

					if(currentGraphSupport.size()>=threshold)
						thresholdRule2graphEdge.put(newRule, currentGraphSupport);
				}
			}
		}

		return thresholdRule2graphEdge;
	}

	/**
	 * Add a rule atome to the current rule
	 * @param rule
	 * @param graph2EdgeSupport
	 */
	public void addRuleAtom(RuleAtom rule, Map<Graph<T>,Set<Edge<T>>> graph2EdgeSupport){

		if(rules.contains(rule))
			return;
		rules.add(rule);
		boolean newVariable = false;

		//to remove all the graph that do not respect the rule anymore
		Set<Graph<T>> graphToRemove = Sets.newHashSet();
		graphToRemove.addAll(this.currentNodes.keySet());
		graphToRemove.removeAll(graph2EdgeSupport.keySet());

		this.currentNodes.clear();

		for(Graph<T> g:graph2EdgeSupport.keySet()){
			Set<T> currentNewNodes = Sets.newHashSet();

			Map<T,String> instance2variable = this.graph2instanceVariable.get(g);
			for(Edge<T> supportEdge:graph2EdgeSupport.get(g)){

				T endingNode = supportEdge.getNodeEnd();
				currentNewNodes.add(endingNode);
				String variableEnding = instance2variable.get(endingNode);
				if(variableEnding==null){
					variableEnding = "v"+variableCount;
					instance2variable.put(endingNode, variableEnding);
					newVariable = true;
				}
				currentVariable = variableEnding;
			}
			this.currentNodes.put(g, currentNewNodes);

		}
		if(newVariable){
			variableCount++;
		}

		if(graphToRemove.size()==0)
			return;

		this.graph2instanceVariable.keySet().retainAll(graph2EdgeSupport.keySet());
	}

	/**
	 * Initialise a Horn rule by giving a map containing a pair start-node as a key, and the corresponding graph as a value
	 * @param example2graph
	 */
	public HornRule(Map<Pair<T,T>,Graph<T>> example2graph){
		this();

		for(Pair<T,T> example:example2graph.keySet()){
			Graph<T> g = example2graph.get(example);
			Set<T> graphCurrentNodes = Sets.newHashSet();
			graphCurrentNodes.add(example.getLeft());
			graphCurrentNodes.add(example.getRight());
			this.currentNodes.put(g, graphCurrentNodes);

			Map<T,String> currentInstanceVariable = Maps.newHashMap();
			currentInstanceVariable.put(example.getLeft(), START_NODE);
			currentInstanceVariable.put(example.getRight(), END_NODE);
			this.graph2instanceVariable.put(g, currentInstanceVariable);
		}
		this.currentVariable = START_NODE;
	}

	private HornRule(){
		this.currentNodes = Maps.newHashMap();
		this.rules = Sets.newHashSet();
		this.graph2instanceVariable = Maps.newHashMap();
		this.variableCount = 0;
		this.currentVariable = null;
	}

	/**
	 * Duplicate the current rule and return a new duplicated rule
	 * @return
	 */
	public HornRule<T> duplicateRule(){
		HornRule<T> newRule = new HornRule<T>();
		Map<Graph<T>,Set<T>> currentNodes = Maps.newHashMap();
		Map<Graph<T>,Map<T,String>> graph2instanceVariable = Maps.newHashMap();
		for(Graph<T> g:this.currentNodes.keySet()){
			Set<T> graphCurrentNodes = Sets.newHashSet(this.currentNodes.get(g));
			currentNodes.put(g, graphCurrentNodes);

			Map<T,String> currentGraph2instanceVariable = Maps.newHashMap(this.graph2instanceVariable.get(g));
			graph2instanceVariable.put(g, currentGraph2instanceVariable);

		}
		newRule.currentNodes = currentNodes;
		newRule.graph2instanceVariable = graph2instanceVariable;

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
	 * Get the number of graphs supporting the rule
	 * @return
	 */
	public int getSupport(){
		return this.currentNodes.size();
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

}
