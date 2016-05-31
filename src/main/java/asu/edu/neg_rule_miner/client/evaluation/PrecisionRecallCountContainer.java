package asu.edu.neg_rule_miner.client.evaluation;

public class PrecisionRecallCountContainer {
	long ruleCorrectExamplesCount = 0;
	long ruleWrongExamplesRuleCount = 0;
	long ruleUnknownCount = 0;
	long ruleExamplesCount = 0; 
	long totalSivlerNegativeCount = 0;

	public double getPrecision(){
		return (ruleCorrectExamplesCount+0.)/(ruleWrongExamplesRuleCount+ruleCorrectExamplesCount);
	}

	public double getRecall(){
		return (ruleCorrectExamplesCount+0.)/totalSivlerNegativeCount;
	}

	public double getUnknown(){
		return (ruleUnknownCount+0.)/ruleExamplesCount;
	}

	public long getTotalRuleExamples(){
		return ruleExamplesCount;
	}

}
