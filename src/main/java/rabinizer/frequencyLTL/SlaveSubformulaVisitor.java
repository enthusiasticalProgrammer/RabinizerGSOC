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

import java.util.HashSet;
import java.util.Set;

import ltl.*;
import ltl.visitors.Visitor;

public class SlaveSubformulaVisitor implements Visitor<Set<UnaryModalOperator>> {

    @Override
    public Set<UnaryModalOperator> defaultAction(Formula formula) {
        return new HashSet<>();
    }

    @Override
    public Set<UnaryModalOperator> visit(Conjunction conjunction) {
        return conjunction.union(f -> f.accept(this));
    }

    @Override
    public Set<UnaryModalOperator> visit(Disjunction disjunction) {
        return disjunction.union(f -> f.accept(this));
    }

    @Override
    public Set<UnaryModalOperator> visit(FOperator fOperator) {
        Set<UnaryModalOperator> result = fOperator.operand.accept(this);
        if (fOperator instanceof FOperatorForMojmir) {
            result.add(fOperator);
        }
        return result;
    }

    @Override
    public Set<UnaryModalOperator> visit(GOperator gOperator) {
        Set<UnaryModalOperator> result = gOperator.operand.accept(this);
        result.add(gOperator);
        return result;
    }

    @Override
    public Set<UnaryModalOperator> visit(UOperator uOperator) {
        Set<UnaryModalOperator> result = uOperator.left.accept(this);
        result.addAll(uOperator.right.accept(this));
        return result;
    }

    @Override
    public Set<UnaryModalOperator> visit(XOperator xOperator) {
        return xOperator.operand.accept(this);
    }
}
