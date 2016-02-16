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

public class ContainsVisitor implements Visitor<Boolean> {
    private final Class<? extends Formula> c;

    public ContainsVisitor(Class<? extends Formula> cl) {
        if (!Formula.class.isAssignableFrom(cl)) {
            throw new IllegalArgumentException("");
        }
        this.c = cl;
    }

    @Override
    public Boolean defaultAction(@NotNull Formula formula) {
        throw new AssertionError();
    }

    @Override
    public Boolean visit(@NotNull BooleanConstant booleanConstant) {
        return c.equals(BooleanConstant.class);
    }

    @Override
    public Boolean visit(@NotNull Conjunction conjunction) {
        if (c.equals(Conjunction.class)) {
            return true;
        }
        return conjunction.children.stream().anyMatch(child -> child.accept(this));
    }

    @Override
    public Boolean visit(@NotNull Disjunction disjunction) {
        if (c.equals(Disjunction.class)) {
            return true;
        }
        return disjunction.children.stream().anyMatch(child -> child.accept(this));
    }

    @Override
    public Boolean visit(@NotNull FOperator fOperator) {
        if (c.equals(FOperator.class)) {
            return true;
        }
        return fOperator.operand.accept(this);
    }

    @Override
    public Boolean visit(@NotNull GOperator gOperator) {
        if (c.equals(GOperator.class)) {
            return true;
        }
        return gOperator.operand.accept(this);
    }

    @Override
    public Boolean visit(@NotNull Literal literal) {
        return c.equals(Literal.class);
    }

    @Override
    public Boolean visit(@NotNull UOperator uOperator) {

        if (c.equals(UOperator.class)) {
            return true;
        }
        return uOperator.left.accept(this) || uOperator.right.accept(this);
    }

    @Override
    public Boolean visit(@NotNull XOperator xOperator) {
        if (c.equals(XOperator.class)) {
            return true;
        }
        return xOperator.operand.accept(this);
    }

}
