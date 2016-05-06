package asu.edu.neg_rule_miner.model;


import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class HornRuleTest {

	private HornRule hornRule;
	@Before
	public void bringUp(){
		hornRule = new HornRule();
		int count=0;
		RuleAtom r1 = new RuleAtom(HornRule.START_NODE,"r1",HornRule.LOOSE_VARIABLE_NAME+count);
		hornRule.addAtom(r1);

		RuleAtom r2 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+count,"r2",HornRule.LOOSE_VARIABLE_NAME+(++count));
		hornRule.addAtom(r2);

		RuleAtom r3 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+count,"r3",HornRule.LOOSE_VARIABLE_NAME+(++count));
		hornRule.addAtom(r3);

		RuleAtom r4 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+count,"r4",HornRule.END_NODE);
		hornRule.addAtom(r4);

	}

	@Test
	public void testHornRuleValidity(){
		Assert.assertTrue(hornRule.isValid());

		HornRule otherRule = new HornRule();

		RuleAtom r1 = new RuleAtom(HornRule.START_NODE,"r1",HornRule.LOOSE_VARIABLE_NAME+0);
		otherRule.addAtom(r1);

		RuleAtom r2 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+0,"r2",HornRule.LOOSE_VARIABLE_NAME+1);
		otherRule.addAtom(r2);

		Assert.assertFalse(otherRule.isValid());

		RuleAtom r4 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+2,"r4",HornRule.END_NODE);
		otherRule.addAtom(r4);

		Assert.assertFalse(otherRule.isValid());


		RuleAtom r3 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+1,"r3",HornRule.LOOSE_VARIABLE_NAME+2);
		otherRule.addAtom(r3);

		Assert.assertTrue(otherRule.isValid());
	}

	@Test
	public void testHornRuleEquality(){
		HornRule otherRule = new HornRule();

		RuleAtom r1 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+0,"r4",HornRule.END_NODE);
		RuleAtom r2 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+1,"r3",HornRule.LOOSE_VARIABLE_NAME+0);
		RuleAtom r3 = new RuleAtom(HornRule.LOOSE_VARIABLE_NAME+2,"r2",HornRule.LOOSE_VARIABLE_NAME+1);
		RuleAtom r4 = new RuleAtom(HornRule.START_NODE,"r1",HornRule.LOOSE_VARIABLE_NAME+2);
		RuleAtom r5 = new RuleAtom(HornRule.START_NODE,"r1",HornRule.LOOSE_VARIABLE_NAME+1);
		
		//different number of atoms, they cannot be equal
		otherRule.addAtom(r1);
		Assert.assertFalse(hornRule.equals(otherRule));
		
		//different number of counting variables, they cannot be equals
		otherRule.addAtom(r2);
		otherRule.addAtom(r3);
		otherRule.addAtom(r5);
		Assert.assertFalse(hornRule.equals(otherRule));
		
		//same number of atoms and of variables, plus exchanging variables result in the same rule
		otherRule = new HornRule();
		otherRule.addAtom(r1);
		otherRule.addAtom(r2);
		otherRule.addAtom(r3);
		otherRule.addAtom(r4);
		Assert.assertTrue(hornRule.equals(otherRule));
		
		
		

	}

}
