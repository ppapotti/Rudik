package asu.edu.neg_rule_miner.sparql.jena.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Before;
import org.junit.Test;

import asu.edu.neg_rule_miner.configuration.ConfigurationFacility;
import asu.edu.neg_rule_miner.configuration.Constant;
import asu.edu.neg_rule_miner.model.rdf.graph.Edge;
import asu.edu.neg_rule_miner.model.rdf.graph.Graph;
import asu.edu.neg_rule_miner.sparql.RDFSimpleNodeResourceImplementation;

/**
 * This test class aims to test whether an KB endpoint can be queried and returns expected results
 * @author ortona
 *
 */
public class QuerySparqlRemoteEndpointTest {

	QuerySparqlRemoteEndpoint endpoint;
	Graph<RDFNode> inputGraph;

	@Before
	public void bringUp() throws ConfigurationException{
		ConfigurationFacility.getConfiguration();
		//set in the configuration file the remote endpoint
		Configuration config = new XMLConfiguration("src/test/config/RemoteSparqlEndpointConfiguration.xml");
		this.endpoint = new QuerySparqlRemoteEndpoint(config);
		this.inputGraph = new Graph<RDFNode>();

	}

	@Test
	public void testNormalQuery() throws IOException {
		//read the input entity name
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter the name of the entity:");
		String entityName = br.readLine();
		System.out.println("Enter the number of relationship where entity is subject:");
		int subjectRel = Integer.parseInt(br.readLine());
		System.out.println("Enter the number of relationship where entity is object:");
		int objectRel = Integer.parseInt(br.readLine());

		RDFNode entity = new RDFSimpleNodeResourceImplementation(entityName);
		this.inputGraph.addNode(entity);
		this.endpoint.executeQuery(entity, this.inputGraph);

		Assert.assertTrue(this.inputGraph.getNodes().contains(entity));
		Set<Edge<RDFNode>> neighbours = this.inputGraph.getNeighbours(entity);
		Assert.assertNotNull(neighbours);
		Assert.assertEquals(subjectRel+objectRel, neighbours.size());
		int actualSubjectRel=0;
		int actualObjectRel=0;
		Set<RDFNode> totalNodes = Sets.newHashSet();
		totalNodes.add(entity);
		for(Edge<RDFNode> edge:neighbours){
			if(this.inputGraph.isArtifical(edge))
				actualObjectRel++;
			else
				actualSubjectRel++;
			totalNodes.add(edge.getNodeEnd());
		}
		Assert.assertEquals(subjectRel, actualSubjectRel);
		Assert.assertEquals(objectRel, actualObjectRel);
		Assert.assertEquals(totalNodes, this.inputGraph.getNodes());

		totalNodes.remove(entity);
		for(RDFNode node:totalNodes){
			Set<RDFNode> neighboursNodes = Sets.newHashSet();
			for(Edge<RDFNode> edge:this.inputGraph.getNeighbours(node))
				neighboursNodes.add(edge.getNodeEnd());
			Assert.assertEquals(1, neighboursNodes.size());
			Assert.assertEquals(entity, neighboursNodes.iterator().next());
		}			
	}

	@Test
	public void testLiteralQuery() throws IOException {
		//read the input entity name
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Write the name of two entites that have an number neighbour to evaluate separated by space:");
		String entityNames = br.readLine();
		String firstEntity = entityNames.split(" ")[0];
		String secondEntity = entityNames.split(" ")[1];
		System.out.println("Write the name of the two relationship that connects repsectively first entity " +
				"and second entity to a number separated by space:");
		String relNames = br.readLine();
		String firstRelation = relNames.split(" ")[0];
		String secondRelation = relNames.split(" ")[1];
		System.out.println("Write the relation that connects the first number with the second ("
				+Constant.EQUAL_REL+", "+Constant.GREATER_EQUAL_REL+", "+Constant.LESS_EQUAL_REL+", "+Constant.DIFF_REL+"):");
		String rel = br.readLine();

		RDFNode firstEntityNode = new RDFSimpleNodeResourceImplementation(firstEntity);
		this.inputGraph.addNode(firstEntityNode);
		this.endpoint.executeQuery(firstEntityNode, this.inputGraph);
		//get the first literal neighbour
		RDFNode firstEntityLiteral = null;
		for(Edge<RDFNode> edge:this.inputGraph.getNeighbours(firstEntityNode)){
			if(edge.getLabel().equals(firstRelation)){
				firstEntityLiteral = edge.getNodeEnd();
				break;
			}
		}
		Assert.assertNotNull(firstEntityLiteral);

		RDFNode secondEntityNode = new RDFSimpleNodeResourceImplementation(secondEntity);
		this.inputGraph.addNode(secondEntityNode);
		this.endpoint.executeQuery(secondEntityNode, this.inputGraph);
		//get the second literal neighbour
		RDFNode secondEntityLiteral = null;
		for(Edge<RDFNode> edge:this.inputGraph.getNeighbours(secondEntityNode)){
			if(edge.getLabel().equals(secondRelation)){
				secondEntityLiteral = edge.getNodeEnd();
				break;
			}
		}
		Assert.assertNotNull(secondEntityLiteral);
		this.endpoint.executeQuery(firstEntityLiteral, this.inputGraph);

		//check there exists the path
		Edge<RDFNode> edge = new Edge<RDFNode>(firstEntityNode,firstEntityLiteral,firstRelation);
		Assert.assertTrue(this.inputGraph.getNeighbours(firstEntityNode).contains(edge));

		edge = new Edge<RDFNode>(firstEntityLiteral,secondEntityLiteral,rel);
		Assert.assertTrue(this.inputGraph.getNeighbours(firstEntityLiteral).contains(edge));

		edge = new Edge<RDFNode>(secondEntityLiteral,secondEntityNode,secondRelation);
		Assert.assertTrue(this.inputGraph.getNeighbours(secondEntityLiteral).contains(edge));
		Assert.assertTrue(this.inputGraph.isArtifical(edge));
	}

}
