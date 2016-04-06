package asu.edu.neg_rule_miner.sparql.relational.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.naive.OneExampleRuleDiscovery;
import asu.edu.neg_rule_miner.sparql.RDFSimpleNodeResourceImplementation;
import asu.edu.neg_rule_miner.sparql.SparqlExecutor;
import asu.edu.neg_rule_miner.sparql.relational.util.encoding.EncodingUtility;

public class CreateRelationalDatabase {	

	private final static Logger LOGGER = 
			LoggerFactory.getLogger(CreateRelationalDatabase.class.getName());


	public void createDatabase(){
		try{
			Class.forName("org.postgresql.Driver");

			Connection conn = 
					DriverManager.getConnection(DatabaseParameter.getDatabaseUrl().replace(DatabaseParameter.getDatabaseName(),""), 
							DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
			String sql = "CREATE DATABASE "+DatabaseParameter.getDatabaseName();
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			System.out.println("Database created successfully...");
			stmt.close();
			conn.close();
		}
		catch(Exception e){
			e.printStackTrace();

		}
	}

	public void createIndexes(int numTables) throws Exception{

		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();
		String sql;
		Set<String> permutation = Sets.newHashSet();
		permutation.add("subject");
		permutation.add("object");
		String tableName;
		String schema="database_facts";
		for(int i=0;i<numTables;i++){
			for(String onePermutation:permutation){
				tableName = DatabaseParameter.getDatabaseName()+"_"+onePermutation+"_"+i;
				LOGGER.debug("Creating index for table '{}'...",tableName);
				//create an index on the permutation
				sql = "CREATE INDEX "+tableName+"_"+onePermutation+"_index " +
						"ON "+schema+"."+tableName +" ("+onePermutation+")"; 
				stmt.executeUpdate(sql);
			}
		}
		stmt.close();
		conn.close();

	}


	/**
	 * Create numTables tables*2 (subject and object) plus one table
	 * for each alpha character to map entities to their table
	 * @param numTables
	 * @throws Exception
	 */
	public void createTables(int numTables) throws Exception{
		Class.forName("org.postgresql.Driver");

		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();

		//create subject-object tables
		Set<String> permutation = Sets.newHashSet();
		permutation.add("subject");
		permutation.add("object");
		String tableName;
		String sql;
		String schema="database_facts";
		Set<String> allTables = Sets.newHashSet();
		for(int i=0;i<numTables;i++){
			for(String onePermutation:permutation){
				tableName = DatabaseParameter.getDatabaseName()+"_"+onePermutation+"_"+i;
				String objectType = "character varying(255)";
				if(onePermutation.equals("subject"))
					objectType="text";
				sql = "CREATE TABLE "+schema+"."+tableName+
						"(id bigserial NOT NULL,"+
						"subject character varying(255) NOT NULL,"+
						"predicate character varying(255) NOT NULL,"+
						"object "+objectType+" NOT NULL,"+
						"CONSTRAINT "+tableName +"_pkey PRIMARY KEY (id))"; 
				stmt.executeUpdate(sql);
				allTables.add(schema+"."+tableName);
			}

		}

		//create table for alpha letter to map correspondance between entity and table
		char charcater = 'a';
		schema = "entity_mapping";
		for(int i=0;i<27;i++){
			if(i==26)
				charcater='0'; //indicate special subject/object that do not start with an alpha character
			for(String onePermutation:permutation){
				tableName = DatabaseParameter.getDatabaseName()+"_entity_"+charcater+"_"+onePermutation;
				sql = "CREATE TABLE "+schema+"."+tableName+
						"(entity character varying(255) NOT NULL,"+
						"table_name  character varying(255) NOT NULL,"+
						"CONSTRAINT "+tableName +"_pkey PRIMARY KEY (entity))"; 
				stmt.executeUpdate(sql);
			}

			charcater++;
		}

		//create metadata table
		tableName = DatabaseParameter.getDatabaseName()+"_total";
		sql = "CREATE TABLE metadata.facts_count"+
				"(table_name character varying(255) NOT NULL,"+
				"row_count integer,"+
				"CONSTRAINT facts_count_pkey PRIMARY KEY (table_name))"; 
		stmt.executeUpdate(sql);

		//insert lines for each created table
		for(String table:allTables){
			sql = "INSERT INTO metadata.facts_count"
					+ "(table_name, row_count) " + "VALUES"
					+ "('"+table+"', 0)";
			stmt.executeUpdate(sql);
		}

		//create big final table
		//		tableName = DatabaseParameter.getDatabaseName()+"_total";
		//		sql = "CREATE TABLE "+tableName+
		//				"(id bigserial NOT NULL,"+
		//				"subject character varying(255) NOT NULL,"+
		//				"predicate character varying(255) NOT NULL,"+
		//				"object text NOT NULL,"+
		//				"CONSTRAINT "+tableName +"_pkey PRIMARY KEY (id))"; 
		//		stmt.executeUpdate(sql);

		//create the index 
		System.out.println("Tables set created successfully...");
		stmt.close();
		conn.close();
	}

	public void insertTriples(File triplesFile) throws Exception{
		Class.forName("org.postgresql.Driver");

		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();

		File []toAnalyse = {triplesFile};
		if(triplesFile.isDirectory())
			toAnalyse = triplesFile.listFiles();

		Map<String,Integer> subjectFactsCount = Maps.newHashMap();
		Map<String,Integer> objectFactsCount = Maps.newHashMap();
		this.readTablesFactsCount(subjectFactsCount, objectFactsCount);
		Map<String,Map<String,String>> entity2TableReference = this.getEntity2TableReference();

		Map<String,Map<String,String>> newEntity2TableReference = Maps.newHashMap();

		String sql=null;

		String line;
		String tableName;
		String lineCopy;
		BufferedReader reader;
		String entityMappingTable;

		/**
		 * LOGGING AND SAFETY REASON PURPOSE
		 */
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("entity2table.log"),true));
		StringBuilder appender;
		/**
		 * 
		 */

		for(File tripleFile:toAnalyse){
			if(!tripleFile.getName().endsWith("ttl"))
				continue;
			LOGGER.debug("Analysing file '{}'...",tripleFile);
			writer.write("# file "+tripleFile+"\n");
			reader = new BufferedReader(new FileReader(tripleFile));
			int tripleCount=1;
			appender = new StringBuilder();
			while((line=reader.readLine())!=null){
				try{
					lineCopy=line;
					if(tripleCount%100000==0){
						LOGGER.debug("Inserting triples number '{}'",tripleCount);
						//insert in the logging file
						writer.write(appender.toString());
						writer.flush();
						appender = new StringBuilder();

					}
					int index = lineCopy.indexOf("> <");
					if(index<=0){
						LOGGER.debug("Line '{}' is not a triple.",line);
						continue;
					}
					String subject = lineCopy.substring(1,index);
					lineCopy = lineCopy.substring(index+3);
					index = lineCopy.indexOf("> ");
					if(index<=0){
						LOGGER.debug("Line '{}' is not a triple.",line);
						continue;
					}
					String predicate = lineCopy.substring(0,index);
					lineCopy = lineCopy.substring(index+2);
					if(!lineCopy.endsWith(" .")){
						LOGGER.debug("Line '{}' is not a triple.",line);
						continue;
					}
					String object = lineCopy.substring(0,lineCopy.length()-2);
					boolean isNotLiteral = object.startsWith("<")&&object.endsWith(">");
					if(isNotLiteral)
						object=object.substring(1,object.length()-1);

					//insert subject

					//get the right table
					entityMappingTable = EncodingUtility.getMappingTableName(subject, "subject");
					tableName = entity2TableReference.get(entityMappingTable).get(subject);
					if(tableName==null&&newEntity2TableReference.get(entityMappingTable)!=null)
						tableName = newEntity2TableReference.get(entityMappingTable).get(subject);
					//assign a new table to the entity
					if(tableName==null){
						tableName = this.getNextTable(subjectFactsCount);
						//store the new table in the logging file
						appender.append(subject+"\tsubject\t"+tableName+"\n");
						Map<String,String> newMap = newEntity2TableReference.get(entityMappingTable);
						if(newMap==null){
							newMap=Maps.newHashMap();
							newEntity2TableReference.put(entityMappingTable, newMap);
						}
						newMap.put(subject, tableName);
					}
					sql = "INSERT INTO "+tableName
							+ "(subject, predicate, object) " + "VALUES"
							+ "('"+EncodingUtility.escapeSQLCharacter(subject)+"','"+
							EncodingUtility.escapeSQLCharacter(predicate)+"','"+EncodingUtility.escapeSQLCharacter(object)+"')";
					stmt.executeUpdate(sql);

					//update count for the table
					subjectFactsCount.put(tableName, subjectFactsCount.get(tableName)+1);

					//insert object
					if(isNotLiteral){

						entityMappingTable = EncodingUtility.getMappingTableName(object, "object");
						tableName = entity2TableReference.get(entityMappingTable).get(object);
						if(tableName==null&&newEntity2TableReference.get(entityMappingTable)!=null)
							tableName = newEntity2TableReference.get(entityMappingTable).get(object);
						if(tableName==null){
							//get a new table
							tableName = this.getNextTable(objectFactsCount);
							//store the new table in the logging file
							appender.append(object+"\tobject\t"+tableName+"\n");
							Map<String,String> newMap = newEntity2TableReference.get(entityMappingTable);
							if(newMap==null){
								newMap=Maps.newHashMap();
								newEntity2TableReference.put(entityMappingTable, newMap);
							}
							newMap.put(object, tableName);
						}
						sql = "INSERT INTO "+tableName
								+ "(subject, predicate, object) " + "VALUES"
								+ "('"+EncodingUtility.escapeSQLCharacter(subject)+"','"
								+EncodingUtility.escapeSQLCharacter(predicate)+"','"+EncodingUtility.escapeSQLCharacter(object)+"')";
						stmt.executeUpdate(sql);
						//update count for the table
						objectFactsCount.put(tableName, objectFactsCount.get(tableName)+1);
					}

					//insert into the final big table 
					//					tableName = DatabaseParameter.getDatabaseName()+"_total";
					//					sql = "INSERT INTO "+tableName
					//							+ "(subject, predicate, object) " + "VALUES"
					//							+ "('"+subject+"','"+predicate+"','"+object+"')";
					//					stmt.executeUpdate(sql);
					tripleCount++;
				}
				catch(Exception e){
					LOGGER.debug("Unable to insert triple for the line '{}'",line,e);
				}

			}

			//append the remaining logging information
			writer.write(appender.toString());
			writer.flush();
			LOGGER.debug("Inserted a total of '{}' triples.",(tripleCount-1));
			reader.close();
		}

		writer.close();

		LOGGER.debug("Updating entity mapping reference for {} tables...",newEntity2TableReference.size());
		//insert metadata information
		int tableCount=0;
		for(String table:newEntity2TableReference.keySet()){
			tableCount++;
			Map<String,String> currentEntities = newEntity2TableReference.get(table);
			LOGGER.debug("Table '{}', {} out of {} with {} entities.",table,tableCount,newEntity2TableReference.size(),currentEntities.size());
			for(String entity:currentEntities.keySet()){
				try{
					sql= "INSERT INTO entity_mapping."+table
							+ "(entity, table_name) " + "VALUES"
							+ "('"+EncodingUtility.escapeSQLCharacter(entity)+"','"+currentEntities.get(entity)+"')";
					stmt.executeUpdate(sql);
				}
				catch(Exception e){
					LOGGER.debug("Error while updating primary keys with query '{}'",sql,e);
				}
			}
		}

		LOGGER.debug("Updating facts count information...");
		for(String subject:subjectFactsCount.keySet()){
			stmt.executeUpdate("UPDATE metadata.facts_count SET row_count = "+subjectFactsCount.get(subject)+" WHERE table_name = '"+subject+"'");
		}

		for(String object:objectFactsCount.keySet()){
			stmt.executeUpdate("UPDATE metadata.facts_count SET row_count = "+objectFactsCount.get(object)+" WHERE table_name = '"+object+"'");
		}

		stmt.close();
		conn.close();
	}

	private void checkConsistencyLogFile(File logFile, boolean update) throws Exception{

		Map<String,Map<String,String>> entity2tableReference = this.getEntity2TableReference();

		Map<String,String> entity2table = Maps.newHashMap();

		Map<String,Map<String,String>> newEntity2tableReference = Maps.newHashMap();

		BufferedReader reader = new BufferedReader(new FileReader((logFile)));

		String line;

		String mappingTable;

		String []lineSplit;

		Map<String,String> currentMap;
		while((line=reader.readLine())!=null){

			lineSplit = line.split("\t");
			if(lineSplit.length!=3)
				continue;
			if(entity2table.containsKey(lineSplit[0]+"_"+lineSplit[1]))
				LOGGER.warn("Entity '{}' appears in table '{}' and table '{}'.",lineSplit[0]+"_"+lineSplit[1],lineSplit[2],entity2table.get(
						entity2table.get(lineSplit[0]+"_"+lineSplit[1])));
			else
				entity2table.put(lineSplit[0]+"_"+lineSplit[1], lineSplit[2]);

			if(update){
				mappingTable = EncodingUtility.getMappingTableName(lineSplit[0], lineSplit[1]);

				if(entity2tableReference.containsKey(mappingTable)&&entity2tableReference.get(mappingTable).containsKey(lineSplit[0])){

					if(!entity2tableReference.get(mappingTable).get(lineSplit[0]).equals(lineSplit[2]))
						LOGGER.debug("Different table for entity '{}': table '{}' in the database and table '{}' in the log file.",lineSplit[0],
								entity2tableReference.get(mappingTable).get(lineSplit[0]),lineSplit[2]);
				}
				else{
					currentMap = newEntity2tableReference.get(mappingTable);
					if(currentMap==null){
						currentMap=Maps.newHashMap();
						newEntity2tableReference.put(mappingTable, currentMap);
					}
					currentMap.put(lineSplit[0], lineSplit[2]);
				}
			}
		}
		reader.close();

		if(update){
			Class.forName("org.postgresql.Driver");

			Connection conn = 
					DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
							DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
			Statement stmt = conn.createStatement();

			String sql=null;
			//write tables
			StringBuilder values;
			Map<String,String> currentValues;
			if(newEntity2tableReference.size()==0)
				LOGGER.debug("No entity mapping reference to update.");
			for(String newMappingTable:newEntity2tableReference.keySet()){
				currentValues = newEntity2tableReference.get(newMappingTable);
				LOGGER.debug("Inserting into '{}' {} entities.",newMappingTable,currentValues.size());
				values = new StringBuilder();
				int i=currentValues.size();
				for(String value:currentValues.keySet()){
					i--;
					values.append("('"+EncodingUtility.escapeSQLCharacter(value)+"','"+currentValues.get(value)+"')");
					if(i!=0)
						values.append(", ");
					else
						values.append(";");
				}

				sql = "INSERT INTO entity_mapping."+newMappingTable+" (entity,table_name) VALUES "+values.toString();
				stmt.executeUpdate(sql);
			}

			stmt.close();
			conn.close();
		}

	}

	private void updateRowsCount(int startTable,int lastTable) throws Exception{

		Class.forName("org.postgresql.Driver");

		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();

		String tableName;

		int numTable=0;
		for(int i=startTable;i<lastTable;i++){
			numTable++;
			LOGGER.debug("Considering table {} out of {}",numTable,lastTable-startTable);
			tableName = "database_facts."+DatabaseParameter.getDatabaseName()+"_subject_"+i;

			stmt.executeUpdate("UPDATE metadata.facts_count SET row_count = (SELECT count(*) FROM "+tableName+") WHERE table_name = '"+tableName+"'");

			tableName = "database_facts."+DatabaseParameter.getDatabaseName()+"_object_"+i;

			stmt.executeUpdate("UPDATE metadata.facts_count SET row_count = (SELECT count(*) FROM "+tableName+") WHERE table_name = '"+tableName+"'");

		}

		stmt.close();
		conn.close();

	}


	private String getNextTable(Map<String,Integer> tables){
		Map<String,Integer> newMap = 
				ImmutableSortedMap.copyOf(tables, Ordering.natural().onResultOf(Functions.forMap(tables)).compound(Ordering.natural()));
		return newMap.keySet().iterator().next();

	}


	private void readTablesFactsCount(Map<String,Integer> subject2count, Map<String,Integer> object2count) throws Exception{
		Class.forName("org.postgresql.Driver");

		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();
		String sql = "SELECT * FROM metadata.facts_count";
		ResultSet rs = stmt.executeQuery(sql);

		while(rs.next()){
			String tableName  = rs.getString("table_name");
			int rowCount = rs.getInt("row_count");
			if(tableName.contains("subject"))
				subject2count.put(tableName, rowCount);
			else
				object2count.put(tableName, rowCount);
		}

		stmt.close();
		conn.close();
	}

	private Map<String,Map<String,String>> getEntity2TableReference() throws Exception{
		char charcater = 'a';
		String schema = "entity_mapping";
		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();
		String sql ;
		String tableName;
		//create subject-object tables
		Set<String> permutation = Sets.newHashSet();
		permutation.add("subject");
		permutation.add("object");
		Map<String,Map<String,String>> entity2TableReference = Maps.newHashMap();
		for(int i=0;i<27;i++){
			if(i==26)
				charcater='0'; //indicate special subject/object that do not start with an alpha character
			for(String onePermutation:permutation){
				tableName = DatabaseParameter.getDatabaseName()+"_entity_"+charcater+"_"+onePermutation;
				sql = "SELECT * FROM "+schema+"."+tableName; 
				ResultSet rs = stmt.executeQuery(sql);

				Map<String,String> currentMap = Maps.newHashMap();
				while(rs.next()){
					String entity  = rs.getString("entity");
					String mappedTableName = rs.getString("table_name");
					currentMap.put(entity, mappedTableName);
				}
				rs.close();
				entity2TableReference.put(tableName, currentMap);
			}

			charcater++;
		}
		stmt.close();
		conn.close();
		return entity2TableReference;

	}

	public void emptyTable(String tableName,Statement stmt) throws Exception{
		Class.forName("org.postgresql.Driver");



		//create table for each letter, for subject and object and a total table
		String sql = "DELETE FROM "+tableName;
		stmt.executeUpdate(sql);
	}

	public void emptyAllTables(int numTables) throws Exception{
		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();

		char character = 'a';
		String tableName;
		String schema="entity_mapping";
		for(int i=0;i<27;i++){
			if(i==26)
				character='0';
			tableName = schema+"."+DatabaseParameter.getDatabaseName()+"_entity_"+character+"_subject";
			LOGGER.debug("Emptying table '{}'...",tableName);
			this.emptyTable(tableName,stmt);
			tableName = schema+"."+DatabaseParameter.getDatabaseName()+"_entity_"+character+"_object";
			LOGGER.debug("Emptying table '{}'...",tableName);
			this.emptyTable(tableName,stmt);
			character++;
		}
		//empty facts tables
		schema="database_facts";
		for(int i=0;i<numTables;i++){
			tableName = schema+"."+DatabaseParameter.getDatabaseName()+"_subject_"+i;
			LOGGER.debug("Emptying table '{}'...",tableName);
			this.emptyTable(tableName,stmt);
			//update the metadata record
			stmt.executeUpdate("UPDATE metadata.facts_count SET row_count = 0 WHERE table_name = '"+tableName+"'");
			//restart the sequence
			tableName+="_id_seq";
			stmt.executeUpdate("alter sequence "+tableName+" restart with 1");


			tableName = schema+"."+DatabaseParameter.getDatabaseName()+"_object_"+i;
			LOGGER.debug("Emptying table '{}'...",tableName);
			this.emptyTable(tableName,stmt);
			//update the metadata record
			stmt.executeUpdate("UPDATE metadata.facts_count SET row_count = 0 WHERE table_name = '"+tableName+"'");
			//restart the sequence
			tableName+="_id_seq";
			stmt.executeUpdate("alter sequence "+tableName+" restart with 1");		
		}


		//empty final tablek
		//LOGGER.debug("Emptying table '{}'...",DatabaseParameter.getDatabaseName()+"_total");
		//this.emptyTable(DatabaseParameter.getDatabaseName()+"_total");
	}



	public static void main(String[] args) throws Exception{
		ConfigurationFacility.getConfiguration();
		CreateRelationalDatabase db = new CreateRelationalDatabase();
		//db.createDatabase();
		//db.createTables(1000);
		//File fr = new File("/Users/sortona/Documents/KDR/Data/DBPedia");
		//db.emptyAllTables(1000);
		File fr = new File("/Users/sortona/Documents/KDR/Data/DBPedia/specific-mappingbased-properties_en.ttl");
		//db.insertTriples(fr);

		//db.checkConsistency(0,1000, true);
		//db.checkConsistencyLogFile(new File("entity2table.log"), true);
		//db.updateRowsCount(251, 1000);
		//db.createBigTable(fr);


		//fireQueryTest("http://dbpedia.org/resource/United_States");

		db.query();
	}

	public void createBigTable(File file) throws Exception{

		String line;

		StringBuilder values= new StringBuilder();
		int totalLine=1;
		String lineCopy=null;
		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		while((line=reader.readLine())!=null){
			try{
				if(totalLine%1000000==0)
					LOGGER.debug("Inserted {} of triples.",totalLine);
				if(totalLine%100000==0){
					stmt.executeUpdate("INSERT INTO facts (subject,predicate,object) VALUES "+values.subSequence(0, values.length()-2)+";");
					values=new StringBuilder();
				}
				lineCopy=line;
				int index = lineCopy.indexOf("> <");
				if(index<=0){
					LOGGER.debug("Line '{}' is not a triple.",line);
					continue;
				}
				String subject = lineCopy.substring(1,index);
				lineCopy = lineCopy.substring(index+3);
				index = lineCopy.indexOf("> ");
				if(index<=0){
					LOGGER.debug("Line '{}' is not a triple.",line);
					continue;
				}
				String predicate = lineCopy.substring(0,index);
				lineCopy = lineCopy.substring(index+2);
				if(!lineCopy.endsWith(" .")){
					LOGGER.debug("Line '{}' is not a triple.",line);
					continue;
				}
				String object = lineCopy.substring(0,lineCopy.length()-2);
				boolean isNotLiteral = object.startsWith("<")&&object.endsWith(">");
				if(isNotLiteral)
					object=object.substring(1,object.length()-1);

				values.append("('"+EncodingUtility.escapeSQLCharacter(subject)+"','"+
						EncodingUtility.escapeSQLCharacter(predicate)+"','"+EncodingUtility.escapeSQLCharacter(object)+"'), ");
			}
			catch(Exception e){
				LOGGER.debug("Not able to insert line '{}'",lineCopy);
			}
			finally{totalLine++;}
		}

		reader.close();
		if(values.length()>0)
			stmt.executeUpdate("INSERT INTO facts (subject,predicate,object) VALUES "+values.subSequence(0, values.length()-2)+";");
		LOGGER.debug("Inserted a total of {} triples)",totalLine);
		stmt.close();
		conn.close();

	}

	public static void fireQueryTest(String entity) throws Exception{
		long start = System.currentTimeMillis();
		Class.forName("org.postgresql.Driver");
		Connection conn = DriverManager.getConnection(DatabaseParameter.getDatabaseUrl(), 
				DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();

		Set<String> permutation = Sets.newHashSet();
		permutation.add("subject");
		permutation.add("object");

		Graph<RDFNode> graph = new Graph<RDFNode>();
		RDFNode entityNode = new RDFSimpleNodeResourceImplementation(entity);
		graph.addNode(entityNode);
		String sql;

		//get tables
		String originalTableName;
		Map<String,String> targetTables = Maps.newHashMap();
		for(String currentPermutation:permutation){
			originalTableName="entity_mapping."+EncodingUtility.getMappingTableName(entity, currentPermutation);
			sql = "SELECT table_name FROM "+originalTableName+" WHERE entity= '"+entity+"'";
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				targetTables.put(currentPermutation, rs.getString("table_name"));
			}
		}

		for(String currentPermutation:targetTables.keySet()){
			sql = "SELECT subject, predicate, object FROM database_facts."+targetTables.get(currentPermutation)+" WHERE "+currentPermutation+"= '"+entity+"'";
			ResultSet rs = stmt.executeQuery(sql);
			//STEP 5: Extract data from result set
			String target = "object";
			if(permutation.equals("object"))
				target="subject";
			String queryTarget;
			RDFNode newNode;
			Edge<RDFNode> toAdd;
			while(rs.next()){
				queryTarget  = rs.getString(target);
				newNode = new RDFSimpleNodeResourceImplementation(queryTarget);
				graph.addNode(newNode);
				toAdd = new Edge<RDFNode>(entityNode,newNode,rs.getString("predicate"));
				if(permutation.equals("object"))
					toAdd = new Edge<RDFNode>(newNode,entityNode,rs.getString("predicate"));
				graph.addEdge(toAdd, true);
			}
			rs.close();
		}
		stmt.close();
		conn.close();
		LOGGER.debug("Executed in '{}' seconds.",(System.currentTimeMillis()-start)/1000.);

	}

	public static void fireMultipleQueryTest(File inputEntityFile) throws Exception{
		Set<String> entity = Sets.newHashSet();
		BufferedReader reader = new BufferedReader(new FileReader(inputEntityFile));
		String line;
		while((line=reader.readLine())!=null){
			entity.add(line);
		}
		reader.close();
		long start=System.currentTimeMillis();
		for(String singleEntity:entity){
			fireQueryTest(singleEntity);
		}
		LOGGER.debug("Multiple queries executed in {} seconds.",(System.currentTimeMillis()-start)/1000.);

	}


	public void query() throws Exception{

		int numThreads = 100;
		BufferedReader reader = new BufferedReader(new FileReader(new File("dbpedia_founder_neg_examples")));
		String line;
		String []lineSplit;

		int i=0;
		List<Set<String>> splitLists = Lists.newArrayList();
		while((line=reader.readLine())!=null){
			if(i==numThreads)
				i=0;

			if(splitLists.size()<=i){
				splitLists.add(new HashSet<String>());
			}
			Set<String> currentPair = splitLists.get(i);
			lineSplit=line.split("\t");
			currentPair.add(lineSplit[0]);
			i++;
		}
		reader.close();


		long start = System.currentTimeMillis();
		Map<String,Set<String>> output = Maps.newConcurrentMap();
		List<Thread> activeThreads = Lists.newArrayList();
		for(i=0;i<numThreads;i++){
			Thread current_thread = new Thread(new OneThread(splitLists.get(i),output), "Thread"+i);
			activeThreads.add(current_thread);
			current_thread.start();

		}
		for(Thread t:activeThreads){
			t.join();
		}

		System.out.println("End running time:"+(System.currentTimeMillis()-start)/1000.);
		System.out.println(output.size());

	}

	private class OneThread implements Runnable{


		private Set<String> examples;

		private Map<String,Set<String>> output;


		public OneThread(Set<String> examples, Map<String,Set<String>> output){
			this.examples = examples;
			this.output=output;
		}

		public void run(){
			Connection conn=null;
			Statement stmt=null;
			ResultSet rs=null;
			try{
				Class.forName("org.postgresql.Driver");
				conn = DriverManager.getConnection(DatabaseParameter.getDatabaseUrl(), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
				//conn.setAutoCommit(false);
				stmt = conn.createStatement();
				String encodedExample;
				for(String oneExample:examples){
					long start=System.currentTimeMillis();
					encodedExample = EncodingUtility.escapeSQLCharacter(oneExample);
					rs = stmt.executeQuery("SELECT * from facts where subject='"+encodedExample+"'");
					
					while(rs.next()){
						String relation = rs.getString("predicate");
						String object = rs.getString("object");

						Set<String> currentExamples = output.get(relation);
						if(currentExamples==null){
							currentExamples=Sets.newHashSet();
							output.put(relation, currentExamples);
						}
						currentExamples.add(object);
					}
					rs.close();

					rs = stmt.executeQuery("SELECT * from facts where object='<"+encodedExample+">' and object ~~ '<%'::text LIMIT 1000");

					while(rs.next()){
						String relation = rs.getString("predicate");
						String subject = rs.getString("subject");

						Set<String> currentExamples = output.get(relation);
						if(currentExamples==null){
							currentExamples=Sets.newHashSet();
							output.put(relation, currentExamples);
						}
						currentExamples.add(subject);
					}
					rs.close();
					LOGGER.debug(((System.currentTimeMillis()-start)/1000.)+"");
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
			finally{
				LOGGER.debug("Thread {} done",Thread.currentThread().getId());

				try{
					stmt.close();
					conn.close();
				}
				catch(SQLException e1){
					e1.printStackTrace();
				}
			}

		}

	}
}
