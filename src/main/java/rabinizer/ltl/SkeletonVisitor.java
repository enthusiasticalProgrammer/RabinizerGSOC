package rabinizer.ltl;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SkeletonVisitor implements Visitor<Set<Set<GOperator>>> {

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
}
