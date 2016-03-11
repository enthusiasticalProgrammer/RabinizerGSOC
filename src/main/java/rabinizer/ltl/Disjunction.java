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

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

public final class Disjunction extends PropositionalFormula {

    public Disjunction(@NotNull Collection<? extends Formula> disjuncts) {
        super(disjuncts);
    }

    public Disjunction(@NotNull Formula... disjuncts) {
        super(disjuncts);
    }

    public Disjunction(@NotNull Stream<? extends Formula> formulaStream) {
        super(formulaStream);
    }

    @Override
    public @NotNull Formula not() {
        return new Conjunction(children.stream().map(Formula::not));
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
    protected char getOperator() {
        return '|';
    }

    @Override
    protected PropositionalFormula create(@NotNull Stream<? extends Formula> formulaStream) {
        return new Disjunction(formulaStream);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(Disjunction.class, children);
    }
}
