package asu.edu.neg_rule_miner.sparql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.configuration.Constant;
import asu.edu.neg_rule_miner.model.MultipleGraphHornRule;
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

	protected Set<String> topTypes;

	protected String graphIri;

	protected String typePrefix;

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

		if(!config.containsKey("types.type_prefix")||config.getString("types.type_prefix").length()==0)
			throw new RuleMinerException("No type_prefix specific in the Configuration file.",LOGGER);
		typePrefix = config.getString("types.type_prefix");

		this.topTypes=Sets.newHashSet();

		if(config.containsKey("types.top_type")){
			List<String> objects = (List<String>) config.getList("types.top_type");
			for(String object:objects){
				this.topTypes.add(object);
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

	public abstract Map<Edge<String>,String> executeQuery(String entity,
			Graph<String> inputGraphs, Map<String,Set<String>> entity2types);

	public abstract Set<Pair<String,String>> generateNegativeExamples(Set<String> relations, String typeSubject, 
			String typeObject);

	public abstract Set<Pair<String,String>> generatePositiveExamples(Set<String >relations,String typeSubject,String typeObject);

	public abstract Set<Pair<String,String>> getKBExamples(String query,String subject,String object);

	public String generatePositiveExampleQuery(Set<String> relations,String typeSubject,String typeObject){

		Iterator<String> relationIterator = relations.iterator();
		StringBuilder filterRelation = new StringBuilder();
		while(relationIterator.hasNext()){
			String currentRelation = relationIterator.next();
			filterRelation.append("?targetRelation = <"+currentRelation+">");
			if(relationIterator.hasNext()){
				filterRelation.append(" || ");
			}
		}

		String positiveCandidateQuery = "";
		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				positiveCandidateQuery+=prefix+" ";
			}
		}

		positiveCandidateQuery +=
				"SELECT DISTINCT ?subject ?object ";
		if(this.graphIri!=null&&graphIri.length()>0)
			positiveCandidateQuery+=" FROM "+this.graphIri;

		positiveCandidateQuery+=" WHERE " +
				"{ ?object <"+typePrefix+"> <" + typeObject + ">." +
				"  ?subject <"+typePrefix+"> <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") }";

		return positiveCandidateQuery;
	}

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
				"{ ?object <"+typePrefix+"> <" + typeObject + ">." +
				"  ?subject <"+typePrefix+"> <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?realObject. " +
				"  ?subject ?otherRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") " +
				"  FILTER (" + filterNotRelation.toString() + ") " +
				"  FILTER (?object != ?realObject) " +
				differentRelation.toString();

		negativeCandidateQuery+="}";

		return negativeCandidateQuery;
	}

	public abstract int getSupportivePositiveExamples(Set<RuleAtom> rules,Set<String> relations, String typeSubject, String typeObject,
			Set<Pair<String,String>> subject2objectConstant);

	public abstract Set<Pair<String,String>> getMatchingPositiveExamples(Set<RuleAtom> rules,Set<String> relations, String typeSubject, String typeObject,
			Set<Pair<String,String>> positiveExamples);


	public abstract Map<String,Set<Pair<String,String>>> getRulePositiveSupport(Set<Pair<String,String>> positiveExamples);

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
		query.append("SELECT DISTINCT ?subject ?object");

		/**
		 * Jena does not work with count and nested query with from
		 */
		//if(this.graphIri!=null&&graphIri.length()>0)
		//query.append(" FROM "+this.graphIri);

		query.append(" WHERE {");
		query.append("?object <"+typePrefix+"> <" + typeObject + ">." +
				"  ?subject <"+typePrefix+"> <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") ");

		//check if the query contains an inequality

		StringBuilder atomFilterBuilder = new StringBuilder();
		RuleAtom inequalityAtom = null;
		for(RuleAtom atom:rules){
			if(atom.getRelation().equals("<=") || atom.getRelation().equals(">=") 
					|| atom.getRelation().equals("=")){
				atomFilterBuilder.append("FILTER (?"+atom.getSubject()+atom.getRelation()+"?"+atom.getObject()+") ");
				continue;
			}
			if(atom.getRelation().equals("!=")){
				inequalityAtom = atom;
				continue;
			}
			atomFilterBuilder.append("?"+atom.getSubject()+" <"+atom.getRelation()+"> ?"+atom.getObject()+". ");
		}
		query.append(atomFilterBuilder.toString());
		if(inequalityAtom != null)
			query.append(this.inequalityFilter(rules, inequalityAtom));
		query.append("}");

		return query.toString();
	}


	private String inequalityFilter(Set<RuleAtom> rules, RuleAtom inequalityAtom){
		StringBuilder inequalityFilter = new StringBuilder();
		inequalityFilter.append("FILTER NOT EXISTS {");
		String variableToSubstitue = inequalityAtom.getSubject();
		String replacementVariable = inequalityAtom.getObject();
		if(variableToSubstitue.equals(MultipleGraphHornRule.START_NODE) || variableToSubstitue.equals(MultipleGraphHornRule.END_NODE)){
			variableToSubstitue = inequalityAtom.getObject();
			replacementVariable = inequalityAtom.getSubject();
		}

		if(!replacementVariable.equals(MultipleGraphHornRule.START_NODE)&&!replacementVariable.equals(MultipleGraphHornRule.END_NODE))
			replacementVariable = "other"+replacementVariable;

		for(RuleAtom atom:rules){
			if(atom.equals(inequalityAtom))
				continue;

			String subject = atom.getSubject();
			if(subject.equals(variableToSubstitue))
				subject = replacementVariable;
			else{
				if(!subject.equals(MultipleGraphHornRule.START_NODE) &&!subject.equals(MultipleGraphHornRule.END_NODE))
					subject = "other"+subject;
			}

			String object = atom.getObject();
			if(object.equals(variableToSubstitue))
				object = replacementVariable;
			else{
				if(!object.equals(MultipleGraphHornRule.START_NODE) &&!object.equals(MultipleGraphHornRule.END_NODE))
					object = "other"+object;
			}


			if(atom.getRelation().equals("<=") || atom.getRelation().equals(">=") 
					|| atom.getRelation().equals("=")){
				inequalityFilter.append("FILTER (?"+subject+atom.getRelation()+"?"+object+") ");
				continue;
			}
			inequalityFilter.append("?"+subject+" <"+atom.getRelation()+"> ?"+object+". ");
		}
		inequalityFilter.append("}");

		return inequalityFilter.toString();
	}


	/**
	 * Negative examples must be separated with a tab
	 * @return
	 * @throws IOException 
	 */
	public Set<Pair<String,String>> readExamplesFromFile(File inputFile) throws IOException{
		Set<Pair<String,String>> examples = Sets.newHashSet();
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));

		String line = reader.readLine();
		while(line!=null){
			String firstNode = line.split("\t")[0];
			String secondNode = line.split("\t")[1];
			Pair<String,String> example = Pair.of(firstNode,secondNode);
			if(!examples.contains(Pair.of(secondNode, firstNode)))
				examples.add(example);
			line=reader.readLine();
		}

		reader.close();
		LOGGER.debug("Read {} negative examples from input file.",examples.size());
		return examples;
	}

	/**
	 * If the entity is literal compare it with all others literals
	 * @param entity
	 * @return
	 */
	protected void compareLiterals(String literal, Graph<String> graph){

		String literalLexicalForm = graph.getLexicalForm(literal);
		//get only the literals that belong to the same example. It returns all literals if entity covers a null set of examples
		Set<String> otherLiterals = graph.getLiteralNodes();

		for(String node:otherLiterals){
			if(node.equals(literal) )
				continue;
			String relation = compareLiteral(literalLexicalForm, graph.getLexicalForm(node));
			if(relation!=null && !graph.containsEdge(node, literal, getInverseRelation(relation)))
				graph.addEdge(literal, node, relation, true);
		}


	}

	public static String compareLiteral(String stringLiteralOne, String stringLiteralTwo){

		if(stringLiteralOne==null||
				stringLiteralTwo==null)
			return null;

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

	public static String getInverseRelation(String rel){
		if(rel.equals(Constant.GREATER_EQUAL_REL))
			return Constant.LESS_EQUAL_REL;

		if(rel.equals(Constant.LESS_EQUAL_REL))
			return Constant.GREATER_EQUAL_REL;

		return rel;

	}

}
