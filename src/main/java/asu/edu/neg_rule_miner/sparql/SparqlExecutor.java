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

import asu.edu.neg_rule_miner.configuration.Constant;
import asu.edu.neg_rule_miner.model.HornRule;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
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

	protected Set<String> relationToAvoid;

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

		if(config.containsKey("relation_target_prefix.prefix")){
			this.targetPrefix = Sets.newHashSet();
			List<String> objects = (List<String>) config.getList("relation_target_prefix.prefix");
			for(String object:objects){
				this.targetPrefix.add(object);
			}
		}

		if(config.containsKey("graph_iri")&&config.getString("graph_iri").length()>0){
			this.graphIri = "<"+config.getString("graph_iri")+">";
		}

		this.relationToAvoid = Sets.newHashSet();
		if(config.containsKey("relation_to_avoid.relation")){
			List<String> objects = (List<String>) config.getList("relation_to_avoid.relation");
			for(String object:objects){
				this.relationToAvoid.add(object);
			}
		}
	}

	public abstract Set<Edge<RDFNode>> executeQuery(RDFNode entity,
			Graph<RDFNode> inputGraphs);

	public abstract Set<Pair<RDFNode,RDFNode>> generateNegativeExamples(Set<String> relations, String typeSubject, 
			String typeObject);

	public abstract Set<Pair<RDFNode, RDFNode>> generateFilteredNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject, int totalNumberExample);

	public String generateNegativeExampleQuery(Set<String> relations, String typeSubject, String typeObject){
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
			negativeCandidateQuery+=" FROM "+this.graphIri;

		negativeCandidateQuery+=" WHERE " +
				"{ ?object rdf:type <" + typeObject + ">." +
				"  ?subject rdf:type <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?realObject. " +
				"  ?subject ?otherRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") " +
				"  FILTER (" + filterNotRelation.toString() + ") " +
				"  FILTER (?object != ?realObject) " +
				differentRelation.toString();
		
		negativeCandidateQuery+="}";

		return negativeCandidateQuery;
	}

	public abstract int getSupportivePositiveExamples(Set<RuleAtom> rules,Set<String> relations, String typeSubject, String typeObject);

	public abstract int getTotalCoveredExample(HornRule<RDFNode> rule, String typeSubject, String typeObject);

	public String generatePositiveExampleCountQuery(Set<RuleAtom> rules, Set<String> relations, String typeSubject, 
			String typeObject){
		if(relations==null||relations.size()==0)
			return null;

		if(!(rules.size()>0))
			return null;
		//create the RDF 
		StringBuilder query = new StringBuilder();

		Iterator<String> relationIterator = relations.iterator();
		StringBuilder filterRelation = new StringBuilder();
		while(relationIterator.hasNext()){
			String currentRelation = relationIterator.next();
			filterRelation.append("?targetRelation = <"+currentRelation+">");
			if(relationIterator.hasNext()){
				filterRelation.append(" || ");
			}
		}

		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				query.append(prefix+" ");
			}
		}
		query.append("SELECT (COUNT(*) as ?count) WHERE{SELECT DISTINCT ?subject ?object");

		/**
		 * Jena does not work with count and nested query with from
		 */
		//if(this.graphIri!=null&&graphIri.length()>0)
		//query.append(" FROM "+this.graphIri);

		query.append(" WHERE {");
		for(RuleAtom atom:rules){
			if(atom.getRelation().equals("<=") || atom.getRelation().equals(">=") || atom.getRelation().equals("="))
				query.append("FILTER (?"+atom.getSubject()+atom.getRelation()+"?"+atom.getObject()+") ");
			else
				query.append("?"+atom.getSubject()+" <"+atom.getRelation()+"> ?"+atom.getObject()+". ");
		}
		query.append("?object rdf:type <" + typeObject + ">." +
				"  ?subject rdf:type <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") ");
		query.append("}}");

		return query.toString();
	}

	public String generateTotalExampleCountQuery(HornRule<RDFNode> rule, String typeSubject, 
			String typeObject){

		if(!(rule.getRules().size()>0))
			return null;
		//create the RDF 
		StringBuilder query = new StringBuilder();

		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				query.append(prefix+" ");
			}
		}
		query.append("SELECT (COUNT(*) as ?count) WHERE{SELECT DISTINCT ?subject ?object");

		/**
		 * Jena does not work with count and nested query with from
		 */
		//if(this.graphIri!=null&&graphIri.length()>0)
		//query.append(" FROM "+this.graphIri);

		query.append(" WHERE {");
		for(RuleAtom atom:rule.getRules()){
			if(atom.getRelation().equals("<=") || atom.getRelation().equals(">=") || atom.getRelation().equals("="))
				query.append("FILTER (?"+atom.getObject()+atom.getRelation()+"?"+atom.getObject()+")");
			else
				query.append("?"+atom.getSubject()+" <"+atom.getRelation()+"> ?"+atom.getObject()+". ");
		}
		query.append("?object rdf:type <" + typeObject + ">." +
				"  ?subject rdf:type <"+ typeSubject + ">.");
		query.append("}}");

		return query.toString();
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
	protected void compareLiterals(RDFNode literal, Graph<RDFNode> graph){

		for(RDFNode node:graph.nodes){
			if(node.equals(literal)||!node.isLiteral())
				continue;
			String relation = this.compareLiteral(literal, node);
			if(relation!=null)
				graph.addEdge(literal, node, relation, true);
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
				return Constant.EQUAL_REL;
			if(firstDouble<secondDouble)
				return Constant.LESS_EQUAL_REL;
			return Constant.GREATER_EQUAL_REL;
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
				return Constant.EQUAL_REL;
			if(firstDate.compareTo(secondDate)<0)
				return Constant.LESS_EQUAL_REL;
			return Constant.GREATER_EQUAL_REL;
		}

		//compare them as a string
		if(stringLiteralOne.equals(stringLiteralTwo))
			return Constant.EQUAL_REL;
		return null;
	}

}
