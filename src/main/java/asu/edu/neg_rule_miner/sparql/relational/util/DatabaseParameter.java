package asu.edu.neg_rule_miner.sparql.relational.util;

public class DatabaseParameter {
	
	private static String DB_URL="jdbc:postgresql://localhost";
	private static String USERNAME="postgres";
	private static String PASSWORD="postgres";
	private static String DB_NAME="dbpedia_2";
	
	public static String getDatabaseUrl(){
		return DB_URL+"/"+DB_NAME;
	}
	
	public static String getUsername(){
		return USERNAME;
	}
	
	public static String getPassword(){
		return PASSWORD;
	}
	
	public static String getDatabaseName(){
		return DB_NAME;
	}

}
