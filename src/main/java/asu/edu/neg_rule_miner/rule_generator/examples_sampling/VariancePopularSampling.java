package asu.edu.neg_rule_miner.rule_generator.examples_sampling;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Sets;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomTree;
import weka.clusterers.SimpleKMeans;
import weka.core.AdditionalMeasureProducer;
import weka.core.Capabilities;
import weka.core.DistanceFunction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Randomizable;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;


public class VariancePopularSampling {
	
	double alpha, beta, gamma, subWeight, objWeight;
	int subjectLimit, objectLimit;
	String samplingMode; // to indicate whether it is topK sampling or uniform sampling
	
	public VariancePopularSampling(double alpha, double beta, double gamma, double subWeight, double objWeight, int subjectLimit, int objectLimit, String samplingMode){
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
		this.subWeight = subWeight;
		this.objWeight = objWeight;
		this.samplingMode = samplingMode;
		if(subjectLimit < 0)
			this.subjectLimit = 2; // only for the computation of functionality, doesn't effect the example generation for actual in-degree & out-degree
//			this.subjectLimit = (int)Double.POSITIVE_INFINITY;
		else
			this.subjectLimit = subjectLimit;
		if(objectLimit < 0)
			this.objectLimit = 2; // only for the computation of functionality, doesn't effect the example generation for actual in-degree & out-degree
//			this.objectLimit = (int)Double.POSITIVE_INFINITY;
		else
			this.objectLimit = objectLimit;
	}
	
	public ArrayList<Double> computeEntityStats(String entity, Set<Edge<String>> edges, int limit){
		ArrayList<Double> toReturn = new ArrayList<Double>();
		HashMap<String,ArrayList<Double>> diversePopularity = new HashMap<String,ArrayList<Double>>();
		ArrayList<String> inverseFunctionalSet = new ArrayList<String>(); 
		ArrayList<String> labelSet = new ArrayList<String>();
		for(Edge<String> oneEdge : edges){
			String label = oneEdge.getLabel();
			ArrayList<Double> values = new ArrayList<Double>();
			if(diversePopularity.containsKey(label) && diversePopularity.get(label).get(0)!=null)
				values.add(diversePopularity.get(label).get(0)+1.0);
			else {
				values.add(1.0);
				labelSet.add(label); //new label added
			}
			if(diversePopularity.containsKey(label) && diversePopularity.get(label).get(0)>limit){
				if(!inverseFunctionalSet.contains(label))
					inverseFunctionalSet.add(label);
			}
			
	/*		if(diversePopularity.containsKey(label) && diversePopularity.get(label).get(0)>limit){
				if(diversePopularity.get(label).get(1)!=null)
					values.add(diversePopularity.get(label).get(1)+1.0);
				else
					values.add(1.0);
			}
			else if(diversePopularity.containsKey(label) && diversePopularity.get(label).get(0)<=limit){
				if(diversePopularity.containsKey(label) && diversePopularity.get(label).get(1)!=null)
					values.add(diversePopularity.get(label).get(1));
				else
					values.add(0.0);
			}
			else if(!diversePopularity.containsKey(label))
				values.add(0.0);
	*/		diversePopularity.put(label, values);
		}
		toReturn.add(new Double(diversePopularity.keySet().size())); //added diversity as the number of relations an entity is connected to
		Collection<ArrayList<Double>> valueSet = diversePopularity.values();
		double aggPopularity = 0.0, aggInverseFunctionality = 0.0;
		for(ArrayList<Double> values : valueSet){
			aggPopularity += values.get(0);
		//	aggInverseFunctionality += values.get(1);
		}
		aggInverseFunctionality = (double)inverseFunctionalSet.size() / (double)labelSet.size();
		toReturn.add(aggPopularity);
		toReturn.add(aggInverseFunctionality);
		return toReturn;
	}
	
//	public double computePopularity(String entity, Set<Edge<String>> edges){
//		return 0.0;
//	}
//	
//	public double computeInverseFunctionality(String entity, Set<Edge<String>> edges){
//		return 0.0;
//	}
	
	private static HashMap sortByValues(HashMap map) { 
	       List list = new LinkedList(map.entrySet());
	       // Defined Custom Comparator here
	       Collections.sort(list, new Comparator() {
	            public int compare(Object o1, Object o2) {
	               return ((Comparable) ((Map.Entry) (o2)).getValue())
	                  .compareTo(((Map.Entry) (o1)).getValue());
	            }
	       });

	       // Here I am copying the sorted list in HashMap
	       // using LinkedHashMap to preserve the insertion order
	       HashMap sortedHashMap = new LinkedHashMap();
	       for (Iterator it = list.iterator(); it.hasNext();) {
	              Map.Entry entry = (Map.Entry) it.next();
	              sortedHashMap.put(entry.getKey(), entry.getValue());
	       } 
	       return sortedHashMap;
	  }
	
	public HashMap<Integer,Set<Pair<String,String>>> kMeans(HashMap<Pair<String,String>,Double> scorePerPair, int numClusters){
		String fileName = "kMeansInput";
		File fout = new File(fileName+".arff");
		if (fout.exists()){
			fout.delete();
		}
		int numDimensions = 1; // 1 for variant 1 where we cluster the scores
		String ARFF_HEADER = 
				"@relation kMeansInput\n\n";
		ARFF_HEADER +=
				"@attribute score numeric\n" ;
		ARFF_HEADER += "\n";
		ARFF_HEADER += "@data\n\n";
		try(FileWriter fw = new FileWriter(fileName+".arff", true);
			    BufferedWriter bw = new BufferedWriter(fw);
			    PrintWriter out = new PrintWriter(bw))
			{
			    out.println(ARFF_HEADER);
			    for(Double score:scorePerPair.values()){
			    	out.println(score);
			    }
			    out.flush();
				out.close();
			} catch (IOException e) {
			    //exception handling left as an exercise for the reader
			}
		try{
			SimpleKMeans kmeans = new SimpleKMeans();

	        //DistanceFunction df = new weka.core.ManhattanDistance();
	        DistanceFunction df = new weka.core.EuclideanDistance();
	        kmeans.setDistanceFunction(df);
	        kmeans.setSeed(10);

	        kmeans.setPreserveInstancesOrder(true);
	        kmeans.setNumClusters(numClusters);
	        String arffFile = fileName+".arff";
	        DataSource source = new DataSource(arffFile);
	        Instances instances = source.getDataSet();

	        //inst.setDataset(instances);
	        kmeans.buildClusterer(instances);
	        System.out.println(kmeans.displayStdDevsTipText());

	        // This array returns the cluster number (starting with 0) for each instance
	        // The array has as many elements as the number of instances
	        int[] assignments = kmeans.getAssignments();

	        int i=0;
	        HashMap<Double,Integer> clusters = new HashMap<Double,Integer>();
	        for(int clusterNum : assignments) {
	            clusters.put(instances.instance(i).value(0), clusterNum); //If this is right everything is right
	          //  System.out.println("Instance "+(i+1)+" -> Cluster "+clusterNum);
	            i++;
	        }
	        HashMap<Integer,Set<Pair<String,String>>> resultClusters = new HashMap<Integer,Set<Pair<String,String>>>();
	        Iterator it = scorePerPair.entrySet().iterator();
	        while (it.hasNext()) {
	            Map.Entry pair = (Map.Entry)it.next();
	            Pair<String,String> entityPair = (Pair<String, String>) pair.getKey();
	            Double score = (Double) pair.getValue();
	            int clusterId = clusters.get(score);
	            Set<Pair<String,String>> clusterSet;
	            if(resultClusters.containsKey(clusterId))
	            	clusterSet = resultClusters.get(clusterId);	            
	            else
	            	clusterSet = new HashSet<Pair<String,String>>();
	            clusterSet.add(entityPair);
	            resultClusters.put(clusterId, clusterSet); 
	            it.remove(); // avoids a ConcurrentModificationException
	        }

	        return resultClusters;

		}
		catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	public Set<Pair<String,String>> sampleExamples(Set<Pair<String,String>> allExamples, Graph<String> graph, 
			int genExamplesLimit){
		//compute a weighted score for each pair of entities. alpha * diversity + beta * popularity - gamma * inverse_functionality
		//sort by the weighted score
		if(genExamplesLimit < 0)
			genExamplesLimit = (int)Double.POSITIVE_INFINITY;
		Set<Pair<String,String>> sampleSet = Sets.newHashSet();
		HashMap<String,Double> scorePerEntity = new HashMap<String, Double>();
		HashMap<Pair<String,String>,Double> scorePerPair = new HashMap<Pair<String,String>,Double>();
		
		double maxDiversity = 0.0, maxPopularity = 0.0, maxFunctionality = 0.0;
		HashMap<String,Double> diversityMap = new HashMap<String,Double>();
		HashMap<String,Double> popularityMap = new HashMap<String,Double>();
		HashMap<String,Double> functionalityMap = new HashMap<String,Double>();
		for(Pair<String,String> example:allExamples){
			String subject = example.getLeft();
			String object = example.getRight();
			Set<Edge<String>> subIncidentEdges = graph.getNeighbours(subject);
			Set<Edge<String>> objIncidentEdges = graph.getNeighbours(object);
			double diversity=0.0, popularity=0.0, functionality=0.0;
			ArrayList<Double> entityStats;
			if(!diversityMap.containsKey(subject) && diversityMap.get(subject) == null){
				entityStats = this.computeEntityStats(subject, subIncidentEdges,subjectLimit);
				diversity = entityStats.get(0);
				popularity = entityStats.get(1);
				functionality = 1.0 - entityStats.get(2);
				if(diversity > maxDiversity)
					maxDiversity = diversity;
				if(popularity > maxPopularity)
					maxPopularity = popularity;
				if(functionality > maxFunctionality)
					maxFunctionality = functionality;
				diversityMap.put(subject, diversity);
				popularityMap.put(subject, popularity);
				functionalityMap.put(subject, functionality);
			}
			if(!diversityMap.containsKey(object) && diversityMap.get(object) == null){
				entityStats = this.computeEntityStats(object, objIncidentEdges, objectLimit);
				diversity = entityStats.get(0);
				popularity = entityStats.get(1);
				functionality = 1.0 - entityStats.get(2);
				if(diversity > maxDiversity)
					maxDiversity = diversity;
				if(popularity > maxPopularity)
					maxPopularity = popularity;
				if(functionality > maxFunctionality)
					maxFunctionality = functionality;
				diversityMap.put(object, diversity);
				popularityMap.put(object, popularity);
				functionalityMap.put(object, functionality);
			}
			
			
		}
		
		int i=0;
		for(Pair<String,String> example:allExamples){
			String subject = example.getLeft();
			String object = example.getRight();
			Set<Edge<String>> subIncidentEdges = graph.getNeighbours(subject);
			Set<Edge<String>> objIncidentEdges = graph.getNeighbours(object);
			double diversity=diversityMap.get(subject), popularity = popularityMap.get(subject), functionality = functionalityMap.get(subject), 
					subScore, objScore, pairScore;
			ArrayList<Double> entityStats;
			subScore = 0.0;
			if(!scorePerEntity.containsKey(subject) && scorePerEntity.get(subject) == null){
				if(maxDiversity>0)
					diversity /= maxDiversity;
				if(maxPopularity > 0)
					popularity /= maxPopularity;
				if(maxFunctionality > 0)
					functionality /= maxFunctionality;
				subScore = alpha * diversity + beta * popularity + gamma * functionality;
				scorePerEntity.put(subject, subScore);
			}
			else
				subScore = scorePerEntity.get(subject);
			
			
			diversity=diversityMap.get(object);
			popularity = popularityMap.get(object);
			functionality = functionalityMap.get(object);
			
			objScore = 0.0;
			if(!scorePerEntity.containsKey(object) && scorePerEntity.get(object) == null){
				if(maxDiversity > 0)
					diversity /= maxDiversity;
				if(maxPopularity > 0)
					popularity /= maxPopularity;
				if(maxFunctionality > 0)
					functionality /= maxFunctionality;
				objScore = alpha * diversity + beta * popularity + gamma * functionality;
				scorePerEntity.put(object, objScore);
			}
			else
				objScore = scorePerEntity.get(object);

			
			pairScore = this.subWeight * subScore + this.objWeight * objScore;
			scorePerPair.put(example, pairScore);
		}
		
		if(samplingMode.equals("stratified")){
			int numClusters = 6;
			int numEntityPairs = scorePerPair.size();
			HashMap<Integer,Set<Pair<String,String>>> clusters = kMeans(scorePerPair, numClusters); //iterator in kMeans destroyed scorePerPair by now	
			//stratified sampling proportional to the cluster size per cluster draw
			int remSize = genExamplesLimit;
			for(int clusterNum : clusters.keySet()){
				Set<Pair<String,String>> entityPairs = clusters.get(clusterNum);
				int entityPairsSize = entityPairs.size();
				int proportionalSize = (int)Math.ceil(entityPairsSize * (double)genExamplesLimit / (double)numEntityPairs);		
				if(numEntityPairs <= genExamplesLimit)
					proportionalSize = entityPairsSize-1;
				if(proportionalSize > remSize)
					proportionalSize = remSize;
				remSize -= proportionalSize;
				//Modified Fisher-Yates algorithm to pick random entity pairs within each cluster
				int[] randomIndices = new int[entityPairsSize];
				for(int j=0; j<entityPairsSize; j++){
					randomIndices[j] = j;
				}
				for(int j=0; j<proportionalSize; j++){
					int Min = j+1;
					if(Min>=entityPairsSize)
						Min = entityPairsSize-1;
					int Max = entityPairsSize-1;
					int swapIndex = Min + (int)(Math.random() * (Max - Min));
					//swap j with swapIndex
					int temp = randomIndices[j];
					randomIndices[j] = randomIndices[swapIndex];
					randomIndices[swapIndex] = temp;
				}
				for(int j=0; j<proportionalSize; j++){
					Object[] entityPairArray = entityPairs.toArray();
					sampleSet.add((Pair<String, String>) entityPairArray[randomIndices[j]]);
				}
			}
			return sampleSet;
		}
		HashMap<Pair<String,String>,Double> sortedScorePerPair = sortByValues(scorePerPair);
		int count = 0;
		for(Double value:sortedScorePerPair.values()){
			if(value < 0.0)
				count++;
		}
		int keyNum = 0;
		HashSet<String> seenSet = new HashSet<String>();
		//Adding code for uniform sampling
		if(samplingMode.equals("uniform")){
			int totalSize = sortedScorePerPair.size();
			int sampleInterval = totalSize/genExamplesLimit;
			if(sampleInterval ==0)
				sampleInterval = 1;
			int pairIndex = -1;
			boolean gotSamplePoint = true;
			for(Pair<String,String> key:sortedScorePerPair.keySet()){
				pairIndex = (pairIndex + 1) % sampleInterval;
				if(pairIndex == 0)
					gotSamplePoint = false;
				if(gotSamplePoint)
					continue;
				if(keyNum >= genExamplesLimit)
					break;
				if(key.getLeft() == key.getRight() || seenSet.contains(key.getLeft()) || seenSet.contains(key.getRight()))
					continue;
				sampleSet.add(key);
				seenSet.add(key.getLeft());
				seenSet.add(key.getRight());
				gotSamplePoint = true;
				keyNum++;
			}
		}
		else if(samplingMode.equals("topK")){
			for(Pair<String,String> key:sortedScorePerPair.keySet()){
				if(keyNum >= genExamplesLimit)
					break;
				if(key.getLeft() == key.getRight() || seenSet.contains(key.getLeft()) || seenSet.contains(key.getRight()))
					continue;
				sampleSet.add(key);
				seenSet.add(key.getLeft());
				seenSet.add(key.getRight());
				keyNum++;
			}
		}
		
		return sampleSet;
	}


	public static void main(String[] args){

		Set<Pair<String,String>> examples = Sets.newHashSet();

		//how to use the graph
		Graph<String> graph = new Graph<String>();

		String entity = "http://dbpedia.org/resource/Barack_Obama";

		Set<Edge<String>> edges = graph.getNeighbours(entity);
		VariancePopularSampling vps = new VariancePopularSampling(0.5, 0.4, 0.1, 0.5, 0.5, -1, -1, "topK"); //alpha, beta, gamma, subWeight, objWeight, isTopK
	//	vps.sampleExamples(allExamples, graph, genExamplesLimit);

		for(Edge<String> oneEdge : edges){
			oneEdge.getNodeSource();

			oneEdge.getNodeEnd();

			//name of the relation
			oneEdge.getLabel();

			//if it is false-> edge is outgoing, if it is true, edge is incoming
			oneEdge.isArtificial();
		}
	}

}
