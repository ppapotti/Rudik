package asu.edu.neg_rule_miner.sparql.jena;

import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;
import asu.edu.neg_rule_miner.sparql.jena.remote.QuerySparqlRemoteEndpoint;

public abstract class QueryJenaLibrary extends SparqlExecutor {

	protected QueryExecution openResource;

	public QueryJenaLibrary(Configuration config) {
		super(config);
	}

	private final static Logger LOGGER = LoggerFactory.getLogger(QuerySparqlRemoteEndpoint.class.getName());

	@Override
	public void executeQuery(RDFNode entity, Set<Graph<RDFNode>> inputGraphs) {
		//different query if the entity is a literal
		if(entity.isLiteral()){
			this.compareLiterals(entity, inputGraphs);
			return;
		}

		String sparqlQuery = "SELECT DISTINCT ?sub ?rel ?obj";
		if(this.graphIri!=null&&this.graphIri.length()>0)
			sparqlQuery+="FROM "+this.graphIri;
		sparqlQuery+= " WHERE " +
				"{{<"+entity.toString()+"> ?rel ?obj.} " +
				"UNION " +
				"{?sub ?rel <"+entity.toString()+">.}}";

		long startTime = System.currentTimeMillis();

		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(sparqlQuery);

		if(!results.hasNext())
			LOGGER.debug("Query '{}' returned an empty result!",sparqlQuery);


		long totalTime = System.currentTimeMillis() - startTime;
		if(totalTime>50000)
			LOGGER.debug("Query '{}' took {} seconds to complete.",sparqlQuery, totalTime/1000.);


		while(results.hasNext()){
			QuerySolution oneResult = results.next();

			String relation = oneResult.get("rel").toString();

			boolean isTargetRelation = this.targetPrefix == null;
			if(this.targetPrefix!=null){
				for(String targetRelation:this.targetPrefix)
					if(relation.startsWith(targetRelation)){
						isTargetRelation = true;
						break;
					}
			}
			if(!isTargetRelation)
				continue;
			RDFNode nodeToAdd = null;
			Edge<RDFNode> edgeToAdd = null;

			RDFNode subject = oneResult.get("sub");
			if(subject!=null){
				nodeToAdd = subject;
				edgeToAdd = new Edge<RDFNode>(subject, entity,relation);
			}
			else{
				RDFNode object = oneResult.get("obj");
				if(object != null){
					nodeToAdd = object;
					edgeToAdd = new Edge<RDFNode>(entity, object,relation);
				}
			}

			for(Graph<RDFNode> g:inputGraphs){
				g.addNode(nodeToAdd);
				boolean addEdge = g.addEdge(edgeToAdd, true);
				if(!addEdge)
					LOGGER.warn("Not able to insert the edge '{}' in the graph '{}'.",edgeToAdd,g);
			}

		}
		this.closeResources();

	}

	@Override
	public Set<Pair<RDFNode, RDFNode>> generateNegativeExamples(
			Set<String> relations, String typeObject, String typeSubject,
			Set<String> subjectFilters, Set<String> objectFilters) {
		String negativeCandidateQuery = super.generateNegativeExampleQuery(relations, typeSubject, typeObject, subjectFilters, objectFilters);
		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",negativeCandidateQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(negativeCandidateQuery);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			Pair<RDFNode,RDFNode> negativeExample = 
					Pair.of(oneResult.get("subject"), oneResult.get("object"));
			negativeExamples.add(negativeExample);
		}

		this.closeResources();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}

	@Override
	public Set<Pair<RDFNode, RDFNode>> generateFilteredNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject,
			Set<String> subjectFilters, Set<String> objectFilters) {
		String negativeCandidateQuery = super.generateNegativeExampleQuery(relations, typeSubject, typeObject, subjectFilters, objectFilters);
		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",negativeCandidateQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(negativeCandidateQuery);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		Set<String> consideredRelation = Sets.newHashSet();
		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			String relation = oneResult.get("otherRelation").toString();
			if(consideredRelation.contains(relation))
				continue;
			Pair<RDFNode,RDFNode> negativeExample = 
					Pair.of(oneResult.get("subject"), oneResult.get("object"));
			if(negativeExamples.contains(negativeExample))
				continue;
			negativeExamples.add(negativeExample);
			consideredRelation.add(relation);
		}

		this.closeResources();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}

	public abstract ResultSet executeQuery(String sparqlQuery);

	public void closeResources(){
		if(this.openResource!=null){
			this.openResource.close();
			this.openResource = null;
		}
	}

}
