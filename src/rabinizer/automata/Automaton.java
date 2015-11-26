/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rabinizer.automata;

import rabinizer.bdd.ValuationSet;
import rabinizer.bdd.Valuation;
import java.util.*;
import rabinizer.bdd.BDDForVariables;
import rabinizer.bdd.MyBDD;
import rabinizer.bdd.ValuationSetBDD;
import rabinizer.exec.Main;
import rabinizer.exec.Tuple;

/**
 *
 * @author jkretinsky
 * @param <State>
 */
public abstract class Automaton<State> /*implements AccAutomatonInterface*/ {

    public Set<State> states;
    public Map<State, HashMap<ValuationSet, State>> transitions;
    public State initialState;
    public Set<State> sinks;
    //AccCondition accCondition;
    public Map<Tuple<State, State>, ValuationSet> edgeBetween;
    public Map<State, Integer> statesToNumbers = null;

    public Automaton() {
        states = new HashSet<State>();
        transitions = new HashMap<State, HashMap<ValuationSet, State>>();
        initialState = null;
        sinks = new HashSet<State>();
        //stateLabels = new HashMap();
        edgeBetween = new HashMap<Tuple<State, State>, ValuationSet>();
        statesToNumbers = new HashMap<State, Integer>();
        //this.generate();
    }
    
    public Automaton(Automaton<State> a) {
        states = a.states;
        transitions = a.transitions;
        initialState = a.initialState;
        sinks = a.sinks;
        edgeBetween = a.edgeBetween;
        statesToNumbers = a.statesToNumbers;
    }

    protected abstract State generateInitialState();

    protected abstract State generateSuccState(State s, ValuationSet vs);

    protected abstract Set<ValuationSet> generateSuccTransitions(State s);

    public Automaton<State> generate() {
        initialState = generateInitialState();;
        states.add(initialState);
        //stateLabels.put(initialState, init.right);
        statesToNumbers.put(initialState, 0);
        Main.nonsilent("  Generating automaton for " + initialState);

        Stack<State> workstack = new Stack<State>();
        State curr = initialState;
        workstack.push(curr);

        while (!workstack.empty()) {
            curr = workstack.pop();
            Main.verboseln("\tCurrState: " + curr);
            Set<ValuationSet> succValSets = generateSuccTransitions(curr);
            Main.verboseln("\t  CurrTrans: " + succValSets);
            //if (succValSets.isEmpty()) {  // TODO empty or true and non-progress or just selfloops?
            //    sinks.add(curr);
            //} else {
            transitions.put(curr, new HashMap<ValuationSet, State>());
            for (ValuationSet succVals : succValSets) {
                //Tuple<State, String> succStateLabel = generateSuccState(curr, succVals);
                State succ = generateSuccState(curr, succVals);
                Main.verboseln("\t  SuccState: " + succ);
                if (!states.contains(succ)) {
                    states.add(succ);
                    statesToNumbers.put(succ, statesToNumbers.size());
                    //stateLabels.put(succ, succStateLabel.right);
                    workstack.push(succ);
                }
                Tuple<State, State> statePair = new Tuple<State, State>(curr, succ);
                ValuationSet newVals;
                if (edgeBetween.containsKey(statePair)) {  // update edge
                    ValuationSet oldVals = edgeBetween.get(statePair);
                    newVals = succVals.or(oldVals);
                    transitions.get(curr).remove(oldVals);
                    edgeBetween.remove(statePair, oldVals);
                } else {// new edge
                    newVals = succVals;
                }
                edgeBetween.put(statePair, newVals);
                transitions.get(curr).put(newVals, succ);
            }
            //}
        }
        Main.nonsilent("  Number of states: " + states.size());
        //Misc.verboseln("\tStates: " + states);
        //Misc.verboseln("\tEdges:  " + transitions);
        return this;
    }

    public Automaton<State> useSinks() {
        for (State s : states) {
            Tuple<State, State> selfloop = new Tuple<State, State>(s, s);
            if (edgeBetween.containsKey(selfloop) && edgeBetween.get(selfloop).isAllVals() && !s.equals(initialState)) {
                //(transitions.get(s).size()==1) && (transitions.get(s).values().contains(s))
                sinks.add(s);
                edgeBetween.remove(selfloop);
                transitions.put(s, new HashMap<ValuationSet,State>());
            }
        }
        return this;
    }

    public State succ(State s, Valuation v) {
        for (ValuationSet vs : transitions.get(s).keySet()) {
            if (vs.contains(v)) {
                return transitions.get(s).get(vs);
            }
        }
        return null;
    }

    /*
     protected String nameOfState(State s) {
     return stateLabels.get(s);
     }
     */
    // TODO to abstract ProductAutomaton ?
    protected static Set<ValuationSet> generatePartitioning(Set<Set<ValuationSet>> product) {
        Set<ValuationSet> partitioning = new HashSet<ValuationSet>();
        partitioning.add(new ValuationSetBDD(BDDForVariables.getTrueBDD()));
        for (Set<ValuationSet> vSets : product) {
            Set<ValuationSet> partitioningNew = new HashSet<ValuationSet>();
            for (ValuationSet vSet : vSets) {
                for (ValuationSet vSetOld : partitioning) {
                    partitioningNew.add(vSetOld.and(vSet));
                }
            }
            partitioning = partitioningNew;
        }
        partitioning.remove(new ValuationSetBDD(BDDForVariables.getFalseBDD()));
        return partitioning;
    }

    public int size() {
        return states.size();
    }

    public String toDotty() {
        String r = "digraph \"Automaton for " + initialState + "\" \n{\n";
        for (State s : states) {
            /*if (finalStates.contains(s)) {
             r += "node [shape=Msquare, label=\"" + displayLabels.get(s) + "\"]\"" + displayLabels.get(s) + "\";\n";
             } else*/
            if (s == initialState) {
                r += "node [shape=oval, label=\"" + s + "\"]\"" + s + "\";\n";
            } else {
                r += "node [shape=rectangle, label=\"" + s + "\"]\"" + s + "\";\n";
            }
        }
        for (State s : transitions.keySet()) {
            for (Map.Entry<ValuationSet, State> edge : transitions.get(s).entrySet()) {
                r += "\"" + s + "\" -> \"" + edge.getValue()
                    + "\" [label=\"" + edge.getKey() + "\"];\n";
            }
        }
        return r + "}";
    }

    public String toHOA() {
        String dot = "";
        dot += "HOA: v1\n";
        dot += "tool: \"Rabinizer\" \"3.1\"\n";
        dot += "name: \"Automaton for " + initialState + "\"\n";
        dot += "properties: deterministic\n";
        dot += "properties: complete\n";
        dot += "States: " + states.size() + "\n";
        dot += "Start: " + statesToNumbers.get(initialState) + "\n";
        dot += accName();
        dot += "Acceptance: " + accTypeNumerical() + "\n"; //TODO: handle trivial sets
        dot += "AP: " + BDDForVariables.bijectionIdAtom.size();
        for (Integer i = 0; i < BDDForVariables.bijectionIdAtom.size(); i++) {
            dot += " \"" + BDDForVariables.bijectionIdAtom.atom(i) + "\"";
        }
        dot += "\n";
        dot += "--BODY--\n";

        // Map<Tuple<Formula, KState>, Tuple<Formula, KState>> normalStates = new HashMap<Tuple<Formula, KState>, Tuple<Formula, KState>>();        
        for (State s : states) {
            dot += "State: " + statesToNumbers.get(s) + " \"" + s + "\" " + stateAcc(s) + "\n";
            dot += outTransToHOA(s);
        }
        return dot + "--END--\n";

    }

    public String acc() {
        return "";
    }

    protected String accName() {
        return "";
    }

    protected String accTypeNumerical() {
        return "";
    }

    protected String stateAcc(State s) {
        return "";
    }

    protected String outTransToHOA(State s) {
        String result = "";
        for (ValuationSet vs : transitions.get(s).keySet()) {
            result += "[" + (new MyBDD(vs.toBdd(),true)).BDDtoNumericString() + "] " 
                + statesToNumbers.get(transitions.get(s).get(vs)) + "\n";
        }
        return result;
    }
    
    


}
