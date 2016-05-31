package asu.edu.neg_rule_miner.client.evaluation.amie;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.RuleMinerException;
import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

/**
 * Utility class that for a given relation computes the domain type and the codomain type
 * @author sortona
 *
 */
public class OntologyTypesInference {

	private final static Logger LOGGER = LoggerFactory.getLogger(AmieEvaluationClient.class.getName());

	private Set<String> topTypes;
	/**
	 * 
	 * @param topTypes
	 * 			contains top types that are not returnes, such as Thing or Agent. If it is null or empty, any kind of type will be returned
	 */
	public OntologyTypesInference(Set<String> topTypes){
		this.topTypes = Sets.newHashSet();
		if(topTypes!=null && topTypes.size()>0)
			this.topTypes.addAll(topTypes);

	}

	public OntologyTypesInference(){
		this.topTypes = Sets.newHashSet();
	}


	/**For the input relations, compute type subject and type object and return a map where the key is the relation and value is a pair <typeSubject,typeObject>
	 * 
	 * If typeSubject or typeObject is undefined, null is returned in the pair
	 * @param relations
	 * @return
	 */
	public  Map<String,Pair<String,String>> computeTypes(Set<String> relations){

		Map<String,Pair<String,String>> relation2types = Maps.newHashMap();
		if(relations==null || relations.size()==0)
			return relation2types;
		SparqlExecutor executor = ConfigurationFacility.getSparqlExecutor();
		if(executor.getTypePrefix()==null || executor.getTypePrefix().length()==0)
			throw new RuleMinerException("No type prefix specified in the Configuration file!", LOGGER);
		int count = 0;
		int totRelations = relations.size();
		for(String oneRelation:relations){
			count++;
			LOGGER.debug("Computing types for relation {} ({} out of {})",oneRelation,count,totRelations);
			String typesQuery = "select distinct ?type (count(?type) as ?count) ";
			if(executor.getGraphIri()!=null && executor.getGraphIri().length()>0)
				typesQuery+="from "+executor.getGraphIri()+" ";
			typesQuery+=
					"where { ?subject <"+oneRelation+"> "
							+ "?object. ?CURRENT_VARIABLE <"+executor.getTypePrefix()+"> ?type. } GROUP BY ?type";
			String []subjectObject = new String[]{"subject","object"};
			String []subjectObjectTypes = new String[]{null,null};

			for(int i=0;i<2;i++){
				int mostOccurrence = -1;
				String currentTypeQuery = typesQuery.replaceAll("CURRENT_VARIABLE", subjectObject[i]);
				Set<Pair<String,String>> type2count = executor.getKBExamples(currentTypeQuery, "?type", "?count", true);
				for(Pair<String,String> oneTypeCount:type2count){
					int occurrence = Integer.parseInt(oneTypeCount.getRight());
					String type = oneTypeCount.getLeft();
					if(!topTypes.contains(type) && occurrence>mostOccurrence){
						subjectObjectTypes[i] = type;
						mostOccurrence = occurrence;
					}
				}
			}

			relation2types.put(oneRelation, Pair.of(subjectObjectTypes[0], subjectObjectTypes[1]));
		}

		return relation2types;

	}

	public static void main(String[] args){

		SparqlExecutor exec = ConfigurationFacility.getSparqlExecutor();
		exec.getKBExamples("select distinct ?type (count(?type) as ?count) from <http://bye> where { ?subject <dbo:worldSnookerChampionshipRoundsProperty44> ?object. ?subject <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ?type. } GROUP BY ?type", "type", "count", true);
	}

}
