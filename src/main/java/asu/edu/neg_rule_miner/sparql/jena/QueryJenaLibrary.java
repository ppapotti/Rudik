package asu.edu.neg_rule_miner.sparql.jena;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;
import asu.edu.neg_rule_miner.sparql.jena.remote.QuerySparqlRemoteEndpoint;

public abstract class QueryJenaLibrary extends SparqlExecutor {

	Map<String,Set<String>> predicate2positiveExamplesSupport;

	protected QueryExecution openResource;

	public QueryJenaLibrary(Configuration config) {
		super(config);
	}

	private final static Logger LOGGER = LoggerFactory.getLogger(QuerySparqlRemoteEndpoint.class.getName());

	@Override
	public void executeQuery(String entity, 
			Graph<String> graph, Map<String,Set<String>> entity2types) {
		//different query if the entity is a literal
		if(graph.isLiteral(entity)){
			//this.compareLiterals(entity, graph);
			//if it's a literal do not return any neighbours because they might change based on the graph
			return;
		}

		String sparqlQuery = "SELECT DISTINCT ?sub ?rel ?obj";
		if(this.graphIri!=null&&this.graphIri.length()>0)
			sparqlQuery+=" FROM "+this.graphIri;
		sparqlQuery+= " WHERE {" +
				"{<"+entity+"> ?rel ?obj.} " +
				"UNION " +
				"{?sub ?rel <"+entity+">.}}";

		long startTime = System.currentTimeMillis();

		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(sparqlQuery);

		if(!results.hasNext())
			LOGGER.debug("Query '{}' returned an empty result!",sparqlQuery);


		long totalTime = System.currentTimeMillis() - startTime;
		if(totalTime>50000)
			LOGGER.debug("Query '{}' took {} seconds to complete.",sparqlQuery, totalTime/1000.);

		TripleFilter tripFil = new TripleFilter();
		
		//TO DO: read from config file
		String sub = "sub", rel = "rel", obj = "obj";
		
		ArrayList<QuerySolution> resultTriples = tripFil.doFilter(results, sub, rel, obj, entity, ConfigurationFacility.getSubjectLimit(), 
				ConfigurationFacility.getObjectLimit());

		Set<String> currentTypes = Sets.newHashSet();
		entity2types.put(entity, currentTypes);
		for(QuerySolution oneResult:resultTriples){
			
			String relation = oneResult.get("rel").toString();

			//check the relation is a type relation
			if(relation.equals(this.typePrefix)&&
					oneResult.get("obj")!=null){
				graph.addType(entity, oneResult.get("obj").toString());
			}

			if(this.relationToAvoid!=null&&this.relationToAvoid.contains(relation))
				continue;

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

			String nodeToAdd = null;
			Edge<String> edgeToAdd = null;
			String lexicalForm = null;

			RDFNode subject = oneResult.get("sub");
			if(subject!=null){
				nodeToAdd = subject.toString();
				edgeToAdd = new Edge<String>(nodeToAdd, entity,relation);
				if(subject.isLiteral())
					lexicalForm = subject.asLiteral().getLexicalForm();
			}
			else{
				RDFNode object = oneResult.get("obj");
				if(object != null){
					nodeToAdd = object.toString();
					edgeToAdd = new Edge<String>(entity, nodeToAdd,relation);
					if(object.isLiteral()){
						if(object.asLiteral()==null||object.asLiteral().getLexicalForm()==null)
							lexicalForm = object.toString();
						else
							lexicalForm = object.asLiteral().getLexicalForm();
					}
				}
			}

			if(lexicalForm == null)
				graph.addNode(nodeToAdd);
			else
				graph.addLiteralNode(nodeToAdd, lexicalForm);

			//neighbours.put(edgeToAdd,lexicalForm);
			boolean addEdge = graph.addEdge(edgeToAdd, true);
			if(!addEdge)
				LOGGER.warn("Not able to insert the edge '{}' in the graph '{}'.",edgeToAdd,graph);

		}

		startTime = System.currentTimeMillis();

		this.closeResources();
	}

	@Override
	public Set<Pair<String, String>> generateUnionNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject, boolean subjectFunction, boolean objectFunction) {
		String negativeCandidateQuery = super.generateNegativeExampleUnionQuery(relations, typeSubject, typeObject,subjectFunction,objectFunction);
		Set<Pair<String,String>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",negativeCandidateQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();

		//try to execute normal query
		ResultSet results = null;
		try{
			results = this.executeQuery(negativeCandidateQuery);
		}catch(Exception e){
			//if not able, put a limit of 5000 records
			LOGGER.debug("Not able to execute normal query, setting a limit of 5000 results");
			results = this.executeQuery(negativeCandidateQuery+" LIMIT 5000");
		}
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			String secondResult = oneResult.get("object").toString();
			String firstResult = oneResult.get("subject").toString();
			if(!negativeExamples.contains(Pair.of(secondResult, firstResult))){
				Pair<String,String> negativeExample = 
						Pair.of(firstResult, secondResult);
				negativeExamples.add(negativeExample);

			}
		}

		this.closeResources();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}


	@Override
	public Set<Pair<String, String>> generatePositiveExamples(
			Set<String> relations, String typeSubject, String typeObject) {
		String positiveCandidateQuery = super.generatePositiveExampleQuery(relations, typeSubject, typeObject);
		Set<Pair<String,String>> positiveExamples = Sets.newHashSet();
		if(positiveCandidateQuery==null)
			return positiveExamples;

		LOGGER.debug("Executing positive candidate query selection '{}' on Sparql Endpoint...",positiveCandidateQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(positiveCandidateQuery);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		while(results.hasNext()){
			QuerySolution oneResult = results.next();

			String secondResult = oneResult.get("object").toString();
			String firstResult = oneResult.get("subject").toString();
			if(!positiveExamples.contains(Pair.of(secondResult, firstResult))){
				Pair<String,String> positiveExample = 
						Pair.of(firstResult, secondResult);
				positiveExamples.add(positiveExample);
			}

		}

		this.closeResources();
		LOGGER.debug("{} positive examples retrieved.",positiveExamples.size());

		return positiveExamples;
	}

	@Override
	public Set<Pair<String,String>> getKBExamples(String query,String subject,String object,boolean includeInverted){

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",query);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(query);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		Set<Pair<String,String>> examples = Sets.newHashSet();
		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			String secondResult = oneResult.get(object).toString();
			//check object is not a literal
			if(oneResult.get(object).isLiteral())
				secondResult = oneResult.get(object).asLiteral().getLexicalForm();
			String firstResult = oneResult.get(subject).toString();
			if(includeInverted || !examples.contains(Pair.of(secondResult, firstResult))){
				Pair<String,String> currentExample = 
						Pair.of(firstResult, secondResult);
				examples.add(currentExample);
			}
		}

		this.closeResources();
		LOGGER.debug("{} negative examples retrieved.",examples.size());

		return examples;
	}

	public abstract ResultSet executeQuery(String sparqlQuery);

	public void closeResources(){
		if(this.openResource!=null){
			this.openResource.close();
			this.openResource = null;
		}
	}

	@Override
	public int getSupportivePositiveExamples(Set<RuleAtom> rules,Set<String> relations, String typeSubject, String typeObject,
			Set<Pair<String,String>> positiveExamples) {

		if(rules.size()==0)
			return 0;
		String positiveExamplesCountQuery = super.generateRulesPositiveExampleQuery(rules, 
				relations, typeSubject, typeObject);

		return executeSubjectObjectQuery(positiveExamplesCountQuery,positiveExamples).size();
	}

	@Override
	public Set<Pair<String,String>> getMatchingPositiveExamples(Set<RuleAtom> rules,
			Set<String> relations, String typeSubject, String typeObject, Set<Pair<String,String>> positiveExamples) {

		if(rules.size()==0)
			return Sets.newHashSet();
		String positiveExamplesCountQuery = super.generateRulesPositiveExampleQuery(rules, 
				relations, typeSubject, typeObject);

		return executeSubjectObjectQuery(positiveExamplesCountQuery,positiveExamples);

	}

	@Override
	public Set<Pair<String,String>> getMatchingNegativeExamples(Set<RuleAtom> rules,
			Set<String> relations, String typeSubject, String typeObject, Set<Pair<String,String>> negativeExamples, 
			boolean subjectFuntion, boolean objectFunction) {

		if(rules.size()==0)
			return Sets.newHashSet();
		String negativeExamplesCountQuery = super.generateRulesNegativeExampleQuery(rules, 
				relations, typeSubject, typeObject, subjectFuntion, objectFunction);

		return executeSubjectObjectQuery(negativeExamplesCountQuery,negativeExamples);

	}

	public Set<Pair<String,String>> executeHornRuleQuery(Set<RuleAtom> rules, String typeSubject, String typeObject){
		if(rules.size()==0)
			return Sets.newHashSet();
		String hornRuleQuery = super.generateHornRuleQuery(rules, typeSubject, typeObject);

		return executeSubjectObjectQuery(hornRuleQuery,null);

	}

	public Set<Pair<String,String>> executeRestrictiveHornRuleQuery(String queryRestriction,
			Set<RuleAtom> rules, String typeSubject, String typeObject){
		if(rules.size()==0)
			return Sets.newHashSet();
		String hornRuleQuery = super.generateHornRuleQuery(rules, typeSubject, typeObject);
		hornRuleQuery = hornRuleQuery.replaceAll("WHERE \\{", "WHERE {"+queryRestriction+" ");

		return executeSubjectObjectQuery(hornRuleQuery,null);

	}

	private Set<Pair<String,String>> executeSubjectObjectQuery(String query, Set<Pair<String,String>> examples){
		Set<Pair<String,String>> matchingPositiveExamples = Sets.newHashSet();
		if(query==null)
			return matchingPositiveExamples;


		LOGGER.debug("Executing sparql rule query '{}' on Sparql Endpoint...",query);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(query);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		while(results.hasNext()){

			QuerySolution oneResult = results.next();
			String subject = oneResult.get("subject").toString();
			String object = oneResult.get("object").toString();
			if(subject!=null && object!=null && (examples==null || examples.contains(Pair.of(subject, object)))){
				matchingPositiveExamples.add(Pair.of(subject, object));
			}
		}
		this.closeResources();

		return matchingPositiveExamples;

	}

	public int executeCountQuery(String inputQuery){
		//get the count variable
		if(!inputQuery.contains("count")&&!inputQuery.contains("COUNT"))
			return -1;
		LOGGER.debug("Executing sparql rule query '{}' on Sparql Endpoint...",inputQuery);
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(inputQuery);

		int result = -1;
		List<String> resultsVariable = results.getResultVars();
		if(results.hasNext() && resultsVariable.size()>0){
			QuerySolution oneResult = results.next();
			try{
				result = oneResult.getLiteral(resultsVariable.get(0)).getInt();
			}
			catch(Exception e){
				LOGGER.warn("Not able to parse as integer the result of the count query {}.",inputQuery);
			}
		}
		this.closeResources();

		return result;
	}

	public Map<String,Set<Pair<String,String>>> getRulePositiveSupport(Set<Pair<String,String>> positiveExamples){

		Map<String,Set<Pair<String,String>>> predicate2support = Maps.newHashMap();
		Set<String> totalPermutations = Sets.newHashSet();
		totalPermutations.add("subject");
		totalPermutations.add("object");

		Map<String,Set<String>> entity2predicate = Maps.newHashMap();
		LOGGER.debug("Compute predicate support for all positive examples...");
		int exampleCount=0;

		for(Pair<String,String> oneExample:positiveExamples){
			for(String permutation:totalPermutations){


				exampleCount++;
				if(exampleCount%100==0)
					LOGGER.debug("Computed {} examples out of {}",exampleCount,positiveExamples.size()*2);

				String currentExample = oneExample.getLeft().toString();
				if(permutation.equals("object"))
					currentExample = oneExample.getRight().toString();

				if(entity2predicate.containsKey(currentExample)){
					Set<String> currentPredicates = entity2predicate.get(currentExample);
					for(String predicate:currentPredicates){
						Set<Pair<String,String>> currentPredicateSupport = predicate2support.get(predicate.replaceAll("entity", permutation));
						if(currentPredicateSupport==null){
							currentPredicateSupport=Sets.newHashSet();
							predicate2support.put(predicate.replaceAll("entity", permutation), currentPredicateSupport);
						}

						currentPredicateSupport.add(oneExample);
					}
				}
				else{
					String sparqlQuery = "SELECT ?sub ?rel ?obj";
					if(this.graphIri!=null&&this.graphIri.length()>0)
						sparqlQuery+=" FROM "+this.graphIri;
					sparqlQuery+= " WHERE {" +
							"{<"+currentExample.toString()+"> ?rel ?obj.} " +
							"UNION " +
							"{?sub ?rel <"+currentExample.toString()+">.}}";

					long startTime = System.currentTimeMillis();

					if(this.openResource!=null)
						this.openResource.close();

					try{
						ResultSet results = this.executeQuery(sparqlQuery);

						if(!results.hasNext())
							LOGGER.debug("Query '{}' returned an empty result!",sparqlQuery);


						long totalTime = System.currentTimeMillis() - startTime;
						if(totalTime>50000)
							LOGGER.debug("Query '{}' took {} seconds to complete.",sparqlQuery, totalTime/1000.);

						Set<String> currentEntitySupport = Sets.newHashSet();
						entity2predicate.put(currentExample, currentEntitySupport);
						while(results.hasNext()){
							QuerySolution oneResult = results.next();

							String relation = oneResult.get("rel").toString();

							if(this.relationToAvoid!=null&&this.relationToAvoid.contains(relation))
								continue;

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
							boolean inverse = oneResult.get("sub")!=null;
							String predicate = relation+"("+permutation+",_)";
							if(inverse)
								predicate = relation+"(_,"+permutation+")";

							Set<Pair<String,String>> currentSupportExamples = predicate2support.get(predicate);
							if(currentSupportExamples==null){
								currentSupportExamples=Sets.newHashSet();
								predicate2support.put(predicate, currentSupportExamples);
							}
							currentSupportExamples.add(oneExample);


							currentEntitySupport.add(predicate.replaceAll(permutation, "entity"));
						}

					}
					catch(Exception e){
						LOGGER.warn("Unable to execute query '{}'.",sparqlQuery);
					}
					this.closeResources();
				}
			}
		}

		return predicate2support;
	}

}
