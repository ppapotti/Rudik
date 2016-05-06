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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.configuration.Constant;
import asu.edu.neg_rule_miner.model.GraphHornRule;
import asu.edu.neg_rule_miner.model.HornRule;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public class SequentialNaiveRuleDiscovery {

	protected int numThreads = 1;

	protected int threshold = 0;

	protected int maxRuleLen = 3;

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

	/**
	 * This has to be completed with an ILP solver
	 * @param negativeExamples
	 * @param positiveExamples
	 * @return
	 */
	public Map<Set<RuleAtom>,Set<Pair<String,String>>> partiallyDiscoverHornRules(Set<Pair<String,String>> negativeExamples, 
			Set<Pair<String,String>> positiveExamples){

		long start = System.currentTimeMillis();

		Map<Set<RuleAtom>,Set<Pair<String,String>>> finalRules = Maps.newHashMap();

		Graph<String> graph;
		List<GraphHornRule<String>> rulesToDiscover = Lists.newLinkedList();
		Set<GraphHornRule<String>> discoveredRules = Sets.newHashSet();
		Map<Pair<String,String>,Graph<String>> example2graph = Maps.newHashMap();
		Set<String> nodeToAnalyse = Sets.newHashSet();
		Set<String> analysedNodes = Sets.newHashSet();
		Map<String,Map<Edge<String>,String>> node2neighbours = Maps.newHashMap();

		//record for each node its types
		Map<String,Set<String>> entity2types = Maps.newHashMap();
		int totalExample = negativeExamples.size();
		int currentExample =0;
		for(Pair<String,String> negativeExample:negativeExamples){
			currentExample++;
			LOGGER.debug("Considering example {} ({} out of {})",negativeExample,currentExample,totalExample);
			graph = new Graph<String>();
			graph.addNode(negativeExample.getRight());
			graph.addNode(negativeExample.getLeft());

			rulesToDiscover.clear();
			discoveredRules.clear();

			example2graph.clear();
			example2graph.put(negativeExample, graph);
			rulesToDiscover.add(new GraphHornRule<String>(negativeExample,graph));

			analysedNodes.clear();

			GraphHornRule<String> currentRule;
			while(rulesToDiscover.size()>0){
				currentRule = rulesToDiscover.get(0);
				rulesToDiscover.remove(0);
				if(discoveredRules.contains(currentRule))
					continue;
				discoveredRules.add(currentRule);

				if(currentRule.isValid()){
					Set<Pair<String,String>> supportedExamples = finalRules.get(currentRule.getRules());
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
					Set<String> toAnalyse = currentRule.getCurrentNodes();

					for(String node:toAnalyse){
						if(analysedNodes.contains(node.toString()))
							continue;

						analysedNodes.add(node.toString());
						nodeToAnalyse.add(node);
					}

					LOGGER.debug("Expanding the graph querying for {} nodes with rule {}...",
							nodeToAnalyse.size(),currentRule.toString());
					expandGraphs(nodeToAnalyse, graph,node2neighbours, entity2types,numThreads);
					LOGGER.debug("Graph expansions completed.");
				}

				Map<RuleAtom,Set<Edge<String>>> nextPaths = 
						currentRule.nextPlausibleRules(currentRule.getLen()==maxRuleLen-1);

				for(RuleAtom rule:nextPaths.keySet()){
					GraphHornRule<String> newRule = currentRule.duplicateRule();
					Set<Edge<String>> copySet= Sets.newHashSet(nextPaths.get(rule));
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
	public void printRulesStatistics(Map<Set<RuleAtom>,Set<Pair<String,String>>> rules, 
			Set<String>relations, String typeSubject, 
			String typeObject, int totalNegativeExamples, Set<Pair<String,String>> positiveExamples, File outputFile) throws IOException{
		LOGGER.debug("Ordering rules and computing positive examples coverage for output.");
		long start = System.currentTimeMillis();
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFile)); 
		outputWriter.write("\"Id\",\"Rule\",\"Tot. Neg.\",\"Neg. Coverage\",\"Neg. Fraction\"," +
				"\"Tot. Pos.\",\"Pos. Coverage\"," +
				"\"Pos. Fraction\",\"Rel. Pos. Coverage\",\"Rel. Pos. Fraction\",\"Score\",\"Weitghted Score\",\"Covered Negative Examples\"\n");
		Map<Pair<String,String>,String> example2id = Maps.newHashMap();
		int exampleCount=0;

		Map<String,Set<Pair<String,String>>> rule2coverage = Maps.newHashMap();
		int ruleCount = 0;
		int previousCount = -1;
		Set<RuleAtom> bestRule = null;

		Set<Set<RuleAtom>> seenRules = Sets.newHashSet();

		int totalPositiveExamples = positiveExamples.size();
		Map<String,Set<Pair<String,String>>> predicate2support = this.getSparqlExecutor().getRulePositiveSupport(positiveExamples);

		int currentRule=0;
		int totalRule = rules.size();
		BufferedWriter writerNegExamplesCovered = new BufferedWriter(new FileWriter(new File("negative_examples_coverage")));
		while(rules.size()>0){
			currentRule++;
			LOGGER.debug("Considering rule {} out of {}",currentRule,totalRule);
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

			//add this rule and the equivalent rule to avoid rules duplicate
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
			Set<Pair<String,String>> coveredExamples = rules.get(bestRule);
			writerNegExamplesCovered.write(coveredExamples+"\n");

			rules.remove(bestRule);
			previousCount = mostCover;

			String ruleId = "r_"+ruleCount;
			ruleCount++;

			String coveredExamplesString = "[";

			int i=0;
			for(Pair<String,String> coveredExample:coveredExamples){
				if(i>2500){
					coveredExamplesString+="..., ";
					break;
				}
				String currentExample = example2id.get(coveredExample);
				if(currentExample==null){
					currentExample="e"+exampleCount;
					example2id.put(coveredExample, currentExample);
					exampleCount++;
				}
				coveredExamplesString+=currentExample+", ";
			}
			coveredExamplesString=
					coveredExamplesString.substring(0,coveredExamplesString.length()-2)+"]";

			boolean isSubset=false;
			//check if the covered examples are subset of something else
			for(String otherRule:rule2coverage.keySet()){
				Set<Pair<String,String>> otherRuleCoverage = rule2coverage.get(otherRule);
				if(otherRuleCoverage.containsAll(coveredExamples)){
					coveredExamplesString="subset of "+otherRule+" - "+coveredExamplesString;
					isSubset=true;
					break;
				}
			}
			if(!isSubset)
				rule2coverage.put(ruleId,coveredExamples);

			Set<Pair<String,String>> totalCoveredExamples = Sets.newHashSet();
			totalCoveredExamples.addAll(positiveExamples);
			for(RuleAtom oneRule:bestRule){
				//do not consider literals
				if(oneRule.getRelation().equals(">=")||oneRule.getRelation().equals("<=")||oneRule.getRelation().equals("=")||
						oneRule.getRelation().equals("!=")){
					continue;
				}
				Set<Pair<String,String>> currentRuleCoveredExamples;
				if(oneRule.getSubject().equals(HornRule.START_NODE)){
					currentRuleCoveredExamples = predicate2support.get(oneRule.getRelation()+"(subject,_)");
					if(currentRuleCoveredExamples==null||currentRuleCoveredExamples.size()==0){
						totalCoveredExamples.clear();
						break;	
					}
					else
						totalCoveredExamples.retainAll(currentRuleCoveredExamples);
				}
				if(oneRule.getObject().equals(HornRule.START_NODE)){
					currentRuleCoveredExamples = predicate2support.get(oneRule.getRelation()+"(_,subject)");
					if(currentRuleCoveredExamples==null||currentRuleCoveredExamples.size()==0){
						totalCoveredExamples.clear();
						break;	
					}
					else
						totalCoveredExamples.retainAll(currentRuleCoveredExamples);
				}

				if(oneRule.getSubject().equals(HornRule.END_NODE)){
					currentRuleCoveredExamples = predicate2support.get(oneRule.getRelation()+"(object,_)");
					if(currentRuleCoveredExamples==null||currentRuleCoveredExamples.size()==0){
						totalCoveredExamples.clear();
						break;	
					}
					else
						totalCoveredExamples.retainAll(currentRuleCoveredExamples);
				}

				if(oneRule.getObject().equals(HornRule.END_NODE)){
					currentRuleCoveredExamples = predicate2support.get(oneRule.getRelation()+"(_,object)");
					if(currentRuleCoveredExamples==null||currentRuleCoveredExamples.size()==0){
						totalCoveredExamples.clear();
						break;	
					}
					else
						totalCoveredExamples.retainAll(currentRuleCoveredExamples);
				}
			}
			int relativePosCoverage = totalCoveredExamples.size();


			int posCoverage = -1;
			if(relativePosCoverage!=0){
				try{

					posCoverage = this.getSparqlExecutor().
							getSupportivePositiveExamples(bestRule, 
									relations, typeObject, typeSubject,positiveExamples);
				}
				catch(Exception e){
					//continue
				}
			}
			else
				posCoverage = 0;
			int totalSupport = -1;
			//do not count total coverage, takes too long
			//			try{
			//				totalSupport = this.getSparqlExecutor().getTotalCoveredExample(rule, typeSubject, typeObject);
			//			}
			//			catch(Exception e){
			//				//continue
			//			}

			int negCoverage = coveredExamples.size();
			double negFraction = (negCoverage+.0)/totalNegativeExamples;
			double posFraction = -1;
			if(posCoverage!=-1)
				posFraction = (posCoverage+0.)/totalPositiveExamples;
			double relPosFraction = -1;
			if(posCoverage!=-1)
				relPosFraction = (posCoverage+0.)/relativePosCoverage;

			double score = 1.1;
			if(posCoverage!=-1&&relativePosCoverage!=0){
				score = 0.33*(1-negFraction) + 0.33*relPosFraction + 0.33*(1-relativePosCoverage/(totalPositiveExamples+0.));
			}
			double weightedScore = score;
			if(weightedScore!=1.1){
				weightedScore = 0.4*(1-negFraction) + 0.5*relPosFraction + 0.1*(1-relativePosCoverage/(totalPositiveExamples+0.));
			}


			outputWriter.write("\""+ruleId+"\",\""+bestRule+"\",\""+totalNegativeExamples+"\",\""+negCoverage+"\",\""+
					negFraction+"\",\""+totalPositiveExamples+"\",\""+posCoverage+"\",\""+posFraction+"\",\""+relativePosCoverage+"\",\""+relPosFraction+"\",\""
					+score+"\",\""+weightedScore+"\",\""+coveredExamplesString+"\"\n");

		}
		writerNegExamplesCovered.close();
		outputWriter.write("\nExamples Id Mappings:\n");
		for(Pair<String,String> example:example2id.keySet()){
			outputWriter.write("\""+example+"\",\""+example2id.get(example)+"\"\n");
		}
		outputWriter.close();
		LOGGER.debug("Final report writeen in '{}' in {} seconds.",(System.currentTimeMillis()-start)/1000.);
	}

	protected void expandGraphs(Set<String> toAnalyse, Graph<String> graph,
			Map<String,Map<Edge<String>,String>> node2neghbours,Map<String,Set<String>> entity2types,
			int numThreads){

		//remove already expanded nodes
		Set<String> filteredToAnalyse = Sets.newHashSet();
		filteredToAnalyse.addAll(toAnalyse);
		if(node2neghbours!=null){
			for(String currentNode:toAnalyse){
				if(node2neghbours.containsKey(currentNode)){
					Map<Edge<String>,String> edge2literalForm = node2neghbours.get(currentNode);
					for(Edge<String> edge:edge2literalForm.keySet()){
						String lexicalForm = edge2literalForm.get(edge);
						String toAdd = edge.getNodeSource();
						if(currentNode.equals(toAdd))
							toAdd = edge.getNodeEnd();
						if(lexicalForm==null)
							graph.addNode(toAdd);
						else
							graph.addLiteralNode(toAdd, lexicalForm);
						graph.addEdge(edge, true);
					}
					filteredToAnalyse.remove(currentNode);
				}
			}
		}

		if(filteredToAnalyse.size()==0)
			return;

		List<Thread> activeThreads = Lists.newLinkedList();
		List<Set<String>> currentInputs = 
				this.splitNodesThreads(filteredToAnalyse, numThreads);

		int i=0;
		for(Set<String> currentInput:currentInputs){

			i++;
			// Create the thread and add it to the list
			final Thread current_thread = new Thread(new OneExampleRuleDiscovery(currentInput,graph,
					this.getSparqlExecutor(),node2neghbours,entity2types), "Thread"+i);
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

	private List<Set<String>> splitNodesThreads(Set<String> input, int numThread){
		List<Set<String>> outputThreads = Lists.newLinkedList();

		int i=0;
		for(String currentNode:input){
			if(i==numThread)
				i=0;
			if(i>=outputThreads.size()){
				Set<String> currentSet = Sets.newHashSet();
				outputThreads.add(currentSet);
			}
			Set<String> currentSet = outputThreads.get(i);
			currentSet.add(currentNode);
			i++;
		}
		return outputThreads;
	}


	public Set<Pair<String,String>> generatePositiveExamples(
			Set<String> relations, String typeSubject, String typeObject){
		return this.getSparqlExecutor().generatePositiveExamples(relations, typeSubject, typeObject);

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
	public Set<Pair<String,String>> generateNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject, boolean subjectFunction, boolean objectFunction){
		return this.getSparqlExecutor().generateUnionNegativeExamples(relations, 
				typeSubject, typeObject,subjectFunction,objectFunction);

	}
	
	public Set<Pair<String,String>> generateNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject){
		return this.getSparqlExecutor().generateUnionNegativeExamples(relations, 
				typeSubject, typeObject,true,true);

	}

	public Set<Pair<String,String>> getKBExamples(String query, String subject, 
			String object){
		return this.getSparqlExecutor().getKBExamples(query, subject, object, false);
	}

	/**
	 * Read negative example from an input file.
	 * File must contain for each line a pair subject object separated with a tab character
	 * @param inputFile
	 * @return
	 */
	public Set<Pair<String,String>> generateNegativeExamples(File inputFile){
		try{
			return this.getSparqlExecutor().readExamplesFromFile(inputFile);
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
	protected SparqlExecutor getSparqlExecutor(){

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
