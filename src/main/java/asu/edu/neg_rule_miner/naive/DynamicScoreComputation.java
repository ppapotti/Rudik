package asu.edu.neg_rule_miner.naive;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Maps;

import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.model.MultipleGraphHornRule;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.model.statistic.StatisticsContainer;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public class DynamicScoreComputation {
	private double alpha = 0.3;
	private double beta = 0.6;
	private double gamma = 0.1;

	private double totalMiningExamplesCount;
	private double totalOtherExamplesCount;

	private Set<Pair<String,String>> solutionCoveredMiningExamples;
	private Set<Pair<String,String>> solutionCoveredRelativeOtherExamples;

	private Map<Set<RuleAtom>,Integer> rule2otherExamplesScore;

	private SparqlExecutor executor;

	private Set<String> relations;

	private String typeSubject;

	private String typeObject;

	private Set<Pair<String,String>> otherExamples;

	//keep track of valid expanded rules
	private Map<Set<RuleAtom>,Boolean> validRules2canBeExpanded;

	//true if goal is to discover positive rules
	private boolean minePositive = false;

	private boolean subjectFunction = true;

	private boolean objectFunction = true;

	private Set<String> notAdmissibleRules;

	public DynamicScoreComputation(int totalInductionExamples, int totalOtherExamples,SparqlExecutor executor,
			Set<String> relations, String typeSubject, String typeObject,Set<Pair<String,String>> otherExamples){

		this.totalMiningExamplesCount = totalInductionExamples;
		this.totalOtherExamplesCount = totalOtherExamples;
		this.solutionCoveredMiningExamples = Sets.newHashSet();
		this.solutionCoveredRelativeOtherExamples = Sets.newHashSet();
		this.rule2otherExamplesScore = Maps.newHashMap();
		this.executor = executor;
		this.relations = relations;
		this.typeSubject = typeSubject;
		this.typeObject = typeObject;
		this.otherExamples = otherExamples;
		this.validRules2canBeExpanded = Maps.newHashMap();
	}


	public double getScore(MultipleGraphHornRule<String> negativeHornRule, 
			Set<Pair<String,String>> positiveRelativeCoverage, int coveragePositive, int maxAtomLen){

		Set<Pair<String,String>> retainCoveredNegative = Sets.newHashSet();
		retainCoveredNegative.addAll(negativeHornRule.getCoveredExamples());
		retainCoveredNegative.removeAll(solutionCoveredMiningExamples);

		Set<Pair<String,String>> retainCoveredRelativePositive = Sets.newHashSet();
		retainCoveredRelativePositive.addAll(positiveRelativeCoverage);
		retainCoveredRelativePositive.removeAll(solutionCoveredRelativeOtherExamples);

		//compute the score
		double score = -alpha*(retainCoveredNegative.size()/totalMiningExamplesCount);

		score+=beta*coveragePositive/retainCoveredRelativePositive.size();

		score-=gamma*retainCoveredRelativePositive.size()/totalOtherExamplesCount;

		return score;
	}

	public void addCoveredNegative(Set<Pair<String,String>> negativeExamples){
		this.solutionCoveredMiningExamples.addAll(negativeExamples);
	}

	public void addCoveredRelativePositive(Set<Pair<String,String>> positiveExamples){
		this.solutionCoveredRelativeOtherExamples.addAll(positiveExamples);
	}

	public Pair<MultipleGraphHornRule<String>,Double> getNextBestRule(Set<MultipleGraphHornRule<String>> negativeHornRules,
			Map<Set<RuleAtom>,Set<Pair<String,String>>> rule2relativePositiveCoverage, int maxAtomLen){

		double bestScore = Integer.MAX_VALUE;
		MultipleGraphHornRule<String> bestRule = null;

		//when computing the next best score, rules with score >=0 can be deleted
		Set<MultipleGraphHornRule<String>> positiveScoreRules = Sets.newHashSet();
		for(MultipleGraphHornRule<String> plausibleRule:negativeHornRules){

			double currentScore = this.getScore(plausibleRule, rule2relativePositiveCoverage.get(plausibleRule.getRules()), 0, maxAtomLen);
			if(currentScore>=0){
				positiveScoreRules.add(plausibleRule);
				continue;
			}

			if(plausibleRule.isValid()){
				if(!this.rule2otherExamplesScore.containsKey(plausibleRule.getRules())){
					int positiveCoverage = this.getRuleMatchingOtherExamples(plausibleRule.getRules());

					this.rule2otherExamplesScore.put(plausibleRule.getRules(),positiveCoverage);
					if(plausibleRule.isExpandible(maxAtomLen)){
						if(positiveCoverage == 0)
							this.validRules2canBeExpanded.put(plausibleRule.getRules(), false);
						else
							this.validRules2canBeExpanded.put(plausibleRule.getRules(), true);
					}
				}
				int positiveCoverage = this.rule2otherExamplesScore.get(plausibleRule.getRules());
				//check if the rule is elegible to be further expanded
				if(this.validRules2canBeExpanded.get(plausibleRule.getRules())!=null&&
						this.validRules2canBeExpanded.get(plausibleRule.getRules()))
					currentScore = this.getScore(plausibleRule, rule2relativePositiveCoverage.get(plausibleRule.getRules()), 0, maxAtomLen);
				else
					currentScore = this.getScore(plausibleRule, rule2relativePositiveCoverage.get(plausibleRule.getRules()), positiveCoverage, maxAtomLen);
			}

			if(currentScore>=0){
				positiveScoreRules.add(plausibleRule);
				continue;
			}

			if(currentScore<bestScore){
				bestScore = currentScore;
				bestRule = plausibleRule;
			}
		}

		//remove rules with positive scores from the set of plausible rules
		negativeHornRules.removeAll(positiveScoreRules);

		//if the rule has a negative score, update covered negative and covered relative positive
		if(bestScore<0){
			//if the rule has to be avoided, remove it and recompute the next best rule
			if(containsForbiddenAtoms(bestRule)){
				negativeHornRules.remove(bestRule);
				rule2relativePositiveCoverage.remove(bestRule.getRules());
				return getNextBestRule(negativeHornRules, rule2relativePositiveCoverage, maxAtomLen);
			}

			//update solution coverage only if the rule is admissible and it should not be further expanded
			if(this.isAdmissible(bestRule) && (!this.validRules2canBeExpanded.containsKey(bestRule.getRules()) || 
					!this.validRules2canBeExpanded.get(bestRule.getRules()))){
				this.solutionCoveredMiningExamples.addAll(bestRule.getCoveredExamples());
				this.solutionCoveredRelativeOtherExamples.addAll(rule2relativePositiveCoverage.get(bestRule.getRules()));
			}
			return Pair.of(bestRule, bestScore);
		}

		return null;
	}

	public boolean isExpandible(Set<RuleAtom> rule){
		return this.validRules2canBeExpanded.get(rule);
	}

	public void setMandatoryExpandible(Set<RuleAtom> rule){
		this.validRules2canBeExpanded.put(rule, false);
	}

	/**
	 * A rule is admissibe if it is valid and it covers less or equal 20% of the positive
	 * @return
	 */
	public boolean isAdmissible(MultipleGraphHornRule<String> rule){
		if(!rule.isValid() || this.rule2otherExamplesScore.get(rule.getRules())==null )
			return false;

		//check the 20% score
		int positiveScore = this.rule2otherExamplesScore.get(rule.getRules());
		if(positiveScore/totalOtherExamplesCount > 0.2)
			return false;

		return true;
	}

	private boolean containsForbiddenAtoms(MultipleGraphHornRule<String> rule){
		if(notAdmissibleRules!=null){
			if(notAdmissibleRules.contains(rule.toString()))
				return true;
			for(String oneNotAdmissibleRule : notAdmissibleRules){
				if(rule.toString().contains(oneNotAdmissibleRule))
					return true;
			}
		}

		return false;
	}

	public void setMinePositive(boolean minePositive, boolean subjectFunction, boolean objectFunction){
		this.minePositive = minePositive;

		//if trying to mine positive, target relations connecting subjects and objects are not plausible rules
		if(this.minePositive == true){
			this.notAdmissibleRules = Sets.newHashSet();
			String argumentRelation = "("+MultipleGraphHornRule.START_NODE+","+MultipleGraphHornRule.END_NODE+")";
			for(String relation:this.relations){
				notAdmissibleRules.add(relation+argumentRelation);
			}
			this.subjectFunction = subjectFunction;
			this.objectFunction = objectFunction;
		}
	}

	public int getRuleMatchingOtherExamples(Set<RuleAtom> rules){
		StatisticsContainer.increaseValidationQuery();
		
		long startingTime = System.currentTimeMillis();
		int coverage = -1;
		try{
			if(minePositive){
				coverage = this.executor.getMatchingNegativeExamples(rules, 
						relations, typeSubject, typeObject, otherExamples,subjectFunction,objectFunction).size();
			}
			else{
				coverage = this.executor.getMatchingPositiveExamples(rules, 
						relations, typeSubject, typeObject, otherExamples).size();
			}
			if(coverage==-1)
				coverage = Integer.MAX_VALUE;
		}
		catch(Exception e){
			coverage = Integer.MAX_VALUE;
		}
		long endingTime = System.currentTimeMillis();
		StatisticsContainer.increaseTimeValidationQuery(endingTime-startingTime);
		
		return coverage;
	}

	public int getRulePositiveScore(Set<RuleAtom> rule){
		Integer coverage = this.rule2otherExamplesScore.get(rule);
		return coverage!=null ? coverage : 0;
	}

}
