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

package rabinizer.frequencyLTL;

import ltl.Conjunction;
import ltl.visitors.DefaultConverter;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.Formula;
import ltl.GOperator;
import ltl.UOperator;
import ltl.XOperator;

public class UnfoldNoSlaveOperatorVisitor extends DefaultConverter {

    @Override
    public Formula visit(GOperator g) {
        return g;
    }

    @Override
    public Formula visit(XOperator x) {
        return x;
    }

    @Override
    public Formula visit(FOperator fOperator) {
        if (fOperator instanceof FOperatorForMojmir) {
            return fOperator;
        }
        return new Disjunction(fOperator.operand.accept(this), fOperator);
    }

    @Override
    public Formula visit(UOperator uOperator) {
        return new Disjunction(uOperator.right.accept(this), new Conjunction(uOperator.left.accept(this), uOperator));
    }
}
