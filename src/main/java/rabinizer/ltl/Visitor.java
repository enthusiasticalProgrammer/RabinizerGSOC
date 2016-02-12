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

public interface Visitor<R> {

    R defaultAction(@NotNull Formula formula);

    default R visit(@NotNull BooleanConstant booleanConstant) {
        return defaultAction(booleanConstant);
    }

    default R visit(@NotNull Conjunction conjunction) {
        return defaultAction(conjunction);
    }

    default R visit(@NotNull Disjunction disjunction) {
        return defaultAction(disjunction);
    }

    default R visit(@NotNull FOperator fOperator) {
        return defaultAction(fOperator);
    }

    default R visit(@NotNull GOperator gOperator) {
        return defaultAction(gOperator);
    }

    default R visit(@NotNull Literal literal) {
        return defaultAction(literal);
    }

    default R visit(@NotNull UOperator uOperator) {
        return defaultAction(uOperator);
    }

    default R visit(@NotNull XOperator xOperator) {
        return defaultAction(xOperator);
    }
}
