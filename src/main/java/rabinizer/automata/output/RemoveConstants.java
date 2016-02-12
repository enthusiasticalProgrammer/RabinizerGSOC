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

package rabinizer.automata.output;

import jhoafparser.ast.Atom;
import jhoafparser.ast.BooleanExpression;

public class RemoveConstants<T extends Atom> implements Visitor<T, BooleanExpression<T>> {

    @Override
    public BooleanExpression<T> visitTrue(BooleanExpression<T> b) {
        return b;
    }

    @Override
    public BooleanExpression<T> visitFalse(BooleanExpression<T> f) {
        return f;
    }

    @Override
    public BooleanExpression<T> visitNot(BooleanExpression<T> n) {
        throw new UnsupportedOperationException();

    }

    @Override
    public BooleanExpression<T> visitOr(BooleanExpression<T> o) {
        BooleanExpression<T> l = visit(o.getLeft());
        BooleanExpression<T> r = visit(o.getRight());
        if (l.isTRUE() || r.isTRUE()) {
            return new BooleanExpression<>(BooleanExpression.Type.EXP_TRUE, null, null);
        } else if (l.isFALSE()) {
            return r;
        } else if (r.isFALSE()) {
            return l;
        } else {
            return new BooleanExpression<>(BooleanExpression.Type.EXP_OR, l, r);
        }
    }

    @Override
    public BooleanExpression<T> visitAnd(BooleanExpression<T> a) {
        BooleanExpression<T> l = visit(a.getLeft());
        BooleanExpression<T> r = visit(a.getRight());
        if (l.isFALSE() || r.isFALSE()) {
            return new BooleanExpression<>(BooleanExpression.Type.EXP_FALSE, null, null);
        } else if (l.isTRUE()) {
            return r;
        } else if (r.isTRUE()) {
            return l;
        } else {
            return new BooleanExpression<>(BooleanExpression.Type.EXP_AND, l, r);
        }
    }

    @Override
    public BooleanExpression<T> visitAtom(BooleanExpression<T> at) {
        return at;
    }

}
