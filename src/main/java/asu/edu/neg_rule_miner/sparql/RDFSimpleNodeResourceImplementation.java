package asu.edu.neg_rule_miner.sparql;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFVisitor;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class RDFSimpleNodeResourceImplementation implements RDFNode{

	private String nodeName;

	private boolean isLiteral;

	private Literal literalForm=null;

	public RDFSimpleNodeResourceImplementation(String name){
		this.nodeName = name;
		isLiteral = false;
	}

	@Override
	public Node asNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((nodeName == null) ? 0 : nodeName.hashCode());
		return result;
	}

	public void setIsLiteral(boolean isLiteral){
		this.isLiteral = isLiteral;
		if(this.isLiteral){
			String literal = this.toString();
			if(literal.startsWith("\"")&&literal.contains("\"^^<"))
				literal = literal.substring(1, literal.lastIndexOf("\"^^<"));
			if(literal.startsWith("\"")&&literal.contains("\""))
				literal = literal.substring(1,literal.lastIndexOf("\""));
			if(literal.contains("^^<")&&literal.endsWith(">"))
				literal=literal.substring(0,literal.indexOf("^^<"));
			literalForm = ResourceFactory.createPlainLiteral(literal);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RDFSimpleNodeResourceImplementation other = (RDFSimpleNodeResourceImplementation) obj;
		if (nodeName == null) {
			if (other.nodeName != null)
				return false;
		} else if (!nodeName.equals(other.nodeName))
			return false;
		return true;
	}

	@Override
	public <T extends RDFNode> T as(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Literal asLiteral() {
		return this.literalForm;
	}

	@Override
	public Resource asResource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends RDFNode> boolean canAs(Class<T> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RDFNode inModel(Model arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAnon() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLiteral() {
		return this.isLiteral;
	}

	@Override
	public boolean isResource() {
		return true;
	}

	@Override
	public boolean isURIResource() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object visitWith(RDFVisitor arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String toString(){
		return this.nodeName;
	}

	@Override
	public Model getModel() {
		// TODO Auto-generated method stub
		return null;
	}

}
