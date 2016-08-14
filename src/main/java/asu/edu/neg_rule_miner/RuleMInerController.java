package asu.edu.neg_rule_miner;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import asu.edu.neg_rule_miner.configuration.*;
import asu.edu.neg_rule_miner.rule_generator.OneExampleRuleDiscovery;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import asu.edu.neg_rule_miner.App;
import asu.edu.neg_rule_miner.rule_miner;;

/*
 *  Here Webserver will look at /FoodItem Path for any requests.
 *  Author: Varun Gaur
 */
@Path("RuleMiner")
public class RuleMInerController {
	
	private final static Logger LOGGER = 
			LoggerFactory.getLogger(OneExampleRuleDiscovery.class.getName());
	
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllResponse()  {
    	App app = new App();
    	Configuration confDefault = ConfigurationFacility.getConfiguration();
    	int numThreads=1, maxRules=3;
    	double alpha=0.5, beta=0.5, gamma=0.5;
        try
        {
        	
        	if(confDefault.containsKey(Constant.CONF_NUM_THREADS)){
    			try{
    				numThreads = confDefault.getInt(Constant.CONF_NUM_THREADS);
    				System.out.println("numThreads ---"+numThreads);
    			}
    			catch(Exception e){
    				LOGGER.error("Error while trying to read the numbuer of "
    						+ "threads configuration parameter. Set to 1.");
    			}
    		}
        	if(confDefault.containsKey(Constant.CONF_SCORE_ALPHA)){
    			try{
    				alpha = confDefault.getDouble(Constant.CONF_SCORE_ALPHA);
    				System.out.println("alpha ---"+alpha);
    			}
    			catch(Exception e){
    				LOGGER.error("Error while trying to read the alpha "
    						+ " configuration parameter. Set to 1.");
    			}
    		}
        	if(confDefault.containsKey(Constant.CONF_SCORE_BETA)){
    			try{
    				beta = confDefault.getDouble(Constant.CONF_SCORE_BETA);
    				System.out.println("numThreads ---"+beta);
    			}
    			catch(Exception e){
    				LOGGER.error("Error while trying to read the numbuer of "
    						+ "threads configuration parameter. Set to 1.");
    			}
    		}
        	if(confDefault.containsKey(Constant.CONF_NUM_THREADS)){
    			try{
    				gamma = confDefault.getDouble(Constant.CONF_SCORE_GAMMA);
    				System.out.println("numThreads ---"+gamma);
    			}
    			catch(Exception e){
    				LOGGER.error("Error while trying to read the numbuer of "
    						+ "threads configuration parameter. Set to 1.");
    			}
    		}
        	if(confDefault.containsKey(Constant.CONF_MAX_RULE_LEN)){
    			try{
    				maxRules = confDefault.getInt(Constant.CONF_NUM_THREADS);
    				System.out.println("numThreads ---"+maxRules);
    			}
    			catch(Exception e){
    				LOGGER.error("Error while trying to read the numbuer of "
    						+ "threads configuration parameter. Set to 1.");
    			}
    		}
        	
        	
        	//rule_miner rm = new rule_miner();
        	String result = "alpha:" + alpha  + "|beta:"+beta+ "|gamma:"+ gamma + "|noOfThreads:" +numThreads+
        			"|maxNoRule:"+maxRules;
        	
        	JSONObject obj = new JSONObject();
        	obj.put("alphaR",new Double(alpha));
        	obj.put("betaR",new Double(beta));
        	obj.put("gammaR",new Double(gamma));
        	obj.put("noOfThreads",new Integer(numThreads));
        	obj.put("maxNoRule",new Integer(maxRules));
        	obj.put("kbase","yago");
        	obj.put("subType","Organisation");
        	obj.put("objType","Person");
        	System.out.println("result --"+obj);
        	
        	StringWriter out = new StringWriter();
            obj.writeJSONString(out);
            String jsonText = out.toString();
        	
            //createJsonReturnStrGet();
        	
        	return jsonText;
        	
        	
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        	return "\n\n<Error>\n\t<Error Message>Error While Getting !!!!</Error Message>\n<Error> ";
        }
    }

   
	public static String createJsonReturnStrGet()
	{
        //

		HashMap<String, ArrayList<String>> temp = new HashMap<String, ArrayList<String>>();
		List<String> covExm1 = new ArrayList<String>();
		List<String> covExm2 = new ArrayList<String>();
		covExm1.add("(http://dbpedia.org/resource/Non_Phixion,http://dbpedia.org/resource/Necro_(rapper))");
		covExm1.add("(http://dbpedia.org/resource/Az_Yet,http://dbpedia.org/resource/Babyface_(musician))");
		covExm1.add("(http://dbpedia.org/resource/The_Housemartins,http://dbpedia.org/resource/Norman_Cook)");
		covExm2.add("(http://dbpedia.org/resource/Luny_Tunes,http://dbpedia.org/resource/DJ_Nelson)");
		covExm2.add("(http://dbpedia.org/resource/Eli_Young_Band,http://dbpedia.org/resource/Frank_Liddell)");
		
temp.put("http://dbpedia.org/ontology/associatedBand(object,subject) & http://dbpedia.org/ontology/associatedMusicalArtist(object,v0) & http://dbpedia.org/ontology/associatedMusicalArtist(v0,v0)",(ArrayList<String>) covExm1);
temp.put("http://dbpedia.org/ontology/producer(v0,object) & http://dbpedia.org/ontology/album(v1,v0) & http://dbpedia.org/ontology/producer(v1,subject)",(ArrayList<String>) covExm2);

		

        // Inserting Rules !!!
		JSONArray rulesHead = new JSONArray();
		JSONArray ruleIds = new JSONArray();
		
		
		
        for(int i=0;i<temp.size();i++)
        {
        	JSONObject innerRuleDet = new JSONObject();
        	innerRuleDet.put("RuleID"+i, temp.keySet().toArray()[i]);
        	JSONArray covExamples = new JSONArray();
        	for(int j=0;j<temp.get(temp.keySet().toArray()[i]).size();j++)
        	{
        		covExamples.add(temp.get(temp.keySet().toArray()[i]).get(j));
        	}
        	innerRuleDet.put("CovExamples"+i, covExamples);
        	rulesHead.add(innerRuleDet);
        	
////        	JSONArray covExamples = new JSONArray();
////        	for(int j=0;j<outputRules.size();j++)
////        	{
////        		covExamples.add("abc"+(i+j));
////        	}
////        	inner1.put("CovExamples", covExamples);
////        	System.out.println("Rule1:--"+outputRules.get(i).toString());
////        	inner1.put("RuleId",outputRules.get(i).toString());
//        	list2.add(inner1.get("CovExamples"));
        	System.out.println(rulesHead);
        	
        }
        //list2.add(inner1);
        JSONObject finalObj = new JSONObject();
        finalObj.put("rows", rulesHead);
        System.out.println(finalObj);
        
        return finalObj.toString();
        
        //	
	}
    
	
	@POST
	@Path("UpdateResult")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public String updateRuleResult(String jsonString) {
		try {
			System.out.println("**********UpdateRuleResultString  = " + jsonString);
			//rule_miner rm = new ObjectMapper().readValue(jsonString, rule_miner.class);
			//System.out.println("Last Name  = " + rm.getMaxNoRule());
			String result = "Result read !!!!" ;
			System.out.println(result);

			return (result);
		} catch (Exception e) {
			e.printStackTrace();
			return "\n\n<Error>\n\t<Error Message>Please check the passed Input XML !!!!</Error Message>\n<Error> ";
		}
	}
	
   
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String parseRuleMinerRequest(String jsonString) 
    {
    	try
    	{
    		System.out.println("First Name = "+jsonString);
	    	rule_miner rm = new ObjectMapper().readValue(jsonString, rule_miner.class);
	    	System.out.println("First Name = "+jsonString);
	        System.out.println("Last Name  = "+rm.getMaxNoRule());
	    	App app = new App();
	    	
    		//String result = app.parseRuleMineReq(rm);
	        
//	        String result = "{\"Rules\":[{\"title\":\"Beautiful title 1\"},{\"title\":\"Beautiful title 2\"},{\"title\":\"Beautiful title 3\"}],\"Generation Samples\":[{\"title\":\"Beautiful title 1\"}]}";
	       // System.out.println(result);
	        
	        String result = createJsonReturnStrGet();
	        System.out.println(result);
	        
	        
        	return (result);
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    		return "\n\n<Error>\n\t<Error Message>Please check the passed Input XML !!!!</Error Message>\n<Error> ";
    	}
	}
}
