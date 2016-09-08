package asu.edu.neg_rule_miner.model.statistic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import asu.edu.neg_rule_miner.model.horn_rule.HornRule;
import asu.edu.neg_rule_miner.model.horn_rule.RuleAtom;

public class StatisticsContainer {

	private static long startTime;
	private static long endTime;

	private static int numberValidationQuery;
	private static long totalTimeValidationQuery;

	private static int nodeNumber;
	private static int edgesNumber;

	private static int numberExpansionQuery;
	private static long totalTimeExpansionQuery;

	private static File outputFile;

	private static String id;

	private static double negativeSetTime;

	private static double positiveSetTime;

	private static List<HornRule> outputRules;	
	
	private static Set<Pair<String, String>> generationSample;
	
	private static File annotatedRuleFile;

	public static void initialiseContainer(String currentId){
		
		numberValidationQuery = 0;
		totalTimeValidationQuery = 0;
		totalTimeExpansionQuery = 0;
		nodeNumber = 0;
		edgesNumber = 0;
		id=currentId;
		startTime = 0;
		endTime = 0;
		negativeSetTime = 0;
		positiveSetTime = 0;
		outputRules = null;

	}
	
	public static void setFileName(File file){
		outputFile = file;
	}
	
	public static void setAnnotatedRuleFile(File file){
		annotatedRuleFile = file;
	}

	public static void setNegativeSetTime(double time){
		negativeSetTime = time;
	}

	public static void setPositiveSetTime(double time){
		positiveSetTime = time;
	}

	public static void increaseValidationQuery(){
		numberValidationQuery++;
	}

	public static void increaseExpansionQuery(){
		numberExpansionQuery++;
	}

	public static void setNodesNumber(int nodesNumber){
		nodeNumber = nodesNumber;
	}

	public static void setEdgesNumber(int edgesNumberInput){
		edgesNumber = edgesNumberInput;
	}

	public static void increaseTimeValidationQuery(long newTime){
		totalTimeValidationQuery+=newTime;
	}

	public static void increaseTimeExpansionQuery(long newTime){
		totalTimeExpansionQuery+=newTime;
	}

	public static void setStartTime(long currentStartTime){
		startTime = currentStartTime;
	}

	public static void setEndTime(long currentEndTime){
		endTime = currentEndTime;
	}

	public static void setOutputRules(List<HornRule> currentOutputRules){
		outputRules = currentOutputRules;
	}
	
	public static void setGenerationSample(Set<Pair<String, String>> generationExamples){
		generationSample = generationExamples;
	}


	public static void printStatistics() throws IOException{
		if(outputFile==null)
			return;

		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true));
		
		Date startDate = new Date(startTime);
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
		String startDateFormatted = formatter.format(startDate);
		Date endDate = new Date(endTime);
		String endDateFormatted = formatter.format(endDate);

//		writer.write("-------------------------"+id+"_"+ConfigurationFacility.getNegativeExampleLimit()+"_"+ startDateFormatted+"-------------------------\n");
		writer.write("-------------------------"+id+"_"+startDateFormatted+"-------------------------\n");		
		writer.write("Number of Nodes: "+nodeNumber+"\n");
		writer.write("Number of Edges: "+edgesNumber+"\n");
		writer.write("Total time for validation queries: "+(totalTimeValidationQuery/1000.)+" seconds.\n");
		writer.write("Average time for validation queries: "+(((totalTimeValidationQuery+0.)/numberValidationQuery)/1000.)+" seconds.\n");
		writer.write("Number of validation queries: "+numberValidationQuery+"\n");
		writer.write("Total time for expansion queries: "+(totalTimeExpansionQuery/1000.)+" seconds.\n");
		writer.write("Average time for expansion queries: "+(((totalTimeExpansionQuery+0.)/numberExpansionQuery)/1000.)+" seconds.\n");
		writer.write("Number of expansion queries: "+numberExpansionQuery+"\n");
		writer.write("Positive examples generation time: "+positiveSetTime+" seconds.\n");
		writer.write("Negative examples generation time: "+negativeSetTime+" seconds.\n");
		writer.write("Total running time: "+((endTime-startTime)/1000.)+" seconds.\n");
//		writer.write("Generation sample examples: "+generationSample+"\n");
//		writer.write("Output rules: "+outputRules+"\n");
//		writer.write("-------------------------"+id+"_"+ConfigurationFacility.getNegativeExampleLimit()+"_"+ endDateFormatted+"-------------------------");
		writer.write("-------------------------"+id+"_"+endDateFormatted+"-------------------------");
		writer.write("\n");

		writer.flush();
		writer.close();
	}
	
	public static HornRule createHornRule(Set<RuleAtom> rule1){
		HornRule rule = new HornRule();
		for(RuleAtom atom : rule1){
			rule.addRuleAtom(atom);
		}
		return rule;
	}
	
	public static ArrayList<ArrayList<RuleAtom>> permute(RuleAtom[] num) {
		ArrayList<ArrayList<RuleAtom>> result = new ArrayList<ArrayList<RuleAtom>>();
	 
		//start from an empty list
		result.add(new ArrayList<RuleAtom>());
	 
		for (int i = 0; i < num.length; i++) {
			//list of list in current iteration of the array num
			ArrayList<ArrayList<RuleAtom>> current = new ArrayList<ArrayList<RuleAtom>>();
	 
			for (ArrayList<RuleAtom> l : result) {
				// # of locations to insert is largest index + 1
				for (int j = 0; j < l.size()+1; j++) {
					// + add num[i] to different locations
					l.add(j, num[i]);
	 
					ArrayList<RuleAtom> temp = new ArrayList<RuleAtom>(l);
					current.add(temp);
	 
					//System.out.println(temp);
	 
					// - remove num[i] add
					l.remove(j);
				}
			}
	 
			result = new ArrayList<ArrayList<RuleAtom>>(current);
		}
	 
		return result;
	}
	public static RuleAtom[] getArray(Set<RuleAtom> atoms){
		RuleAtom[] array = new RuleAtom[atoms.size()];
		int i=0;
		for(RuleAtom atom:atoms){
			array[i] = atom;
			i++;
		}
		return array;
	}
	public static boolean permutesInto(Set<RuleAtom>rule1, Set<RuleAtom>rule2){
		boolean result = rule1.equals(rule2);
//		Set<RuleAtom> test = getRuleAtomsFromString("http://dbpedia.org/ontology/doctoralAdvisor(subject,object)");
//		RuleAtom testAtom = null;
//		for(RuleAtom atom:test){http://dbpedia.org/ontology/dissolutionYear(v0,194)
//			testAtom = atom;
//		}
//		if(rule1.contains(testAtom)){
//			testAtom = testAtom;
//		}
		RuleAtom[] rule2Arr = getArray(rule2);
		if(!result){
			if(rule2Arr.length!=rule1.size())
				return result;
			RuleAtom[] ruleArr = getArray(rule1);
			ArrayList<ArrayList<RuleAtom>> permutedList = permute(ruleArr);
			for(int i=0; i<permutedList.size(); i++){
				ArrayList<RuleAtom> rule1List = permutedList.get(i);
				assert rule1List.size() == rule2Arr.length;
				result = true;
				for(int j=0; j<rule1List.size(); j++){
					if(!rule1List.get(j).equals(rule2Arr[j])){
						result = false;
						break;
					}
				}
				if(result)
					return result;
			}
			
		}
		return result;
	}
	
	public static boolean isEqualTo(Set<RuleAtom>rule1, Set<RuleAtom>rule2){
		HornRule rule = createHornRule(rule1);
		boolean result = permutesInto(rule1, rule2);
		try{
			if(!result){
				Set<RuleAtom> equivalentRule = rule.getEquivalentRule();
				result = permutesInto(equivalentRule, rule2);
			}
		}
		catch(Exception e){
			System.err.println("ERROR in generating equivalent rule for "+rule);
//			e.printStackTrace();
		}			
		return result;
	}
	
	public static void annotateRules() throws IOException{
		HashMap<Set<RuleAtom>,String> rulesAnnotated = getHashMap();	
		BufferedWriter bw = new BufferedWriter(new FileWriter(annotatedRuleFile,true));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true));
		writer.write("Output rules:\n");
//		int numRules = 0, numValidRules = 0;
		for(HornRule rule:outputRules){
			boolean foundRule = false;
			Set<RuleAtom> ruleAtoms = rule.getRules();
			if(rulesAnnotated.containsKey(ruleAtoms)){
				writer.write(rule+"--"+rulesAnnotated.get(ruleAtoms)+"\n");
				foundRule = true;				
			}
			else{
				for(Set<RuleAtom> key : rulesAnnotated.keySet()){
					if(isEqualTo(key,ruleAtoms)){
						writer.write(rule+"--"+rulesAnnotated.get(key)+"\n");
						foundRule = true;
					}
				}
			}
			if(!foundRule){
				writer.write(rule+"\n");
				bw.write(rule+"--??FILLUP\n");
			}
//			numRules++;
//			if(foundRule)
//				numValidRules++;
//			bw.write("numRules: "+numRules+", numValidRules: "+numValidRules+"\n");
		}
//		writer.write("\n");
		writer.flush();
		bw.flush();
		bw.close();
		writer.close();
	}
	
	public static String trimCommaSpace(String str){
		//String str = "kushal,mayurv,narendra,dhrumil,mark, ,,,, ";
        String splitted[] = str.split(",");
        StringBuffer sb = new StringBuffer();
        String retrieveData = "";
        for(int i =0; i<splitted.length; i++){
            retrieveData = splitted[i];
            if((retrieveData.trim()).length()>0){

                if(i!=0){
                    sb.append(",");
                }
                sb.append(retrieveData);

            }
        }

        str = sb.toString();
        return str;
	}
	
	public static Set<RuleAtom> getRuleAtomsFromString(String token){
	//	String keyString = trimCommaSpace(token);
		String keyString = token;
	//	String keyString = token.replaceAll(", $", "");
		System.out.println("keyString: "+keyString);
		return HornRule.readHornRule(keyString);
	}
	
	public static String getAnnotation(String token, HashMap<Set<RuleAtom>,String> rulesAnnotated) throws IOException{	
		Set<RuleAtom> rule = getRuleAtomsFromString(token);
//		rule = getRuleAtomsFromString("http://dbpedia.org/ontology/doctoralStudent(v0,object) & http://dbpedia.org/ontology/doctoralAdvisor(subject,object) & http://dbpedia.org/ontology/doctoralAdvisor(subject,v0)");
        if(rulesAnnotated.containsKey(rule)){
        	return rulesAnnotated.get(rule);
        }
        else{
        	for(Set<RuleAtom> key : rulesAnnotated.keySet()){
				if(isEqualTo(key,rule))
					return rulesAnnotated.get(key);
			}
        }
		System.err.println("ERROR RAISED WITH RULE: "+rule);
//		System.exit(0);
		return null;
	}
	
	public static HashMap<Set<RuleAtom>,String> getHashMap() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(annotatedRuleFile));
		String thisLine;
		HashMap<Set<RuleAtom>,String> rulesAnnotated = new HashMap<Set<RuleAtom>, String>();
		while ((thisLine = br.readLine()) != null) {
           String[] tokens = thisLine.split("--");
           Set<RuleAtom> key = getRuleAtomsFromString(tokens[0]);
           rulesAnnotated.put(key, tokens[1]);
         } 
		br.close();
		return rulesAnnotated;
	}
	
	public static void writeExpHeader(double alpha, double beta, double gamma, String samplingMode, int sign) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true));
		if(samplingMode.equals("topK")){
			if(sign==0){
				writer.write("-----------------------------------------------------------------\n"
						+ "ALPHA="+alpha+", BETA="+beta+", GAMMA = "+gamma+", NEGATIVE RULES, TOP-K SMART SAMPLING\n"
						+ "---------------------------------------------------------------------------------\n");
			}
			else if(sign==1){
				writer.write("-----------------------------------------------------------------\n"
						+ "ALPHA="+alpha+", BETA="+beta+", GAMMA = "+gamma+", POSITIVE RULES, TOP-K SMART SAMPLING\n"
						+ "---------------------------------------------------------------------------------\n");
			}
		}
		else if(samplingMode.equals("uniform")){
			if(sign==0){
				writer.write("-----------------------------------------------------------------\n"
						+ "ALPHA="+alpha+", BETA="+beta+", GAMMA = "+gamma+", NEGATIVE RULES, UNIFORM SMART SAMPLING\n"
						+ "---------------------------------------------------------------------------------\n");
			}
			else if(sign==1){
				writer.write("-----------------------------------------------------------------\n"
						+ "ALPHA="+alpha+", BETA="+beta+", GAMMA = "+gamma+", POSITIVE RULES, UNIFORM SMART SAMPLING\n"
						+ "---------------------------------------------------------------------------------\n");
			}
		}
		else if(samplingMode.equals("stratified")){
			if(sign==0){
				writer.write("-----------------------------------------------------------------\n"
						+ "ALPHA="+alpha+", BETA="+beta+", GAMMA = "+gamma+", NEGATIVE RULES, STRATIFIED SMART SAMPLING\n"
						+ "---------------------------------------------------------------------------------\n");
			}
			else if(sign==1){
				writer.write("-----------------------------------------------------------------\n"
						+ "ALPHA="+alpha+", BETA="+beta+", GAMMA = "+gamma+", POSITIVE RULES, STRATIFIED SMART SAMPLING\n"
						+ "---------------------------------------------------------------------------------\n");
			}
		}
		else if(samplingMode.equals("random")){
			if(sign==0){
				writer.write("-----------------------------------------------------------------\n"
						+ "BASELINE RERUN 5 TIMES NEGATIVE RULES, RANDOM SAMPLING\n"
						+ "---------------------------------------------------------------------------------\n");
			}
			else if(sign==1){
				writer.write("-----------------------------------------------------------------\n"
						+ "BASELINE RERUN 5 TIMES POSITIVE RULES, RANDOM SAMPLING\n"
						+ "---------------------------------------------------------------------------------\n");
			}
		}
		writer.flush();
	}
	
	public static HashMap<String,Integer> getBestRules() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(outputFile+".out"));
		String thisLine;
		HashMap<String,Integer> bestRules = new HashMap<String, Integer>();
		String lastRel = null;
		while ((thisLine = br.readLine()) != null) {
			if(thisLine.contains("---[http")){
				Pattern p = Pattern.compile("\\[(.*?)\\]");
				Matcher m = p.matcher(thisLine);
				String[] relTokens = null;
				while(m.find()){
					relTokens = m.group(1).split("/");
				}				
				lastRel = relTokens[relTokens.length-1];
			}
			else if(thisLine.contains("numRules")){
				int numValidRules = Integer.parseInt(thisLine.split(",")[1].split(":")[1]);
				if(bestRules.containsKey(lastRel)){
					int rules = bestRules.get(lastRel);
					if(numValidRules > rules)
						bestRules.put(lastRel, numValidRules);
				}
				else
					bestRules.put(lastRel, numValidRules);					
			}
		}
		return bestRules;
	}
	
	public static void writeStats(boolean baselineRun, boolean sampleRun, HashMap<Pair<String, Double>,Double[]> excelStats, Set<String> relNames, Set<Double> sampleSizes, BufferedWriter bw) throws IOException{
//		Set<String> relNames = bestRules.keySet();
//		double[] sampleSizes = {5, 10, 20, 50, 100, 500, 1000, 1500};
		int numRuns = 1;
		if(baselineRun && !sampleRun)
			numRuns = 5;
		HashMap<Double,Double> avgBaselineTime = null;
		HashMap<Double,Double> minBaselineTime = null;
		HashMap<Double,Double> maxBaselineTime = null;
		HashMap<Double,Double> avgBaselineQuality = null;
		HashMap<Double,Double> minBaselineQuality = null;
		HashMap<Double,Double> maxBaselineQuality = null;
		if(baselineRun && !sampleRun){
			avgBaselineTime = new HashMap<Double,Double>();
			minBaselineTime = new HashMap<Double,Double>();
			maxBaselineTime = new HashMap<Double,Double>();
			avgBaselineQuality = new HashMap<Double,Double>();
			minBaselineQuality = new HashMap<Double,Double>();
			maxBaselineQuality = new HashMap<Double,Double>();
		}
		for(int run = 0; run<numRuns; run++){
			//header
			bw.write("SampleSize\t");
			for(String relName: relNames){
				bw.write(relName+"Time\t");
				bw.write(relName+"Rules\t");
				bw.write(relName+"GoodRules\t");
				bw.write(relName+"Precision\t");
				bw.write(relName+"Recall\t");
				bw.write(relName+"Quality\t");
			}
			bw.write("AvgTime\tAvgQuality\n");
			for(double sampleSize : sampleSizes){
				bw.write(sampleSize+"\t");
				double avgTime =0.0, avgQuality= 0.0;
				for(String relName : relNames){
					Pair<String, Double> keyPair = Pair.of(relName, sampleSize);
					int offset = run * 6;
					if(sampleSize == 1500)
						offset = 0;
					if(!excelStats.containsKey(keyPair))
						continue;
					Double[] valSet = excelStats.get(keyPair);
					if(offset>=valSet.length)
						continue;
					for(int i=0; i<6; i++){
						bw.write(valSet[offset+i]+"\t");
					}
					avgTime += valSet[offset+0];
					avgQuality += valSet[offset+5];
				}
				avgTime /= relNames.size();
				avgQuality /= relNames.size();
				bw.write(avgTime+"\t"+avgQuality+"\n");
				if(numRuns==5){
					if(avgBaselineTime.containsKey(sampleSize))
						avgBaselineTime.put(sampleSize, avgBaselineTime.get(sampleSize)+avgTime);
					else
						avgBaselineTime.put(sampleSize, avgTime);
					if(!minBaselineTime.containsKey(sampleSize) ||avgTime < minBaselineTime.get(sampleSize))
						minBaselineTime.put(sampleSize, avgTime);
					if(!maxBaselineTime.containsKey(sampleSize) ||avgTime > maxBaselineTime.get(sampleSize))
						maxBaselineTime.put(sampleSize, avgTime);
					if(avgBaselineQuality.containsKey(sampleSize))
						avgBaselineQuality.put(sampleSize, avgBaselineQuality.get(sampleSize)+avgQuality);
					else
						avgBaselineQuality.put(sampleSize, avgQuality);
					if(!minBaselineQuality.containsKey(sampleSize) ||avgQuality < minBaselineQuality.get(sampleSize))
						minBaselineQuality.put(sampleSize, avgQuality);
					if(!maxBaselineQuality.containsKey(sampleSize) ||avgQuality > maxBaselineQuality.get(sampleSize))
						maxBaselineQuality.put(sampleSize, avgQuality);
				}
			}
		}
		if(numRuns==5){
			bw.write("sampleSize\tavgBaselineTime\tminBaselineTime\tmaxBaselineTime\tavgBaselineQuality\tminBaselineQuality\tmaxBaselineQuality\n");
			for(double sampleSize : sampleSizes){
				avgBaselineTime.put(sampleSize,avgBaselineTime.get(sampleSize)/numRuns);
				avgBaselineQuality.put(sampleSize,avgBaselineQuality.get(sampleSize)/numRuns);
				bw.write((int)sampleSize+"\t"+avgBaselineTime.get(sampleSize)+"\t"+
						minBaselineTime.get(sampleSize)+"\t"+maxBaselineTime.get(sampleSize)+"\t"+
						avgBaselineQuality.get(sampleSize)+"\t"+minBaselineQuality.get(sampleSize)+"\t"+
						maxBaselineQuality.get(sampleSize)+"\n");				
			}
		}
		bw.flush();
	}
	
	public static void generateExcelFeed() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(outputFile+".out"));
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile+".excel"));
		HashMap<String,Integer> bestRules = getBestRules();
		String thisLine;
		boolean baselineRun = false, sampleRun = false, inBlock = false;
		HashMap<Pair<String, Double>,Double[]> excelStats = null;
		String relName = null;
		double sampleSize = 0.0;
		double runTime = 0.0;
		Double[] stats;
		Set<Double> sampleSizes = new HashSet<Double>();
		Set<String> relNames = new HashSet<String>();
		Pair<String, Double> keyPair = null;
		int offset = 0;
		while ((thisLine = br.readLine()) != null) {
			if(thisLine.contains("---") && !thisLine.contains("http")){
				bw.write(thisLine+"\n");
				if((baselineRun || sampleRun) && inBlock==false){
					writeStats(baselineRun, sampleRun, excelStats, relNames, sampleSizes, bw);
					baselineRun = false;
					sampleRun = false;
					sampleSizes = new HashSet<Double>();
					relNames = new HashSet<String>();
				}
			}
			else if(thisLine.contains("BASELINE RERUN") || thisLine.contains("UNLIMITED")){
				bw.write(thisLine+"\n");
				baselineRun = true;
				inBlock = true;
				excelStats = new HashMap<Pair<String, Double>,Double[]>();
				if((outputFile+".out").contains("positive") || (outputFile+".out").contains("Positive"))
					offset = 3;
				else
					offset = 4;
			}
			else if(thisLine.contains("ALPHA") && thisLine.contains("BETA") && thisLine.contains("GAMMA")){
				bw.write(thisLine+"\n");
				sampleRun = true;
				inBlock = true;
				excelStats = new HashMap<Pair<String, Double>,Double[]>();
				offset = 5;
			}
			else if(thisLine.contains("---[http")){
				Pattern p = Pattern.compile("\\[(.*?)\\]");
				Matcher m = p.matcher(thisLine);
				String[] relTokens = null;
				while(m.find()){
					relTokens = m.group(1).split("/");
				}				
				relName = relTokens[relTokens.length-1];
				String[] tokens = thisLine.split("_");
				sampleSize = Double.parseDouble(tokens[offset]); // 3 is the offset in ---[http://dbpedia.org/ontology/foundedBy, http://dbpedia.org/ontology/founder]_-1_-1_500_-1_-1 and 5 is the offset in ---[http://dbpedia.org/ontology/spouse]_-1_-1_-1_-1_50
				if(sampleSize==-1)
					sampleSize = 1500;
			}
			else if(thisLine.contains("Total running time")){
				runTime = Double.parseDouble(thisLine.split(": ")[1].split(" ")[0]);
				runTime = runTime;
			}
			else if(thisLine.contains("numRules")){
				stats = new Double[6]; //time, numRules, goodRules, precision, recall, F-Measure
				Double perfectRecall = (double)(bestRules.get(relName));
				if(perfectRecall ==0)
					perfectRecall = 0.00666667;
				stats[0] = runTime;
				stats[1] = Double.parseDouble(thisLine.split(",")[0].split(":")[1]);
				if(stats[1]==0)
					stats[1] = 0.00666667; // 1/100 to avoid infinity for F-measure
				stats[2] = Double.parseDouble(thisLine.split(",")[1].split(":")[1]);
				if(stats[2] == 0)
					stats[2] = 0.00666667;
				stats[3] = stats[2] / stats[1]; //precision
				stats[4] = stats[2] / perfectRecall; //recall
				stats[5] = 2 * stats[3] * stats[4] / (stats[3]+stats[4]); //F-Measure
				if(sampleSize==-1)
					sampleSize = 1500;
				keyPair = Pair.of(relName, sampleSize);
				sampleSizes.add(sampleSize);
				relNames.add(relName);
				if(excelStats.containsKey(keyPair)){
					assert(baselineRun==true && sampleRun==false);
					Double[] existingVal = excelStats.get(keyPair);
					Double[] extendedVal = new Double[existingVal.length+stats.length];
					for(int i=0; i<extendedVal.length; i++){
						if(i<existingVal.length)
							extendedVal[i] = existingVal[i];
						else
							extendedVal[i] = stats[i-existingVal.length];
					}
					excelStats.put(keyPair, extendedVal);
				}
				else{
					excelStats.put(keyPair, stats);
				}
				inBlock = false;
			}
			
		}
	}
	
	public static void postAnnotationCleanUp() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(outputFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile+".out"));
//		Set<RuleAtom> testRule = getRuleAtomsFromString("http://dbpedia.org/ontology/associatedBand(v0,v0) & "
//				+ "http://dbpedia.org/ontology/birthPlace(object,v1) & http://dbpedia.org/ontology/artist(v0,subject) & http://dbpedia.org/ontology/recordedIn(v0,v1)");
//		HornRule testHornRule = createHornRule(testRule);
//		Set<RuleAtom> eqRule = testHornRule.getEquivalentRule();
//		HornRule test1 = getHornRuleFromString("http://dbpedia.org/ontology/doctoralAdvisor(subject,v0) & "
//				+ "http://dbpedia.org/ontology/doctoralAdvisor(subject,object) "
//				+ "& http://dbpedia.org/ontology/doctoralStudent(v0,object)");
//		http://dbpedia.org/ontology/doctoralAdvisor(subject,v0) & http://dbpedia.org/ontology/doctoralAdvisor(subject,object) & http://dbpedia.org/ontology/doctoralStudent(v0,object);
//		HornRule test2 = getHornRuleFromString("http://dbpedia.org/ontology/doctoralStudent(v0,object) &"
//				+ " http://dbpedia.org/ontology/doctoralAdvisor(subject,v0) & http://dbpedia.org/ontology/doctoralAdvisor(subject,object)");
//		boolean testRes = test1.equals(test2);
		
		HashMap<Set<RuleAtom>,String> rulesAnnotated = getHashMap();
		String thisLine;
		int numRules = 0, numValidRules = 0;
		boolean inBlock = false;
		while ((thisLine = br.readLine()) != null) {
			if(thisLine.contains("Output rules:")){
				if(!inBlock){
					numRules = 0;
					numValidRules = 0;
					inBlock = true;
				}
			}
			else if (thisLine.contains("http") && !thisLine.contains("---")){				
				if(thisLine.contains("false")){
					numRules++;
					writer.write(thisLine+"\n");
					continue;
				}
				else if(thisLine.contains("true")){
					numRules++;
					numValidRules++;
					writer.write(thisLine+"\n");
					continue;
				}				
				String annotation;
				if(!thisLine.contains("--")){
					annotation = getAnnotation(thisLine, rulesAnnotated);
					writer.write(thisLine+"--"+annotation+"\n");
				}
				else{
					String[] tokens = thisLine.split("--");
			        annotation = getAnnotation(tokens[0], rulesAnnotated);  
			        writer.write(tokens[0]+"--"+annotation+"\n");
				}
				if(annotation.equals("true")){
					numRules++;
			        numValidRules++;
			    }
			    else if(annotation.equals("false"))
			    	numRules++;
				
			}
			else{
				if(inBlock){
					writer.write("numRules:"+numRules+",numValidRules:"+numValidRules+"\n");
					inBlock = false;
				}
				writer.write(thisLine+"\n");
			}
		}
		writer.write("numRules:"+numRules+",numValidRules:"+numValidRules+"\n");
		writer.write("-----------------------------------------------------------------\n");
		writer.flush();
		writer.close();
		generateExcelFeed();
	}

}
