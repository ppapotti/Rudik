package asu.edu.neg_rule_miner.naive;

import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;


/**
 * Query the knwoeldge base through sparql using multiple threads
 * 
 * @author ortona
 *
 */
public class OneExampleRuleDiscovery implements Runnable{

	private final static Logger LOGGER = LoggerFactory.getLogger(OneExampleRuleDiscovery.class.getName());

	private Set<RDFNode> input;

	private SparqlExecutor queryEndpoint;

	Map<RDFNode,Set<Edge<RDFNode>>> node2neigbours;
	
	Graph<RDFNode> graph;


	public OneExampleRuleDiscovery(Set<RDFNode> input,Graph<RDFNode> graph,SparqlExecutor queryEndpoint,
			Map<RDFNode,Set<Edge<RDFNode>>> node2neigbours){
		this.input = input;
		this.queryEndpoint = queryEndpoint;
		this.node2neigbours = node2neigbours;
		this.graph=graph;
	}

	public void run(){
		for(RDFNode entity:input){
			try{
				Set<Edge<RDFNode>> currentNeighbours= 
						this.queryEndpoint.executeQuery(entity,this.graph);
				if(currentNeighbours!=null&&currentNeighbours.size()>0)
					this.node2neigbours.put(entity, currentNeighbours);

			}

			catch(Exception e){
				LOGGER.error("Thread with entity '{}' was not able to complet its job.",entity.toString(),e);
			}
		}
	}

}
