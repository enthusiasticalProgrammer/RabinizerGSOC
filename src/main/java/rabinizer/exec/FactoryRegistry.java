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

import com.google.common.collect.BiMap;
import rabinizer.collections.valuationset.BDDValuationSetFactory;
import rabinizer.collections.valuationset.ValuationSetFactory;
import ltl.Formula;
import ltl.equivalence.BDDEquivalenceClassFactory;
import ltl.equivalence.EquivalenceClassFactory;

public class FactoryRegistry {

    public static final Backend DEFAULT_BACKEND = Backend.BDD;

    public static EquivalenceClassFactory createEquivalenceClassFactory(Formula formula) {
        return createEquivalenceClassFactory(DEFAULT_BACKEND, formula);
    }

    public static EquivalenceClassFactory createEquivalenceClassFactory(Backend backend, Formula formula) {
        try {
            switch (backend) {
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
        return createValuationSetFactory(DEFAULT_BACKEND, formula, null);
    }

    /**
     * Does not use mapping, can therefore have unexpected behaviour
     */
    @Deprecated
    public static ValuationSetFactory createValuationSetFactory(Backend backend, Formula formula) {
        return createValuationSetFactory(backend, formula, null);
    }

    public static ValuationSetFactory createValuationSetFactory(Backend backend, Formula formula, BiMap<String, Integer> mapping) {
        try {
            switch (backend) {
                case BDD:
                default:
                    return new BDDValuationSetFactory(formula, mapping);
            }
        } catch (Exception e) {
            System.err.println("Unable to instantiate factory with " + backend + " backend. Falling back to the BDD backend. (" + e + ")");
            return new BDDValuationSetFactory(formula);
        }
    }

    // TODO: expose different BDD backends.
    public enum Backend {
        BDD
    }
}
