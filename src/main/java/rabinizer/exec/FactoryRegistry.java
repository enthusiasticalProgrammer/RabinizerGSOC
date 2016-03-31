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

package rabinizer.exec;

import rabinizer.collections.valuationset.BDDValuationSetFactory;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.collections.valuationset.Z3ValuationSetFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.equivalence.BDDEquivalenceClassFactory;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.equivalence.Z3EquivalenceClassFactory;

import java.util.Set;

public class FactoryRegistry {

    public static final Backend defaultBackend = Backend.BDD;

    public static EquivalenceClassFactory createEquivalenceClassFactory(Formula formula) {
        return createEquivalenceClassFactory(defaultBackend, formula);
    }

    public static EquivalenceClassFactory createEquivalenceClassFactory(Backend backend, Formula formula) {
        try {
            switch (backend) {
                case Z3:
                    return new Z3EquivalenceClassFactory(formula);

                case BDD:
                default:
                    return new BDDEquivalenceClassFactory(formula);
            }
        } catch (Exception e) {
            System.err.println("Unable to instantiate factory with " + backend + " backend. Falling back to the BDD backend. (" + e + ")");
            return new BDDEquivalenceClassFactory(formula);
        }
    }

    public static ValuationSetFactory createValuationSetFactory(Formula formula) {
        return createValuationSetFactory(defaultBackend, formula);
    }

    public static ValuationSetFactory createValuationSetFactory(Backend backend, Formula formula) {
        try {
            switch (backend) {
                case Z3:
                    return new Z3ValuationSetFactory(formula);

                case BDD:
                default:
                    return new BDDValuationSetFactory(formula);
            }
        } catch (Exception e) {
            System.err.println("Unable to instantiate factory with " + backend + " backend. Falling back to the BDD backend. (" + e + ")");
            return new BDDValuationSetFactory(formula);
        }
    }

    public enum Backend {
        BDD, Z3
    }
}
