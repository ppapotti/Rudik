package asu.edu.neg_rule_miner.sparql.jena.remote;


import org.apache.commons.configuration.Configuration;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.sparql.jena.QueryJenaLibrary;


public class QuerySparqlRemoteEndpoint extends QueryJenaLibrary{

	private final static Logger LOGGER = LoggerFactory.getLogger(QuerySparqlRemoteEndpoint.class.getName());

	private String sparqlEndpoint;

	/**
	 * prefixRelation can be equal to null
	 * @param endpoint
	 * @param prefixQuery
	 * @param input
	 */
	public QuerySparqlRemoteEndpoint(Configuration config){
		super(config);
		if(!config.containsKey("parameters.sparql_endpoint"))
			throw new RuleMinerException("Cannot initialise Virtuoso engine without specifying the "
					+ "sparql url endpoint in the configuration file.", LOGGER);
		this.sparqlEndpoint = config.getString("parameters.sparql_endpoint");
	}

	@Override
	public ResultSet executeQuery(String sparqlQuery) {
		QueryExecution qexec = QueryExecutionFactory.sparqlService(this.sparqlEndpoint, sparqlQuery);
		this.openResource = qexec;
		return qexec.execSelect();
	}

}
