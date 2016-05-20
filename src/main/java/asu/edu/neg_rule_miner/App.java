package asu.edu.neg_rule_miner;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.client.evaluation.amie.AmieEvaluationClient;
import asu.edu.neg_rule_miner.model.RuleAtom;
import asu.edu.neg_rule_miner.rule_generator.DynamicPruningRuleDiscovery;
/**
 * Hello world!
 *
 */
public class App 
{
	private final static Logger LOGGER = LoggerFactory.getLogger(AmieEvaluationClient.class.getName());
	public static void main( String[] args ) throws IOException
	{
		Set<String> relations = Sets.newHashSet();
		//		relations.add("http://dbpedia.org/ontology/founder");
		//		relations.add("http://dbpedia.org/ontology/foundedBy");

		//		relations.add("http://dbpedia.org/ontology/spouse");

		//		relations.add("http://dbpedia.org/ontology/director");

		//		relations.add("http://dbpedia.org/ontology/architect");

		//relations.add("http://dbpedia.org/ontology/presidentOf");

		//		relations.add("http://dbpedia.org/ontology/academicAdvisor");


		//		relations.add("http://dbpedia.org/ontology/ceremonialCounty");

		//		relations.add("http://dbpedia.org/ontology/child");

		//		relations.add("http://yago-knowledge.org/resource/hasWonPrize");

		//relations.add("http://yago-knowledge.org/resource/worksAt");

		//		relations.add("http://yago-knowledge.org/resource/isMarriedTo");

		//yago founder
		//				relations.add("http://yago-knowledge.org/resource/created");

		//		relations.add("http://yago-knowledge.org/resource/dealsWith");

		//		relations.add("http://yago-knowledge.org/resource/hasChild");

		//		relations.add("http://yago-knowledge.org/resource/influences");

		//		relations.add("http://yago-knowledge.org/resource/wroteMusicFor");

		//wikidata memeber of relation
		//		relations.add("http://www.wikidata.org/prop/direct/P463");

		//wikidata spouse
		//		relations.add("http://www.wikidata.org/prop/direct/P26");

		//wikidata founder
		//		relations.add("http://www.wikidata.org/prop/direct/P112");

		//wikidata architect 
		//		relations.add("http://www.wikidata.org/prop/direct/P84");

		//wikidata queenOrKing UK
		//relations.add("http://www.wikidata.org/prop/direct/queenOrKing");

		//wikidata plays for
		//		relations.add("http://www.wikidata.org/prop/direct/P54");

		//wikidata oath made by
		//				relations.add("http://www.wikidata.org/prop/direct/P543");

		//wikidata child
		//		relations.add("http://www.wikidata.org/prop/direct/P40");

		//wikidata creator of painting
		//		relations.add("http://www.wikidata.org/prop/direct/P170");


		//		String typeSubject = "http://dbpedia.org/ontology/Organisation";
		//		String typeObject ="http://dbpedia.org/ontology/Person";

		//		String typeSubject = "http://schema.org/Movie";
		//		String typeObject = "http://dbpedia.org/ontology/Person";

		//		String typeSubject = "http://dbpedia.org/ontology/ArchitecturalStructure";
		//		String typeObject="http://dbpedia.org/ontology/Person";

		//dbpedia cerimonal county
		//		String typeSubject = "http://dbpedia.org/ontology/PopulatedPlace";
		//		String typeObject = "http://dbpedia.org/ontology/Region";

		//		String typeSubject = "http://xmlns.com/foaf/0.1/Person";
		//		String typeObject = "http://schema.org/Country";

		//		String typeSubject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
		//		String typeObject ="http://yago-knowledge.org/resource/wordnet_award_106696483";

		//				String typeSubject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
		//				String typeObject = "http://yago-knowledge.org/resource/wordnet_organization_108008335";

		//		String typeSubject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
		//		String typeObject = "http://yago-knowledge.org/resource/wordnet_person_100007846";

		//		String typeSubject = "http://yago-knowledge.org/resource/wordnet_country_108544813";
		//		String typeObject = "http://yago-knowledge.org/resource/wordnet_country_108544813";

		//yago wrote music for movie
		//		String typeSubject = "http://yago-knowledge.org/resource/wordnet_person_100007846";
		//		String typeObject = "http://yago-knowledge.org/resource/wordnet_movie_106613686";

		//		String typeSubject = "http://dbpedia.org/ontology/Person";
		//		String typeObject ="http://dbpedia.org/ontology/Person";

		//wikidata country and political union
		//		String typeSubject = "http://www.wikidata.org/entity/Q6256";
		//		String typeObject = "http://www.wikidata.org/entity/Q1140229";

		//wikidata businees enterprise and person
		//		String typeSubject = "http://www.wikidata.org/entity/Q4830453";
		//		String typeObject = "http://www.wikidata.org/entity/Q5";
		//		
		//wikidata person
		//		String typeSubject = "http://www.wikidata.org/entity/Q5";
		//		String typeObject = "http://www.wikidata.org/entity/Q5";

		//wikidata person and country
		//		String typeSubject = "http://www.wikidata.org/entity/Q5";
		//		String typeObject = "http://www.wikidata.org/entity/Q6256";

		//wikidata person and football national team
		//		String typeSubject = "http://www.wikidata.org/entity/Q5";
		//		String typeObject = "http://www.wikidata.org/entity/Q6979593";

		//wikidata olympic games and persons
		//		String typeSubject = "http://www.wikidata.org/entity/Q159821";
		//		String typeObject = "http://www.wikidata.org/entity/Q5";

		//wikidata person and palace
		//		String typeSubject = "http://www.wikidata.org/entity/Q16560";
		//		String typeObject = "http://www.wikidata.org/entity/Q5";

		//wikidata painting and person
		//		String typeSubject = "http://www.wikidata.org/entity/Q3305213";
		//		String typeObject = "http://www.wikidata.org/entity/Q5";


		String prefix = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> prefix dbo: <http://dbpedia.org/ontology/> prefix wdt: <http://www.wikidata.org/prop/direct/> prefix wd: <http://www.wikidata.org/entity/> ";

		String queryDirectorRestrict = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://schema.org/Movie>. ?object rdf:type <http://dbpedia.org/ontology/Person>. ?subject ?relTarget ?realObject. ?realSubject ?relTarget ?object. ?subject ?relation ?object. FILTER (?relTarget = <http://dbpedia.org/ontology/director>) FILTER (?relation != <http://dbpedia.org/ontology/director>) FILTER NOT EXISTS {?subject <http://dbpedia.org/ontology/director> ?object.} }";
		String queryDirectorUnion = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://schema.org/Movie>. ?object rdf:type <http://dbpedia.org/ontology/Person>. { {?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.} } ?subject ?relation ?object. FILTER (?relTarget = <http://dbpedia.org/ontology/director>) FILTER (?relation != <http://dbpedia.org/ontology/director>) FILTER NOT EXISTS {?subject <http://dbpedia.org/ontology/director> ?object.} }";

		String queryFounderUnion = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type dbo:Organisation. ?object rdf:type dbo:Person. {{?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.}} ?subject ?relation ?object. FILTER (?relTarget = dbo:founder || ?relTarget = dbo:foundedBy) FILTER (?relation != dbo:founder && ?relation != dbo:foundedBy) FILTER NOT EXISTS {?subject dbo:founder ?object.} FILTER NOT EXISTS {?subject dbo:foundedBy ?object.}}";
		String queryFounderPositiveUnion = prefix+"SELECT DISTINCT ?subject ?object WHERE {?subject rdf:type dbo:Organisation. ?object rdf:type dbo:Person. ?subject ?relTarget ?object. FILTER (?relTarget = dbo:founder || ?relTarget = dbo:foundedBy)}";
		String queryDBPediaFounderWithoutBand = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type dbo:Organisation. ?object rdf:type dbo:Person. {{?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.}} ?subject ?relation ?object. FILTER (?relTarget = dbo:founder || ?relTarget = dbo:foundedBy) FILTER (?relation != dbo:founder && ?relation != dbo:foundedBy) FILTER NOT EXISTS {?subject dbo:founder ?object.} FILTER NOT EXISTS {?subject dbo:foundedBy ?object.} FILTER NOT EXISTS {?subject rdf:type <http://dbpedia.org/ontology/Group>.}}";

		String queryDBPediaArchitectUnion = prefix+"select distinct ?subject ?object where { ?subject ?rel ?object. { {?subject <http://dbpedia.org/ontology/architect> ?realObj.} UNION {?realSubj <http://dbpedia.org/ontology/architect> ?object.} } ?subject rdf:type <http://dbpedia.org/ontology/ArchitecturalStructure>. ?object rdf:type <http://dbpedia.org/ontology/Person>. FILTER NOT EXISTS {?subject <http://dbpedia.org/ontology/architect> ?object.} }";

		String queryWorksAtUnion = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_organization_108008335>. { {?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.} } ?subject ?relation ?object. FILTER (?relTarget = <http://yago-knowledge.org/resource/worksAt>) FILTER (?relation != <http://yago-knowledge.org/resource/worksAt>) FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/worksAt> ?object.} }";
		String queryWorksAtRestrict = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_organization_108008335>. ?subject ?relTarget ?realObject. ?realSubject ?relTarget ?object. ?subject ?relation ?object. FILTER (?relTarget = <http://yago-knowledge.org/resource/worksAt>) FILTER (?relation != <http://yago-knowledge.org/resource/worksAt>) FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/worksAt> ?object.} }";

		String queryIsMarriedToRestrict = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?subject ?relTarget ?realObject. ?realSubject ?relTarget ?object. ?subject ?relation ?object. FILTER (?relTarget = <http://yago-knowledge.org/resource/isMarriedTo>) FILTER (?relation != <http://yago-knowledge.org/resource/isMarriedTo>) FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/isMarriedTo> ?object.} }";
		String queryIsMarriedToUnion = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. {{?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.}} ?subject ?relation ?object. FILTER (?relTarget = <http://yago-knowledge.org/resource/isMarriedTo>) FILTER (?relation != <http://yago-knowledge.org/resource/isMarriedTo>) FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/isMarriedTo> ?object.} } ";

		String queryPresidentOf = prefix+"SELECT DISTINCT ?subject ?object WHERE { ?subject rdf:type <http://xmlns.com/foaf/0.1/Person>. ?subject ?relation ?object. FILTER NOT EXISTS {?subject dbo:presidentOf ?object.} FILTER (?object = <http://dbpedia.org/resource/United_States>)} LIMIT 1000";


		String queryDealsWith = prefix+"select distinct ?subject ?object where { ?subject ?rel ?object. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_country_108544813>. FILTER NOT EXISTS {?subjcet <http://yago-knowledge.org/resource/dealsWith> ?object} FILTER (?subject=<http://yago-knowledge.org/resource/China>)}";
		String queryDealsWithPositive = prefix+"select distinct ?subject ?object where { ?subject <http://yago-knowledge.org/resource/dealsWith> ?object. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_country_108544813>. FILTER (?subject=<http://yago-knowledge.org/resource/China>)}";

		String queryWikidataEUmember = prefix+"SELECT distinct ?subject ?object WHERE { ?subject wdt:P463 ?object. ?subject wdt:P31 wd:Q6256. FILTER (?object = wd:Q458) }";
		String queryWikidataNOTEUMember = prefix+"SELECT distinct ?subject ?object WHERE { ?subject ?rel ?object. ?subject wdt:P31 wd:Q6256. FILTER NOT EXISTS {?subject wdt:P463 ?object.} FILTER (?object = wd:Q458) }";

		String queryDBPediaSpouseUnion = prefix+" select distinct ?subject ?object from <http://dbpedia.org> where { ?subject rdf:type <http://dbpedia.org/ontology/Person>. ?object rdf:type <http://dbpedia.org/ontology/Person>. {{?subject ?relTarget ?realObject.} UNION {?realSubject ?relTarget ?object.}} ?subject ?relation ?object. FILTER (?relTarget = <http://dbpedia.org/ontology/spouse>) FILTER (?relation != <http://dbpedia.org/ontology/spouse>) FILTER NOT EXISTS {?subject <http://dbpedia.org/ontology/spouse> ?object.} }";
		String queryDBPediaSopusePositive = prefix+" select distinct ?subject ?object from <http://dbpedia.org> where { ?subject rdf:type <http://dbpedia.org/ontology/Person>. ?object rdf:type <http://dbpedia.org/ontology/Person>. ?subject <http://dbpedia.org/ontology/spouse> ?object. }";

		String queryWikidataSpouseUnion = prefix+"select distinct  ?subject ?object where { ?subject ?rel ?object. ?subject wdt:P31 wd:Q5. ?object wdt:P31 wd:Q5. { {?subject wdt:P26 ?realobject.} UNION {?realsubject wdt:P26 ?object.} } FILTER NOT EXISTS {?subject wdt:P26 ?object.} }";
		String queryWikidataSpousePositive = prefix+"select distinct ?subject ?object where { ?subject wdt:P26 ?object. ?subject wdt:P31 wd:Q5. ?object wdt:P31 wd:Q5. }";

		String queryWikidataFounderUnion = prefix+"select distinct ?subject ?object where { ?subject ?rel ?object. ?subject wdt:P31 wd:Q4830453. ?object wdt:P31 wd:Q5. { {?subject wdt:P112 ?realObject.} UNION {?realSubject wdt:P112 ?object.} } FILTER NOT EXISTS {?subject wdt:P112 ?object.} }";

		String wikidataArchitectUnion = prefix+"select distinct * where { ?subject ?relation ?object. ?subject wdt:P31 wd:Q16560. ?object wdt:P31 wd:Q5. FILTER NOT EXISTS {?subject wdt:P84 ?object.} }";

		String queryCreatedYagoUnion = prefix+"select distinct ?subject ?object where { ?subject ?rel ?object. ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_organization_108008335>. { {?subject <http://yago-knowledge.org/resource/created> ?realObj.} UNION {?realSubj <http://yago-knowledge.org/resource/created> ?object.} } FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/created> ?object.} }";

		String wikidataNotQueenOrKingUK = prefix+"select distinct ?subject ?object where { ?subject ?rel ?object. ?subject wdt:P31 wd:Q5. ?object wdt:P31 wd:Q6256. FILTER (?object = wd:Q145) FILTER NOT EXISTS {?subject wdt:queenOrKing ?object.} } LIMIT 1000";

		String wikidataNotMemberNationalFootballTeam = prefix+"select distinct ?subject ?object where { ?subject ?rel ?object. ?subject wdt:P31 wd:Q5. ?object wdt:P31 wd:Q6979593. FILTER NOT EXISTS {?subject wdt:P54 ?object.} }";

		String wikidataNotOathGivenOlympicGames = prefix+"select distinct ?subject ?object where {?object ?rel ?subject. ?object wdt:P31 wd:Q5. ?subject wdt:P31 wd:Q159821. { {?subject wdt:P543 ?realObject.} UNION {?realSubject wdt:P543 ?object.} } FILTER NOT EXISTS {?subject wdt:P543 ?object.} } LIMIT 1000";

		String dbpediaNotAdvisorUnion = prefix+"select distinct ?subject ?object where { ?subject ?rel ?object. ?subject rdf:type <http://dbpedia.org/ontology/Person>. ?object rdf:type <http://dbpedia.org/ontology/Person>. { {?subject <http://dbpedia.org/ontology/academicAdvisor> ?realObj.} UNION {?realSubj <http://dbpedia.org/ontology/academicAdvisor> ?object.} } FILTER NOT EXISTS {?subject <http://dbpedia.org/ontology/academicAdvisor> ?object.} }";

		String dbpediaNotCerimonialCounty = prefix+" select distinct ?subject ?object where { ?subject rdf:type <http://dbpedia.org/ontology/PopulatedPlace>. ?object rdf:type <http://dbpedia.org/ontology/Region>. { {?subject <http://dbpedia.org/ontology/ceremonialCounty> ?realObject.} UNION {?realSubject <http://dbpedia.org/ontology/ceremonialCounty> ?object.} } ?subject ?relation ?object. FILTER NOT EXISTS {?subject <http://dbpedia.org/ontology/ceremonialCounty> ?object.} }";

		String dbpediaNotChild = prefix+"select distinct ?subject ?object where { ?subject rdf:type <http://dbpedia.org/ontology/Person>. ?object rdf:type <http://dbpedia.org/ontology/Person>. ?subject ?relation ?object. { {?subject <http://dbpedia.org/ontology/child> ?realObject.} UNION {?realSubject <http://dbpedia.org/ontology/child> ?object.} } FILTER NOT EXISTS{?subject <http://dbpedia.org/ontology/child> ?object.} }";

		String yagoNotHasChild = prefix+"select distinct ?subject ?object where { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?subject ?relation ?object. { {?subject <http://yago-knowledge.org/resource/hasChild> ?realObject.} UNION {?realSubject <http://yago-knowledge.org/resource/hasChild> ?object.} } FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/hasChild> ?object.} }";

		String yagoNotInfluence = prefix+"select distinct ?subject ?object where { ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?object rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. ?subject ?relation ?object. { {?realSubject <http://yago-knowledge.org/resource/influences> ?object.} UNION {?subject <http://yago-knowledge.org/resource/influences> ?realObject.} } FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/influences> ?object.} }";

		String yagoWroteMusicFor = prefix +"select distinct ?subject ?object where { ?object rdf:type <http://yago-knowledge.org/resource/wordnet_movie_106613686>. ?subject rdf:type <http://yago-knowledge.org/resource/wordnet_person_100007846>. FILTER NOT EXISTS {?subject <http://yago-knowledge.org/resource/wroteMusicFor> ?object.} ?subject ?relation ?object. { {?subject <http://yago-knowledge.org/resource/wroteMusicFor> ?realObject.} UNION {?realSubject <http://yago-knowledge.org/resource/wroteMusicFor> ?object.} } }";

		String wikidataNotChild = prefix+"select distinct ?subject ?object where { ?subject wdt:P31 wd:Q5. ?object wdt:P31 wd:Q5. ?subject ?relation ?object. { {?subject wdt:P40 ?realObject.} UNION {?realSubject wdt:P40 ?object.} } FILTER NOT EXISTS{?subject wdt:P40 ?object.}}";

		//wikidata painting creator 
		String wikidataNotCreator = prefix+"select distinct ?subject ?object where { ?subject wdt:P31 wd:Q3305213. ?object wdt:P106 wd:Q1028181. ?subject ?relation ?object. { {?subject wdt:P170 ?realObject.} UNION {?realSubject wdt:P170 ?object.} } FILTER NOT EXISTS {?subject wdt:P170 ?object.} }";



		DynamicPruningRuleDiscovery naive = new DynamicPruningRuleDiscovery();

		//		relations.add("http://dbpedia.org/ontology/child");
		//
		//		String typeSubject = "http://dbpedia.org/ontology/Person";
		//		String typeObject = "http://dbpedia.org/ontology/Person";

		relations.add("http://dbpedia.org/ontology/artery");

		String typeSubject = "http://dbpedia.org/ontology/AnatomicalStructure";
		String typeObject = "http://dbpedia.org/ontology/AnatomicalStructure";


		Set<Pair<String,String>> negativeExamples = naive.generateNegativeExamples(relations, typeSubject, typeObject, false, false);

		//		Set<Pair<String,String>> negativeExamples = naive.generateNegativeExamples(new File("negExamples"));



		Set<Pair<String,String>> positiveExamples = naive.generatePositiveExamples(relations, typeSubject, typeObject);

		//		Set<Pair<String,String>> positiveExamples = naive.generateNegativeExamples(new File("posExamples"));



		//		naive.discoverNegativeHornRules(negativeExamples,positiveExamples,relations,typeSubject,typeObject);

		System.out.println(naive.discoverPositiveHornRules(negativeExamples, positiveExamples, relations, typeSubject, typeObject, false, false));

		//		computeMultiplePositiveRules();


		//setting limits

		//set the last parameter to the desired limit to reduce neg examples
		//naive.generateNegativeExamples(relations, typeSubject, typeObject, 100);

		//set the last parameter to the desired limit to reduce pos examples
		//naive.generatePositiveExamples(relations, typeSubject, typeObject, 100);

		//set limit on subject incoming edges when expanding a single entity
		naive.setSubjectLimit(100);

		//set limit on object incoming edges when expanding a single entity
		naive.setObjectLimit(100);
	}


	public static void computeMultiplePositiveRules(){
		Map<Set<String>,Pair<String,String>> relation2typeSubjectObject = Maps.newHashMap();

		//		Set<String> relations1 = Sets.newHashSet();
		//		relations1.add("http://dbpedia.org/ontology/founder");
		//		relations1.add("http://dbpedia.org/ontology/foundedBy");
		//		relation2typeSubjectObject.put(relations1, Pair.of("http://dbpedia.org/ontology/Organisation", "http://dbpedia.org/ontology/Person"));
		//		//
		//		Set<String> relations2 = Sets.newHashSet();
		//		relations2.add("http://dbpedia.org/ontology/spouse");
		//		relation2typeSubjectObject.put(relations2, Pair.of("http://dbpedia.org/ontology/Person", "http://dbpedia.org/ontology/Person"));
		//		//
		Set<String> relations3 = Sets.newHashSet();
		relations3.add("http://dbpedia.org/ontology/academicAdvisor");
		relation2typeSubjectObject.put(relations3, Pair.of("http://dbpedia.org/ontology/Person", "http://dbpedia.org/ontology/Person"));
		//		//
		//		Set<String> relations4 = Sets.newHashSet();
		//		relations4.add("http://dbpedia.org/ontology/successor");
		//		relation2typeSubjectObject.put(relations4, Pair.of("http://dbpedia.org/ontology/Person", "http://dbpedia.org/ontology/Person"));

		//		Set<String> relations5 = Sets.newHashSet();
		//		relations5.add("http://dbpedia.org/ontology/artery");
		//		relation2typeSubjectObject.put(relations5, Pair.of("http://dbpedia.org/ontology/AnatomicalStructure", "http://dbpedia.org/ontology/AnatomicalStructure"));

		//		Set<String> relations6 = Sets.newHashSet();
		//		relations6.add("http://dbpedia.org/ontology/ceremonialCounty");
		//		relation2typeSubjectObject.put(relations6, Pair.of("http://dbpedia.org/ontology/PopulatedPlace", "http://dbpedia.org/ontology/Region"));


		long startTime;

		DynamicPruningRuleDiscovery naive = new DynamicPruningRuleDiscovery();

		Map<String,Long> relation2runningTime = Maps.newHashMap();
		Map<String,List<Set<RuleAtom>>> relation2output = Maps.newHashMap();

		int count=0;
		for(Set<String> currentRelations: relation2typeSubjectObject.keySet()){
			count++;
			String relation = currentRelations.iterator().next();
			LOGGER.debug("Computing output rules for relation {} ({} out of {}).",relation,count,
					relation2typeSubjectObject.size());
			try{

				startTime = System.currentTimeMillis();
				String typeSubject = relation2typeSubjectObject.get(currentRelations).getLeft();
				String typeObject = relation2typeSubjectObject.get(currentRelations).getRight();

				Set<Pair<String,String>> negativeExamples = naive.generateNegativeExamples(currentRelations, typeSubject, typeObject, false, false);



				Set<Pair<String,String>> positiveExamples = naive.generatePositiveExamples(currentRelations, typeSubject, typeObject);

				List<Set<RuleAtom>> output = 
						naive.discoverPositiveHornRules(negativeExamples, positiveExamples, currentRelations, typeSubject, typeObject, false, false);

				relation2runningTime.put(relation, (System.currentTimeMillis()-startTime));
				relation2output.put(relation, output);
			}

			catch(Exception e){
				LOGGER.warn("Error computing output rules.",e);
			}

		}

		for(String relationOutput:relation2output.keySet()){
			System.out.println(relationOutput+"\t"+relation2output.get(relationOutput));
			Long runningTime = relation2runningTime.get(relationOutput);
			if(runningTime!=null){
				System.out.println(relationOutput+"\t"+(runningTime/1000.));
			}
		}






	}
}
