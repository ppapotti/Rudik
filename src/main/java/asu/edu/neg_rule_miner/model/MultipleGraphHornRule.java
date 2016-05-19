package asu.edu.neg_rule_miner.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.ext.com.google.common.collect.Maps;

import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;

/**
 * Implements a HornRule structure that keeps tracks of a graph for each input generation example
 * @author sortona
 *
 * @param <T>
 */
public class MultipleGraphHornRule<T> extends HornRule{

	private Graph<T> g;

	Map<T,Map<Pair<T,T>,String>> node2example2variable;
	public Map<T,Set<Pair<T,T>>> currentNode2examples;

	Set<Pair<T,T>> coveredExamples;

	/**
	 * For each supported graph, get the set of current nodes identified by the rule
	 * @return
	 */
	public Set<T> getCurrentNodes(){
		if(node2example2variable==null ||
				currentNode2examples==null)
			initialiseBoundingVariable();
		return currentNode2examples.keySet();
	}

	public Set<Pair<T,T>> getCoveredExamples(){
		return this.coveredExamples;
	}

	/**
	 * Return a new Rule Atom that is supported by at least threshold graphs
	 * @param threshold
	 * @return
	 */
	public Set<MultipleGraphHornRule<T>> nextPlausibleRules(int maxAtomThreshold, int minRuleSupport){

		Set<MultipleGraphHornRule<T>> nextPlausibleRules = Sets.newHashSet();

		/**
		 * FOR DEBUGGING PURPOSE AND AVOID OUT OF MEMORY
		 */
		if(this.rules.size()==2 &&
				this.rules.get(0).getRelation().equals("http://yago-knowledge.org/resource/hasGender") &&
				this.rules.get(1).getRelation().equals("http://yago-knowledge.org/resource/hasGender"))
			return nextPlausibleRules;

		if(this.rules.size()==2 &&
				this.rules.get(0).getRelation().equals("http://www.wikidata.org/prop/direct/P31") &&
				this.rules.get(1).getRelation().equals("http://www.wikidata.org/prop/direct/P31"))
			return nextPlausibleRules;
		/** */

		this.initialiseBoundingVariable();

		boolean isLast = this.getLen()==maxAtomThreshold-1;
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
				return nextPlausibleRules;
			if(!containsStart)
				obligedVariable = START_NODE;
			if(!containsEnd)
				obligedVariable = END_NODE;

			if(obligedVariable!=null && (obligedVariable.equals(START_NODE) || obligedVariable.equals(END_NODE)))
				return this.nextOneHopPlausibleRules(maxAtomThreshold, minRuleSupport, obligedVariable);
		}

		Map<RuleAtom,Boolean> rule2newVariable = Maps.newHashMap();
		Map<RuleAtom,Set<Pair<T,T>>> rule2coveredExamples = Maps.newHashMap();


		for(T currentNode:this.currentNode2examples.keySet()){
			//get the examples where the current node is
			Set<Pair<T,T>> currentCoveredExamples = currentNode2examples.get(currentNode);

			Set<Edge<T>> neighbors = g.getNeighbours(currentNode);
			for(Edge<T> e:neighbors){
				for(Pair<T,T> oneCoveredExample:currentCoveredExamples){
					boolean isNewVariable = false;
					boolean isArtifical = e.isArtificial();
					String newVariable = null;
					if(node2example2variable.containsKey(e.getNodeEnd()))
						newVariable = node2example2variable.get(e.getNodeEnd()).get(oneCoveredExample);

					if(newVariable==null){
						if(isLast)
							continue;
						newVariable = "v"+variableCount;
						isNewVariable = true;
					}
					if(obligedVariable!=null && !newVariable.equals(obligedVariable))
						continue;
					RuleAtom newRule = null;
					if(isArtifical)
						newRule= new RuleAtom(newVariable, e.getLabel(), currentVariable);
					else
						newRule= new RuleAtom(currentVariable, e.getLabel(), newVariable);
					if(rules.contains(newRule))
						continue;

					//TO DO: different check
					if(currentVariable.equals(newVariable)&&!(e.getNodeEnd().equals(e.getNodeSource())))
						continue;

					if(!rule2newVariable.containsKey(newRule))
						rule2newVariable.put(newRule, isNewVariable);

					Set<Pair<T,T>> newNodeCoveredExamples = rule2coveredExamples.get(newRule);
					if(newNodeCoveredExamples == null){
						newNodeCoveredExamples = Sets.newHashSet();
						rule2coveredExamples.put(newRule, newNodeCoveredExamples);
					}
					newNodeCoveredExamples.add(oneCoveredExample);
				}
			}
		}

		//return only rules which cover something above the threshold
		for(RuleAtom oneAtom:rule2newVariable.keySet()){
			if(rule2coveredExamples.get(oneAtom).size()<=minRuleSupport)
				continue;

			MultipleGraphHornRule<T> newHornRule = this.duplicateRule();
			newHornRule.addRuleAtom(oneAtom, rule2newVariable.get(oneAtom),rule2coveredExamples.get(oneAtom));
			nextPlausibleRules.add(newHornRule);

		}
		return nextPlausibleRules;
	}



	/**
	 * Add a rule atome to the current rule and modify the covered examples
	 * 
	 * @param rule
	 * @param newVariable
	 * @param newCoveredExamples
	 */

	public void addRuleAtom(RuleAtom rule, boolean newVariable, Set<Pair<T,T>> newCoveredExamples){

		this.addRuleAtom(rule, newVariable);

		this.coveredExamples.clear();
		this.coveredExamples.addAll(newCoveredExamples);
	}

	/**
	 * Initialise a Horn rule by giving a map containing a pair start-node as a key, and the corresponding graph as a value
	 * @param example2graph
	 */
	public MultipleGraphHornRule(Graph<T> g, boolean startsSubject, Set<Pair<T,T>> coveredExamples){
		this();

		this.g = g;

		if(startsSubject)
			this.startingVariable = START_NODE;
		else
			this.startingVariable = END_NODE;

		this.coveredExamples.addAll(coveredExamples);
	}

	private MultipleGraphHornRule(){
		super();
		this.coveredExamples = Sets.newHashSet();
	}

	/**
	 * Duplicate the current rule WITHOUT COPYING THE CURRENT COVERED EXAMPLES
	 * @return
	 */
	public MultipleGraphHornRule<T> duplicateRule(){
		MultipleGraphHornRule<T> newRule = new MultipleGraphHornRule<T>();

		List<RuleAtom> rules = Lists.newArrayList(this.rules);
		newRule.rules = rules;

		newRule.variableCount = variableCount;
		newRule.startingVariable = this.startingVariable;


		newRule.g = this.g;

		return newRule;
	}
	

	private void initialiseBoundingVariable(){

		if(this.currentVariable!=null && this.node2example2variable!=null && this.currentNode2examples!=null)
			return;

		Set<Pair<T,T>> examples = g.getExamples();
		//build initial node2example2variable and currentNode2examples
		this.node2example2variable = Maps.newHashMap();
		this.currentNode2examples = Maps.newHashMap();
		for(Pair<T,T> oneExample:examples){
			T subject = oneExample.getLeft();
			T object = oneExample.getRight();

			Map<Pair<T,T>,String> currentExample2variable = this.node2example2variable.get(subject);
			if(currentExample2variable==null){
				currentExample2variable = Maps.newHashMap();
				this.node2example2variable.put(subject, currentExample2variable);
			}
			currentExample2variable.put(oneExample, START_NODE);

			currentExample2variable = this.node2example2variable.get(object);
			if(currentExample2variable==null){
				currentExample2variable = Maps.newHashMap();
				this.node2example2variable.put(object, currentExample2variable);
			}
			currentExample2variable.put(oneExample, END_NODE);

			T currentNode = startingVariable.equals(START_NODE) ? subject : object;
			Set<Pair<T,T>> currentCoveredExamples = this.currentNode2examples.get(currentNode);
			if(currentCoveredExamples==null){
				currentCoveredExamples = Sets.newHashSet();
				this.currentNode2examples.put(currentNode, currentCoveredExamples);
			}
			currentCoveredExamples.add(oneExample);
		}

		String currentVariable = startingVariable;
		Set<String> seenVariables = Sets.newHashSet();
		seenVariables.add(START_NODE);
		seenVariables.add(END_NODE);
		Set<Pair<T,T>> notCoveredExamples = Sets.newHashSet();
		this.currentVariable = startingVariable;

		for(RuleAtom oneAtom : rules){
			Map<T,Set<Pair<T,T>>> newCurrentNodes = Maps.newHashMap();
			String relation = oneAtom.getRelation();
			boolean isInverse = !oneAtom.getSubject().equals(currentVariable);
			String newVariable = isInverse ? oneAtom.getSubject() : oneAtom.getObject();


			for(T oneNode:currentNode2examples.keySet()){

				notCoveredExamples.clear();
				notCoveredExamples.addAll(currentNode2examples.get(oneNode));
				Set<Edge<T>> neighbours = g.getNeighbours(oneNode);
				for(Edge<T> oneNeighbour:neighbours){
					if(!oneNeighbour.getLabel().equals(relation) || oneNeighbour.isArtificial()!=isInverse)
						continue;

					T endNode = oneNeighbour.getNodeEnd();
					//for the current node get the not covered examples
					for(Pair<T,T> oneExample:currentNode2examples.get(oneNode)){
						String currentNodeVariable = node2example2variable.get(endNode)!=null ? node2example2variable.get(endNode).get(oneExample) : null;
						//check if the variable is the same
						boolean isGoodNode = false;
						if(seenVariables.contains(newVariable)&&(currentNodeVariable!=null&&currentNodeVariable.equals(newVariable))){
							isGoodNode = true;
						}
						if(!seenVariables.contains(newVariable)&&currentNodeVariable==null){
							Map<Pair<T,T>,String> example2variable = node2example2variable.get(endNode);
							if(example2variable==null){
								example2variable = Maps.newHashMap();
								node2example2variable.put(endNode, example2variable);
							}
							example2variable.put(oneExample, newVariable);
							isGoodNode = true;
						}

						if(isGoodNode){
							notCoveredExamples.remove(oneExample);
							Set<Pair<T,T>> newCurrentNodeCoveredExamples = newCurrentNodes.get(endNode);
							if(newCurrentNodeCoveredExamples==null){
								newCurrentNodeCoveredExamples = Sets.newHashSet();
								newCurrentNodes.put(endNode, newCurrentNodeCoveredExamples);
							}
							newCurrentNodeCoveredExamples.add(oneExample);
						}
					}

				}

				//remove the not covered examples
				node2example2variable.get(oneNode).keySet().removeAll(notCoveredExamples);
				if(node2example2variable.get(oneNode).size()==0)
					node2example2variable.remove(oneNode);
			}

			currentVariable = newVariable;
			seenVariables.add(newVariable);
			currentNode2examples = newCurrentNodes;
			this.currentVariable = newVariable;
		}
	}

	public void dematerialiseRule(){
		this.node2example2variable = null;
		this.currentNode2examples = null;
		this.currentVariable = null;
	}

	public Set<Pair<T,T>> getCoveredExamples(T specifiNode){
		if(this.currentNode2examples==null)
			this.initialiseBoundingVariable();
		return this.currentNode2examples.get(specifiNode);
	}

	public Set<MultipleGraphHornRule<T>> nextOneHopPlausibleRules(int maxAtomThreshold, int minRuleSupport, String variable){

		Set<MultipleGraphHornRule<T>> nextPlausibleRules = Sets.newHashSet();
		if(!variable.equals(START_NODE)&&!variable.equals(END_NODE))
			return nextPlausibleRules;

		if(this.currentNode2examples==null)
			this.initialiseBoundingVariable();

		Set<T> obligedEndingNodes = Sets.newHashSet();
		for(Pair<T,T> oneExample:this.coveredExamples){
			if(variable.equals(START_NODE))
				obligedEndingNodes.add(oneExample.getLeft());
			else
				obligedEndingNodes.add(oneExample.getRight());
		}

		//keep only variables for ending node
		this.node2example2variable.keySet().retainAll(obligedEndingNodes);

		Map<RuleAtom,Set<Pair<T,T>>> rule2coveredExamples = Maps.newHashMap();
		//examine all obliged end nodes to see if they are connected to one or more currentNodes
		Set<Edge<T>> neighbours;
		Set<Pair<T,T>> currentExamples;
		for(T singleObligedNode:obligedEndingNodes){
			neighbours = this.g.getNeighbours(singleObligedNode);

			for(Edge<T> oneNeighbour:neighbours){
				T endNode = oneNeighbour.getNodeEnd();
				if(!this.currentNode2examples.containsKey(endNode))
					continue;

				//get current examples
				currentExamples = this.currentNode2examples.get(endNode);
				for(Pair<T,T> oneExample:currentExamples){
					//check the node is actually the desired node in the current example
					if(node2example2variable.get(singleObligedNode).get(oneExample)==null||
							!node2example2variable.get(singleObligedNode).get(oneExample).equals(variable))
						continue;

					RuleAtom newRule = null;
					if(oneNeighbour.isArtificial())
						newRule= new RuleAtom(currentVariable, oneNeighbour.getLabel(), variable);
					else
						newRule= new RuleAtom(variable, oneNeighbour.getLabel(), currentVariable);
					if(rules.contains(newRule))
						continue;

					//TO DO: different check
					if(currentVariable.equals(variable)&&!(oneNeighbour.getNodeEnd().equals(oneNeighbour.getNodeSource())))
						continue;

					Set<Pair<T,T>> newNodeCoveredExamples = rule2coveredExamples.get(newRule);
					if(newNodeCoveredExamples == null){
						newNodeCoveredExamples = Sets.newHashSet();
						rule2coveredExamples.put(newRule, newNodeCoveredExamples);
					}
					newNodeCoveredExamples.add(oneExample);
				}

			}
		}

		//create new rules
		for(RuleAtom oneAtom:rule2coveredExamples.keySet()){
			if(rule2coveredExamples.get(oneAtom).size()<=minRuleSupport)
				continue;

			MultipleGraphHornRule<T> newHornRule = this.duplicateRule();
			newHornRule.addRuleAtom(oneAtom, false,rule2coveredExamples.get(oneAtom));
			nextPlausibleRules.add(newHornRule);

		}
		return nextPlausibleRules;
	}

}
