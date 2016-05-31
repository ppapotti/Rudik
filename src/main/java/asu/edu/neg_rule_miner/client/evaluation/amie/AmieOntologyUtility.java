package asu.edu.neg_rule_miner.client.evaluation.amie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;

public class AmieOntologyUtility {

	private final static Logger LOGGER = LoggerFactory.getLogger(AmieOntologyUtility.class.getName());


	/**
	 * Modify an ontology file to amie standard, for instance in amie http://dbpedia.org/resource/ is replaced with db:
	 * and special characters are encoded in UTF-8, like ',' is encoded in %2C
	 * 
	 * IT DOES NOT WITH LITERALS
	 * @throws IOException 
	 */
	public static void adaptOntologyFileToAmieStandard(File inputFile, File outputFile,String separator, String endLine,
			Map<String,String> prefix2substitution) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));


		String line;
		int count=0;
		while((line = reader.readLine())!=null){
			count++;
			if(count%10000==0)
				System.out.println("Analysing "+count+"nth line...");
			String []lineSplit = line.split(">"+separator+"<");
			if(lineSplit.length!=3){
				writer.write(line+"\n");
				continue;
			}

			//modify 
			StringBuilder newTriple = new StringBuilder();
			for(String oneElement:lineSplit){
				if(oneElement.startsWith("<"))
					oneElement = oneElement.substring(1, oneElement.length());
				if(oneElement.endsWith(">"+endLine))
					oneElement = oneElement.substring(0, oneElement.length()-(1+endLine.length()));

				for(String onePrefix:prefix2substitution.keySet()){
					if(oneElement.contains(onePrefix)){
						oneElement = oneElement.substring(0,oneElement.indexOf(onePrefix)) + prefix2substitution.get(onePrefix) + 
								URLEncoder.encode(oneElement.substring(oneElement.indexOf(onePrefix)+onePrefix.length()),"UTF-8");
						break;
					}
				}
				newTriple.append("<"+oneElement+">"+separator);
			}

			writer.write(newTriple.subSequence(0, newTriple.length()-separator.length()).toString()+endLine+"\n");
		}
		reader.close();
		writer.close();

	}

	public static void modifyDbpediaForAmie(File inputFile, File outputFile) throws IOException{
		Map<String,String> prefix2substitution = Maps.newHashMap();
		prefix2substitution.put("http://dbpedia.org/resource/", "db:");
		prefix2substitution.put("http://dbpedia.org/ontology/", "dbo:");

		//use to separate subejct object and relatio in the input file
		String separator = " ";
		//ending line after the object
		String endLine =" .";

		adaptOntologyFileToAmieStandard(inputFile, outputFile, separator, endLine, prefix2substitution);
	}

	public static void computWikidataInstanceOf(File ontologyFile, File newFactsFile) throws IOException{
		ConfigurationFacility.getConfiguration();
		//read all subject and object
		Set<String> entities = Sets.newHashSet();

		BufferedReader reader = new BufferedReader(new FileReader(ontologyFile));
		String line;
		LOGGER.info("Reading ontology entities...");
		while((line = reader.readLine())!=null){
			String []lineSplit = line.split("\t");
			String subject = lineSplit[0].substring(1, lineSplit[0].length()-1);
			String object = lineSplit[2];
			if(object.endsWith(" ."))
				object = object.substring(0, object.length()-2);
			object = object.substring(1,object.length()-1);
			entities.add(subject);
			entities.add(object);
		}
		reader.close();
		LOGGER.info("Read a total of {} entities.",entities.size());

		SparqlExecutor executor = ConfigurationFacility.getSparqlExecutor();
		//for each entity compute instance of
		Set<String> newFacts = Sets.newHashSet();
		LOGGER.info("Computing types for each entity...");
		int count = 0;
		for(String entity:entities){
			count++;
			if(count%10000==0)
				LOGGER.info("Analysing {}th entity...",count);
			String newEntity = entity.substring(entity.lastIndexOf("_")+1);
			if(!newEntity.startsWith("Q")){
				LOGGER.warn("Entity {} does not start with a Q!",entity);
				continue;
			}

			try{
				String query = "SELECT distinct ?type ?label from "+executor.getGraphIri()+" where {<http://www.wikidata.org/entity/"+newEntity+"> <"+executor.getTypePrefix()+"> ?type. "
						+ "?type <http://www.w3.org/2000/01/rdf-schema#label> ?label.}";
				Set<Pair<String,String>> subject2types = executor.getKBExamples(query, "type", "label", true);
				for(Pair<String,String> oneType:subject2types){
					String type = oneType.getLeft();
					if(!type.startsWith("http://www.wikidata.org/entity/"))
						LOGGER.warn("Type '{}' for entity '{}' does not start with the right prefix.",type,entity);
					else
						type = type.replaceAll("http://www.wikidata.org/entity/", "");
					if(oneType.getRight()!=null){
						type = oneType.getRight().replaceAll(" ", "_") + "_" + type;
					}
					newFacts.add("<"+entity+">\t<instance_of_P31>\t<"+type+"> .");
				}
			}
			catch(Exception e ){
				LOGGER.warn("Unable to retrieve types for entity '{}'.",entity,e);
			}
		}
		LOGGER.info("Types for each entity computed with {} new facts produced.",newFacts.size());

		LOGGER.info("Writing new facts in the final file...");
		//write new facts
		BufferedWriter writer = new BufferedWriter(new FileWriter(newFactsFile));
		for(String fact: newFacts){
			writer.write(fact+"\n");
		}
		writer.close();

	}

	public static void main(String[] args) throws Exception{
		File inputFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/wikidata/wikidata.2014.12.tsv");
		File outputFile = new File("/Users/sortona/Documents/KDR/evaluation/amie/Data/wikidata/wikidata_instance_of.tsv");

		//modifyDbpediaForAmie(inputFile, outputFile);

		computWikidataInstanceOf(inputFile, outputFile);

	}

}
