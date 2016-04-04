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

package rabinizer.automata.nxt;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import rabinizer.automata.Automaton;
import rabinizer.automata.IState;
import rabinizer.automata.Optimisation;
import rabinizer.collections.Collections3;
import rabinizer.collections.valuationset.ValuationSet;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.Literal;
import rabinizer.ltl.equivalence.EquivalenceClass;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;

import javax.annotation.Nullable;
import java.util.*;

public class DetLimitSlave extends Automaton<DetLimitSlave.State> {

    protected final EquivalenceClass initialFormula;
    protected final EquivalenceClass True;
    protected final boolean eager;
    protected final boolean removeCover;

    private final LoadingCache<State, ValuationSet> acceptanceCache;

    public DetLimitSlave(EquivalenceClass formula, EquivalenceClassFactory equivalenceClassFactory, ValuationSetFactory valuationSetFactory, Collection<Optimisation> optimisations) {
        super(valuationSetFactory);
        eager = optimisations.contains(Optimisation.EAGER);
        removeCover = optimisations.contains(Optimisation.COVER);
        initialFormula = eager ? formula.unfold(true) : formula;
        True = equivalenceClassFactory.getTrue();

        CacheLoader<State, ValuationSet> acceptanceLoader = new AcceptanceCacheLoader();
        acceptanceCache = CacheBuilder.newBuilder().build(acceptanceLoader);
    }

    public ValuationSet getAcceptance(State state) {
        return acceptanceCache.getUnchecked(state);
    }

    @Override
    protected State generateInitialState() {
        return new State(initialFormula, True);
    }

    private static class AcceptanceCacheLoader extends CacheLoader<State, ValuationSet> {
        @Override
        public ValuationSet load(State arg) {
            return arg.getAcceptance();
        }
    }

    public final class State implements IState<State> {

        final EquivalenceClass current;
        final EquivalenceClass next;

        State(EquivalenceClass current, EquivalenceClass next) {
            this.current = current;
            this.next = next;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State that = (State) o;
            return Objects.equals(current, that.current) &&
                    Objects.equals(next, that.next) &&
                    Objects.equals(initialFormula, that.getInitialFormula());
        }

        @Override
        public int hashCode() {
            return Objects.hash(current, next, initialFormula);
        }

        @Override
        public String toString() {
            return "{" + current.getRepresentative() + ", " + next.getRepresentative() + '}';
        }

        @Nullable
        @Override
        public State getSuccessor(BitSet valuation) {
            EquivalenceClass successor = step(current, valuation);
            EquivalenceClass nextSuccessor = step(next, valuation);

            // We cannot recover from false. (non-accepting trap)
            if (successor.isFalse() || nextSuccessor.isFalse()) {
                return null;
            }

            // Successor is done and we can switch components.
            if (successor.isTrue()) {
                return new State(nextSuccessor.and(initialFormula), True);
            }

            if (removeCover && successor.implies(nextSuccessor)) {
                nextSuccessor = True;
            }

            if (!removeCover || !successor.implies(initialFormula)) {
                nextSuccessor = nextSuccessor.and(initialFormula);
            }

            return new State(successor, nextSuccessor);
        }

        public ValuationSet getAcceptance() {
            BitSet sensitiveLetters = new BitSet();

            for (Formula literal : current.unfold(true).getSupport()) {
                if (literal instanceof Literal) {
                    sensitiveLetters.set(((Literal) literal).getAtom());
                }
            }

            ValuationSet acceptingLetters = valuationSetFactory.createEmptyValuationSet();

            for (BitSet valuation : Collections3.powerSet(sensitiveLetters)) {
                EquivalenceClass successor = step(current, valuation);
                if (successor.isTrue()) {
                    acceptingLetters.addAll(valuationSetFactory.createValuationSet(valuation, sensitiveLetters));
                }
            }

            return acceptingLetters;
        }

        @Override
        public BitSet getSensitiveAlphabet() {
            BitSet sensitiveLetters = new BitSet();

            current.unfold(true).getSupport().forEach(f -> {
                if (f instanceof Literal) {
                    sensitiveLetters.set(((Literal) f).getAtom());
                }
            });

            next.unfold(true).getSupport().forEach(f -> {
                if (f instanceof Literal) {
                    sensitiveLetters.set(((Literal) f).getAtom());
                }
            });

            return sensitiveLetters;
        }

        @Override
        public ValuationSetFactory getFactory() {
            return valuationSetFactory;
        }

        private EquivalenceClass getInitialFormula() {
            return initialFormula;
        }

        private EquivalenceClass step(EquivalenceClass clazz, BitSet valuation) {
            if (eager) {
                return clazz.temporalStep(valuation).unfold(true);
            } else {
                return clazz.unfold(true).temporalStep(valuation);
            }
        }
    }
}
