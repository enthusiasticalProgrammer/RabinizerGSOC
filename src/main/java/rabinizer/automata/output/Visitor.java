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

public interface Visitor<T extends Atom, R> {
    R visitTrue(BooleanExpression<T> b);

    R visitFalse(BooleanExpression<T> f);

    R visitNot(BooleanExpression<T> n);

    R visitOr(BooleanExpression<T> o);

    R visitAnd(BooleanExpression<T> a);

    R visitAtom(BooleanExpression<T> at);

    default R visit(BooleanExpression<T> b) {
        if (b.isNOT()) {
            return visitNot(b);
        } else if (b.isAND()) {
            return visitAnd(b);
        } else if (b.isOR()) {
            return visitOr(b);
        } else if (b.isAtom()) {
            return visitAtom(b);
        } else if (b.isTRUE()) {
            return visitTrue(b);
        } else if (b.isFALSE()) {
            return visitFalse(b);
        }
        throw new RuntimeException("never occuring case");
    }
}
