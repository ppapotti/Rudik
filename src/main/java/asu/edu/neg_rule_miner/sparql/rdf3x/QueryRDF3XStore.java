package asu.edu.neg_rule_miner.sparql.rdf3x;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.RDFSimpleNodeResourceImplementation;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public class QueryRDF3XStore extends SparqlExecutor{

	private final static Logger LOGGER = LoggerFactory.getLogger(QueryRDF3XStore.class.getName());

	private String dbLocation;

	private String rdf3xExecutable;

	public QueryRDF3XStore(Configuration config){
		super(config);
		if(!config.containsKey("parameters.db_location"))
			throw new RuleMinerException("Cannot initialise RDF3X engine without specifying the "
					+ "db_location directory in the configuration file.", LOGGER);
		this.dbLocation = config.getString("parameters.db_location");
		if(!config.containsKey("parameters.executable"))
			throw new RuleMinerException("Cannot initialise RDF3X engine without specifying the "
					+ "executable file in the configuration file.", LOGGER);
		this.rdf3xExecutable = config.getString("parameters.executable");

	}


	@Override
	public void executeQuery(RDFNode entity, Set<Graph<RDFNode>> inputGraphs) {
		long startTime = System.currentTimeMillis();

		String dbPediaQuery = "SELECT DISTINCT ?sub ?rel ?obj";
		if(this.graphIri!=null&&this.graphIri.length()>0)
			dbPediaQuery+=" FROM "+this.graphIri;
		dbPediaQuery+=" WHERE {{<"+entity.toString()+"> ?rel ?obj.} UNION" +
				"{?sub ?rel <"+entity+">.}}";

		//different query if the entity is a literal
		if(!entity.toString().startsWith("http")){
			//			dbPediaQuery = "SELECT DISTINCT ?rel ?sub WHERE {?sub ?rel ?obj. "
			//					+ "FILTER(?obj="+entity.toString()+")}";
			return;
		}

		String line;
		Process p;
		try {
			ProcessBuilder pb = new ProcessBuilder(this.rdf3xExecutable, 
					this.dbLocation,dbPediaQuery);
			p = pb.start();
		} catch (IOException e) {
			LOGGER.error("Error while starting the external process to execute '{}' executable file.",this.rdf3xExecutable,e);
			return;
		}

		BufferedReader in = new BufferedReader(
				new InputStreamReader(p.getInputStream()) );

		try {
			line = in.readLine();
		} catch (IOException e) {
			LOGGER.error("Error while reading the output from the external process.",e);
			return;
		}
		if(line == null)
			LOGGER.debug("Query '{}' returned an empty result!",dbPediaQuery);

		while (line != null) {
			String[] oneResult = line.split(" ");

			String relation = oneResult[1];
			if(oneResult.length==2)
				relation = oneResult[0];
			relation = relation.replaceAll("[<|>]", "");

			boolean isTargetRelation = this.targetPrefix == null;
			if(this.targetPrefix!=null&&this.targetPrefix.size()>0){
				for(String targetRelation:this.targetPrefix)
					if(relation.startsWith(targetRelation)){
						isTargetRelation = true;
						break;
					}
			}
			if(!isTargetRelation){
				try {
					line = in.readLine();
				} catch (IOException e) {
					LOGGER.error("Error while reading further lines from the external process output.",e);
					return;
				}
				continue;
			}

			Edge<RDFNode> edgeToAdd = null;
			RDFSimpleNodeResourceImplementation newNode = null;

			String subject = null;
			String object = null;
			//there is only subjects
			if(oneResult.length==2){
				subject = line.substring(relation.length()+3);
				object = "NULL";
			}
			else{
				subject = oneResult[0];
				object = line.substring(subject.length()+relation.length()+4);
			}
			if(subject.equals("NULL"))
				subject = null;
			if(object.equals("NULL"))
				object = null;

			if(subject!=null){
				subject = subject.replaceAll("[<|>]", "");
				newNode = new RDFSimpleNodeResourceImplementation(subject);
				edgeToAdd = new Edge<RDFNode>(newNode, entity, relation);
			}
			else{
				if(object != null){
					boolean isLiteral = true;
					if(object.startsWith("<")){
						object = object.replaceAll("[<|>]", "");
						isLiteral = false;
					}
					newNode = new RDFSimpleNodeResourceImplementation(object);
					newNode.setIsLiteral(isLiteral);

					edgeToAdd = new Edge<RDFNode>(entity, newNode, 
							relation);
				}
			}

			for(Graph<RDFNode> g:inputGraphs){
				g.addNode(newNode);
				g.addEdge(edgeToAdd, true);
			}
			try {
				line = in.readLine();
			} catch (IOException e) {
				LOGGER.error("Error while reading further lines from the external process output.",e);
				return;
			}
		}

		try {
			in.close();
			p.destroy();
		} catch (IOException e) {
			LOGGER.error("Error while trying to destroy the process executing the '{}' executable file.",e);
		}

		long totalTime = System.currentTimeMillis() - startTime;
		if(totalTime>50000)
			LOGGER.debug("Query '{}' took {} seconds to complete.",dbPediaQuery, totalTime/1000.);

	}


	@Override
	public Set<Pair<RDFNode, RDFNode>> generateNegativeExamples(
			Set<String> relations, String typeObject, String typeSubject,
			Set<String> subjectFilters, Set<String> objectFilters) {

		String negativeCandidateQuery = super.generateNegativeExampleQuery(relations, typeSubject, typeObject, subjectFilters, objectFilters);	
		Set<Pair<RDFNode, RDFNode>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		long startTime = System.currentTimeMillis();
		LOGGER.debug("Executing negative candidate query selection on Sparql Endpoint...",negativeCandidateQuery);

		String line;
		Process p;
		try {
			ProcessBuilder pb = new ProcessBuilder(this.rdf3xExecutable, 
					this.dbLocation,negativeCandidateQuery);
			p = pb.start();
		} catch (IOException e) {
			LOGGER.error("Error while starting the external process to execute '{}' executable file.",this.rdf3xExecutable,e);
			return negativeExamples;
		}

		BufferedReader in = new BufferedReader(
				new InputStreamReader(p.getInputStream()) );

		try {
			line = in.readLine();
		} catch (IOException e) {
			LOGGER.error("Error while reading the output from the external process.",e);
			return negativeExamples;
		}
		if(line == null)
			LOGGER.debug("Query '{}' returned an empty result!",negativeCandidateQuery);

		while (line != null) {

			String[] oneResult = line.split(" ");

			if(oneResult.length!=2)
				continue;

			RDFNode subject = new RDFSimpleNodeResourceImplementation(oneResult[0].replaceAll("[<|>]", ""));
			RDFNode object = new RDFSimpleNodeResourceImplementation(oneResult[1].replaceAll("[<|>]", ""));

			negativeExamples.add(Pair.of(subject, object));
			try {
				line = in.readLine();
			} catch (IOException e) {
				LOGGER.error("Error while reading further lines from the external process output, " +
						"returning only current negative examples: '{}'.",negativeExamples,e);
				return negativeExamples;
			}
		}

		try {
			in.close();
			p.destroy();
		} catch (IOException e) {
			LOGGER.error("Error while trying to destroy the process executing the '{}' executable file.",e);
		}

		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}


	@Override
	public Set<Pair<RDFNode, RDFNode>> generateFilteredNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject,
			Set<String> subjectFilters, Set<String> objectFilters) {
		// TODO Auto-generated method stub
		return null;
	}

}
