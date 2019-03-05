package asu.edu.rule_miner.rudik;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.FileNotFoundException;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;

import java.util.HashMap;
 import java.util.Map.Entry;
 import java.util.stream.Collectors;
import java.util.LinkedHashMap;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.ext.com.google.common.collect.Lists;

import asu.edu.rule_miner.rudik.model.horn_rule.HornRule;
import asu.edu.rule_miner.rudik.rule_generator.DynamicPruningRuleDiscovery;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import asu.edu.rule_miner.rudik.model.horn_rule.RuleAtom;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.QuerySolution;


// xml library
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class Rudik2_App
{


	public static String toDbpedia_resource(String resource) {
		final String result = "http://dbpedia.org/resource/" + resource;
	    return result;
	} 
	public static String toDbpedia_property(String property) {
	    return "http://dbpedia.org/property/" + property;
	} 
    
	public static final String graph = " <http://dbpedia.org> ";
	public static final String type_prefix = " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ";
	public static final String ontology_prefix = "http://dbpedia.org/ontology";
	public static final String agent_type = "http://dbpedia.org/ontology/Agent";
	public static final String settlement_type = "http://dbpedia.org/ontology/Settlement";
	public static final String location_type = "http://dbpedia.org/ontology/Location";
	public static final String organisation_type = "http://dbpedia.org/ontology/Organisation";
	public static final String person_type = "http://dbpedia.org/ontology/Person";

 	//public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/foundedBy");
	//public static final String typeSubject = "http://dbpedia.org/ontology/Agent";
	//public static final String typeObject = "http://dbpedia.org/ontology/Agent";

  	public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/spouse");
	public static final String typeSubject = "http://dbpedia.org/ontology/Person";
	public static final String typeObject = "http://dbpedia.org/ontology/Person";


 // 	public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/vicePresident");
	// public static final String typeSubject = "http://dbpedia.org/ontology/Person";
	// public static final String typeObject = "http://dbpedia.org/ontology/Person";

 // 	public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/deathPlace");
	// public static final String typeSubject = "http://dbpedia.org/ontology/Person";
	// public static final String typeObject = "http://dbpedia.org/ontology/Place";

 // 	public static final Set<String> relations = Sets.newHashSet("http://dbpedia.org/ontology/birthYear");
	// public static final String typeSubject = "http://dbpedia.org/ontology/Person";
	// public static final String typeObject = null;

    public static final Set<Pair<String,Double>> set_positive_rules = Sets.newHashSet();
    public static final Set<String> good_positive_rules = Sets.newHashSet();
    public static final Set<Pair<String,Double>> set_negative_rules = Sets.newHashSet();
    public static final Set<String> good_negative_rules = Sets.newHashSet();



	public static boolean double_check(String subject, String object, String relation, boolean label) {
		final String s_query = "SELECT ?o  FROM <http://dbpedia.org>" + " WHERE {" + " <" +  subject + "> " + " <" +  relation + "> " + " ?o }  ";

	    QueryExecution qexec = QueryExecutionFactory.sparqlService("http://localhost:8890/sparql", s_query); // http://localhost:8890/sparql
	    final ResultSet s_results = qexec.execSelect();  
      	Set<String> s_set = Sets.newHashSet();
       
        while (s_results.hasNext()) {            
              QuerySolution qs = s_results.next();
              Iterator<String> itVars = qs.varNames();
              while (itVars.hasNext()) {
                  String szVar = itVars.next().toString();
                  String szVal = qs.get(szVar).toString();
                  s_set.add(szVal);                
              } 
          
      	}

      	qexec.close();

		final String o_query = "SELECT ?o  FROM <http://dbpedia.org>" + " WHERE { ?o " + " <" +  relation + "> " + " <" +  object + "> " + " }  ";
	    qexec = QueryExecutionFactory.sparqlService("http://localhost:8890/sparql", o_query); // http://localhost:8890/sparql
	    final ResultSet o_results = qexec.execSelect(); 
      	Set<String> o_set = Sets.newHashSet();
        
        while (o_results.hasNext()) {            
              QuerySolution qs = o_results.next();
              Iterator<String> itVars = qs.varNames();
              while (itVars.hasNext()) {
                  String szVar = itVars.next().toString();
                  String szVal = qs.get(szVar).toString();
                  o_set.add(szVal);                
              } 
          
      	}
      	qexec.close();

      	if (label) {
      		if (s_set.contains(object) || o_set.contains(subject))
      			return true;
      		else
      			return false;
      	}
      	else {
      		if ((s_set.size()>0 || o_set.size()>0) && ((!s_set.contains(object)) && (!o_set.contains(subject))))
      			return true;
      		else
      			return false;
      	}

	}

	public static Set<String> get_type(String sparqlQuery) {

	    QueryExecution qexec = QueryExecutionFactory.sparqlService("http://localhost:8890/sparql", sparqlQuery); // http://localhost:8890/sparql

	    //qexec.setTimeout(20000);
	    ResultSet results = qexec.execSelect();
	    final Set<String> setTypes = Sets.newHashSet();
	    
	    if (!results.hasNext()) {
	      setTypes.add("empty_result");
	    }  
	    else {    
	        while (results.hasNext()) {
	         
	            
	            // Get Result
	            final QuerySolution qs = results.next();
	            
	            // Get Variable Names
	            final Iterator<String> itVars = qs.varNames();

	            // Display Result
	            while (itVars.hasNext()) {
	                final String szVar = itVars.next().toString();
	                final String szVal = qs.get(szVar).toString();
	                // if (szVal.contains(ontology_prefix)) 
	                    setTypes.add(szVal);
	                //System.out.println("[" + szVar + "]: " + szVal);
	            } 
	        }
	    }
	    qexec.close();
	    return setTypes;
	}


	public static void extend_and_write_xml(String relation, String xmlFilePath, Map<String, Integer> subjectTypes, Map<String, Integer> objectTypes, Map<HornRule, Double> outputRules_positive, Map<HornRule, Double> outputRules_negative, DynamicPruningRuleDiscovery naive) {

		try {	
			List<HornRule> rule_positive = (outputRules_positive != null) ? Lists.newArrayList(outputRules_positive.keySet()) : null;
			List<HornRule> rule_negative = (outputRules_negative!= null) ? Lists.newArrayList(outputRules_negative.keySet()) : null;

			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

			Document document = null;

			File xml = new File(xmlFilePath);
			if (xml.exists() && xml.isFile()) 
				document = documentBuilder.parse(xml);			
			else
				document = documentBuilder.newDocument();
			Element root = null;
			if (document.getElementsByTagName(relation).getLength() >0 ) {
				NodeList restElmLst = document.getElementsByTagName(relation);
    			root = (Element)restElmLst.item(0);   			
			}
			else {
				root = document.createElement(relation);
				document.appendChild(root);

			}
			
			
			Element sub_root = document.createElement("Execution");
			root.appendChild(sub_root);

			Element stypes = document.createElement("Subject_Types");

			sub_root.appendChild(stypes);

			List<String> list_subjectTypes = (subjectTypes!= null) ? Lists.newArrayList(subjectTypes.keySet()) : null;
			for(final String type:list_subjectTypes.subList(0,1)){
					Element t = document.createElement("type");
					t.appendChild(document.createTextNode(type));
					stypes.appendChild(t);			
			}

			Element otypes = document.createElement("Object_Types");

			sub_root.appendChild(otypes);

			List<String> list_objectTypes = (objectTypes!= null) ? Lists.newArrayList(objectTypes.keySet()) : null;
			for(final String type:list_subjectTypes.subList(0,1)){
					Element t = document.createElement("type");
					t.appendChild(document.createTextNode(type));
					otypes.appendChild(t);			
			}

			Element p_rules = document.createElement("Positive_Rules");
			sub_root.appendChild(p_rules);
			for(final HornRule rule:rule_positive){
				if (outputRules_positive.get(rule) != null) {
					Element t = document.createElement("rule");
					p_rules.appendChild(t);		
					Element t1 = document.createElement("atoms");
					t1.appendChild(document.createTextNode(rule.toString()));	
					t.appendChild(t1);
					Element t2 = document.createElement("confidence");
					t2.appendChild(document.createTextNode(Double.toString(outputRules_positive.get(rule))));	
					t.appendChild(t2);		
					double old_conf = outputRules_positive.get(rule);
					if (outputRules_positive.get(rule) > 0.2) {
						Set<RuleAtom> new_hornRule = rule.getRules();
						Set<RuleAtom> new_hornRule_2 = rule.getRules();
						new_hornRule.add(new RuleAtom("subject", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", list_subjectTypes.get(0)));
						new_hornRule_2.add(new RuleAtom("object", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", list_objectTypes.get(0)));

						Pair<Integer, Integer> support = naive.getRuleSupport(rule.getRules(), relations, typeSubject, typeObject, true);
						Pair<Integer, Integer> new_support = naive.getRuleSupport(new_hornRule, relations, typeSubject, typeObject, true);
						Pair<Integer, Integer> new_support_2 = naive.getRuleSupport(new_hornRule_2, relations, typeSubject, typeObject, true);
						if (5*new_support.getLeft()>support.getLeft() && 5*new_support_2.getLeft()>support.getLeft()) {
							rule.addRuleAtom(new RuleAtom("subject", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", list_subjectTypes.get(0)), false);
							rule.addRuleAtom(new RuleAtom("object", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", list_objectTypes.get(0)), false);
							double new_conf = naive.getRuleConfidence(rule.getRules(), relations, typeSubject, typeObject, true);
							if (new_conf>old_conf) {
								t = document.createElement("rule");
								p_rules.appendChild(t);		
								t1 = document.createElement("atoms");
								t1.appendChild(document.createTextNode(rule.toString()));	
								t.appendChild(t1);
								t2 = document.createElement("confidence");
								t2.appendChild(document.createTextNode(Double.toString(new_conf)));	
								t.appendChild(t2);						
							}			
						}
					}
				}
			}

			Element n_rules = document.createElement("Negative_Rules");
			sub_root.appendChild(n_rules);
			for(final HornRule rule:rule_negative){
				if (outputRules_negative.get(rule) != null) {
					Element t = document.createElement("rule");
					n_rules.appendChild(t);		
					Element t1 = document.createElement("atoms");
					t1.appendChild(document.createTextNode(rule.toString()));	
					t.appendChild(t1);
					Element t2 = document.createElement("confidence");
					t2.appendChild(document.createTextNode(Double.toString(outputRules_negative.get(rule))));	
					t.appendChild(t2);	
					double old_conf = outputRules_negative.get(rule);

					if ((outputRules_negative.get(rule)>0.2) && (outputRules_negative.get(rule)<0.90)) {

						boolean flag_1 = false;
						boolean flag_2 = false;
						Set<RuleAtom> new_hornRule = rule.getRules();
						new_hornRule.add(new RuleAtom("subject", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "v_type_1"));
						new_hornRule.add(new RuleAtom("v_type_1","!=",list_subjectTypes.get(0)));

						Set<RuleAtom> new_hornRule_2 = rule.getRules();
						new_hornRule_2.add(new RuleAtom("object", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "v_type_2"));
						new_hornRule_2.add(new RuleAtom("v_type_2","!=",list_objectTypes.get(0)));
						Pair<Integer, Integer> support = naive.getRuleSupport(rule.getRules(), relations, typeSubject, typeObject, false);					
						Pair<Integer, Integer> new_support = naive.getRuleSupport(new_hornRule, relations, typeSubject, typeObject, false);
						Pair<Integer, Integer> new_support_2 = naive.getRuleSupport(new_hornRule_2, relations, typeSubject, typeObject, false);
						if (5*new_support.getLeft()>support.getLeft() && 5*new_support_2.getLeft()>support.getLeft()) {
							double new_conf = 1.0*new_support.getLeft()/(new_support.getLeft()+23*new_support.getRight());
							double new_conf_2 = 1.0*new_support_2.getLeft()/(new_support_2.getLeft()+23*new_support_2.getRight());
							if (new_conf>old_conf &&  new_conf_2>old_conf) {
								flag_2 = true;
							}
								
						}						

						new_hornRule = rule.getRules();
						new_hornRule_2 = rule.getRules();
						new_hornRule.add(new RuleAtom("subject", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", list_subjectTypes.get(0)));
						new_hornRule_2.add(new RuleAtom("object", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", list_objectTypes.get(0)));	

						new_support = naive.getRuleSupport(new_hornRule, relations, typeSubject, typeObject, false);
						new_support_2 = naive.getRuleSupport(new_hornRule_2, relations, typeSubject, typeObject, false);
						if (5*new_support.getLeft()>support.getLeft() && 5*new_support_2.getLeft()>support.getLeft()) {
							double new_conf = 1.0*new_support.getLeft()/(new_support.getLeft()+23*new_support.getRight());
							double new_conf_2 = 1.0*new_support_2.getLeft()/(new_support_2.getLeft()+23*new_support_2.getRight());
							if (new_conf>old_conf &&  new_conf_2>old_conf) {
								flag_1 = true;
							}
						}

						if (flag_1 == true && flag_2 == false)	{
							rule.addRuleAtom(new RuleAtom("subject", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", list_subjectTypes.get(0)), false);
							rule.addRuleAtom(new RuleAtom("object", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", list_objectTypes.get(0)), false);						
							double new_conf = naive.getRuleConfidence(rule.getRules(), relations, typeSubject, typeObject, false);
							if (new_conf>old_conf) {
								t = document.createElement("rule");
								n_rules.appendChild(t);		
								t1 = document.createElement("atoms");
								t1.appendChild(document.createTextNode(rule.toString()));	
								t.appendChild(t1);
								t2 = document.createElement("confidence");
								t2.appendChild(document.createTextNode(Double.toString(new_conf)));	
								t.appendChild(t2);						
							}	
						}

						else if (flag_1 == false && flag_2 == true) {
							rule.addRuleAtom(new RuleAtom("subject", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "v_type_1"), false);
							rule.addRuleAtom(new RuleAtom("v_type_1","!=",list_subjectTypes.get(0)), false);	
							rule.addRuleAtom(new RuleAtom("object", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "v_type_2"), false);
							rule.addRuleAtom(new RuleAtom("v_type_2","!=",list_objectTypes.get(0)), false);		
							double new_conf = naive.getRuleConfidence(rule.getRules(), relations, typeSubject, typeObject, false);
							if (new_conf>old_conf) {
								t = document.createElement("rule");
								n_rules.appendChild(t);		
								t1 = document.createElement("atoms");
								t1.appendChild(document.createTextNode(rule.toString()));	
								t.appendChild(t1);
								t2 = document.createElement("confidence");
								t2.appendChild(document.createTextNode(Double.toString(new_conf)));	
								t.appendChild(t2);						
							}			
						}

					}
				}
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
			DOMSource domSource = new DOMSource(document);
			StreamResult streamResult = new StreamResult(new File(xmlFilePath));

			transformer.transform(domSource, streamResult);	

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (SAXException sfe) {
			sfe.printStackTrace();
		} catch (IOException ife) {
			ife.printStackTrace();
		}
	
	}

	public static void write_xml(String relation, String xmlFilePath,  Map<HornRule, Double> outputRules_positive, Map<HornRule, Double> outputRules_negative) {

		try {	
			List<HornRule> rule_positive = (outputRules_positive != null) ? Lists.newArrayList(outputRules_positive.keySet()) : null;
			List<HornRule> rule_negative = (outputRules_negative!= null) ? Lists.newArrayList(outputRules_negative.keySet()) : null;

			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

			Document document = null;

			File xml = new File(xmlFilePath);
			if (xml.exists() && xml.isFile()) 
				document = documentBuilder.parse(xml);			
			else
				document = documentBuilder.newDocument();
			Element root = null;
			if (document.getElementsByTagName(relation).getLength() >0 ) {
				NodeList restElmLst = document.getElementsByTagName(relation);
    			root = (Element)restElmLst.item(0);   			
			}
			else {
				root = document.createElement(relation);
				document.appendChild(root);

			}
			
			
			Element sub_root = document.createElement("Execution");
			root.appendChild(sub_root);

			

			Element p_rules = document.createElement("Positive_Rules");
			sub_root.appendChild(p_rules);
			for(final HornRule rule:rule_positive){
				if (outputRules_positive.get(rule) != null) {
					Element t = document.createElement("rule");
					p_rules.appendChild(t);		
					Element t1 = document.createElement("atoms");
					t1.appendChild(document.createTextNode(rule.toString()));	
					t.appendChild(t1);
					Element t2 = document.createElement("confidence");
					t2.appendChild(document.createTextNode(Double.toString(outputRules_positive.get(rule))));	
					t.appendChild(t2);		
				}
			}

			Element n_rules = document.createElement("Negative_Rules");
			sub_root.appendChild(n_rules);
			for(final HornRule rule:rule_negative){
				if (outputRules_negative.get(rule) != null) {
					Element t = document.createElement("rule");
					n_rules.appendChild(t);		
					Element t1 = document.createElement("atoms");
					t1.appendChild(document.createTextNode(rule.toString()));	
					t.appendChild(t1);
					Element t2 = document.createElement("confidence");
					t2.appendChild(document.createTextNode(Double.toString(outputRules_negative.get(rule))));	
					t.appendChild(t2);	
				}
			}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
			DOMSource domSource = new DOMSource(document);
			StreamResult streamResult = new StreamResult(new File(xmlFilePath));

			transformer.transform(domSource, streamResult);	

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (SAXException sfe) {
			sfe.printStackTrace();
		} catch (IOException ife) {
			ife.printStackTrace();
		}
	
	}

	public static void load_rules_from_xml(String xmlFilePath) {
	    try {

			File fXmlFile = new File(xmlFilePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
					
			doc.getDocumentElement().normalize();

			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
					
			NodeList nList = doc.getElementsByTagName("Execution");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
						

				Element eElement = (Element) nNode;

				

				Element sElement = (Element) eElement.getElementsByTagName("Positive_Rules").item(0);
				NodeList sNodelist = sElement.getElementsByTagName("rule");
				// Set<Pair<Set<RuleAtom>,Double>> rule_conf = Sets.newHashSet();
				for (int i = 0; i < sNodelist.getLength(); i ++) {
					Element sEle = (Element) sNodelist.item(i);
					String e_rule =  sEle.getElementsByTagName("atoms").item(0).getTextContent();

					double e_conf = Double.parseDouble(sEle.getElementsByTagName("confidence").item(0).getTextContent());
					set_positive_rules.add(Pair.of(e_rule, e_conf));
					if (e_conf > 0.5)
						good_positive_rules.add(e_rule);
						
				}
		

				sElement = (Element) eElement.getElementsByTagName("Negative_Rules").item(0);
				sNodelist = sElement.getElementsByTagName("rule");
				
				// rule_conf = Sets.newHashSet();
				for (int i = 0; i < sNodelist.getLength(); i ++) {
					Element sEle = (Element) sNodelist.item(i);
					String e_rule =  sEle.getElementsByTagName("atoms").item(0).getTextContent();
					double e_conf = Double.parseDouble(sEle.getElementsByTagName("confidence").item(0).getTextContent());
					set_negative_rules.add(Pair.of(e_rule, e_conf));
					if (e_conf > 0.5)
						good_negative_rules.add(e_rule);
					
				}				

			}		

			System.out.println("----------------------------");
			System.out.println(set_positive_rules.size());
			System.out.println(good_positive_rules.size());
			System.out.println(set_negative_rules.size());
			System.out.println(good_negative_rules.size());
	    } catch (Exception e) {
			e.printStackTrace();
	    }
	}

	private final static Logger LOGGER = LoggerFactory.getLogger(App.class.getName());
	public static void main( String[] args ) throws IOException
	{

		String sparqlQuery = "";

	    String path_file = "";

   		DynamicPruningRuleDiscovery naive = new DynamicPruningRuleDiscovery();
	
	   	Set<Pair<String,String>> negativeExamples = Sets.newHashSet();
	    Set<Pair<String,String>> positiveExamples = Sets.newHashSet();

	    Map<String, Integer> entity2types_sub = new HashMap<String, Integer>();
	    Map<String, Integer> entity2types_obj = new HashMap<String, Integer>();

	    if (args.length > 0) {
	        path_file = args[0];
		    File file = new File(path_file); 
		    try {
		        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
		        // BufferedWriter wr_1 = new BufferedWriter(new FileWriter("founder_manual.tsv"));
		        // List<String> manual_pairs = new ArrayList<String>();
		        // BufferedWriter wr_2 = new BufferedWriter(new FileWriter("founder_2.tsv"));
		        String tmp = br.readLine();
		        // String tmp = "";
		        while ((tmp = br.readLine()) != null) {
		            String[] sample = tmp.split("\t"); 
		            // String[] sample = tmp.split(","); 
		            final Pair<String,String> pair = Pair.of(toDbpedia_resource(sample[0]), toDbpedia_resource(sample[1]));
		            // final Pair<String,String> pair = Pair.of(toDbpedia_resource(sample[1].replaceAll("[<>]","")), toDbpedia_resource(sample[3].replaceAll("[<>]","")));
	                Set<String> currentTypes_sub = naive.get_type(toDbpedia_resource(sample[0]));
	                Set<String> currentTypes_obj = naive.get_type(toDbpedia_resource(sample[1]));
	                // Set<String> currentTypes_sub = naive.get_type(toDbpedia_resource(sample[1].replaceAll("[<>]","")));
	                // Set<String> currentTypes_obj = naive.get_type(toDbpedia_resource(sample[3].replaceAll("[<>]","")));

	                if (currentTypes_sub.size() == 0 || currentTypes_obj.size() ==0) {
	                    System.out.println("Sample " + sample[0] + "-" + sample[1] + " has no types --> Ignored " );
	                    // System.out.println("Sample " + sample[1] + "-" + sample[3] + " has no types --> Ignored " );

	                }
	                else {
			            if (sample[3].equals("1")) {
	                	// if (sample[4].equals("1")) {
	                	// if (true) {
			            	if (double_check(pair.getLeft(), pair.getRight(),relations.iterator().next(), true)) {
			                	positiveExamples.add(pair);
			                	// if (currentTypes_sub.contains(organisation_type) && (currentTypes_obj.contains(organisation_type) || currentTypes_obj.contains(person_type)))
			                		// manual_pairs.add(tmp + "\n");
			                	// 	wr_1.write(tmp + "\n");
			                	// if (currentTypes_sub.contains(organisation_type) && currentTypes_obj.contains(person_type))
			                	// 	wr_2.write(tmp + "\n");			                	
			                	for (final String type_ : currentTypes_sub) 
		                			if (type_.contains(ontology_prefix) && !type_.contains(agent_type) && !type_.contains(typeSubject)) 
		                    			entity2types_sub.put(type_, entity2types_sub.getOrDefault(type_, 0) + 1);
				                for (final String type_ : currentTypes_obj) 
			    	            	if (type_.contains(ontology_prefix) && !type_.contains(agent_type) && !type_.contains(typeObject)) 
		            		        	entity2types_obj.put(type_, entity2types_obj.getOrDefault(type_, 0) + 1);
			            	}
			            }
			            else {
			            	if (double_check(pair.getLeft(), pair.getRight(),relations.iterator().next(), false)) {
			                	negativeExamples.add(pair);
			                	// if (currentTypes_sub.contains(organisation_type) && currentTypes_obj.contains(organisation_type))
			                	// 	wr_1.write(tmp + "\n");
			                	// if (currentTypes_sub.contains(organisation_type) && currentTypes_obj.contains(person_type))
			                	// 	wr_2.write(tmp + "\n");	
			                	
			                	for (final String type_ : currentTypes_sub) 
		                			if (type_.contains(ontology_prefix) && !type_.contains(agent_type) && !type_.contains(typeSubject)) 
		                    			entity2types_sub.put(type_, entity2types_sub.getOrDefault(type_, 0) + 1);
				                for (final String type_ : currentTypes_obj) 
			    	            	if (type_.contains(ontology_prefix) && !type_.contains(agent_type) && !type_.contains(typeObject)) 
		            		        	entity2types_obj.put(type_, entity2types_obj.getOrDefault(type_, 0) + 1);
		            		    
			            	}
			            }
	            	}
	            
		        }
		        br.close();
		        // wr_1.close();
		        // wr_2.close();
		        // Collections.shuffle(manual_pairs);
		        // for (int i = 0; i < 800; i++) {
		        	// wr_1.write(manual_pairs.get(i));
		        // }
		        System.out.println(" Length Positive Set : " + positiveExamples.size() );
		        System.out.println(" Length Negative Set : " + negativeExamples.size() );  


				entity2types_sub = 
				     entity2types_sub.entrySet().stream()
				    .sorted(Entry.<String, Integer>comparingByValue().reversed())
				    .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
				                              (e1, e2) -> e1, LinkedHashMap::new));
				entity2types_obj = 
				     entity2types_obj.entrySet().stream()
				    .sorted(Entry.<String, Integer>comparingByValue().reversed())
				    .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
				                              (e1, e2) -> e1, LinkedHashMap::new));

		        System.out.println(" Subject Types : " + entity2types_sub);
				System.out.println(" Object Types : " + entity2types_obj);			

				// System.exit(1);
					    	
		    } 
	    	catch (FileNotFoundException e) {
	        	System.out.println(" File not found !!");
	        	System.exit(1);
	    	}


	    }
	    else {
	        System.out.println("No dataset specified...Try to generate it using rudik");
	    	
	        System.out.println(" Generate Positive Examples ");
			positiveExamples = naive.generatePositiveExamples(relations, typeSubject, typeObject, 500);
			
			for (final  Pair<String,String> example  :positiveExamples) {
				System.out.println("Positive example: " + example.getLeft() + " --- " + example.getRight());
			}
			
	        System.out.println(" Generate Negative Examples ");
			negativeExamples= naive.generateNegativeExamples(relations, typeSubject, typeObject, 500);
			
			for (final  Pair<String,String> example  :negativeExamples) {
				System.out.println("Negative example: " + example.getLeft() + " --- " + example.getRight());
			}
			
	    }

	
  
	    naive.discoverNegativeHornRules(negativeExamples, positiveExamples, relations, typeSubject, typeObject);

	    naive.discoverPositiveHornRules(negativeExamples, positiveExamples, relations, typeSubject, typeObject);

		// extend_and_write_xml("deathPlace", "score_deathPlace.xml", entity2types_sub, entity2types_obj, outputRules_positive, outputRules_negative, naive);
		//write_xml("foundedBy", "foundedBy_cluster.xml", outputRules_positive, outputRules_negative);

		// load_rules_from_xml("score_foundedBy.xml");



		
	}
		





	
}