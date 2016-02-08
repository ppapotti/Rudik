package asu.edu.neg_rule_miner.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.model.rdf.graph.Graph;

/**
 * Abstract class to model a Sparql engine endpoint
 * The method executeQuery must executed a select query on the input entity and write the results on the input graph
 * 
 * The method generateNegativeExamples has to produce negative examples as pairs of RDFNodes
 * @author ortona
 *
 */
public abstract class SparqlExecutor {

	private final static Logger LOGGER = LoggerFactory.getLogger(SparqlExecutor.class.getName());

	protected Set<String> prefixQuery;

	protected Set<String> targetPrefix;

	protected String graphIri;

	@SuppressWarnings("unchecked")
	public SparqlExecutor(Configuration config){

		if(config.containsKey("relation_prefix")){
			this.prefixQuery = Sets.newHashSet();
			List<String> objects = (List<String>) config.getList("relation_prefix.prefix");
			for(String object:objects){
				this.prefixQuery.add(object);
			}
		}

		if(config.containsKey("relation_target_prefix")){
			this.targetPrefix = Sets.newHashSet();
			List<String> objects = (List<String>) config.getList("relation_target_prefix.prefix");
			for(String object:objects){
				this.targetPrefix.add(object);
			}
		}

		if(config.containsKey("graph_iri")){
			this.graphIri = config.getString("graph_iri");
		}
	}

	public abstract void executeQuery(RDFNode entity,Set<Graph<RDFNode>> inputGraphs);

	public abstract Set<Pair<RDFNode,RDFNode>> generateNegativeExamples(Set<String> relations, String typeObject, 
			String typeSubject, Set<String> subjectFilters, Set<String> objectFilters);

	public abstract Set<Pair<RDFNode, RDFNode>> generateFilteredNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject,
			Set<String> subjectFilters, Set<String> objectFilters);

	public String generateNegativeExampleQuery(Set<String> relations, String typeSubject, String typeObject, Set<String> subjectFilters,
			Set<String> objectFilters){
		if(relations==null||relations.size()==0)
			return null;

		//create the RDF 
		StringBuilder filterRelation = new StringBuilder();
		StringBuilder filterNotRelation = new StringBuilder();
		StringBuilder differentRelation = new StringBuilder();
		Iterator<String> relationIterator = relations.iterator();

		while(relationIterator.hasNext()){
			String currentRelation = relationIterator.next();
			filterRelation.append("?targetRelation = <"+currentRelation+">");
			filterNotRelation.append("?otherRelation != <"+currentRelation+">");
			if(relationIterator.hasNext()){
				filterRelation.append(" || ");
				filterNotRelation.append(" && ");
			}
			differentRelation.append("  FILTER NOT EXISTS {?subject <"+currentRelation+"> ?object.} ");
		}

		String negativeCandidateQuery = "";
		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				negativeCandidateQuery+=prefix+" ";
			}
		}

		negativeCandidateQuery +=
				"SELECT DISTINCT ?subject ?otherRelation ?object ";
		if(this.graphIri!=null&&graphIri.length()>0)
			negativeCandidateQuery+="FROM ";

		negativeCandidateQuery+=" WHERE " +
				"{ ?object rdf:type <" + typeObject + ">." +
				"  ?subject rdf:type <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?realObject. " +
				"  ?subject ?otherRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") " +
				"  FILTER (" + filterNotRelation.toString() + ") " +
				"  FILTER (?object != ?realObject) " +
				differentRelation.toString();
		if(objectFilters!=null && objectFilters.size()>0)
		{
			negativeCandidateQuery+="FILTER(";
			Iterator<String> filterObject = objectFilters.iterator();
			while(filterObject.hasNext()){
				negativeCandidateQuery+="?object = <"+filterObject.next()+">";
				if(filterObject.hasNext()){
					negativeCandidateQuery+=" || ";
				}
			}	
			negativeCandidateQuery+=") ";
		}

		if(subjectFilters!=null && subjectFilters.size()>0)
		{
			negativeCandidateQuery+="FILTER(";
			Iterator<String> filterSubject = subjectFilters.iterator();
			while(filterSubject.hasNext()){
				negativeCandidateQuery+="?subject = <"+filterSubject.next()+">";
				if(filterSubject.hasNext()){
					negativeCandidateQuery+=" || ";
				}
			}	
			negativeCandidateQuery+=")";

		}
		negativeCandidateQuery+="}";

		return negativeCandidateQuery;
	}

	/**
	 * Negative examples must be separated with a tab
	 * @return
	 * @throws IOException 
	 */
	public Set<Pair<RDFNode,RDFNode>> readNegativeExamplesFromFile(File inputFile) throws IOException{
		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));

		String line = reader.readLine();
		while(line!=null){
			RDFNode firstNode = new RDFSimpleNodeResourceImplementation(line.split("\t")[0]);
			RDFNode secondNode = new RDFSimpleNodeResourceImplementation(line.split("\t")[1]);
			Pair<RDFNode,RDFNode> negativeExample = Pair.of(firstNode,secondNode);
			negativeExamples.add(negativeExample);
			line=reader.readLine();
		}

		reader.close();
		LOGGER.debug("Read {} negative examples from input file.",negativeExamples.size());
		return negativeExamples;


	}

}
