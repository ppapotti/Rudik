package asu.edu.neg_rule_miner.naive;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.configuration.Constant;
import asu.edu.neg_rule_miner.model.HornRule;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public class SequentialNaiveRuleDiscovery {

	private int numThreads = 1;

	private int threshold = 0;

	private int maxRuleLen = 3;

	private final static Logger LOGGER = 
			LoggerFactory.getLogger(OneExampleRuleDiscovery.class.getName());

	public SequentialNaiveRuleDiscovery(){
		Configuration config = ConfigurationFacility.getConfiguration();
		//read number of threads if specified in the conf file
		if(config.containsKey(Constant.CONF_NUM_THREADS)){
			try{
				numThreads = config.getInt(Constant.CONF_NUM_THREADS);
			}
			catch(Exception e){
				LOGGER.error("Error while trying to read the numbuer of "
						+ "threads configuration parameter. Set to 1.");
			}
		}

		//read threshold if specified in the conf file
		if(ConfigurationFacility.getConfiguration().containsKey(Constant.CONF_THRESHOLD)){
			try{
				threshold = config.getInt(Constant.CONF_THRESHOLD);
			}
			catch(Exception e){
				LOGGER.error("Error while trying to read the "
						+ "threshold configuration parameter. Set to 0.");
			}
		}

		//read maxRuleLength if specified in the conf file
		if(ConfigurationFacility.getConfiguration().containsKey(Constant.CONF_MAX_RULE_LEN)){
			try{
				maxRuleLen = config.getInt(Constant.CONF_MAX_RULE_LEN);
			}
			catch(Exception e){
				LOGGER.error("Error while trying to read the "
						+ "maximum rule length configuration parameter. Set to 3.");
			}
		}

	}

	public Map<Set<RuleAtom>,Set<Pair<RDFNode,RDFNode>>> 
	discoverHornRules(Set<Pair<RDFNode,RDFNode>> negativeExamples){

		long start = System.currentTimeMillis();

		Map<Set<RuleAtom>,Set<Pair<RDFNode,RDFNode>>> finalRules = Maps.newHashMap();

		Graph<RDFNode> graph;
		List<HornRule<RDFNode>> rulesToDiscover = Lists.newLinkedList();
		Set<HornRule<RDFNode>> discoveredRules = Sets.newHashSet();
		Map<Pair<RDFNode,RDFNode>,Graph<RDFNode>> example2graph = Maps.newHashMap();
		Set<RDFNode> nodeToAnalyse = Sets.newHashSet();
		Set<String> analysedNodes = Sets.newHashSet();
		Map<RDFNode,Set<Edge<RDFNode>>> node2neighbours = Maps.newHashMap();
		int totalExample = negativeExamples.size();
		int currentExample =0;
		for(Pair<RDFNode,RDFNode> negativeExample:negativeExamples){
			currentExample++;
			LOGGER.debug("Considering example {} ({} out of {})",negativeExample,currentExample,totalExample);
			graph = new Graph<RDFNode>();
			graph.addNode(negativeExample.getRight());
			graph.addNode(negativeExample.getLeft());

			rulesToDiscover.clear();
			discoveredRules.clear();

			example2graph.clear();
			example2graph.put(negativeExample, graph);
			rulesToDiscover.add(new HornRule<RDFNode>(negativeExample,graph));

			analysedNodes.clear();

			HornRule<RDFNode> currentRule;
			while(rulesToDiscover.size()>0){
				currentRule = rulesToDiscover.get(0);
				rulesToDiscover.remove(0);
				if(discoveredRules.contains(currentRule))
					continue;
				discoveredRules.add(currentRule);

				if(currentRule.isValid()){
					Set<Pair<RDFNode,RDFNode>> supportedExamples = finalRules.get(currentRule.getRules());
					if(supportedExamples==null){
						supportedExamples = Sets.newHashSet();
					}
					supportedExamples.add(negativeExample);
					//add as a final rule only if it supports is bigger than threshold
					if(supportedExamples.size()>threshold){
						finalRules.put(currentRule.getRules(), supportedExamples);
					}
					LOGGER.debug("Found rule: '{}', current support now {}.",currentRule,supportedExamples.size());
				}

				if(!currentRule.isExpandible(maxRuleLen)){
					continue;
				}

				nodeToAnalyse.clear();

				//since last step has to end either in the ending nodes or in an already seen nodes, no need to expand
				if(currentRule.getLen()<maxRuleLen-1){
					Set<RDFNode> toAnalyse = currentRule.getCurrentNodes();

					for(RDFNode node:toAnalyse){
						if(analysedNodes.contains(node.toString()))
							continue;
						//check if the entity has been already expanded
						if(node2neighbours.containsKey(node)){
							for(Edge<RDFNode> edge:node2neighbours.get(node)){
								RDFNode toAdd = edge.getNodeSource();
								if(node.equals(toAdd))
									toAdd = edge.getNodeEnd();
								graph.addNode(toAdd);
								graph.addEdge(edge, true);
							}
							continue;
						}
						analysedNodes.add(node.toString());
						nodeToAnalyse.add(node);
					}

					//LOGGER.debug("Expanding the graph querying for {} nodes...",nodeToAnalyse.size());
					expandGraphs(nodeToAnalyse, graph,node2neighbours, numThreads);
					//LOGGER.debug("Graph expansions completed.");
				}

				Map<RuleAtom,Set<Edge<RDFNode>>> nextPaths = 
						currentRule.nextPlausibleRules(currentRule.getLen()==maxRuleLen-1);

				for(RuleAtom rule:nextPaths.keySet()){
					HornRule<RDFNode> newRule = currentRule.duplicateRule();
					Set<Edge<RDFNode>> copySet= Sets.newHashSet(nextPaths.get(rule));
					newRule.addRuleAtom(rule, copySet);
					rulesToDiscover.add(newRule);
				}

			}

		}


		LOGGER.debug("Total time to compute all possible rules: {} seconds.",(System.currentTimeMillis()-start)/1000.);

		return finalRules;

	}

	/**
	 * Utility method to print final statistic about all the rules computed
	 * @param rules
	 * @param relations
	 * @param typeSubject
	 * @param typeObject
	 * @param outputFile
	 * @throws IOException
	 */
	public void printRulesStatistics(Map<Set<RuleAtom>,Set<Pair<RDFNode,RDFNode>>> rules, 
			Set<String>relations, String typeSubject, 
			String typeObject, File outputFile) throws IOException{
		LOGGER.debug("Ordering rules and computing positive examples coverage for output.");
		long start = System.currentTimeMillis();
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile)); 
		outputWriter.write("\"Id\",\"Rule\",\"Negative Examples Coverage\",\"Positive Examples Coverage\",\"Total Coverage\",\"Covered Negatve Examples\"\n");
		Map<Pair<RDFNode,RDFNode>,String> example2id = Maps.newHashMap();
		int exampleCount=0;

		Map<String,Set<Pair<RDFNode,RDFNode>>> rule2coverage = Maps.newHashMap();
		int ruleCount = 0;
		int previousCount = -1;
		Set<RuleAtom> bestRule = null;

		Set<Set<RuleAtom>> seenRules = Sets.newHashSet();
		while(rules.size()>0){
			//get the most covered rule
			int mostCover = -1;
			for(Set<RuleAtom> rule:rules.keySet()){
				int cover = rules.get(rule).size();
				if(cover == previousCount){
					bestRule = rule;
					mostCover = cover;
					break;
				}
				if(cover>mostCover){
					mostCover = cover;
					bestRule = rule;

				}
			}
			if(seenRules.contains(bestRule)){
				rules.remove(bestRule);
				continue;
			}
			
			//addd this rule and the equivalent rule to avoid rules duplicate
			seenRules.add(bestRule);
			Set<RuleAtom> equivalentRule = Sets.newHashSet();
			Set<String> variables = Sets.newHashSet();
			for(RuleAtom rule:bestRule){
				variables.add(rule.getSubject());
				variables.add(rule.getObject());
			}
			if(variables.size()>3){
				for(RuleAtom rule:bestRule){
					String subject = rule.getSubject();
					if(!subject.equals(HornRule.START_NODE)&&!subject.equals(HornRule.END_NODE)){
						subject = "v"+(variables.size()-2-Integer.parseInt(subject.replaceAll("v", ""))-1);
					}
					String object = rule.getObject();
					if(!object.equals(HornRule.START_NODE)&&!object.equals(HornRule.END_NODE)){
						object = "v"+(variables.size()-2-Integer.parseInt(object.replaceAll("v", ""))-1);
					}
					equivalentRule.add(new RuleAtom(subject, rule.getRelation(), object));
				}
				seenRules.add(equivalentRule);
			}

			//read the examples covered
			Set<Pair<RDFNode,RDFNode>> coveredExamples = rules.get(bestRule);

			rules.remove(bestRule);
			previousCount = mostCover;

			String ruleId = "r_"+ruleCount;
			ruleCount++;

			for(Pair<RDFNode,RDFNode> coveredExample:coveredExamples){
				String currentExample = example2id.get(coveredExample);
				if(currentExample==null){
					currentExample="e"+exampleCount;
					example2id.put(coveredExample, currentExample);
					exampleCount++;
				}
			}

			String coveredExamplesString = coveredExamples+"";
			boolean isSubset=false;
			//check if the covered examples are subset of something else
			for(String otherRule:rule2coverage.keySet()){
				Set<Pair<RDFNode,RDFNode>> otherRuleCoverage = rule2coverage.get(otherRule);
				if(otherRuleCoverage.containsAll(coveredExamples)){
					coveredExamplesString="subset of "+otherRule+" - "+coveredExamplesString;
					isSubset=true;
					break;
				}
			}
			if(!isSubset)
				rule2coverage.put(ruleId,coveredExamples);


			int positiveExampleSupport = -1;
			try{
				positiveExampleSupport = this.getSparqlExecutor().
						getSupportivePositiveExamples(bestRule, 
								relations, typeObject, typeSubject);
			}
			catch(Exception e){
				//continue
			}
			int totalSupport = -1;
			//do not count total coverage, takes too long
			//			try{
			//				totalSupport = this.getSparqlExecutor().getTotalCoveredExample(rule, typeSubject, typeObject);
			//			}
			//			catch(Exception e){
			//				//continue
			//			}
			outputWriter.write("\""+ruleId+"\",\""+bestRule+"\",\""+coveredExamples.size()+
					"\",\""+positiveExampleSupport+"\",\""+totalSupport+"\",\""+coveredExamplesString+"\"\n");

		}
		outputWriter.write("\nExamples Id Mappings:\n");
		for(Pair<RDFNode,RDFNode> example:example2id.keySet()){
			outputWriter.write("\""+example+"\",\""+example2id.get(example)+"\"\n");
		}
		outputWriter.close();
		LOGGER.debug("Final report writeen in '{}' in {} seconds.",(System.currentTimeMillis()-start)/1000.);
	}

	private void expandGraphs(Set<RDFNode> toAnalyse, Graph<RDFNode> graph,Map<RDFNode,Set<Edge<RDFNode>>> node2neghbours,
			int numThreads){

		List<Thread> activeThreads = Lists.newLinkedList();
		List<Set<RDFNode>> currentInputs = 
				this.splitNodesThreads(toAnalyse, numThreads);

		int i=0;
		for(Set<RDFNode> currentInput:currentInputs){
			i++;
			// Create the thread and add it to the list
			final Thread current_thread = new Thread(new OneExampleRuleDiscovery(currentInput,graph,
					this.getSparqlExecutor(),node2neghbours), "Thread"+i);
			activeThreads.add(current_thread);

			// start the thread and querydbpedia
			try {
				current_thread.start();
			}
			// if thread is not able to finish its job, just continue and save
			// into the log the problem
			catch (final IllegalThreadStateException e) {
				LOGGER.error("Thread with id '{}' encountered a problem.", current_thread.getId(), e);
				continue;
			}
		}

		for (final Thread t : activeThreads) {
			try {

				t.join(0);

			} catch (final InterruptedException e) {
				LOGGER.error("Thread with id '{}' was unable to complete its job.", t.getId());
				continue;
			}
		}
	}

	private List<Set<RDFNode>> splitNodesThreads(Set<RDFNode> input, int numThread){
		List<Set<RDFNode>> outputThreads = Lists.newLinkedList();

		int i=0;
		for(RDFNode currentNode:input){
			if(i==numThread)
				i=0;
			if(i>=outputThreads.size()){
				Set<RDFNode> currentSet = Sets.newHashSet();
				outputThreads.add(currentSet);
			}
			Set<RDFNode> currentSet = outputThreads.get(i);
			currentSet.add(currentNode);
			i++;
		}
		return outputThreads;
	}

	/**
	 * Generate negative examples for the input relations
	 * @param relations
	 * 			name of input relations to generate negative examples for
	 * @param typeObject
	 * 			type of the object of the relation
	 * @param typeSubject
	 * 			type of the subject of the relation
	 * @param filtered
	 * 			decide whether returns only a subset of the negative examples, one for each relation
	 * @return
	 */
	public Set<Pair<RDFNode,RDFNode>> generateNegativeExamples(Set<String> relations, String typeSubject, 
			String typeObject){


		int totalNumberExample = -1;

		try{
			//read total number examples from configuration file
			Configuration config = ConfigurationFacility.getConfiguration();
			if(config.containsKey(Constant.CONF_NUM_EXAMPLE)){
				totalNumberExample = config.getInt(Constant.CONF_NUM_EXAMPLE);
			}
		}
		catch(Exception e){
			LOGGER.error("Not able to read the total number examples parameter from configuration file.",e);
		}
		if(totalNumberExample>=0){
			return this.getSparqlExecutor().generateFilteredNegativeExamples(relations, 
					typeSubject, typeObject,totalNumberExample);
		}
		else
			return this.getSparqlExecutor().generateNegativeExamples(relations, 
					typeSubject, typeObject);

	}

	/**
	 * Read negative example from an input file.
	 * File must contain for each line a pair subject object separated with a tab character
	 * @param inputFile
	 * @return
	 */
	public Set<Pair<RDFNode,RDFNode>> generateNegativeExamples(File inputFile){
		try{
			return this.getSparqlExecutor().readNegativeExamplesFromFile(inputFile);
		}
		catch(IOException e){
			LOGGER.error("Error while reading the input negative examples file '{}'",
					inputFile.getAbsolutePath(),e);
		}
		return null;

	}

	/**
	 * Read the sparql executor from configuration file
	 * @return
	 */
	private SparqlExecutor getSparqlExecutor(){

		if(!ConfigurationFacility.getConfiguration().containsKey(Constant.CONF_SPARQL_ENGINE))
			throw new RuleMinerException("Sparql engine parameters not found in the configuration file.", 
					LOGGER);

		Configuration subConf = ConfigurationFacility.getConfiguration().subset(Constant.CONF_SPARQL_ENGINE);

		if(!subConf.containsKey("class"))
			throw new RuleMinerException("Need to specify the class implementing the Sparql engine "
					+ "in the configuration file under parameter 'class'.", LOGGER);

		SparqlExecutor endpoint;
		try{
			Constructor<?> c = Class.forName(subConf.getString("class")).
					getDeclaredConstructor(Configuration.class);
			c.setAccessible(true);
			endpoint = (SparqlExecutor) 
					c.newInstance(new Object[] {subConf});
		}
		catch(Exception e){
			throw new RuleMinerException("Error while instantiang the sparql executor enginge.", e,LOGGER);
		}
		return endpoint;
	}

}
