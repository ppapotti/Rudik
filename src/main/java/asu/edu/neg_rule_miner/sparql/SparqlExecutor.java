package asu.edu.neg_rule_miner.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
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

		if(config.containsKey("relation_prefix.prefix")){
			this.prefixQuery = Sets.newHashSet();
			try{
				HierarchicalConfiguration hierarchicalConfig = (HierarchicalConfiguration) config;
				List<HierarchicalConfiguration> prefixes = hierarchicalConfig.configurationsAt("relation_prefix.prefix");
				for(HierarchicalConfiguration prefix:prefixes){
					String name = prefix.getString("name");
					String uri = prefix.getString("uri");
					if(name!=null&&name.length()>0&&uri!=null&&uri.length()>0)
						prefixQuery.add("PREFIX "+name+": <"+uri+">");
				}

			}
			catch(Exception e){
				LOGGER.error("Error while reading relation_prefix.prefix parameter from the configuration file.",e);
			}
		}

		if(config.containsKey("relation_target_prefix")){
			this.targetPrefix = Sets.newHashSet();
			List<String> objects = (List<String>) config.getList("relation_target_prefix.prefix");
			for(String object:objects){
				this.targetPrefix.add(object);
			}
		}

		if(config.containsKey("graph_iri")&&config.getString("graph_iri").length()>0){
			this.graphIri = "<"+config.getString("graph_iri")+">";
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
			negativeCandidateQuery+="FROM "+this.graphIri;

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

	/**
	 * If the entity is literal compare it with all others literals
	 * @param entity
	 * @return
	 */
	protected void compareLiterals(RDFNode literal, Set<Graph<RDFNode>> graphs){

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

}
