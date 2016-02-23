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

import java.util.Objects;
import java.util.Set;

public final class XOperator extends ModalOperator {

    public XOperator(Formula f) {
        super(f);
    }

    @Override
    public @NotNull Formula unfold(boolean unfoldG) {
        return this;
    }

    @Override
    public @NotNull Formula not() {
        return new XOperator(operand.not());
    }

    @Override
    public @NotNull Formula temporalStep(@NotNull Set<String> valuation) {
        return operand;
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
        return operand.isPureEventual();
    }

    @Override
    public boolean isPureUniversal() {
        return operand.isPureUniversal();
    }

    @Override
    public boolean isSuspendable() {
        return operand.isSuspendable();
    }

    @Override
    protected char getOperator() {
        return 'X';
    }

    @Override
    protected ModalOperator build(Formula operand) {
        return new XOperator(operand);
    }

    @Override
    protected int hashCodeOnce() {
        return Objects.hash(XOperator.class, operand);
    }
}
