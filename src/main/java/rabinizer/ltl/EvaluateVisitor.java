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

package rabinizer.ltl;

import org.jetbrains.annotations.NotNull;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

public class EvaluateVisitor implements Visitor<Formula> {

    private final EquivalenceClassFactory factory;
    private final EquivalenceClass environment;

    public EvaluateVisitor(EquivalenceClassFactory factory, Formula environment) {
        this.factory = factory;
        this.environment = factory.createEquivalenceClass(environment);
    }

    @Override
    public Formula defaultAction(@NotNull Formula f) {
        if (environment.implies(factory.createEquivalenceClass(f))) {
            return BooleanConstant.TRUE;
        }

        return f;
    }

    @Override
    public Formula visit(@NotNull Conjunction c) {
        return new Conjunction(c.children.stream().map(e -> e.accept(this)));
    }

    @Override
    public Formula visit(@NotNull Disjunction d) {
        Formula defaultAction = defaultAction(d);

        if (defaultAction instanceof BooleanConstant) {
            return defaultAction;
        }

        return new Disjunction(d.children.stream().map(e -> e.accept(this)));
    }
}
