package asu.edu.neg_rule_miner.sparql.virtuoso;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;


public class QuerySparqlVirtuosoEndpoint extends SparqlExecutor{

	private final static Logger LOGGER = LoggerFactory.getLogger(QuerySparqlVirtuosoEndpoint.class.getName());

	private String sparqlEndpoint;

	/**
	 * prefixRelation can be equal to null
	 * @param endpoint
	 * @param prefixQuery
	 * @param input
	 */
	public QuerySparqlVirtuosoEndpoint(Configuration config){
		super(config);
		if(!config.containsKey("parameters.sparql_endpoint"))
			throw new RuleMinerException("Cannot initialise Virtuoso engine without specifying the "
					+ "sparql url endpoint in the configuration file.", LOGGER);
		this.sparqlEndpoint = config.getString("parameters.sparql_endpoint");
	}

	public void executeQuery(RDFNode entity,Set<Graph<RDFNode>> inputGraphs) {

		long startTime = System.currentTimeMillis();

		String dbPediaQuery = "SELECT DISTINCT ?sub ?rel ?obj";
		if(this.graphIri!=null&&this.graphIri.length()>0)
			dbPediaQuery+="FROM "+this.graphIri;
		dbPediaQuery+= " WHERE " +
				"{{<"+entity.toString()+"> ?rel ?obj.} " +
				"UNION " +
				"{?sub ?rel <"+entity.toString()+">.}}";

		//different query if the entity is a literal
		if(entity.isLiteral()){
			this.compareLiterals(entity, inputGraphs);
			return;
			//dbPediaQuery = this.generateLiteralQuery(entity);
		}
		QueryExecution qexec = QueryExecutionFactory.sparqlService(this.sparqlEndpoint, dbPediaQuery);
		ResultSet results = qexec.execSelect();

		if(!results.hasNext())
			LOGGER.debug("Query '{}' returned an empty result!",dbPediaQuery);

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

		long totalTime = System.currentTimeMillis() - startTime;
		if(totalTime>50000)
			LOGGER.debug("Query '{}' took {} seconds to complete.",dbPediaQuery, totalTime/1000.);
	}

	/**
	 * If the entity is literal compare it with all others literals
	 * @param entity
	 * @return
	 */
	private void compareLiterals(RDFNode literal, Set<Graph<RDFNode>> graphs){

		for(Graph<RDFNode> g:graphs){
			for(RDFNode node:g.nodes){
				if(node.equals(literal)||!node.isLiteral())
					continue;
				String relation = this.compareLiteral(literal, node);
				if(relation!=null)
					g.addEdge(literal, node, relation, true);
			}

		}

	}

	private String compareLiteral(RDFNode literalOne, RDFNode literalTwo){
		String stringLiteralOne = literalOne.asLiteral().getLexicalForm();
		String stringLiteralTwo = literalTwo.asLiteral().getLexicalForm();

		//compare them as integer
		Double firstDouble = null;
		try{
			firstDouble = Double.parseDouble(stringLiteralOne);
		}
		catch(Exception e){
			//just continue
		}
		Double secondDouble = null;
		try{
			secondDouble = Double.parseDouble(stringLiteralTwo);
			if(firstDouble==null)
				return null;
			if(firstDouble==secondDouble)
				return "equal";
			if(firstDouble<secondDouble)
				return "lessOrEqual";
			return "greaterOrEqual";
		}
		catch(Exception e){
			if(firstDouble!=null)
				return null;
		}

		//compare them as date
		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
		DateTimeFormatter formatterTime = DateTimeFormatter.ISO_DATE_TIME;
		LocalDate firstDate = null;
		try{
			firstDate = LocalDate.parse(stringLiteralOne, formatter);
		}
		catch(Exception e){
			try {
				firstDate = LocalDate.parse(stringLiteralOne, formatterTime);
			} catch (Exception e1) {
				//just continue
			}
		}
		LocalDate secondDate = null;
		try{
			secondDate = LocalDate.parse(stringLiteralTwo, formatter);
			if(firstDate==null)
				return null;
		}
		catch(Exception e){
			try {
				secondDate = LocalDate.parse(stringLiteralTwo, formatterTime);
				if(firstDate==null)
					return null;
			} 
			catch (Exception e1) {
				if(firstDate!=null)
					return null;
			}	
		}
		if(firstDate!=null&&secondDate!=null){
			if(firstDate.compareTo(secondDate)==0)
				return "equal";
			if(firstDate.compareTo(secondDate)<0)
				return "lessOrEqual";
			return "greaterOrEqual";
		}

		//compare them as a string
		if(stringLiteralOne.equals(stringLiteralTwo))
			return "equals";
		return null;
	}

	public Set<Pair<RDFNode, RDFNode>> generateHalfSizeNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject,
			Set<String> subjectFilters, Set<String> objectFilters) {

		String negativeCandidateQuery = super.generateNegativeExampleQuery(relations, typeSubject, typeObject, subjectFilters, objectFilters);
		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",negativeCandidateQuery);
		long startTime = System.currentTimeMillis();
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, negativeCandidateQuery);
		ResultSet results = qexec.execSelect(); 
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		Map<String,List<Pair<RDFNode,RDFNode>>> relation2count = Maps.newHashMap();
		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			String relation = oneResult.get("otherRelation").toString();
			Pair<RDFNode,RDFNode> negativeExample = 
					Pair.of(oneResult.get("subject"), oneResult.get("object"));
			List<Pair<RDFNode,RDFNode>> currentExamples = relation2count.get(relation);
			if(currentExamples==null){
				currentExamples=Lists.newLinkedList();
				relation2count.put(relation, currentExamples);
			}
			currentExamples.add(negativeExample);
		}
		for(String relation:relation2count.keySet()){
			List<Pair<RDFNode,RDFNode>> currentExamples = relation2count.get(relation);
			if(currentExamples.size()>0)
				negativeExamples.addAll(currentExamples.subList(0, currentExamples.size()/2));
			else
				negativeExamples.addAll(currentExamples);
		}

		qexec.close();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}

	@Override
	public Set<Pair<RDFNode, RDFNode>> generateNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject,
			Set<String> subjectFilters, Set<String> objectFilters) {

		String negativeCandidateQuery = super.generateNegativeExampleQuery(relations, typeSubject, typeObject, subjectFilters, objectFilters);
		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",negativeCandidateQuery);
		long startTime = System.currentTimeMillis();
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, negativeCandidateQuery);
		ResultSet results = qexec.execSelect(); 
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			Pair<RDFNode,RDFNode> negativeExample = 
					Pair.of(oneResult.get("subject"), oneResult.get("object"));
			negativeExamples.add(negativeExample);
		}

		qexec.close();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}

	public Set<Pair<RDFNode, RDFNode>> generateFilteredNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject,
			Set<String> subjectFilters, Set<String> objectFilters) {

		String negativeCandidateQuery = super.generateNegativeExampleQuery(relations, typeSubject, typeObject, subjectFilters, objectFilters);
		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",negativeCandidateQuery);
		long startTime = System.currentTimeMillis();
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, negativeCandidateQuery);
		ResultSet results = qexec.execSelect(); 
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

		qexec.close();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}

}
