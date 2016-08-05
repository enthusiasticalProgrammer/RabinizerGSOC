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

import ltl.visitors.DefaultConverter;
import ltl.FOperator;
import ltl.Formula;
import ltl.FrequencyG;
import ltl.GOperator;

/**
 * This Visitor visits a formula and it replaces 'ordinary' F-operators by
 * FOperatorForMojmir. The motivation is that for FOperatorForMojmir, we make a
 * Mojmir-automaton and for ordinary FOperators not.
 */
public class MojmirOperatorVisitor extends DefaultConverter {

    @Override
    public FOperatorForMojmir visit(FOperator fOperator) {
        return new FOperatorForMojmir(fOperator.operand.accept(this));
    }

    @Override
    public Formula visit(GOperator gOperator) {
        if (gOperator instanceof FrequencyG) {
            FrequencyG freq = (FrequencyG) gOperator;
            return new FrequencyG(gOperator.operand.accept(this), freq.bound, freq.cmp, freq.limes);
        }
        return super.visit(gOperator);
    }
}
