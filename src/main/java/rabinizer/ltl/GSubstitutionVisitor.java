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

import java.util.function.Function;

public class GSubstitutionVisitor implements Visitor<Formula> {

    private final Function<GOperator, BooleanConstant> subst;

    public GSubstitutionVisitor(Function<GOperator, BooleanConstant> subst) {
        this.subst = subst;
    }

    @Override
    public Formula defaultAction(@NotNull Formula f) {
        return f;
    }

    @Override
    public Formula visit(@NotNull Conjunction c) {
        return new Conjunction(c.children.stream().map(e -> e.accept(this)));
    }

    @Override
    public Formula visit(@NotNull Disjunction d) {
        return new Disjunction(d.children.stream().map(e -> e.accept(this)));
    }

    @Override
    public Formula visit(@NotNull FOperator f) {
        return new FOperator(f.operand.accept(this));
    }

    @Override
    public Formula visit(@NotNull GOperator g) {
        BooleanConstant substitutedFormula = subst.apply(g);

        if (substitutedFormula != null) {
            return substitutedFormula;
        }

        return new GOperator(g.operand.accept(this));
    }

    @Override
    public Formula visit(@NotNull UOperator u) {
        return new UOperator(u.left.accept(this), u.right.accept(this));
    }

    @Override
    public Formula visit(@NotNull XOperator x) {
        return new XOperator(x.operand.accept(this));
    }
}
