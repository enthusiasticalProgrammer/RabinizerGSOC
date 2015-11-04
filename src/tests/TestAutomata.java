package tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import rabinizer.automata.MasterFolded;
import rabinizer.bdd.BDDForVariables;
import rabinizer.bdd.BijectionIdAtom;
import rabinizer.formulas.*;

public class TestAutomata {
	
	
	
	@Test
	public void testMasterFoldedNew(){
		Formula f1=FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f2=FormulaFactory.mkLit("p1", BDDForVariables.bijectionIdAtom.id("p1"), false);
		Formula f3=FormulaFactory.mkOr(f1,f2);
		Formula f4=FormulaFactory.mkG(f3);
		
		MasterFolded m=new MasterFolded(f4);
		assertEquals(f4,m.formula);
	}
}
