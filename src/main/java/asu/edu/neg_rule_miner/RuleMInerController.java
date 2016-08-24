package asu.edu.neg_rule_miner;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import asu.edu.neg_rule_miner.configuration.*;
import asu.edu.neg_rule_miner.rule_generator.DynamicPruningRuleDiscovery;
import asu.edu.neg_rule_miner.rule_generator.OneExampleRuleDiscovery;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
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
        	
//            createJsonReturnStrTest();
        	
        	return jsonText;
        	
        	
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        	return "\n\n<Error>\n\t<Error Message>Error While Getting !!!!</Error Message>\n<Error> ";
        }
    }

   
	public static String createJsonReturnStrTest()
	{
		
		Set<Pair<String, String>> genSamples = new HashSet<Pair<String, String>>();
		genSamples.add(Pair.of("(http://dbpedia.org/resource/II_Corps_(United_Kingdom)", "http://dbpedia.org/resource/Bernard_Montgomery_1st_Viscount_Montgomery_of_Alamein)"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/Kidz_in_the_Hall", "http://dbpedia.org/resource/Just_Blaze"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/Angeldust", "http://dbpedia.org/resource/Klayton"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/Angel_&_Khriz", "http://dbpedia.org/resource/Daddy_Yankee"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/The_Oh_Hellos", "http://dbpedia.org/resource/Sufjan_Stevens"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/Belvision_Studios", "http://dbpedia.org/resource/Raymond_Leblanc"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/Az_Yet", "http://dbpedia.org/resource/Babyface_musician"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/Nefew", "http://dbpedia.org/resource/Masta_Ace"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/Thomasschule_zu_Leipzig", "http://dbpedia.org/resource/Thomas_the_Apostle"));
		genSamples.add(Pair.of("http://dbpedia.org/resource/Cassiber", "http://dbpedia.org/resource/Chris_Cutler"));
		
		for(Pair<String, String> genSampl:genSamples)
		{
			String[] tempL,tempR;
			tempL=(genSampl.getLeft().split(","))[0].split("/");
			tempR=(genSampl.getRight().split(","))[0].split("/");
			//System.out.println("LEFT=\t"+genSampl.getLeft()+"\tRight=\t"+genSampl.getRight());
			System.out.println(tempL[(tempL.length)-1] + "\t" +  tempR[tempR.length-1]);
		}
		
		//("(http://dbpedia.org/resource/II_Corps_(United_Kingdom)","http://dbpedia.org/resource/Bernard_Montgomery_1st_Viscount_Montgomery_of_Alamein)");
		
		return "OK";
//		for(Pair<String, String> genSampl:generationSample)
//		{
//			genSamArray.add(genSampl.getLeft());
//		}
	}
    
	
	@POST
	@Path("UpdateResult")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public String updateRuleResult(String jsonString) {
		try {
			System.out.println("**********UpdateRuleResultString  = " + jsonString);
			ResultUpdate resUpdate = new ObjectMapper().readValue(jsonString, ResultUpdate.class);
			System.out.println("Results Name  = " + resUpdate.getUpdateValidVal());
			String result = "Result Updated for following rules:\n" + resUpdate.getRules();
			System.out.println(result);

			return (result);
		} catch (Exception e) {
			e.printStackTrace();
			return "\n\n<Error>\n\t<Error Message>Please che ck the passed Input XML !!!!</Error Message>\n<Error> ";
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
	    	
    		String result = app.parseRuleMineReq(rm);
	        
//	        String result = "{\"Rules\":[{\"title\":\"Beautiful title 1\"},{\"title\":\"Beautiful title 2\"},{\"title\":\"Beautiful title 3\"}],\"Generation Samples\":[{\"title\":\"Beautiful title 1\"}]}";
	       // System.out.println(result);
	        
	        //String result = createJsonReturnStrGet();
	        System.out.println(result);
	        
	        
        	return (result);
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    		return "\n\n<Error>\n\t<Error Message>Please check the passed Input XML !!!!</Error Message>\n<Error> ";
    	}
	}

	
	@POST
	@Path("ExecuteRule")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public String executeRuleForExample(String jsonString) {
		try {
			System.out.println("**********UpdateRuleResultString  = " + jsonString);
			singleRule execQuery = new ObjectMapper().readValue(jsonString, singleRule.class);
			System.out.println("Results Name  = " + execQuery.getRuleStr());
			
			DynamicPruningRuleDiscovery dynaRp = new DynamicPruningRuleDiscovery();
			String result =  dynaRp.executeQueryForEx(execQuery.getRuleStr());
			
			return (result);
		} catch (Exception e) {
			e.printStackTrace();
			return "\n\n<Error>\n\t<Error Message>Please che ck the passed Input XML !!!!</Error Message>\n<Error> ";
		}
	}
	


}
