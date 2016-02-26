package asu.edu.neg_rule_miner.sparql.jena;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
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

import asu.edu.neg_rule_miner.model.HornRule;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;
import asu.edu.neg_rule_miner.sparql.jena.remote.QuerySparqlRemoteEndpoint;

public abstract class QueryJenaLibrary extends SparqlExecutor {

	protected QueryExecution openResource;

	public QueryJenaLibrary(Configuration config) {
		super(config);
	}

	private final static Logger LOGGER = LoggerFactory.getLogger(QuerySparqlRemoteEndpoint.class.getName());

	@Override
	public Set<Edge<RDFNode>> executeQuery(RDFNode entity, 
			Graph<RDFNode> graph) {
		Set<Edge<RDFNode>> neighbours = Sets.newHashSet();
		//different query if the entity is a literal
		if(entity.isLiteral()){
			this.compareLiterals(entity, graph);
			//if it's a literal do not return any neighbours because they might change based on the graph
			return neighbours;
		}

		String sparqlQuery = "SELECT DISTINCT ?sub ?rel ?obj";
		if(this.graphIri!=null&&this.graphIri.length()>0)
			sparqlQuery+=" FROM "+this.graphIri;
		sparqlQuery+= " WHERE " +
				"{{<"+entity.toString()+"> ?rel ?obj.} " +
				"UNION " +
				"{?sub ?rel <"+entity.toString()+">.}}";

		long startTime = System.currentTimeMillis();

		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(sparqlQuery);

		if(!results.hasNext())
			LOGGER.debug("Query '{}' returned an empty result!",sparqlQuery);


		long totalTime = System.currentTimeMillis() - startTime;
		if(totalTime>50000)
			LOGGER.debug("Query '{}' took {} seconds to complete.",sparqlQuery, totalTime/1000.);


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

			neighbours.add(edgeToAdd);
			graph.addNode(nodeToAdd);
			boolean addEdge = graph.addEdge(edgeToAdd, true);
			if(!addEdge)
				LOGGER.warn("Not able to insert the edge '{}' in the graph '{}'.",edgeToAdd,graph);

		}
		this.closeResources();
		return neighbours;

	}

	@Override
	public Set<Pair<RDFNode, RDFNode>> generateNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject) {
		String negativeCandidateQuery = super.generateNegativeExampleQuery(relations, typeSubject, typeObject);
		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",negativeCandidateQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(negativeCandidateQuery);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			Pair<RDFNode,RDFNode> negativeExample = 
					Pair.of(oneResult.get("subject"), oneResult.get("object"));
			negativeExamples.add(negativeExample);
		}

		this.closeResources();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}


	@Override
	public Set<Pair<RDFNode, RDFNode>> generatePositiveExamples(
			Set<String> relations, String typeSubject, String typeObject) {
		String positiveCandidateQuery = super.generatePositiveExampleQuery(relations, typeSubject, typeObject);
		Set<Pair<RDFNode,RDFNode>> positiveExamples = Sets.newHashSet();
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
			Pair<RDFNode,RDFNode> positiveExample = 
					Pair.of(oneResult.get("subject"), oneResult.get("object"));
			positiveExamples.add(positiveExample);
		}

		this.closeResources();
		LOGGER.debug("{} positive examples retrieved.",positiveExamples.size());

		return positiveExamples;
	}

	@Override
	public Set<Pair<RDFNode, RDFNode>> generateFilteredNegativeExamples(
			Set<String> relations, String typeSubject, String typeObject, int numberExampleOutput) {
		String negativeCandidateQuery = super.generateNegativeExampleQuery(relations, typeSubject, typeObject);
		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		if(negativeCandidateQuery==null)
			return negativeExamples;

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",negativeCandidateQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(negativeCandidateQuery);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);


		double totalExample = 0.;
		Map<String,Set<Pair<RDFNode,RDFNode>>> relation2examples = Maps.newHashMap();
		while(results.hasNext()){
			totalExample++;
			QuerySolution oneResult = results.next();
			String relation = oneResult.get("otherRelation").toString();
			Pair<RDFNode,RDFNode> negativeExample = 
					Pair.of(oneResult.get("subject"), oneResult.get("object"));
			Set<Pair<RDFNode,RDFNode>> currentExamples = relation2examples.get(relation);
			if(currentExamples==null){
				currentExamples=Sets.newHashSet();
				relation2examples.put(relation, currentExamples);
			}
			currentExamples.add(negativeExample);
		}
		this.closeResources();

		if(totalExample<=numberExampleOutput){
			for(Set<Pair<RDFNode,RDFNode>> examples:relation2examples.values())
				negativeExamples.addAll(examples);
			LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());
			return negativeExamples;
		}

		//first add each relation that has higher appereance than threshold
		Set<String> consideredRelation = Sets.newHashSet();
		for(String relation:relation2examples.keySet()){
			Set<Pair<RDFNode,RDFNode>> currentExamples = relation2examples.get(relation);
			double repetition = Math.floor(currentExamples.size()/totalExample*numberExampleOutput);
			if(repetition > 0){
				consideredRelation.add(relation);
				int i=0;
				while(i<repetition||currentExamples.size()<=0){
					Set<RDFNode> consideredSubject = Sets.newHashSet();
					Set<Pair<RDFNode,RDFNode>> consideredExamples = Sets.newHashSet();
					for(Pair<RDFNode,RDFNode> example:currentExamples){
						RDFNode subject = example.getLeft();
						if(consideredSubject.contains(subject))
							continue;
						i++;
						consideredSubject.add(subject);
						consideredExamples.add(example);
						negativeExamples.add(example);
						if(i>=repetition)
							break;
					}
					currentExamples.removeAll(consideredExamples);
				}
			}
		}
		relation2examples.keySet().removeAll(consideredRelation);

		//add remaining examples
		int i=0;
		int totalRemainingExample = numberExampleOutput-negativeExamples.size();
		consideredRelation.clear();
		String bestRelation = null;
		while(i<totalRemainingExample){
			if(consideredRelation.equals(relation2examples.keySet()))
				consideredRelation.clear();

			//get next best example		
			int bestRule = 0;
			for(String relation:relation2examples.keySet()){
				if(consideredRelation.contains(relation))
					continue;
				if(relation2examples.get(relation).size()==0)
					consideredRelation.add(relation);
				if(relation2examples.get(relation).size()>bestRule){
					bestRule = relation2examples.get(relation).size();
					bestRelation = relation;

				}
			}
			//stop
			if(bestRule==0)
				break;
			negativeExamples.add(relation2examples.get(bestRelation).iterator().next());
			relation2examples.get(bestRelation).remove(relation2examples.get(bestRelation).iterator().next());
			consideredRelation.add(bestRelation);
			i++;
		}

		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}

	@Override
	public Set<Pair<RDFNode,RDFNode>> readNegativeExamplesFromFile(File inputFile) throws IOException{
		Set<Pair<RDFNode,RDFNode>> plainNegativeExamples = super.readNegativeExamplesFromFile(inputFile);
		if(plainNegativeExamples==null||plainNegativeExamples.size()==0)
			return plainNegativeExamples;

		StringBuilder subjectFilters = new StringBuilder();
		subjectFilters.append("FILTER(");
		StringBuilder objectFilters = new StringBuilder();
		objectFilters.append("FILTER(");
		Iterator<Pair<RDFNode,RDFNode>> examplesIterator = plainNegativeExamples.iterator();
		while(examplesIterator.hasNext()){
			Pair<RDFNode,RDFNode> example = examplesIterator.next();
			subjectFilters.append("?subject=<"+example.getLeft()+">");
			objectFilters.append("?object=<"+example.getRight()+">");
			if(examplesIterator.hasNext()){
				subjectFilters.append(" || ");
				objectFilters.append(" || ");
			}
			else{
				subjectFilters.append(")");
				objectFilters.append(")");
			}
		}

		String query = "SELECT DISTINCT ?subject ?relation ?object WHERE {?subject ?relation ?object. "+subjectFilters+" "
				+objectFilters+"}";

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",query);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(query);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			Pair<RDFNode,RDFNode> negativeExample = 
					Pair.of(oneResult.get("subject"), oneResult.get("object"));
			negativeExamples.add(negativeExample);
		}

		this.closeResources();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}

	@Override
	public Set<Pair<RDFNode,RDFNode>> getKBExamples(String query,String subject,String object){

		LOGGER.debug("Executing negative candidate query selection '{}' on Sparql Endpoint...",query);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(query);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		Set<Pair<RDFNode,RDFNode>> negativeExamples = Sets.newHashSet();
		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			Pair<RDFNode,RDFNode> negativeExample = 
					Pair.of(oneResult.get(subject), oneResult.get(object));
			negativeExamples.add(negativeExample);
		}

		this.closeResources();
		LOGGER.debug("{} negative examples retrieved.",negativeExamples.size());

		return negativeExamples;
	}

	public abstract ResultSet executeQuery(String sparqlQuery);

	public void closeResources(){
		if(this.openResource!=null){
			this.openResource.close();
			this.openResource = null;
		}
	}

	@Override
	public int getSupportivePositiveExamples(Set<RuleAtom> rules,Set<String> relations, String typeSubject, String typeObject, String subjectConstant,
			String objectConstant) {


		if(rules.size()==0)
			return 0;
		String positiveExamplesCountQuery = super.generatePositiveExampleCountQuery(rules, 
				relations, typeSubject, typeObject, subjectConstant, objectConstant);
		if(positiveExamplesCountQuery==null)
			return 0;


		LOGGER.debug("Executing positive examples coverage rule query '{}' on Sparql Endpoint...",positiveExamplesCountQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(positiveExamplesCountQuery);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		int coverage = 0;
		if(results.hasNext()){
			QuerySolution oneResult = results.next();
			String varName = oneResult.varNames().next();
			try{
				coverage = oneResult.get(varName).asLiteral().getInt();
			}
			catch(Exception e){
				LOGGER.debug("Error while reading result from the positive example count query.",e);
			}
		}
		this.closeResources();

		return coverage;

	}
	
	@Override
	public int getRelativeSupportivePositiveExamples
	(Set<RuleAtom> rules,Set<String> relations, String typeSubject, String typeObject,String subjectConstant,
			String objectConstant) {


		if(rules.size()==0)
			return 0;
		String relativePositiveExamplesCountQuery = super.generateRelativePositiveExampleCountQuery(rules, 
				relations, typeSubject, typeObject, subjectConstant, objectConstant);
		if(relativePositiveExamplesCountQuery==null)
			return 0;


		LOGGER.debug("Executing positive examples coverage rule query '{}' on Sparql Endpoint...",relativePositiveExamplesCountQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(relativePositiveExamplesCountQuery);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		int coverage = 0;
		if(results.hasNext()){
			QuerySolution oneResult = results.next();
			String varName = oneResult.varNames().next();
			try{
				coverage = oneResult.get(varName).asLiteral().getInt();
			}
			catch(Exception e){
				LOGGER.debug("Error while reading result from the positive example count query.",e);
			}
		}
		this.closeResources();

		return coverage;

	}

	public int getTotalCoveredExample(HornRule<RDFNode> rule, String typeSubject, String typeObject){
		if(rule.getRules().size()==0)
			return 0;
		String totalExamplesCountQuery = super.generateTotalExampleCountQuery(rule, typeSubject, typeObject);
		if(totalExamplesCountQuery==null)
			return 0;


		LOGGER.debug("Executing total coverage rule query '{}' on Sparql Endpoint...",totalExamplesCountQuery);
		long startTime = System.currentTimeMillis();
		if(this.openResource!=null)
			this.openResource.close();
		ResultSet results = this.executeQuery(totalExamplesCountQuery);
		LOGGER.debug("Query executed in {} seconds.",(System.currentTimeMillis()-startTime)/1000.0);

		int coverage = 0;
		if(results.hasNext()){
			QuerySolution oneResult = results.next();
			String varName = oneResult.varNames().next();
			try{
				coverage = oneResult.get(varName).asLiteral().getInt();
			}
			catch(Exception e){
				LOGGER.debug("Error while reading result from the positive example count query.",e);
			}
		}
		this.closeResources();

		return coverage;
	}

}
