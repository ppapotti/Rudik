package asu.edu.neg_rule_miner;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

public class ResultUpdate {
    private List<String> rules;
    private List<String> updateValidVal;
    
	public ResultUpdate(List<String> rules, List<String> updateValidVal) {
//		super();
		this.rules = rules;
		this.updateValidVal = updateValidVal;
	}
	
	public ResultUpdate() {
		super();
	}

	public List<String> getRules() {
		return rules;
	}
	public void setRules(List<String> rules) {
		this.rules = rules;
	}
	public List<String> getUpdateValidVal() {
		return updateValidVal;
	}
	public void setUpdateValidVal(List<String> updateValidVal) {
		this.updateValidVal = updateValidVal;
	}
}
