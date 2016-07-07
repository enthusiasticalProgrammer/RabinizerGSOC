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

import com.google.common.collect.Sets;

import ltl.Conjunction;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.Formula;
import ltl.GOperator;
import ltl.ModalOperator;
import ltl.UOperator;
import ltl.Visitor;
import ltl.XOperator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SkeletonVisitor implements Visitor<Set<Set<ModalOperator>>> {

    public SkeletonVisitor() {

    }

    @Override
    public Set<Set<ModalOperator>> defaultAction(Formula formula) {
        return Collections.singleton(new HashSet<>());
    }

    @Override
    public Set<Set<ModalOperator>> visit(Conjunction conjunction) {
        Set<Set<ModalOperator>> skeleton = Collections.singleton(new HashSet<>());

        for (Formula child : conjunction.children) {
            Set<Set<ModalOperator>> skeletonNext = new HashSet<>();

            for (Set<ModalOperator> skeletonChild : child.accept(this)) {
                for (Set<ModalOperator> skeletonElement : skeleton) {
                    Set<ModalOperator> union = new HashSet<>(skeletonChild);
                    union.addAll(skeletonElement);
                    skeletonNext.add(union);
                }
            }

            skeleton = skeletonNext;
        }

        return skeleton;
    }

    @Override
    public Set<Set<ModalOperator>> visit(Disjunction disjunction) {
        Set<Set<ModalOperator>> skeleton = new HashSet<>();
        disjunction.children.forEach(e -> skeleton.addAll(e.accept(this)));
        final Set<Set<Set<ModalOperator>>> result = Sets.powerSet(skeleton);

        Set<Set<ModalOperator>> finalResult = new HashSet<>();
        for (Set<Set<ModalOperator>> s : result) {
            if (s.isEmpty()) {
                continue;
            }

            Set<ModalOperator> union = new HashSet<>();
            s.stream().forEach(union::addAll);
            finalResult.add(union);
        }
        return finalResult;
    }

    @Override
    public Set<Set<ModalOperator>> visit(FOperator fOperator) {
        Set<Set<ModalOperator>> result = fOperator.operand.accept(this);
        if (fOperator instanceof FOperatorForMojmir) {
            Set<ModalOperator> singleton = new HashSet<>();
            singleton.add(fOperator);
            result.add(singleton);
        }
        return result;
    }

    @Override
    public Set<Set<ModalOperator>> visit(GOperator gOperator) {
        Set<Set<ModalOperator>> skeleton = new HashSet<>();

        for (Set<ModalOperator> element : gOperator.operand.accept(this)) {
            element.add(gOperator);
            skeleton.add(element);
        }

        return skeleton;
    }

    @Override
    public Set<Set<ModalOperator>> visit(UOperator uOperator) {
        return new Disjunction(uOperator.right, new Conjunction(uOperator.right, uOperator.left)).accept(this);
    }

    @Override
    public Set<Set<ModalOperator>> visit(XOperator xOperator) {
        return xOperator.operand.accept(this);
    }
}
