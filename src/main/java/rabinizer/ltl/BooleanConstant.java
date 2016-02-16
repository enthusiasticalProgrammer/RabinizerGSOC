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

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class BooleanConstant extends ImmutableObject implements Formula {

    public static final BooleanConstant TRUE = new BooleanConstant(true);
    public static final BooleanConstant FALSE = new BooleanConstant(false);

    public final boolean value;

    private BooleanConstant(boolean value) {
        this.value = value;
    }

    public static BooleanConstant get(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }

    @Override
    public @NotNull BooleanConstant not() {
        return value ? FALSE : TRUE;
    }

    @Override
    public @NotNull Formula evaluate(Literal literal) {
        return this;
    }

    @Override
    public Literal getAnUnguardedLiteral() {
        return null;
    }

    @Override
    public @NotNull Formula temporalStep(@NotNull Set<String> valuation) {
        return this;
    }

    @Override
    public @NotNull Set<Formula> getPropositions() {
        return new HashSet<>();
    }

    @Override
    public @NotNull Set<String> getAtoms() {
        return new HashSet<>();
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <A, B> A accept(BinaryVisitor<A, B> v, B f) {
        return v.visit(this, f);
    }

    @Override
    public <A, B, C> A accept(TripleVisitor<A, B, C> v, B f, C c) {
        return v.visit(this, f, c);
    }

    @Override
    public boolean isPureEventual() {
        return true;
    }

    @Override
    public boolean isPureUniversal() {
        return true;
    }

    @Override
    public boolean isSuspendable() {
        return true;
    }

    @Override
    public boolean equals2(ImmutableObject o) {
        BooleanConstant that = (BooleanConstant) o;
        return value == that.value;
    }

    @Override
    public @NotNull Formula unfold(boolean unfoldG) {
        return this;
    }

    @Override
    public @NotNull BooleanConstant evaluate(@NotNull Set<GOperator> Gs, @NotNull EvaluationStrategy s) {
        return this;
    }

    @Override
    public @NotNull Set<GOperator> gSubformulas() {
        return Sets.newHashSet();
    }

    @Override
    public @NotNull Set<GOperator> topmostGs() {
        return Sets.newHashSet();
    }

    @Override
    protected int hashCodeOnce() {
        return Boolean.hashCode(value);
    }
}
