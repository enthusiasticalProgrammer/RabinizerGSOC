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

import org.junit.Test;

import rabinizer.Util;
import omega_automaton.collections.valuationset.*;
import ltl.Formula;
import ltl.equivalence.EquivalenceClassFactory;

public class TestDTGRAFactory {

    @Test
    public void testAcceptance1() { // It is created to reproduce a bug
        Formula formula = Util.createFormula("G(a | G b)");

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(formula);
        ValuationSetFactory val = new BDDValuationSetFactory(2);

        DTGRAFactory automatonFactory = new DTGRAFactory(formula, factory, val, AutomatonClassTest.standard);
        ProductRabinizer dtgra = automatonFactory.constructAutomaton();
        assertTrue(dtgra.getAcceptance().unmodifiableCopyOfAcceptanceCondition().stream().allMatch(pair -> pair.right.stream().allMatch(p -> !p.isEmpty())));
    }

}
