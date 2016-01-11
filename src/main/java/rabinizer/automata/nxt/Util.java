package rabinizer.automata.nxt;

import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import rabinizer.ltl.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class Util {

    // TODO: Create Mapping class

    public static final Converter converter = new Converter();

    public static <K> int getId(Map<K, Integer> map, K key) {
        Integer r = map.get(key);

        if (r == null) {
            int id = map.size();
            map.put(key, id);
            return id;
        }

        return r;
    }

    public static class Converter implements Visitor<BooleanExpression<AtomLabel>> {
        @Override
        public BooleanExpression<AtomLabel> defaultAction(Formula f) {
            throw new IllegalArgumentException("Cannot convert " + f + " to BooleanExpression.");
        }

        @Override
        public BooleanExpression<AtomLabel> visit(BooleanConstant b) {
            return new BooleanExpression<>(b.value);
        }

        @Override
        public BooleanExpression<AtomLabel> visit(Conjunction c) {
            return c.getChildren().stream().map(e -> e.accept(this)).reduce(new BooleanExpression<>(true), (e1, e2) -> e1.and(e2));
        }

        @Override
        public BooleanExpression<AtomLabel> visit(Disjunction d) {
            return d.getChildren().stream().map(e -> e.accept(this)).reduce(new BooleanExpression<>(false), (e1, e2) -> e1.or(e2));
        }

        @Override
        public BooleanExpression<AtomLabel> visit(Literal l) {
            BooleanExpression<AtomLabel> atom = new BooleanExpression<>(AtomLabel.createAlias(l.getAtom()));

            if (l.getNegated()) {
                atom = atom.not();
            }

            return atom;
        }
    }

    public static void checkPartition(Collection<ValuationSet> set) {
        checkPartition(set, false);
    }

    public static void checkPartition(Collection<ValuationSet> set, boolean checkUniverse) {
        List<ValuationSet> list = new ArrayList<>(set);

        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < i; j++) {
                ValuationSet s = list.get(i).clone();
                s.retainAll(list.get(j));

                if (!s.isEmpty()) {
                    throw new IllegalArgumentException(list.get(i).toString() + " " + list.get(j).toString());
                }
            }
        }

        if (checkUniverse) {
            ValuationSet vs = list.get(0).clone();
            list.forEach(e -> vs.addAll(e));
            if (!vs.isUniverse()) {
                throw new IllegalArgumentException(vs.toString());
            }
        }
    }

}
