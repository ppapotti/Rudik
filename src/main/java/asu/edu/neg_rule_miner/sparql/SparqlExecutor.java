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
import asu.edu.neg_rule_miner.model.horn_rule.HornRule;
import asu.edu.neg_rule_miner.model.horn_rule.RuleAtom;
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

	protected String typePrefix;

	protected int subjectLimit = -1;

	protected int objectLimit = -1;

	protected int negativeExampleLimit = -1;

	protected int positiveExampleLimit = -1;

	protected boolean includeLiterals = true;

	@SuppressWarnings("unchecked")
	public SparqlExecutor(Configuration config){

		if(config.containsKey(Constant.CONF_RELATION_PREFIX)){
			this.prefixQuery = Sets.newHashSet();
			try{
				HierarchicalConfiguration hierarchicalConfig = (HierarchicalConfiguration) config;
				List<HierarchicalConfiguration> prefixes = hierarchicalConfig.configurationsAt(Constant.CONF_RELATION_PREFIX);
				for(HierarchicalConfiguration prefix:prefixes){
					String name = prefix.getString("name");
					String uri = prefix.getString("uri");
					if(name!=null&&name.length()>0&&uri!=null&&uri.length()>0)
						prefixQuery.add("PREFIX "+name+": <"+uri+">");
				}

			}
			catch(Exception e){
				LOGGER.error("Error while reading "+Constant.CONF_RELATION_PREFIX+" parameter from the configuration file.",e);
			}
		}

		if(!config.containsKey(Constant.CONF_TYPE_PREFIX)||config.getString(Constant.CONF_TYPE_PREFIX).length()==0)
			throw new RuleMinerException("No "+Constant.CONF_TYPE_PREFIX+" specific in the Configuration file.",LOGGER);
		typePrefix = config.getString(Constant.CONF_TYPE_PREFIX);

		if(config.containsKey(Constant.CONF_RELATION_TARGET_REFIX)){
			this.targetPrefix = Sets.newHashSet();
			List<String> objects = (List<String>) config.getList(Constant.CONF_RELATION_TARGET_REFIX);
			for(String object:objects){
				this.targetPrefix.add(object);
			}
		}

		if(config.containsKey(Constant.CONF_GRAPH_IRI)&&config.getString(Constant.CONF_GRAPH_IRI).length()>0){
			this.graphIri = "<"+config.getString(Constant.CONF_GRAPH_IRI)+">";
		}

		this.relationToAvoid = Sets.newHashSet();
		if(config.containsKey(Constant.CONF_RELATION_TO_AVOID)){
			List<String> objects = (List<String>) config.getList(Constant.CONF_RELATION_TO_AVOID);
			for(String object:objects){
				this.relationToAvoid.add(object);
			}
		}

		//read limit from config file
		if(config.containsKey(Constant.CONF_SUBJECT_LIMIT)){
			try{
				this.subjectLimit = config.getInt(Constant.CONF_SUBJECT_LIMIT);
			}
			catch(Exception e){
				LOGGER.error("Error while setting "+Constant.CONF_SUBJECT_LIMIT+" configuration parameter.",e);
			}
		}

		if(config.containsKey(Constant.CONF_OBJECT_LIMIT)){
			try{
				this.objectLimit = config.getInt(Constant.CONF_OBJECT_LIMIT);
			}
			catch(Exception e){
				LOGGER.error("Error while setting "+Constant.CONF_OBJECT_LIMIT+" configuration parameter.",e);
			}
		}

		if(config.containsKey(Constant.CONF_POS_EXAMPLES_LIMIT)){
			try{
				this.positiveExampleLimit = config.getInt(Constant.CONF_POS_EXAMPLES_LIMIT);
			}
			catch(Exception e){
				LOGGER.error("Error while setting "+Constant.CONF_POS_EXAMPLES_LIMIT+" configuration parameter.",e);
			}
		}

		if(config.containsKey(Constant.CONF_NEG_EXAMPLES_LIMIT)){
			try{
				this.negativeExampleLimit = config.getInt(Constant.CONF_NEG_EXAMPLES_LIMIT);
			}
			catch(Exception e){
				LOGGER.error("Error while setting "+Constant.CONF_NEG_EXAMPLES_LIMIT+" configuration parameter.",e);
			}
		}

		if(config.containsKey(Constant.CONF_INCLUDE_LITERALS)){
			try{
				this.includeLiterals = config.getBoolean(Constant.CONF_INCLUDE_LITERALS);
			}
			catch(Exception e){
				LOGGER.error("Error while setting "+Constant.CONF_INCLUDE_LITERALS+" configuration parameter.",e);
			}
		}

	}

	public abstract void executeQuery(String entity,
			Graph<String> inputGraphs, Map<String,Set<String>> entity2types);

	public abstract Set<Pair<String,String>> generateUnionNegativeExamples(Set<String> relations, String typeSubject, 
			String typeObject, boolean subjectFunction, boolean objectFunction);

	public abstract Set<Pair<String,String>> generatePositiveExamples(Set<String >relations,String typeSubject,String typeObject);

	/**
	 * 
	 * @param query
	 * @param subject
	 * @param object
	 * @param includeInverted
	 * 			if set to false, the output will not include the pair <a,b> if the output already contains the pair <b,a>
	 * @return
	 */
	public abstract Set<Pair<String,String>> getKBExamples(String query,String subject,String object,boolean includeInverted);

	public String generatePositiveExampleQuery(Set<String> relations,String typeSubject,String typeObject){

		if(relations==null||relations.size()==0)
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


		if(this.graphIri!=null&&graphIri.length()>0)
			query.append(" FROM "+this.graphIri);

		query.append(" WHERE {");

		if(typeObject!=null)
			query.append("  ?object <"+typePrefix+"> <" + typeObject + ">.");
		if(typeSubject!=null)
			query.append("  ?subject <"+typePrefix+"> <"+ typeSubject + ">.");
		query.append("  ?subject ?targetRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") }");

		if(this.positiveExampleLimit >= 0)
			query.append(" ORDER BY RAND() LIMIT "+this.positiveExampleLimit);

		return query.toString();
	}

	public String generateRulesPositiveExampleQuery(Set<RuleAtom> rules, Set<String> relations, String typeSubject, 
			String typeObject){

		if(!(rules.size()>0))
			return null;


		String query = this.generatePositiveExampleQuery(relations, typeSubject, typeObject);
		query = query.substring(0, query.length()-1)+" "+this.getHornRuleAtomQuery(rules)+" }";

		return query;
	}

	public String generateRulesNegativeExampleQuery(Set<RuleAtom> rules, Set<String> relations, String typeSubject, 
			String typeObject, boolean subjectFunction, boolean objectFunction){

		if(!(rules.size()>0))
			return null;


		String query = this.generateNegativeExampleUnionQuery(relations, typeSubject, typeObject, subjectFunction, objectFunction);
		query = query.substring(0, query.length()-1)+" "+this.getHornRuleAtomQuery(rules)+" }";

		return query;
	}

	public String getHornRuleAtomQuery(Set<RuleAtom> rules){
		StringBuilder atomFilterBuilder = new StringBuilder();
		RuleAtom inequalityAtom = null;
		for(RuleAtom atom:rules){
			if(atom.getRelation().equals(Constant.GREATER_EQUAL_REL) || atom.getRelation().equals(Constant.LESS_EQUAL_REL) 
					|| atom.getRelation().equals(Constant.GREATER_REL) || atom.getRelation().equals(Constant.LESS_REL)
					|| atom.getRelation().equals(Constant.EQUAL_REL)){
				atomFilterBuilder.append("FILTER (?"+atom.getSubject()+atom.getRelation()+"?"+atom.getObject()+") ");
				continue;
			}
			if(atom.getRelation().equals("!=")){
				inequalityAtom = atom;
				continue;
			}
			atomFilterBuilder.append("?"+atom.getSubject()+" <"+atom.getRelation()+"> ?"+atom.getObject()+". ");
		}
		atomFilterBuilder.toString();
		if(inequalityAtom != null)
			atomFilterBuilder.append(this.inequalityFilter(rules, inequalityAtom));
		return atomFilterBuilder.toString();

	}

	private String inequalityFilter(Set<RuleAtom> rules, RuleAtom inequalityAtom){
		StringBuilder inequalityFilter = new StringBuilder();
		inequalityFilter.append("FILTER NOT EXISTS {");
		String variableToSubstitue = inequalityAtom.getSubject();
		String replacementVariable = inequalityAtom.getObject();
		if(variableToSubstitue.equals(HornRule.START_NODE) || variableToSubstitue.equals(HornRule.END_NODE)){
			variableToSubstitue = inequalityAtom.getObject();
			replacementVariable = inequalityAtom.getSubject();
		}

		if(!replacementVariable.equals(HornRule.START_NODE)&&!replacementVariable.equals(HornRule.END_NODE))
			replacementVariable = "other"+replacementVariable;

		for(RuleAtom atom:rules){
			if(atom.equals(inequalityAtom))
				continue;

			String subject = atom.getSubject();
			if(subject.equals(variableToSubstitue))
				subject = replacementVariable;
			else{
				if(!subject.equals(HornRule.START_NODE) &&!subject.equals(HornRule.END_NODE))
					subject = "other"+subject;
			}

			String object = atom.getObject();
			if(object.equals(variableToSubstitue))
				object = replacementVariable;
			else{
				if(!object.equals(HornRule.START_NODE) &&!object.equals(HornRule.END_NODE))
					object = "other"+object;
			}


			if(atom.getRelation().equals(Constant.GREATER_EQUAL_REL) || atom.getRelation().equals(Constant.LESS_EQUAL_REL) 
					|| atom.getRelation().equals(Constant.GREATER_REL) || atom.getRelation().equals(Constant.LESS_REL)
					|| atom.getRelation().equals(Constant.EQUAL_REL)){
				inequalityFilter.append("FILTER (?"+subject+atom.getRelation()+"?"+object+") ");
				continue;
			}
			inequalityFilter.append("?"+subject+" <"+atom.getRelation()+"> ?"+object+". ");
		}
		inequalityFilter.append("}");

		return inequalityFilter.toString();
	}

	public String generateOneSideExampleQuery(Set<String> relations,String type,String variable){

		Iterator<String> relationIterator = relations.iterator();
		StringBuilder filterRelation = new StringBuilder();
		while(relationIterator.hasNext()){
			String currentRelation = relationIterator.next();
			filterRelation.append("?targetRelation = <"+currentRelation+">");
			if(relationIterator.hasNext()){
				filterRelation.append(" || ");
			}
		}

		String oneSideExampleQuery = "";
		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				oneSideExampleQuery+=prefix+" ";
			}
		}

		oneSideExampleQuery +=
				"SELECT DISTINCT ?"+variable+" ";
		//		if(this.graphIri!=null&&graphIri.length()>0)
		//			oneSideExampleQuery+=" FROM "+this.graphIri;

		oneSideExampleQuery+=" WHERE " +
				"{ ?"+variable+" <"+typePrefix+"> <" + type + ">.";

		oneSideExampleQuery+=" ?subject ?targetRelation ?object. ";

		oneSideExampleQuery+="  FILTER (" + filterRelation.toString() + ") }";

		return oneSideExampleQuery;
	}



	public String generateNegativeExampleUnionQuery(Set<String> relations, String typeSubject, String typeObject, 
			boolean subjectFunction, boolean objectFunction){
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
				"SELECT DISTINCT ?subject ?object ";
		if(this.graphIri!=null&&graphIri.length()>0)
			negativeCandidateQuery+=" FROM "+this.graphIri;

		negativeCandidateQuery+=" WHERE {";
		if(typeObject!=null)
			negativeCandidateQuery+=" ?object <"+typePrefix+"> <" + typeObject + ">.";
		if(typeSubject!=null)
			negativeCandidateQuery+="  ?subject <"+typePrefix+"> <"+ typeSubject + ">.";

		//if both true or false, include both
		if(subjectFunction == false && objectFunction == false){
			negativeCandidateQuery+=" {{?subject ?targetRelation ?realObject.} UNION " +
					" {?realSubject ?targetRelation ?object.}} ";
		}
		else{
			if(subjectFunction)
				negativeCandidateQuery+=" ?subject ?targetRelation ?realObject. ";
			if(objectFunction)
				negativeCandidateQuery+=" ?realSubject ?targetRelation ?object. ";
		}


		negativeCandidateQuery+="  ?subject ?otherRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") " +
				"  FILTER (" + filterNotRelation.toString() + ") " +
				differentRelation.toString();

		negativeCandidateQuery+="}";
		if(this.negativeExampleLimit>=0)
			negativeCandidateQuery+=" ORDER BY RAND() LIMIT "+this.negativeExampleLimit;

		return negativeCandidateQuery;
	}

	/**
	 * If switchSubjectObject is set to true, looks for pair where object is in relation with subject rather than subject in relation with object
	 * @param relations
	 * @param typeSubject
	 * @param typeObject
	 * @param switchSubjectObject
	 * @return
	 */
	public String generateNegativeExampleRestrictedQuery(Set<String> relations, String typeSubject, String typeObject,
			boolean switchSubjectObject){
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
				"SELECT DISTINCT ?subject ?object ";
		if(this.graphIri!=null&&graphIri.length()>0)
			negativeCandidateQuery+=" FROM "+this.graphIri;

		negativeCandidateQuery+=" WHERE " +
				"{ ?object <"+typePrefix+"> <" + typeObject + ">." +
				"  ?subject <"+typePrefix+"> <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?realObject. " +
				"  ?realSubject ?targetRelation ?object. ";
		if(switchSubjectObject)
			negativeCandidateQuery+="  ?object ?otherRelation ?subject. ";
		else
			negativeCandidateQuery+="  ?subject ?otherRelation ?object. ";

		negativeCandidateQuery+="  FILTER (" + filterRelation.toString() + ") " +
				"  FILTER (" + filterNotRelation.toString() + ") " +
				differentRelation.toString();

		negativeCandidateQuery+="}";

		return negativeCandidateQuery;
	}

	public String generateNegativeExtendedExampleQuery(Set<String> relations, String typeSubject, String typeObject){
		if(relations==null||relations.size()==0)
			return null;

		//create the RDF 
		StringBuilder filterRelation = new StringBuilder();
		StringBuilder differentRelation = new StringBuilder();
		Iterator<String> relationIterator = relations.iterator();

		while(relationIterator.hasNext()){
			String currentRelation = relationIterator.next();
			filterRelation.append("?targetRelation = <"+currentRelation+">");
			if(relationIterator.hasNext()){
				filterRelation.append(" || ");
			}
		}

		String negativeCandidateQuery = "";
		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				negativeCandidateQuery+=prefix+" ";
			}
		}

		negativeCandidateQuery +=
				"SELECT DISTINCT ?subject ?object ";
		if(this.graphIri!=null&&graphIri.length()>0)
			negativeCandidateQuery+=" FROM "+this.graphIri;

		negativeCandidateQuery+=" WHERE " +
				"{ ?object <"+typePrefix+"> <" + typeObject + ">." +
				"  ?subject <"+typePrefix+"> <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?realObject. " +
				"  ?realSubject ?targetRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") " +
				"  FILTER NOT EXISTS {?subject ?relation ?object.} " +
				differentRelation.toString();
		negativeCandidateQuery+="}";

		return negativeCandidateQuery;
	}

	public String generateExampleWithRelationsQuery(Set<String> relations, String typeSubject, String typeObject){
		if(relations==null||relations.size()==0)
			return null;

		//create the RDF 
		StringBuilder filterRelation = new StringBuilder();
		Iterator<String> relationIterator = relations.iterator();

		while(relationIterator.hasNext()){
			String currentRelation = relationIterator.next();
			filterRelation.append("?targetRelation = <"+currentRelation+">");
			if(relationIterator.hasNext()){
				filterRelation.append(" || ");
			}
		}

		String negativeCandidateQuery = "";
		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				negativeCandidateQuery+=prefix+" ";
			}
		}

		negativeCandidateQuery +=
				"SELECT DISTINCT ?subject ?object ";

		//		if(this.graphIri!=null&&graphIri.length()>0)
		//			negativeCandidateQuery+=" FROM "+this.graphIri;

		negativeCandidateQuery+=" WHERE " +
				"{ ?object <"+typePrefix+"> <" + typeObject + ">." +
				"  ?subject <"+typePrefix+"> <"+ typeSubject + ">." +
				"  ?subject ?targetRelation ?realObject. " +
				"  ?realSubject ?targetRelation ?object. " +
				"  FILTER (" + filterRelation.toString() + ") " +
				"  ?subject ?relation ?object.}";

		return negativeCandidateQuery;
	}

	public abstract int getSupportivePositiveExamples(Set<RuleAtom> rules,Set<String> relations, String typeSubject, String typeObject,
			Set<Pair<String,String>> subject2objectConstant);

	public abstract Set<Pair<String,String>> getMatchingPositiveExamples(Set<RuleAtom> rules,Set<String> relations, String typeSubject, String typeObject,
			Set<Pair<String,String>> positiveExamples);

	public abstract Set<Pair<String,String>> getMatchingNegativeExamples(Set<RuleAtom> rules,
			Set<String> relations, String typeSubject, String typeObject, Set<Pair<String,String>> negativeExamples, 
			boolean subjectFunction, boolean objectFunction);


	public abstract Map<String,Set<Pair<String,String>>> getRulePositiveSupport(Set<Pair<String,String>> positiveExamples);

	public abstract Set<Pair<String,String>> executeHornRuleQuery(Set<RuleAtom> rules, String typeSubject, String typeObject);

	public  abstract Set<Pair<String,String>> executeRestrictiveHornRuleQuery(String queryRestriction,
			Set<RuleAtom> rules, String typeSubject, String typeObject);

	public abstract int executeCountQuery(String inputQuery);
	public String generateHornRuleQuery(Set<RuleAtom> rules, String typeSubject, 
			String typeObject, boolean overloaded){

		if(!(rules.size()>0))
			return null;
		//create the RDF 
		StringBuilder query = new StringBuilder();

		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				query.append(prefix+" ");
			}
		}
		query.append("SELECT DISTINCT ");
		for(RuleAtom atom : rules){
			String rel = atom.getRelation();
			String sub = atom.getSubject();
			String obj = atom.getObject();
			query.append("<"+rel+"(>?"+sub+"<,>?"+obj+"<)>");
		}

		String subject = typeSubject!=null ? "?subject" : "";
		String object = typeObject!=null ? "?object" : "";
//		query.append("SELECT DISTINCT "+subject+" "+object);

		/**
		 * Jena does not work with count and nested query with from
		 */
		if(this.graphIri!=null&&graphIri.length()>0)
			query.append(" FROM "+this.graphIri);

		query.append(" WHERE {");
		if(subject.length()>0)
			query.append(subject+" <"+typePrefix+"> <"+ typeSubject + ">. ");
		if(object.length()>0)
			query.append(object+" <"+typePrefix+"> <"+ typeObject + ">. ");

		//check if the query contains an inequality

		query.append(this.getHornRuleAtomQuery(rules));
		query.append("}");

		return query.toString();
	}

	public String generateHornRuleQuery(Set<RuleAtom> rules, String typeSubject, 
			String typeObject){

		if(!(rules.size()>0))
			return null;
		//create the RDF 
		StringBuilder query = new StringBuilder();

		if(this.prefixQuery!=null&&this.prefixQuery.size()>0){
			for(String prefix:this.prefixQuery){
				query.append(prefix+" ");
			}
		}

		String subject = typeSubject!=null ? "?subject" : "";
		String object = typeObject!=null ? "?object" : "";
		query.append("SELECT DISTINCT "+subject+" "+object);

		/**
		 * Jena does not work with count and nested query with from
		 */
		if(this.graphIri!=null&&graphIri.length()>0)
			query.append(" FROM "+this.graphIri);

		query.append(" WHERE {");
		if(subject.length()>0)
			query.append(subject+" <"+typePrefix+"> <"+ typeSubject + ">. ");
		if(object.length()>0)
			query.append(object+" <"+typePrefix+"> <"+ typeObject + ">. ");

		//check if the query contains an inequality

		query.append(this.getHornRuleAtomQuery(rules));
		query.append("}");

		return query.toString();
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
		LOGGER.debug("Read {} examples from input file.",examples.size());
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
			Set<String> outputRelations = getLiteralRelation(literalLexicalForm, graph.getLexicalForm(node));
			if(outputRelations!=null){
				for(String relation : outputRelations){
					if(!graph.containsEdge(node, literal, getInverseRelation(relation)))
						graph.addEdge(literal, node, relation, true);
				}
			}
		}


	}

	public static Set<String> getLiteralRelation(String stringLiteralOne, String stringLiteralTwo){

		Set<String> outputRelations = Sets.newHashSet();

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
			if(firstDouble==secondDouble){
				outputRelations.add(Constant.EQUAL_REL);
				outputRelations.add(Constant.LESS_EQUAL_REL);
				outputRelations.add(Constant.GREATER_EQUAL_REL);
				return outputRelations;
			}
			if(firstDouble<secondDouble)
				outputRelations.add(Constant.LESS_REL);
			else
				outputRelations.add(Constant.GREATER_REL);
			return outputRelations;
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
			if(firstDate.compareTo(secondDate)==0){
				outputRelations.add(Constant.EQUAL_REL);
				outputRelations.add(Constant.LESS_EQUAL_REL);
				outputRelations.add(Constant.GREATER_EQUAL_REL);
				return outputRelations;
			}
			if(firstDate.compareTo(secondDate)<0)
				outputRelations.add(Constant.LESS_REL);
			else
				outputRelations.add(Constant.GREATER_REL);
			return outputRelations;
		}

		//compare them as a string
		if(stringLiteralOne.equals(stringLiteralTwo)){
			outputRelations.add(Constant.EQUAL_REL);
			return outputRelations;
		}
		return null;
	}

	public static Integer compareLiteral(String stringLiteralOne, String stringLiteralTwo){

		Set<String> outputRelations = Sets.newHashSet();

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
			if(firstDouble==secondDouble){
				return 0;
			}
			if(firstDouble<secondDouble)
				return -1;
			else
				return +1;
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
			if(firstDate.compareTo(secondDate)==0){
				outputRelations.add(Constant.EQUAL_REL);
				outputRelations.add(Constant.LESS_EQUAL_REL);
				outputRelations.add(Constant.GREATER_EQUAL_REL);
				return 0;
			}
			if(firstDate.compareTo(secondDate)<0)
				return -1;
			else
				outputRelations.add(Constant.GREATER_REL);
			return +1;
		}

		//compare them as a string
		if(stringLiteralOne.equals(stringLiteralTwo)){
			outputRelations.add(Constant.EQUAL_REL);
			return 0;
		}
		return null;
	}

	public boolean isLiteralNumber(String literalValue){
		try{
			Double.parseDouble(literalValue);
			return true;
		}
		catch(Exception e){
			try{
				DateTimeFormatter.ISO_DATE.parse(literalValue);
				return true;
			}
			catch(Exception e1){
				try{
					DateTimeFormatter.ISO_DATE_TIME.parse(literalValue);
					return true;
				}
				catch(Exception e2){

				}
			}
		}
		return false;
	}

	public static String getInverseRelation(String rel){
		if(rel.equals(Constant.GREATER_EQUAL_REL))
			return Constant.LESS_EQUAL_REL;

		if(rel.equals(Constant.LESS_EQUAL_REL))
			return Constant.GREATER_EQUAL_REL;

		if(rel.equals(Constant.GREATER_REL))
			return Constant.LESS_REL;

		if(rel.equals(Constant.LESS_REL))
			return Constant.GREATER_REL;

		if(rel.equals(Constant.EQUAL_REL))
			return rel;

		return null;

	}

	public String getGraphIri(){
		return this.graphIri;
	}

	public String getTypePrefix(){
		return this.typePrefix;
	}

	public void setSubjectLimit(int limit){
		this.subjectLimit = limit;
	}
	
	public int getSubjectLimit(){
		return this.subjectLimit;
	}

	public void setObjectLimit(int limit){
		this.objectLimit = limit;
	}
	
	public int getObjectLimit(){
		return this.objectLimit;
	}

	public void setPosExamplesLimit(int limit){
		this.positiveExampleLimit = limit;
	}

	public void setNegExamplesLimit(int limit){
		this.negativeExampleLimit = limit;
	}

}
