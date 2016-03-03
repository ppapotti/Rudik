package asu.edu.neg_rule_miner.sparql.jena.tbd;

import org.apache.commons.configuration.Configuration;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.sparql.jena.QueryJenaLibrary;

public class QueryJenaRDFAPI extends QueryJenaLibrary{

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
	public ResultSet executeQuery(String sparqlQuery) {
		dataset.begin(ReadWrite.READ) ;
		QueryExecution qExec = QueryExecutionFactory.create(sparqlQuery, dataset);
		this.openResource = qExec;
		ResultSet results = qExec.execSelect() ;
		dataset.end();
		return results;
	}

}
