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

import ltl.*;
import ltl.visitors.Visitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SkeletonVisitor implements Visitor<Set<Set<UnaryModalOperator>>> {

    public SkeletonVisitor() {

    }

    @Override
    public Set<Set<UnaryModalOperator>> defaultAction(Formula formula) {
        return Collections.singleton(new HashSet<>());
    }

    @Override
    public Set<Set<UnaryModalOperator>> visit(Conjunction conjunction) {
        Set<Set<UnaryModalOperator>> skeleton = Collections.singleton(new HashSet<>());

        for (Formula child : conjunction.children) {
            Set<Set<UnaryModalOperator>> skeletonNext = new HashSet<>();

            for (Set<UnaryModalOperator> skeletonChild : child.accept(this)) {
                for (Set<UnaryModalOperator> skeletonElement : skeleton) {
                    Set<UnaryModalOperator> union = new HashSet<>(skeletonChild);
                    union.addAll(skeletonElement);
                    skeletonNext.add(union);
                }
            }

            skeleton = skeletonNext;
        }

        return skeleton;
    }

    @Override
    public Set<Set<UnaryModalOperator>> visit(Disjunction disjunction) {
        Set<Set<UnaryModalOperator>> skeleton = new HashSet<>();
        disjunction.children.forEach(e -> skeleton.addAll(e.accept(this)));
        final Set<Set<Set<UnaryModalOperator>>> result = Sets.powerSet(skeleton);

        Set<Set<UnaryModalOperator>> finalResult = new HashSet<>();
        for (Set<Set<UnaryModalOperator>> s : result) {
            if (s.isEmpty()) {
                continue;
            }

            Set<UnaryModalOperator> union = new HashSet<>();
            s.stream().forEach(union::addAll);
            finalResult.add(union);
        }
        return finalResult;
    }

    @Override
    public Set<Set<UnaryModalOperator>> visit(FOperator fOperator) {
        Set<Set<UnaryModalOperator>> result = fOperator.operand.accept(this);
        if (fOperator instanceof FOperatorForMojmir) {
            Set<UnaryModalOperator> singleton = new HashSet<>();
            singleton.add(fOperator);
            result.add(singleton);
        }
        return result;
    }

    @Override
    public Set<Set<UnaryModalOperator>> visit(GOperator gOperator) {
        Set<Set<UnaryModalOperator>> skeleton = new HashSet<>();

        for (Set<UnaryModalOperator> element : gOperator.operand.accept(this)) {
            element.add(gOperator);
            skeleton.add(element);
        }

        return skeleton;
    }

    @Override
    public Set<Set<UnaryModalOperator>> visit(UOperator uOperator) {
        return new Disjunction(uOperator.right, new Conjunction(uOperator.right, uOperator.left)).accept(this);
    }

    @Override
    public Set<Set<UnaryModalOperator>> visit(XOperator xOperator) {
        return xOperator.operand.accept(this);
    }
}
