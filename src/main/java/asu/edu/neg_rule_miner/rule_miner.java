package asu.edu.neg_rule_miner;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


//@XmlRootElement(name = "rule_miner")       //only needed if we also want to generate XML
//@XmlType(propOrder ={"alpha","beta","gamma","maxNoRule","noOfThreads"})

/*
 *  Here Webserver will look at /FoodItem Path for any requests.
 *  Author: Varun Gaur
 */
public class rule_miner {
    
    private double alpha;
	private double beta;
    private double gamma;
    private double maxNoRule;
    private double noOfThreads;
    private String kBase;
    private String typeOfSubject;
    private String typeOfObject;
    
    private String relName;
    private int edgeLimit;
    private int genLimit;
    private boolean genNegRules;
    
    private boolean useSmartSampling;
	//parameters for smart sampling
    private double alphaSmart;
    private double betaSmart;
    private double gammaSmart;
    private double subWeight;
    private double objWeight;
    private boolean topK;


	public void setTypeOfSubject(String typeOfSubject) {
		this.typeOfSubject = typeOfSubject;
	}



	public String getTypeOfObject() {
		return typeOfObject;
	}



	public void setTypeOfObject(String typeOfObject) {
		this.typeOfObject = typeOfObject;
	}


   
 

	public rule_miner(double alpha, double beta, double gamma, double maxNoRule, double noOfThreads, String relName,
			int edgeLimit, int genLimit, double alphaSmart, double betaSmart, double gammaSmart, double subWeight,
			double objWeight, boolean topK) {
		super();
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
		this.maxNoRule = maxNoRule;
		this.noOfThreads = noOfThreads;
		this.relName = relName;
		this.edgeLimit = edgeLimit;
		this.genLimit = genLimit;
		this.alphaSmart = alphaSmart;
		this.betaSmart = betaSmart;
		this.gammaSmart = gammaSmart;
		this.subWeight = subWeight;
		this.objWeight = objWeight;
		this.topK = topK;
	}



	public boolean isTopK() {
		return topK;
	}



	public void setTopK(boolean topK) {
		this.topK = topK;
	}



	public rule_miner() {
	}
	
	public String getRelName() {
		return relName;
	}
	public void setRelName(String relName) {
		this.relName = relName;
	}
	public int getEdgeLimit() {
		return edgeLimit;
	}
	public void setEdgeLimit(int edgeLimit) {
		this.edgeLimit = edgeLimit;
	}
	public int getGenLimit() {
		return genLimit;
	}
	public void setGenLimit(int genLimit) {
		this.genLimit = genLimit;
	}
	public double getAlphaSmart() {
		return alphaSmart;
	}
	public void setAlphaSmart(double alphaSmart) {
		this.alphaSmart = alphaSmart;
	}
	public double getBetaSmart() {
		return betaSmart;
	}
	public void setBetaSmart(double betaSmart) {
		this.betaSmart = betaSmart;
	}
	public double getGammaSmart() {
		return gammaSmart;
	}
	public void setGammaSmart(double gammaSmart) {
		this.gammaSmart = gammaSmart;
	}
	public double getSubWeight() {
		return subWeight;
	}
	public void setSubWeight(double subWeight) {
		this.subWeight = subWeight;
	}
	public double getObjWeight() {
		return objWeight;
	}
	public void setObjWeight(double objWeight) {
		this.objWeight = objWeight;
	}
	public double getAlpha() {
		return alpha;
	}
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}
	public double getBeta() {
		return beta;
	}
	public void setBeta(double beta) {
		this.beta = beta;
	}
	public double getGamma() {
		return gamma;
	}
	public void setGamma(double gamma) {
		this.gamma = gamma;
	}
	public double getMaxNoRule() {
		return maxNoRule;
	}
	public void setMaxNoRule(double maxNoRule) {
		this.maxNoRule = maxNoRule;
	}
	public double getNoOfThreads() {
		return noOfThreads;
	}
	public void setNoOfThreads(double noOfThreads) {
		this.noOfThreads = noOfThreads;
	}
	
	public boolean isUseSmartSampling() {
		return useSmartSampling;
	}

	public void setUseSmartSampling(boolean useSmartSampling) {
		this.useSmartSampling = useSmartSampling;
	}

	public boolean isGenNegRules() {
		return genNegRules;
	}
	
	public void setGenNegRules(boolean genNegRules) {
		this.genNegRules = genNegRules;
	}
	
    public String getkBase() {
		return kBase;
	}



	public void setkBase(String kBase) {
		this.kBase = kBase;
	}



	public String getTypeOfSubject() {
		return typeOfSubject;
	}
    
}