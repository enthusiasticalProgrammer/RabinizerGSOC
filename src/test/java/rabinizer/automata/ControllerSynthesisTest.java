/*
 * Copyright (C) 2016  (See AUTHORS)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rabinizer.automata;

import static org.junit.Assert.*;

import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import jhoafparser.consumer.HOAConsumerNull;
import jhoafparser.consumer.HOAIntermediateCheckValidity;
import ltl.Formula;
import ltl.equivalence.EquivalenceClassFactory;
import omega_automaton.Edge;
import omega_automaton.collections.valuationset.BDDValuationSetFactory;
import omega_automaton.collections.valuationset.ValuationSet;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.Util;
import rabinizer.DTGRMAAcceptance.BoundAndReward;
import rabinizer.DTGRMAAcceptance.GeneralisedRabinWithMeanPayoffAcceptance;
import rabinizer.automata.Master.State;
import rabinizer.exec.Main;

public class ControllerSynthesisTest {
    static final Set<Optimisation> standard = EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION);

    @Before
    public final void setUp() {
        Main.silent = true;
    }

    // First of all the trivial tests:

    @Test
    public void testMDPConditionPresent() {
        Formula formula = Util.createFormula("G {inf >= 0.4} a");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(1);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();

        assertTrue(dtgra.getAcceptance() instanceof GeneralisedRabinWithMeanPayoffAcceptance);

        GeneralisedRabinWithMeanPayoffAcceptance acc = (GeneralisedRabinWithMeanPayoffAcceptance) dtgra.getAcceptance();

        assertEquals(1, acc.acceptanceMDP.size());

        Set<BoundAndReward> accMDP = new HashSet<>(acc.acceptanceMDP.get(0));
        assertEquals(1, accMDP.size());

        for (BoundAndReward reward : accMDP) {
            assertEquals(1, reward.relevantEntries().size());
        }
    }

    @Test
    public void testTransitionIsNotSuppressed() {
        Formula formula = Util.createFormula("G {inf >= 0.4} a");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(1);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();

        ValuationSet valu = valuationSetFactory.createEmptyValuationSet();
        Map<Edge<Product<FrequencySelfProductSlave.State>.ProductState>, ValuationSet> map = dtgra.getSuccessors(dtgra.getInitialState());
        for (Entry<Edge<Product<FrequencySelfProductSlave.State>.ProductState>, ValuationSet> entry : map.entrySet()) {
            valu.addAll(entry.getValue());
        }

        assertTrue(valu.isUniverse());
    }

    @Test
    public void testTransitionIsNotSuppressedRefined() {
        Formula formula = Util.createFormula("G {inf >= 0.4} a");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(1);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();

        assertNotNull(dtgra.primaryAutomaton.getSuccessor(dtgra.getInitialState().primaryState, new BitSet(1)));
    }

    @Test
    public void testTransitionIsNotSuppressedRefined2() {
        Formula formula = Util.createFormula("G {inf >= 0.4} a");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(1);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();

        ValuationSet successors = valuationSetFactory.createEmptyValuationSet();

        for (Entry<Edge<State>, ValuationSet> entry : dtgra.primaryAutomaton.getSuccessors(dtgra.primaryAutomaton.getInitialState()).entrySet()) {
            successors.addAll(entry.getValue());
        }

        assertTrue(successors.isUniverse());

    }

    @Test
    public void testTransitionIsNotSuppressedRefined3() {
        Formula formula = Util.createFormula("G {inf >= 0.4} a");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(1);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, Collections.emptySet());
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();

        ValuationSet valu = valuationSetFactory.createEmptyValuationSet();
        Map<Edge<Product<FrequencySelfProductSlave.State>.ProductState>, ValuationSet> map = dtgra.getSuccessors(dtgra.getInitialState());
        for (Entry<Edge<Product<FrequencySelfProductSlave.State>.ProductState>, ValuationSet> entry : map.entrySet()) {
            valu.addAll(entry.getValue());
        }

        assertTrue(valu.isUniverse());
    }

    @Test
    public void noExceptionOccurringForFOperator() {
        Formula formula = Util.createFormula("G {inf >= 0.4} a");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(1);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();
        dtgra.toHOA(new HOAIntermediateCheckValidity(new HOAConsumerNull()), null);
    }

    @Test
    public void testNoExceptionOccurring() {
        Formula formula = Util.createFormula("G {sup >= 0.6} (a | X a | X X a)");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(1);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();
        dtgra.toHOA(new HOAIntermediateCheckValidity(new HOAConsumerNull()), null);
    }

    @Test
    public void testDifferentRewardsWhenTwoMojmirStatesAcceptAtTheSameTime() {
        Formula formula = Util.createFormula("G {sup >= 0.6} (a | X a | X X a)");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(1);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();
        GeneralisedRabinWithMeanPayoffAcceptance acc = (GeneralisedRabinWithMeanPayoffAcceptance) dtgra.getAcceptance();

        assertEquals(1, acc.acceptanceMDP.get(0).size());

        acc.acceptanceMDP.get(0).forEach(bound -> {
            assertEquals(3, bound.getNumberOfRewardSets());
        });
    }

    @Test
    public void testNotNondeterministic() {
        Formula formula = Util.createFormula("G {sup >= 0.11} ((X b & !a)|(a & X ! a & X X b) )");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(2);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();

        for (Product<FrequencySelfProductSlave.State>.ProductState state : dtgra.getStates()) {
            ValuationSet valu = valuationSetFactory.createEmptyValuationSet();
            for (Entry<Edge<Product<FrequencySelfProductSlave.State>.ProductState>, ValuationSet> edge : dtgra.getSuccessors(state).entrySet()) {
                assertFalse(valu.intersects(edge.getValue()));
                valu.addAll(edge.getValue());
            }
        }
    }

    @Test
    public void testNotNondeterministicSlaves() {
        Formula formula = Util.createFormula("G {sup >= 0.11} ((X b & !a)|(a & X ! a & X X b) )");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(2);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();
        dtgra.secondaryAutomata.forEach((key, secondary) -> {

            for (FrequencySelfProductSlave.State state : secondary.getStates()) {
                ValuationSet valu = valuationSetFactory.createEmptyValuationSet();
                for (Entry<Edge<FrequencySelfProductSlave.State>, ValuationSet> edge : secondary.getSuccessors(state).entrySet()) {
                    assertFalse(valu.intersects(edge.getValue()));
                    valu.addAll(edge.getValue());
                }
            }
        });
    }

    @Test
    public void testNotNondeterministicMojmir() {
        Formula formula = Util.createFormula("G {sup >= 0.11} ((X b & !a)|(a & X ! a & X X b) )");

        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(2);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();
        dtgra.secondaryAutomata.forEach((key, secondary) -> {

            for (MojmirSlave.State state : secondary.mojmir.getStates()) {
                ValuationSet valu = valuationSetFactory.createEmptyValuationSet();
                for (Entry<Edge<MojmirSlave.State>, ValuationSet> edge : secondary.mojmir.getSuccessors(state).entrySet()) {
                    assertFalse(valu.intersects(edge.getValue()));
                    valu.addAll(edge.getValue());
                }
            }
        });

    }

    @Test
    public void testRewardsAreCorrectWhenMerging() {
        Formula formula = Util.createFormula("G {sup >= 0.11} ((X b & !a)|(a & X ! a & X X b) )");
        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(2);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();

        GeneralisedRabinWithMeanPayoffAcceptance acc = (GeneralisedRabinWithMeanPayoffAcceptance) dtgra.getAcceptance();

        acc.acceptanceMDP.stream().forEach(set -> {
            set.forEach(reward -> {
                reward.relevantEntries().forEach(entry -> {
                    assertTrue(entry.getKey().equals(2) || entry.getKey().equals(1));
                });
            });
        });
    }

    @Test
    public void testNoNPEOccurring() {
        Formula formula = Util.createFormula("G {sup >= 0.11} false");
        EquivalenceClassFactory equivalenceClassFactory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(0);

        DTGRMAFactory automatonFactory = new DTGRMAFactory(formula, equivalenceClassFactory, valuationSetFactory, standard);
        ProductControllerSynthesis dtgra = automatonFactory.constructAutomaton();

        dtgra.toHOA(new HOAIntermediateCheckValidity(new HOAConsumerNull()), null);
    }
}
