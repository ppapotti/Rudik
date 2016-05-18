package asu.edu.neg_rule_miner.sparql.jena;

import java.util.ArrayList;
import java.util.HashMap;
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


public class TripleFilter {
	public HashMap<String,ArrayList<QuerySolution>> hMap;
	public TripleFilter(){
		hMap= new HashMap<String,ArrayList<QuerySolution>>();
	}
	public ArrayList<QuerySolution> doFilter(ResultSet results, String sub, String rel, String obj, String entity, int limitSubject, int limitObject){
		if(limitSubject < 0)
			limitSubject = (int)Double.POSITIVE_INFINITY;
		if(limitObject < 0)
			limitObject = (int)Double.POSITIVE_INFINITY;
		while(results.hasNext()){
			QuerySolution oneResult = results.next();
			String subject, object;
			if(!oneResult.contains(sub))
				subject = entity;
			else
				subject = oneResult.get(sub).toString();
			if(!oneResult.contains(obj))
				object = entity;
			else
				object = oneResult.get(obj).toString();
			String relation = oneResult.get(rel).toString();
			String subRelKey = relation+"_sub";
			String objRelKey = relation+"_obj";
			if(subject.equals(entity)){
				if(hMap.containsKey(subRelKey)){
					ArrayList<QuerySolution> valSet = hMap.get(subRelKey);
					if(valSet.size() < limitSubject){
						valSet.add(oneResult);
						hMap.put(subRelKey, valSet);
					}
				}
				else{
					ArrayList<QuerySolution> valSet = new ArrayList<QuerySolution>();
					if(valSet.size() < limitSubject){
						valSet.add(oneResult);
						hMap.put(subRelKey, valSet);
					}
				}				
			}
			else if(object.equals(entity)){
				if(hMap.containsKey(objRelKey)){
					ArrayList<QuerySolution> valSet = hMap.get(objRelKey);
					if(valSet.size() < limitObject){
						valSet.add(oneResult);
						hMap.put(objRelKey, valSet);
					}
				}
				else{
					ArrayList<QuerySolution> valSet = new ArrayList<QuerySolution>();
					if(valSet.size() < limitObject){
						valSet.add(oneResult);
						hMap.put(objRelKey, valSet);
					}
				}
			}
		}
		ArrayList<QuerySolution> resultValues = new ArrayList<QuerySolution>();
		for(ArrayList<QuerySolution>valueSet : hMap.values())
			resultValues.addAll(valueSet);
		return resultValues;
	}
/*	public static void main(String[] args){
		QueryJenaLibrary executor = (QueryJenaLibrary) ConfigurationFacility.getSparqlExecutor();
		String entity = "http://dbpedia.org/resource/Mani_Ratnam";
		ResultSet results = executor.executeQuery("SELECT DISTINCT ?sub ?rel ?obj WHERE { {<"+entity+"> ?rel ?obj.} UNION {?sub ?rel <"+entity+">.} }");
		TripleFilter tripFil = new TripleFilter();
		int limitSubject = 100, limitObject = 100;
		String sub = "sub", rel = "rel", obj = "obj";
		ArrayList<QuerySolution> resultTriples = tripFil.doFilter(results, sub, rel, obj, entity, limitSubject, limitObject);
		System.out.println(resultTriples);
	}
*/
}
