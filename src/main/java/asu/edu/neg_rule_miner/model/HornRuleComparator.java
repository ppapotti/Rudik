package asu.edu.neg_rule_miner.model;

import java.util.Comparator;

@SuppressWarnings("rawtypes")
public class HornRuleComparator implements Comparator<HornRule> {


	@Override
	public int compare(HornRule o1, HornRule o2) {

		if(o1.getSupport()!=o2.getSupport())
			return o2.getSupport()-o1.getSupport();

		if(o1.isValid()!=o2.isValid()){
			if(o1.isValid())
				return -1;
			return 1;
		}

		if(o1.getLen()!=o2.getLen())
			return o2.getLen()-o1.getLen();

		if(o1.equals(o2))
			return 0;
		return -1;

	}

}
