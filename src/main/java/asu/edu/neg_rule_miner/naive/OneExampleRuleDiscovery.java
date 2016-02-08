package asu.edu.neg_rule_miner.naive;

import java.util.Map;
import java.util.Set;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;


public class OneExampleRuleDiscovery implements Runnable{

	private final static Logger LOGGER = LoggerFactory.getLogger(OneExampleRuleDiscovery.class.getName());

	private Map<RDFNode,Set<Graph<RDFNode>>> input;
	
	private SparqlExecutor queryEndpoint;

	public OneExampleRuleDiscovery(Map<RDFNode,Set<Graph<RDFNode>>> input,SparqlExecutor queryEndpoint){
		this.input = input;
		this.queryEndpoint = queryEndpoint;
	}

	public void run(){

		int succeded = 0;
		int failed = 0;
		for(RDFNode entity:input.keySet()){
			try{
				this.queryEndpoint.executeQuery(entity,this.input.get(entity));
				succeded++;
			}

			catch(Exception e){
				LOGGER.error("Thread with entity '{}' was not able to complet its job.",entity.toString(),e);
				failed++;
			}
		}
		//LOGGER.debug("Succeded: {}, Failed: {}",succeded,failed);
	}

}
