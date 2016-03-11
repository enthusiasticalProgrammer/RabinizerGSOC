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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SkeletonVisitor implements Visitor<Set<Set<GOperator>>> {

    private final SkeletonApproximation strategy;

    public SkeletonVisitor() {
        this(SkeletonApproximation.BOTH);
    }

    public SkeletonVisitor(SkeletonApproximation appr) {
        strategy = appr;
    }

    @Override
    public Set<Set<GOperator>> defaultAction(@NotNull Formula formula) {
        return Collections.singleton(new HashSet<>());
    }

    @Override
    public Set<Set<GOperator>> visit(@NotNull Conjunction conjunction) {
        Set<Set<GOperator>> skeleton = Collections.singleton(new HashSet<>());

        for (Formula child : conjunction.children) {
            Set<Set<GOperator>> skeletonNext = new HashSet<>();

            for (Set<GOperator> skeletonChild : child.accept(this)) {
                for (Set<GOperator> skeletonElement : skeleton) {
                    Set<GOperator> union = new HashSet<>(skeletonChild);
                    union.addAll(skeletonElement);
                    skeletonNext.add(union);
                }
            }

            skeleton = skeletonNext;
        }

        return skeleton;
    }

    @Override
    public Set<Set<GOperator>> visit(@NotNull Disjunction disjunction) {
        Set<Set<GOperator>> skeleton = new HashSet<>();
        disjunction.children.forEach(e -> skeleton.addAll(e.accept(this)));
        if (strategy == SkeletonApproximation.LOWER_BOUND) {
            final Set<Set<Set<GOperator>>> result = Sets.powerSet(skeleton);

            Set<Set<GOperator>> finalResult = new HashSet<>();
            for (Set<Set<GOperator>> s : result) {
                if (s.isEmpty()) {
                    continue;
                }

                Set<GOperator> union = new HashSet<>();
                s.stream().forEach(union::addAll);
                finalResult.add(union);
            }
            return finalResult;
        }

        return skeleton;
    }

    @Override
    public Set<Set<GOperator>> visit(@NotNull FOperator fOperator) {
        return fOperator.operand.accept(this);
    }

    @Override
    public Set<Set<GOperator>> visit(@NotNull GOperator gOperator) {
        Set<Set<GOperator>> skeleton = new HashSet<>();

        for (Set<GOperator> element : gOperator.operand.accept(this)) {
            element.add(gOperator);
            skeleton.add(element);
        }

        return skeleton;
    }

    @Override
    public Set<Set<GOperator>> visit(@NotNull UOperator uOperator) {
        return new Disjunction(uOperator.right, new Conjunction(uOperator.right, uOperator.left)).accept(this);
    }

    @Override
    public Set<Set<GOperator>> visit(@NotNull XOperator xOperator) {
        return xOperator.operand.accept(this);
    }

    public enum SkeletonApproximation {
        LOWER_BOUND, BOTH
    }
}
