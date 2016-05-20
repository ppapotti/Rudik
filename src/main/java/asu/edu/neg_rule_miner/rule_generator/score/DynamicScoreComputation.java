package asu.edu.neg_rule_miner.rule_generator.score;

import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.configuration.Constant;
import asu.edu.neg_rule_miner.model.MultipleGraphHornRule;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.model.statistic.StatisticsContainer;
import asu.edu.neg_rule_miner.rule_generator.DynamicPruningRuleDiscovery;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

/**
 * Implements score computation based on coverge over generation set, coverage over validation set and unbouned coverage over validation set
 * @author sortona
 *
 */
public class DynamicScoreComputation {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(DynamicScoreComputation.class.getName());
	
	private double alpha = 0.3;
	private double beta = 0.6;
	private double gamma = 0.1;

	//by default set to 0.2
	private double validationExamplesThreshold = 0.2;

	private double totalGenerationExamples;
	private double totalValidationExamples;

	private Set<Pair<String,String>> solutionCoveredGenerationExamples;
	private Set<Pair<String,String>> solutionCoveredValidationExamples;

	private Map<Set<RuleAtom>,Integer> rule2validationCoverage;

	private SparqlExecutor executor;

	private Set<String> relations;

	private String typeSubject;

	private String typeObject;

	private Set<Pair<String,String>> validationExamples;

	//keep track of valid expanded rules
	private Map<Set<RuleAtom>,Boolean> validRules2canBeExpanded;

	//true if goal is to discover positive rules
	private boolean minePositive = false;

	private boolean subjectFunction = true;

	private boolean objectFunction = true;

	//if mining positive rules with target relation should not be mined
	private Set<String> notAdmissibleRules;

	public DynamicScoreComputation(int totalGenerationExamples, int totalValidationExamples,SparqlExecutor executor,
			Set<String> relations, String typeSubject, String typeObject,Set<Pair<String,String>> validationExamples){

		this.totalGenerationExamples = totalGenerationExamples;
		this.totalValidationExamples = totalValidationExamples;
		this.solutionCoveredGenerationExamples = Sets.newHashSet();
		this.solutionCoveredValidationExamples = Sets.newHashSet();
		this.rule2validationCoverage = Maps.newHashMap();
		this.executor = executor;
		this.relations = relations;
		this.typeSubject = typeSubject;
		this.typeObject = typeObject;
		this.validationExamples = validationExamples;
		this.validRules2canBeExpanded = Maps.newHashMap();

		//read parameters from config file
		Configuration config = ConfigurationFacility.getConfiguration();
		
		//read validationExamplesThreshold
		if(config.containsKey(Constant.CONF_VALIDATION_THRESHOLD)){
			try{
				validationExamplesThreshold = config.getDouble(Constant.CONF_VALIDATION_THRESHOLD);
			}
			catch(Exception e){
				LOGGER.error("Error while trying to read the "
						+ "validation threshold configuration parameter. Set to 0.2.");
			}
		}

		//read alpha, beta, gamma
		if(config.containsKey(Constant.CONF_SCORE_ALPHA) && config.containsKey(Constant.CONF_SCORE_BETA) && 
				config.containsKey(Constant.CONF_SCORE_GAMMA)){
			try{
				alpha = config.getDouble(Constant.CONF_SCORE_ALPHA);
				beta = config.getDouble(Constant.CONF_SCORE_BETA);
				gamma = config.getDouble(Constant.CONF_SCORE_GAMMA);
			}
			catch(Exception e){
				LOGGER.error("Error while trying to read the "
						+ "alpha, beta, gamma score configuration parameters. Set to 0.3, 0.6, 0.1.");
			}
		}
	}


	public double getScore(MultipleGraphHornRule<String> hornRule, 
			Set<Pair<String,String>> validationRelativeCoverage, int validationCoverage, int maxAtomLen){

		Set<Pair<String,String>> retainCoveredNegative = Sets.newHashSet();
		retainCoveredNegative.addAll(hornRule.getCoveredExamples());
		retainCoveredNegative.removeAll(solutionCoveredGenerationExamples);

		Set<Pair<String,String>> retainCoveredRelativePositive = Sets.newHashSet();
		retainCoveredRelativePositive.addAll(validationRelativeCoverage);
		retainCoveredRelativePositive.removeAll(solutionCoveredValidationExamples);

		//compute the score
		double score = -alpha*(retainCoveredNegative.size()/totalGenerationExamples);

		score+=beta*validationCoverage/retainCoveredRelativePositive.size();

		score-=gamma*retainCoveredRelativePositive.size()/totalValidationExamples;

		return score;
	}

	public void addCoveredGeneration(Set<Pair<String,String>> examples){
		this.solutionCoveredGenerationExamples.addAll(examples);
	}

	public void addCoveredRelativeValidation(Set<Pair<String,String>> examples){
		this.solutionCoveredValidationExamples.addAll(examples);
	}

	public Pair<MultipleGraphHornRule<String>,Double> getNextBestRule(Set<MultipleGraphHornRule<String>> rules,
			Map<Set<RuleAtom>,Set<Pair<String,String>>> rule2relativeValidationCoverage, int maxAtomLen){

		double bestScore = Integer.MAX_VALUE;
		MultipleGraphHornRule<String> bestRule = null;

		//when computing the next best score, rules with score >=0 can be deleted
		Set<MultipleGraphHornRule<String>> positiveScoreRules = Sets.newHashSet();
		for(MultipleGraphHornRule<String> plausibleRule:rules){

			double currentScore = this.getScore(plausibleRule, rule2relativeValidationCoverage.get(plausibleRule.getRules()), 0, maxAtomLen);
			if(currentScore>=0){
				positiveScoreRules.add(plausibleRule);
				continue;
			}

			if(plausibleRule.isValid()){
				if(!this.rule2validationCoverage.containsKey(plausibleRule.getRules())){
					int positiveCoverage = this.getRuleMatchingValidationExamples(plausibleRule.getRules());

					this.rule2validationCoverage.put(plausibleRule.getRules(),positiveCoverage);
					if(plausibleRule.isExpandible(maxAtomLen)){
						if(positiveCoverage == 0)
							this.validRules2canBeExpanded.put(plausibleRule.getRules(), false);
						else
							this.validRules2canBeExpanded.put(plausibleRule.getRules(), true);
					}
				}
				int positiveCoverage = this.rule2validationCoverage.get(plausibleRule.getRules());
				//check if the rule is elegible to be further expanded
				if(this.validRules2canBeExpanded.get(plausibleRule.getRules())!=null&&
						this.validRules2canBeExpanded.get(plausibleRule.getRules()))
					currentScore = this.getScore(plausibleRule, rule2relativeValidationCoverage.get(plausibleRule.getRules()), 0, maxAtomLen);
				else
					currentScore = this.getScore(plausibleRule, rule2relativeValidationCoverage.get(plausibleRule.getRules()), positiveCoverage, maxAtomLen);
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
		rules.removeAll(positiveScoreRules);

		//if the rule has a negative score, update covered negative and covered relative positive
		if(bestScore<0){
			//if the rule has to be avoided, remove it and recompute the next best rule
			if(containsForbiddenAtoms(bestRule)){
				rules.remove(bestRule);
				rule2relativeValidationCoverage.remove(bestRule.getRules());
				return getNextBestRule(rules, rule2relativeValidationCoverage, maxAtomLen);
			}

			//update solution coverage only if the rule is admissible and it should not be further expanded
			if(this.isAdmissible(bestRule) && (!this.validRules2canBeExpanded.containsKey(bestRule.getRules()) || 
					!this.validRules2canBeExpanded.get(bestRule.getRules()))){
				this.solutionCoveredGenerationExamples.addAll(bestRule.getCoveredExamples());
				this.solutionCoveredValidationExamples.addAll(rule2relativeValidationCoverage.get(bestRule.getRules()));
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
	 * A rule is admissible if it is valid and it covers less or equal validation_threshold of validation set
	 * @return
	 */
	public boolean isAdmissible(MultipleGraphHornRule<String> rule){
		if(!rule.isValid() || this.rule2validationCoverage.get(rule.getRules())==null )
			return false;

		//check the validation threshold coverage
		int validationCoverage = this.rule2validationCoverage.get(rule.getRules());
		if(validationCoverage/totalValidationExamples > this.validationExamplesThreshold)
			return false;

		//check covered validation examples are not more than covered generation examples
		if(validationCoverage+1 >= rule.getCoveredExamples().size())
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

	/**
	 * Execute validation query
	 * @param rules
	 * @return
	 */
	public int getRuleMatchingValidationExamples(Set<RuleAtom> rules){
		StatisticsContainer.increaseValidationQuery();

		long startingTime = System.currentTimeMillis();
		int coverage = -1;
		try{
			if(minePositive){
				coverage = this.executor.getMatchingNegativeExamples(rules, 
						relations, typeSubject, typeObject, validationExamples,subjectFunction,objectFunction).size();
			}
			else{
				coverage = this.executor.getMatchingPositiveExamples(rules, 
						relations, typeSubject, typeObject, validationExamples).size();
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

	public int getRuleValidationScore(Set<RuleAtom> rule){
		Integer coverage = this.rule2validationCoverage.get(rule);
		return coverage!=null ? coverage : 0;
	}

}