package asu.edu.neg_rule_miner.sparql.jena;

import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public class QueryJenaRDFAPI extends SparqlExecutor{

	private Dataset dataset;

	private final static Logger LOGGER = LoggerFactory.getLogger(QueryJenaRDFAPI.class.getName());

	public QueryJenaRDFAPI(Configuration config) {
		super(config);
		if(!config.containsKey("parameters.directory"))
			throw new RuleMinerException("Cannot initialise Jena API without specifying the "
					+ "database directory in the configuration file.", LOGGER);
		String directory = config.getString("parameters.directory");
		Dataset dataset = TDBFactory.createDataset(directory) ;
		this.dataset = dataset;
	}

	@Override
	public void executeQuery(RDFNode entity, Set<Graph<RDFNode>> inputGraphs) {
		long startTime = System.currentTimeMillis();

		String dbPediaQuery = "SELECT DISTINCT ?sub ?rel ?obj";
		if(this.graphIri!=null)
			dbPediaQuery += " FROM "+this.graphIri;
		dbPediaQuery+=" WHERE " +
				"{{<"+entity.toString()+"> ?rel ?obj.} " +
				"UNION " +
				"{?sub ?rel <"+entity.toString()+">.}}";

		//different query if the entity is a literal
		if(entity.isLiteral()){
			//this.compareLiterals(entity, inputGraphs);
			return;
		}


		dataset.begin(ReadWrite.READ) ;

		QueryExecution qExec = QueryExecutionFactory.create(dbPediaQuery, dataset);
		ResultSet results = qExec.execSelect() ;
		dataset.end();

		if(!results.hasNext())
			LOGGER.debug("Query '{}' returned an empty result!",dbPediaQuery);

		while(results.hasNext()){
			QuerySolution oneResult = results.next();

			String relation = oneResult.get("rel").toString();

			boolean isTargetRelation = this.targetPrefix == null;
			if(this.targetPrefix!=null&&this.targetPrefix.size()>0){
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

	@Override
	public Set<Pair<RDFNode, RDFNode>> generateNegativeExamples(
			Set<String> relations, String typeObject, String typeSubject,
			Set<String> subjectFilters, Set<String> objectFilters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Pair<RDFNode, RDFNode>> generateFilteredNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject,
			Set<String> subjectFilters, Set<String> objectFilters) {
		// TODO Auto-generated method stub
		return null;
	}

}
