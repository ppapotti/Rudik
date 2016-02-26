package asu.edu.neg_rule_miner.naive;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.RDFNode;

public class InputPositiveProcess {

	/**
	 * Return a constant pair if subject or objects have something in common
	 * If subject (object) do 
	 * @param positiveExamples
	 * @return
	 */
	public Pair<RDFNode,RDFNode> analysePositiveExamples(Set<Pair<RDFNode,RDFNode>> positiveExamples){
		if(positiveExamples==null||positiveExamples.size()==0)
			return Pair.of(null, null);
		RDFNode subject = positiveExamples.iterator().next().getLeft();
		RDFNode object = positiveExamples.iterator().next().getRight();
		Pair<RDFNode,RDFNode> result = Pair.of(subject, object);
		for(Pair<RDFNode,RDFNode> example:positiveExamples){
			if(result.getLeft()!=null&&!result.getLeft().equals(example.getLeft()))
				result = Pair.of(null, result.getRight());
			if(result.getRight()!=null&&!result.getRight().equals(example.getRight()))
				result = Pair.of(result.getLeft(), null);
			if(result.getLeft()==null&&result.getRight()==null)
				return result;
		}
		return result;
	}
}
