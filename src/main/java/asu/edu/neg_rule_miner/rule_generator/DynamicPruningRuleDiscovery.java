package asu.edu.neg_rule_miner.rule_generator;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.configuration.Constant;
import asu.edu.neg_rule_miner.model.horn_rule.HornRule;
import asu.edu.neg_rule_miner.model.horn_rule.MultipleGraphHornRule;
import asu.edu.neg_rule_miner.model.horn_rule.RuleAtom;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.model.statistic.StatisticsContainer;
import asu.edu.neg_rule_miner.rule_generator.score.DynamicScoreComputation;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

/**
 * Implements A* dynamic pruning discovery algorithm for rules generation
 * 
 * @author sortona
 *
 */
public class DynamicPruningRuleDiscovery extends HornRuleDiscovery{

	private final static Logger LOGGER = LoggerFactory.getLogger(DynamicPruningRuleDiscovery.class.getName());

	public DynamicPruningRuleDiscovery(){
		super();
	}

	public List<HornRule> discoverPositiveHornRules(Set<Pair<String,String>> negativeExamples, Set<Pair<String,String>> positiveExamples,
			Set<String> relations, String typeSubject, String typeObject){
		//switch positive and negative examples
		DynamicScoreComputation score = new DynamicScoreComputation(positiveExamples.size(), 
				negativeExamples.size(), this.getSparqlExecutor(), relations, typeSubject, typeObject, negativeExamples);
		score.setMinePositive(true,false,false);
		return 
				this.discoverHornRules(positiveExamples, negativeExamples, relations, score);

	}

	public List<HornRule> discoverNegativeHornRules(Set<Pair<String,String>> negativeExamples, Set<Pair<String,String>> positiveExamples,
			Set<String> relations, String typeSubject, String typeObject){
		DynamicScoreComputation score = new DynamicScoreComputation(negativeExamples.size(), 
				positiveExamples.size(), this.getSparqlExecutor(), relations, typeSubject, typeObject, positiveExamples);
		return 
				this.discoverHornRules(negativeExamples, positiveExamples, relations, score);

	}

	public List<HornRule> discoverPositiveHornRules(Set<Pair<String,String>> negativeExamples, Set<Pair<String,String>> positiveExamples,
			Set<String> relations, String typeSubject, String typeObject,boolean subjectFunction, boolean objectFunction){
		//switch positive and negative examples
		DynamicScoreComputation score = new DynamicScoreComputation(positiveExamples.size(), 
				negativeExamples.size(), this.getSparqlExecutor(), relations, typeSubject, typeObject, negativeExamples);
		score.setMinePositive(true,subjectFunction,objectFunction);
		return 
				this.discoverHornRules(positiveExamples, negativeExamples, relations, score);

	}

	@SuppressWarnings("unchecked")
	private List<HornRule> discoverHornRules(Set<Pair<String,String>> negativeExamples, Set<Pair<String,String>> positiveExamples,
			Set<String> relations, DynamicScoreComputation score){
		if(negativeExamples.size()==0 || positiveExamples.size()==0)
			return null;

		StatisticsContainer.setStartTime(System.currentTimeMillis());


		Map<String,Set<Pair<String,String>>> expandedNodes2examples = Maps.newHashMap();
		//for literals keep also track which examples expanded
		Map<String,Set<Pair<String,String>>> expandedLiteral2examples = Maps.newHashMap();

		Map<String,Set<String>> entity2types = Maps.newHashMap();
		Set<MultipleGraphHornRule<String>> negativeHornRules = Sets.newHashSet();
		Map<String,Set<Pair<String,String>>> relation2positiveExamples = Maps.newHashMap();

		int negativeCoverageThreshold = negativeExamples.size()/100 +1;
		int positiveCoverageThreshold = positiveExamples.size()/100 +1;

		Graph<String> totalGraph = new Graph<String>();

		this.initialiseRules(negativeExamples, positiveExamples, expandedNodes2examples, 
				entity2types,negativeHornRules, relation2positiveExamples, totalGraph);

		Map<Set<RuleAtom>,Set<Pair<String,String>>> rule2relativePositiveCoverage = Maps.newHashMap();

		if(negativeHornRules.size()!=2){
			throw new RuleMinerException("Initalisation of negative and positive rules contains more or less than two empty rules with subject and object.", 
					LOGGER);
		}

		MultipleGraphHornRule<String> bestNegativeRule = negativeHornRules.iterator().next();

		//the two initial empty rules relatively cover all positive examples
		rule2relativePositiveCoverage.put(bestNegativeRule.getRules(),
				positiveExamples);

		Set<Set<RuleAtom>> seenRules = Sets.newHashSet();

		List<HornRule> outputRules = Lists.newArrayList();

		while(bestNegativeRule!=null){

			boolean deleteRule = true;
			boolean expand = bestNegativeRule.isExpandible(this.maxRuleLen);

			//if the rule is admissible (valid and does not cover many positive examples) outputs it
			if(score.isAdmissible(bestNegativeRule)){

				//if I can expand it more and find better rules I should try
				if(bestNegativeRule.isExpandible(this.maxRuleLen) && score.isExpandible(bestNegativeRule.getRules())){
					deleteRule = false;
					score.setMandatoryExpandible(bestNegativeRule.getRules());
				}
				else{
					LOGGER.debug("Found a new valid rule: {}",bestNegativeRule);
					outputRules.add(bestNegativeRule);
					expand = false;
				}
			}

			//do not delete it if it is a valid rule but can be further expanded and improved
			if(deleteRule)
				negativeHornRules.remove(bestNegativeRule);

			Set<Pair<String,String>> previousRelativeCoverage = rule2relativePositiveCoverage.get(bestNegativeRule.getRules());

			//remove positive relative coverage only if it is not the empty rule and if it has to be deleted
			if(bestNegativeRule.getLen()>0 && deleteRule){
				rule2relativePositiveCoverage.remove(bestNegativeRule.getRules());
			}

			//expand it only if it has less than maxRuleLen atoms
			if(expand){

				LOGGER.debug("Computing next rules for current best rule...");
				//compute next plausible negative rules
				Set<MultipleGraphHornRule<String>> newPlausibleRules = 
						bestNegativeRule.nextPlausibleRules(super.maxRuleLen, negativeCoverageThreshold);

				//destroy the materialised resources for the rule
				bestNegativeRule.dematerialiseRule();


				LOGGER.debug("Computing for each new plausible relative positive support and total score...");
				for(MultipleGraphHornRule<String> plausibleRule:newPlausibleRules){

					if(seenRules.contains(plausibleRule.getRules()))
						continue;

					seenRules.add(plausibleRule.getRules());
					if(plausibleRule.isValid()){
						seenRules.add(plausibleRule.getEquivalentRule());
					}

					Set<Pair<String,String>> positiveRelativeCoverage = this.getPositiveRelativeCoverage(previousRelativeCoverage, 
							bestNegativeRule.getRules(), 
							plausibleRule.getRules(), relation2positiveExamples);

					if(positiveRelativeCoverage.size()<=positiveCoverageThreshold)
						continue;

					//add the rule and its positive to the new rules to consider
					negativeHornRules.add(plausibleRule);
					//add the relative positive coverage of the rule
					rule2relativePositiveCoverage.put(plausibleRule.getRules(), positiveRelativeCoverage);

				}

			}
			else
				bestNegativeRule.dematerialiseRule();

			LOGGER.debug("Computing next best rule according to score...");

			Pair<MultipleGraphHornRule<String>,Double> ruleAndscore = 
					score.getNextBestRule(negativeHornRules, rule2relativePositiveCoverage,this.maxRuleLen);
			if(ruleAndscore==null){

				LOGGER.debug("Next best rule has a positive score, returning all the founded rules.");
				StatisticsContainer.setNodesNumber(totalGraph.getNodesNumber());
				StatisticsContainer.setEdgesNumber(totalGraph.getNodesEdges());

				break;
			}
			bestNegativeRule = ruleAndscore.getLeft();

			LOGGER.debug("Next best rule with best approximate score '{}' ({} covered negative examples, {} relative covered positive, {} positive coverage, score: {})",
					bestNegativeRule,bestNegativeRule.getCoveredExamples().size(),rule2relativePositiveCoverage.get(bestNegativeRule.getRules()).size(),
					score.getRuleValidationScore(bestNegativeRule.getRules()),ruleAndscore.getRight());

			//expand nodes for the current positive and negative examples only if the rule can be expanded
			if(bestNegativeRule.getLen() < maxRuleLen - 1){

				Set<String> toAnalyse = Sets.newHashSet();
				toAnalyse.addAll(bestNegativeRule.getCurrentNodes());
				//different expansions if the node is a literal
				boolean isLiteral = totalGraph.isLiteral(toAnalyse.iterator().next());

				//special case if nodes to expand are literals
				if(isLiteral)
					this.expandLiteral(toAnalyse,totalGraph,bestNegativeRule,expandedLiteral2examples);
				else{
					//expand graph only if nodes has not been expanded before
					toAnalyse.removeAll(expandedNodes2examples.keySet());
					if(toAnalyse.size()>0){
						LOGGER.debug("Expanding {} nodes for negative and positive examples...",toAnalyse.size());
						this.expandGraphs(toAnalyse, totalGraph, entity2types, numThreads);
						LOGGER.debug("...expansion completed.");
					}

					//add inequalities relations
					this.expandInequalityNodes(bestNegativeRule.getCurrentNodes(), totalGraph, bestNegativeRule, expandedNodes2examples);
				}

			}

		}

		StatisticsContainer.setEndTime(System.currentTimeMillis());
		//promove constants for each rule
		for(HornRule rule:outputRules){
			//cast
			((MultipleGraphHornRule<String>) rule).promoteConstant();
			((MultipleGraphHornRule<String>) rule).dematerialiseRule();
		}

		StatisticsContainer.setOutputRules(outputRules);
		return outputRules;

	}


	private void initialiseRules(Set<Pair<String,String>> generationExamples, 
			Set<Pair<String,String>> validationExamples,
			Map<String,Set<Pair<String,String>>> expandedNodes,Map<String,Set<String>> entity2types,
			Set<MultipleGraphHornRule<String>> hornRules,
			Map<String,Set<Pair<String,String>>> relation2validationExample, Graph<String> generationNodesGraph){
		LOGGER.debug("Creating initial graphs and expanding all positive and negative examples...");
		if(generationExamples==null || generationExamples.size()==0 || validationExamples==null || validationExamples.size()==0)
			return;

		//when adding new nodes, add also the corresponding example
		Set<String> toAnalyse = Sets.newHashSet();
		Set<Pair<String,String>> coveredExamples;

		for(Pair<String,String> example:generationExamples){

			generationNodesGraph.addNode(example.getLeft());
			generationNodesGraph.addNode(example.getRight());
			toAnalyse.add(example.getLeft());
			toAnalyse.add(example.getRight());

			coveredExamples = expandedNodes.get(example.getLeft());
			if(coveredExamples == null){
				coveredExamples = Sets.newHashSet();
				expandedNodes.put(example.getLeft(), coveredExamples);
			}
			coveredExamples.add(example);

			coveredExamples = expandedNodes.get(example.getRight());
			if(coveredExamples == null){
				coveredExamples = Sets.newHashSet();
				expandedNodes.put(example.getRight(), coveredExamples);
			}
			coveredExamples.add(example);
		}


		generationNodesGraph.addExamples(generationExamples);
		this.expandGraphs(toAnalyse, generationNodesGraph, entity2types, numThreads);
		MultipleGraphHornRule<String> subjectRule = new MultipleGraphHornRule<String>(generationNodesGraph,true,generationExamples);
		hornRules.add(subjectRule);
		MultipleGraphHornRule<String> objectRule = new MultipleGraphHornRule<String>(generationNodesGraph,false,generationExamples);
		hornRules.add(objectRule);

		Graph<String> positiveNodesGraph = new Graph<String>();
		toAnalyse.clear();
		for(Pair<String,String> example:validationExamples){
			positiveNodesGraph.addNode(example.getLeft());
			positiveNodesGraph.addNode(example.getRight());
			toAnalyse.add(example.getLeft());
			toAnalyse.add(example.getRight());
		}
		this.expandGraphs(toAnalyse, positiveNodesGraph, entity2types, numThreads);

		//create map of positive relative coverage
		Set<Edge<String>> currentEdges;
		Set<Pair<String,String>> currentRelationExamples;
		Set<String> permutations = Sets.newHashSet();
		permutations.add("subject");
		permutations.add("object");
		Set<String> currentExampleRelation = Sets.newHashSet();

		for(Pair<String,String> posExample:validationExamples){
			currentExampleRelation.clear();

			for(String onePermutation:permutations){
				currentEdges = positiveNodesGraph.getNeighbours(posExample.getLeft());
				if(onePermutation.equals("object"))
					currentEdges = positiveNodesGraph.getNeighbours(posExample.getRight());

				for(Edge<String> oneEdge:currentEdges){
					String relation=oneEdge.getLabel();
					if(oneEdge.isArtificial())
						relation+="(_,"+onePermutation+")";
					else
						relation+="("+onePermutation+",_)";
					if(currentExampleRelation.contains(relation))
						continue;
					currentExampleRelation.add(relation);
					currentRelationExamples = relation2validationExample.get(relation);
					if(currentRelationExamples==null){
						currentRelationExamples=Sets.newHashSet();
						relation2validationExample.put(relation, currentRelationExamples);
					}
					currentRelationExamples.add(posExample);
				}
			}

		}

		LOGGER.debug("Graph creation completed.");
	}

	private Set<Pair<String,String>> getPositiveRelativeCoverage(
			Set<Pair<String,String>> previous,Set<RuleAtom> previousRule,Set<RuleAtom> newRule, 
			Map<String,Set<Pair<String,String>>> relation2examples){
		Set<RuleAtom> toVerify = Sets.newHashSet();
		toVerify.addAll(newRule);

		if(previousRule!=null)
			toVerify.removeAll(previousRule);

		Set<Pair<String,String>> newCoverage = Sets.newHashSet();
		newCoverage.addAll(previous);

		for(RuleAtom oneAtom:toVerify){
			if(oneAtom.getSubject().equals(HornRule.START_NODE)){
				String relation = oneAtom.getRelation()+"(subject,_)";
				if(relation2examples.containsKey(relation))
					newCoverage.retainAll(relation2examples.get(relation));
				else{
					newCoverage.clear();
				}
			}

			if(oneAtom.getObject().equals(HornRule.START_NODE)){
				String relation = oneAtom.getRelation()+"(_,subject)";
				if(relation2examples.containsKey(relation))
					newCoverage.retainAll(relation2examples.get(relation));
				else{
					newCoverage.clear();
				}
			}
			if(oneAtom.getSubject().equals(HornRule.END_NODE)){
				String relation = oneAtom.getRelation()+"(object,_)";
				if(relation2examples.containsKey(relation))
					newCoverage.retainAll(relation2examples.get(relation));
				else{
					newCoverage.clear();
				}
			}

			if(oneAtom.getObject().equals(HornRule.END_NODE)){
				String relation = oneAtom.getRelation()+"(_,object)";
				if(relation2examples.containsKey(relation))
					newCoverage.retainAll(relation2examples.get(relation));
				else{
					newCoverage.clear();
				}
			}
			if(newCoverage.size()==0)
				break;
		}

		return newCoverage;
	}

	private void expandLiteral(Set<String> toAnalyse, Graph<String> totalGraph,MultipleGraphHornRule<String> hornRule, 
			Map<String,Set<Pair<String,String>>> expandedLiteral2examples){

		Set<Pair<String,String>> literalCoveredExamplesToAnalyse = Sets.newHashSet();
		for(String literalToAnalyse:toAnalyse){
			literalCoveredExamplesToAnalyse.clear();
			//get the current covered examples of the literal
			if(hornRule.getCoveredExamples(literalToAnalyse)==null)
				continue;
			literalCoveredExamplesToAnalyse.addAll(hornRule.getCoveredExamples(literalToAnalyse));
			//remove the already coveredExamples
			Set<Pair<String,String>> previouslyCoveredExamples = expandedLiteral2examples.get(literalToAnalyse);
			if(previouslyCoveredExamples==null){
				previouslyCoveredExamples = Sets.newHashSet();
				expandedLiteral2examples.put(literalToAnalyse, previouslyCoveredExamples);
			}
			literalCoveredExamplesToAnalyse.removeAll(previouslyCoveredExamples);

			if(literalCoveredExamplesToAnalyse.size()==0)
				continue;
			previouslyCoveredExamples.addAll(literalCoveredExamplesToAnalyse);

			Set<String> targetNodes = Sets.newHashSet();
			for(Pair<String,String> oneExample:literalCoveredExamplesToAnalyse){
				String targetPartExample = hornRule.getStartingVariable().equals(HornRule.START_NODE) ? 
						oneExample.getRight() : oneExample.getLeft();
						targetNodes.add(targetPartExample);
			}

			//for each covered examples, compare the current literal with all the literals of the covered example
			String literalLexicalForm = totalGraph.getLexicalForm(literalToAnalyse);
			Set<String> otherLiterals = totalGraph.getLiterals(targetNodes, this.maxRuleLen-hornRule.getLen()-1);
			for(String otherLiteral:otherLiterals){
				Set<String> outputRelations = SparqlExecutor.getLiteralRelation(literalLexicalForm, totalGraph.getLexicalForm(otherLiteral));

				if(outputRelations!=null){
					for(String relation:outputRelations){
						totalGraph.addEdge(literalToAnalyse, otherLiteral, relation, false);
						String inverseRelation = SparqlExecutor.getInverseRelation(relation);
						if(inverseRelation!=null)
							totalGraph.addEdge(otherLiteral, literalToAnalyse, inverseRelation, false);
					}
				}
			}

		}

	}

	/**
	 * The creation of inequalities edges is allowed only for 1,000,000 examples, otherwise it gets too big
	 * @param toAnalyse
	 * @param totalGraph
	 * @param hornRule
	 * @param expandedInequalityNodes2examples
	 */
	private void expandInequalityNodes(Set<String> toAnalyse, Graph<String> totalGraph,MultipleGraphHornRule<String> hornRule, 
			Map<String,Set<Pair<String,String>>> expandedInequalityNodes2examples){

		Set<Pair<String,String>> nodeCoveredExamplesToAnalyse = Sets.newHashSet();

		int totEdgesCount = 0;

		for(String nodeToAnalyse:toAnalyse){
			nodeCoveredExamplesToAnalyse.clear();
			//get the current covered examples of the node
			if(hornRule.getCoveredExamples(nodeToAnalyse)==null)
				continue;
			nodeCoveredExamplesToAnalyse.addAll(hornRule.getCoveredExamples(nodeToAnalyse));
			//remove the already coveredExamples
			Set<Pair<String,String>> previouslyCoveredExamples = expandedInequalityNodes2examples.get(nodeToAnalyse);
			if(previouslyCoveredExamples==null){
				previouslyCoveredExamples = Sets.newHashSet();
				expandedInequalityNodes2examples.put(nodeToAnalyse, previouslyCoveredExamples);
			}
			nodeCoveredExamplesToAnalyse.removeAll(previouslyCoveredExamples);

			//do not add inequalities if they have been already added or if the current rule does not allow inequality
			if(nodeCoveredExamplesToAnalyse.size()==0 || !hornRule.isInequalityExpandible()
					|| totalGraph.getTypes(nodeToAnalyse).size()==0)
				continue;

			previouslyCoveredExamples.addAll(nodeCoveredExamplesToAnalyse);

			Set<String> targetNodes = Sets.newHashSet();
			//consider only allowed number of examples
			for(Pair<String,String> oneExample:nodeCoveredExamplesToAnalyse){
				String targetPartExample = hornRule.getStartingVariable().equals(HornRule.START_NODE) ? oneExample.getRight() : oneExample.getLeft();
				targetNodes.add(targetPartExample);

			}

			Set<String> sameTypesNodes = totalGraph.getSameTypesNodes(totalGraph.getTypes(nodeToAnalyse),targetNodes, this.maxRuleLen-hornRule.getLen()-1);

			totEdgesCount += sameTypesNodes.size();

			//remove current node if exists
			sameTypesNodes.remove(nodeToAnalyse);
			for(String otherNode:sameTypesNodes){
				totalGraph.addEdge(nodeToAnalyse, otherNode, Constant.DIFF_REL, true);
			}

			if(totEdgesCount > 1000000)
				return;

		}

	}

}
