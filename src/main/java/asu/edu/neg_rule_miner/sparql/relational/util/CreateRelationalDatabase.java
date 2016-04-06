package asu.edu.neg_rule_miner.sparql.relational.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.naive.OneExampleRuleDiscovery;

public class CreateRelationalDatabase {	

	private final static Logger LOGGER = 
			LoggerFactory.getLogger(CreateRelationalDatabase.class.getName());


	public void createDatabase(){
		try{
			Class.forName("org.postgresql.Driver");

			Connection conn = 
					DriverManager.getConnection(DatabaseParameter.getDatabaseUrl(), 
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


	public void createTables() throws Exception{
		Class.forName("org.postgresql.Driver");

		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()
						+"/"+DatabaseParameter.getDatabaseName()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();

		//create table for each letter, for subject and object and a total table
		char charcater = 'a';
		String sql;
		Set<String> permutation = Sets.newHashSet();
		permutation.add("subject");
		permutation.add("object");
		for(int i=0;i<27;i++){
			if(i==26)
				charcater='0'; //indicate special subject/object that do not end with an alpha character

			for(String singlePermutation:permutation){
				String tableName = DatabaseParameter.getDatabaseName()+"_"+singlePermutation+"_"+charcater;
				String objectType = "character varying(255)";
				if(singlePermutation.equals("subject"))
					objectType="text";
				sql = "CREATE TABLE "+tableName+
						"(id bigserial NOT NULL,"+
						"subject character varying(255) NOT NULL,"+
						"predicate character varying(255) NOT NULL,"+
						"object "+objectType+" NOT NULL,"+
						"CONSTRAINT "+tableName +"_pkey PRIMARY KEY (id))"; 
				stmt.executeUpdate(sql);

				//create index
				sql = "CREATE INDEX "+tableName+"_"+singlePermutation+"_index " +
						"ON "+tableName +" ("+singlePermutation+")"; 
				stmt.executeUpdate(sql);
			}

			charcater++;
		}

		//create big final table
		String tableName = DatabaseParameter.getDatabaseName()+"_total";
		sql = "CREATE TABLE "+tableName+
				"(id bigserial NOT NULL,"+
				"subject character varying(255) NOT NULL,"+
				"predicate character varying(255) NOT NULL,"+
				"object text NOT NULL,"+
				"CONSTRAINT "+tableName +"_pkey PRIMARY KEY (id))"; 
		stmt.executeUpdate(sql);

		//create the index 
		System.out.println("Database created successfully...");
		stmt.close();
		conn.close();
	}

	public void insertTriples(File triplesFile) throws Exception{
		BufferedReader reader = new BufferedReader(new FileReader(triplesFile));
		Class.forName("org.postgresql.Driver");

		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()
						+"/"+DatabaseParameter.getDatabaseName()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();

		//create table for each letter, for subject and object and a total table
		String sql;

		String line;
		String tableName;
		int tripleCount=1;
		String lineCopy;
		while((line=reader.readLine())!=null){
			try{
				lineCopy=line;
				if(tripleCount%100000==0)
					LOGGER.debug("Inserting triples number '{}'",tripleCount);
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

				if(subject.contains("'"))
					subject=subject.replaceAll("'", "''");
				if(predicate.contains("'"))
					predicate=predicate.replaceAll("'", "''");
				if(object.contains("'"))
					object=object.replaceAll("'", "''");

				//insert subject
				char lastChar = (subject.charAt(subject.length()-1)+"").toLowerCase().charAt(0);
				if(lastChar<'a'||lastChar>'z'){
					lastChar = '0';
				}
				tableName = DatabaseParameter.getDatabaseName()+"_subject_"+lastChar;
				sql = "INSERT INTO "+tableName
						+ "(subject, predicate, object) " + "VALUES"
						+ "('"+subject+"','"+predicate+"','"+object+"')";
				stmt.executeUpdate(sql);

				//insert object
				if(isNotLiteral){
					lastChar = (subject.charAt(subject.length()-1)+"").toLowerCase().charAt(0);
					if(lastChar<='a'||lastChar>='z'){
						lastChar = '0';
					}
					tableName = DatabaseParameter.getDatabaseName()+"_object_"+lastChar;
					sql = "INSERT INTO "+tableName
							+ "(subject, predicate, object) " + "VALUES"
							+ "('"+subject+"','"+predicate+"','"+object+"')";
					stmt.executeUpdate(sql);
				}

				//insert into the final big table 
				tableName = DatabaseParameter.getDatabaseName()+"_total";
				sql = "INSERT INTO "+tableName
						+ "(subject, predicate, object) " + "VALUES"
						+ "('"+subject+"','"+predicate+"','"+object+"')";
				stmt.executeUpdate(sql);
				tripleCount++;
			}
			catch(Exception e){
				LOGGER.debug("Unable to insert triple for the line '{}'",line,e);
			}

		}
		LOGGER.debug("Inserted a total of '{}' triples.",(tripleCount-1));
		stmt.close();
		conn.close();
		reader.close();
	}

	public void emptyTable(String tableName) throws Exception{
		Class.forName("org.postgresql.Driver");

		Connection conn = 
				DriverManager.getConnection((DatabaseParameter.getDatabaseUrl()
						+"/"+DatabaseParameter.getDatabaseName()), 
						DatabaseParameter.getUsername(), DatabaseParameter.getPassword());
		Statement stmt = conn.createStatement();

		//create table for each letter, for subject and object and a total table
		String sql = "DELETE FROM "+tableName;
		stmt.executeUpdate(sql);
		stmt.close();
		conn.close();
	}

	public void emptyAllTables() throws Exception{

		char character = 'a';
		for(int i=0;i<27;i++){
			if(i==26)
				character='0';
			this.emptyTable(DatabaseParameter.getDatabaseName()+"_subject_"+character);
			this.emptyTable(DatabaseParameter.getDatabaseName()+"_object_"+character);
			character++;
		}
		//empty final table
		this.emptyTable(DatabaseParameter.getDatabaseName()+"_total");
	}

	public static void main(String[] args) throws Exception{
		ConfigurationFacility.getConfiguration();
		CreateRelationalDatabase db = new CreateRelationalDatabase();
		//db.createDatabase();
		//db.createTables();
		File fr = new File("/Users/ortona/Documents/ASU_Collaboration/Developing/Data/mappingbased-properties_en.ttl");
		db.insertTriples(fr);
		//db.emptyAllTables();
	}

}
