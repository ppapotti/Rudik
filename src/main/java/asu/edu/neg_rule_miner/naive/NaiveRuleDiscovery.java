package asu.edu.neg_rule_miner.naive;

import java.io.File;
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
import asu.edu.neg_rule_miner.model.HornRuleComparator;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public class NaiveRuleDiscovery {

	private int numThreads = 1;

	private int threshold = 0;

	private int maxRuleLen = 3;

	private final static Logger LOGGER = 
			LoggerFactory.getLogger(OneExampleRuleDiscovery.class.getName());

	public NaiveRuleDiscovery(){
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

	public void discoverHornRules(Set<Pair<RDFNode,RDFNode>> negativeExamples){

		long start = System.currentTimeMillis();
		Map<Pair<RDFNode,RDFNode>,Graph<RDFNode>> example2graph = Maps.newHashMap();


		Set<String> analysedNodes = Sets.newHashSet();

		//initialise to analyse map with negative examples nodes
		for(Pair<RDFNode,RDFNode> example:negativeExamples){
			Graph<RDFNode> g = new Graph<RDFNode>();
			g.addNode(example.getRight());
			g.addNode(example.getLeft());
			example2graph.put(example, g);
		}

		Set<HornRule<RDFNode>> rules = Sets.newTreeSet(new HornRuleComparator());
		rules.add(new HornRule<RDFNode>(example2graph));

		Set<HornRule<RDFNode>> seenRules = Sets.newHashSet();

		//can be improved, you can delete from the queue all rule with support<= threshold

		HornRule<RDFNode> bestRule;
		Map<RDFNode,Set<Graph<RDFNode>>> node2graphToAnalyse = Maps.newHashMap();
		while(rules.size()>0){
			bestRule = rules.iterator().next();
			rules.remove(bestRule);

			if(seenRules.contains(bestRule))
				continue;
			seenRules.add(bestRule);

			if(bestRule.getSupport()<threshold)
				break;

			LOGGER.debug("Considering rule: '{}' with support {}.",bestRule,bestRule.getSupport());
			if(bestRule.isValid())
				System.out.println("Next best rule:"+bestRule+", Supported by: ");

			if(!bestRule.isExpandible(maxRuleLen)){
				LOGGER.debug("Rule is not expandible!");
				continue;
			}

			node2graphToAnalyse.clear();

			//since last step has to end either in the ending nodes or in an already seen nodes, no need to expand
			if(bestRule.getLen()<maxRuleLen-1){
				Map<Graph<RDFNode>,Set<RDFNode>> graph2currentNodes = bestRule.getCurrentNodes();
				//create the map to analyze
				for(Graph<RDFNode> graph:graph2currentNodes.keySet()){
					Set<RDFNode> toAnalyse = graph2currentNodes.get(graph);

					for(RDFNode node:toAnalyse){
						if(analysedNodes.contains(node.toString()))
							continue;
						Set<Graph<RDFNode>> currentGraphs = node2graphToAnalyse.get(node);
						if(currentGraphs==null){
							currentGraphs = Sets.newHashSet();
							node2graphToAnalyse.put(node, currentGraphs);
						}
						currentGraphs.add(graph);
					}
				}

				LOGGER.debug("Expanding the graph querying for {} nodes...",node2graphToAnalyse.size());
				expandGraphs(node2graphToAnalyse, numThreads);
				LOGGER.debug("Graph expansions completed.");
			}

			//store the node analysed
			for(RDFNode node:node2graphToAnalyse.keySet())
				analysedNodes.add(node.toString());

			Map<RuleAtom,Map<Graph<RDFNode>,Set<Edge<RDFNode>>>> nextPaths = 
					bestRule.nextPlausibleRules(threshold,bestRule.getLen()==maxRuleLen-1);

			for(RuleAtom rule:nextPaths.keySet()){
				HornRule<RDFNode> newRule = bestRule.duplicateRule();
				Map<Graph<RDFNode>,Set<Edge<RDFNode>>> copyMap= Maps.newHashMap(nextPaths.get(rule));
				newRule.addRuleAtom(rule, copyMap);
				rules.add(newRule);
			}


		}

		LOGGER.debug("Total reading time to create RDF graph: {} seconds.",(System.currentTimeMillis()-start)/1000.);

	}

	private void expandGraphs(Map<RDFNode,Set<Graph<RDFNode>>> toAnalyse, int numThreads){

		List<Thread> activeThreads = Lists.newLinkedList();
		List<Map<RDFNode,Set<Graph<RDFNode>>>> currentInputs = 
				this.splitNodesThreads(toAnalyse, numThreads);

		int i=0;
		for(Map<RDFNode,Set<Graph<RDFNode>>> currentInput:currentInputs){
			i++;
			// Create the thread and add it to the list
			final Thread current_thread = new Thread(new OneExampleRuleDiscovery(currentInput,
					this.getSparqlExecutor()), "Thread"+i);
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

	private List<Map<RDFNode,Set<Graph<RDFNode>>>> splitNodesThreads(Map<RDFNode,
			Set<Graph<RDFNode>>> input, int numThread){
		List<Map<RDFNode,Set<Graph<RDFNode>>>> outputThreads = Lists.newLinkedList();

		int i=0;
		for(RDFNode currentNode:input.keySet()){
			if(i==numThread)
				i=0;
			if(i>=outputThreads.size()){
				Map<RDFNode,Set<Graph<RDFNode>>> currentMap = Maps.newHashMap();
				outputThreads.add(currentMap);
			}
			Map<RDFNode,Set<Graph<RDFNode>>> currentMap = outputThreads.get(i);
			currentMap.put(currentNode, input.get(currentNode));
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
	 * @param subjectFilters
	 * 			set of entities to be filtered as subjects. Only entities present in subjectFilters will
	 * 			be returned. If empty or null it will return all the entities
	 * @param objectFilters
	 * 			set of entities to be filtered as objects. Only entities present in objectFilters will
	 * 			be returned. If empty or null it will return all the entities
	 * @param filtered
	 * 			decide whether returns only a subset of the negative examples, one for each relation
	 * @return
	 */
	public Set<Pair<RDFNode,RDFNode>> generateNegativeExamples(Set<String> relations, String typeObject, 
			String typeSubject, Set<String> subjectFilters, Set<String> objectFilters,boolean filtered){
		if(filtered){
			return this.getSparqlExecutor().generateFilteredNegativeExamples(relations, 
					typeObject, typeSubject, subjectFilters, objectFilters);
		}
		else
			return this.getSparqlExecutor().generateNegativeExamples(relations, 
					typeObject, typeSubject, subjectFilters, objectFilters);

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


	//gotta read it from conf file
	private SparqlExecutor getSparqlExecutor(){
//		String sparqlEndpoint = "http://localhost:8890/sparql";
//		Set<String> prefixRelation = Sets.newHashSet();
//		prefixRelation.add("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
//		prefixRelation.add("PREFIX dbo: <http://dbpedia.org/ontology/>");
//		prefixRelation.add("PREFIX dbp: <http://dbpedia.org/property/>");
//		Set<String> targetPrefix = Sets.newHashSet();
//		targetPrefix.add("http://dbpedia.org/");
//		QuerySparqlVirtuosoEndpoint endpoint = new QuerySparqlVirtuosoEndpoint(prefixRelation,
//				null, sparqlEndpoint);
//
//		String dbLocation = "/home/stefano/Downloads/Dataset/RDF3x_dataset/DBPedia/dbpedia";
//		String rdf3xExecutable = "/home/stefano/Downloads/rdf3x-0.3.7/bin/rdf3xquery";
//		//		QueryRDF3XStore endpoint = new QueryRDF3XStore(prefixRelation, targetPrefix, dbLocation, rdf3xExecutable);
//
//		String directory = "/Users/ortona/Documents/ASU_Collaboration/Developing/Data/jena_database/amie_dpediba_2.8";
//		Dataset dataset = TDBFactory.createDataset(directory) ;
//		//		QueryJenaRDFAPI endpoint = new QueryJenaRDFAPI(prefixRelation, null, dataset);

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
