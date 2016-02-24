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

package rabinizer.exec;

import java.io.*;

import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.*;
import rabinizer.ltl.simplifier.Simplifier;

import java.util.Set;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import rabinizer.automata.AccAutomatonInterface;
import rabinizer.automata.DSRA;
import rabinizer.automata.DTGRA;
import rabinizer.automata.DTGRARaw;
import rabinizer.automata.DTRA;
import rabinizer.automata.Optimisation;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.parser.ParseException;


public class Main {

    public static boolean verbose;
    public static boolean silent;
    private static long clock2;

    public static void verboseln(String s) {
        if (verbose) {
            System.out.println(s);
        }
    }

    public static void nonsilent(String s) {
        if (!silent) {
            System.out.println(s);
        }
    }

    public static void errorMessageAndExit(String text) {
        System.out.println("ERROR: " + text);
        System.exit(1);
    }

    public static double stopwatchLocal() {
        long start = clock2;
        clock2 = System.currentTimeMillis();
        return (clock2 - start) / 1000.0;
    }

    public static void main(String... args) throws IOException, HOAConsumerException {
        // Parsing arguments

        CLIParser.CmdArguments arguments;

        try {
            arguments = CLIParser.parseArgs(args);
        } catch (ParseException e1) {
            return;
        }

        silent = arguments.outputLevel == 0;
        verbose = arguments.outputLevel == 2;

        Set<Optimisation> opts = arguments.optimisations;

        if (arguments.format != Format.SIZE || arguments.autType != AutomatonType.TGR) {
            opts.add(Optimisation.COMPUTE_ACC_CONDITION);
        }

        nonsilent("\n******************************************************************************\n"
                + "* Rabinizer 3.1.0 by Jan Kretinsky                                           *\n"
                + "******************************************************************************\n"
                + "* Translator of LTL to deterministic automata                                *\n"
                + "* Release: July 21, 2015                                                     *\n"
                + "* Thanks for reused pieces of code from previous versions of Rabinizer       *\n"
                + "* Version 1 by Andreas Gaiser                                                *\n"
                + "* Version 2 by Ruslan Ledesma-Garza                                          *\n"
                + "* Version 3 by Zuzana Komarkova and Jan Kretinsky                            *\n"
                + "******************************************************************************");

        AccAutomatonInterface automaton = computeAutomaton(arguments.inputFormula, arguments.autType, arguments.simplification, arguments.backend, opts);
        nonsilent("Done!");

        HOAConsumer hoa = new HOAConsumerPrint(arguments.writer);
        switch (arguments.format) {
            case HOA:
                automaton.toHOA(hoa);
                break;
            case DOT:
                automaton.toDotty(new PrintStream(arguments.writer));
                automaton.acc(new PrintStream(arguments.writer));
                break;
            case SIZE:
                arguments.writer.write(String.format("%8s", automaton.size()).getBytes());
                break;
            case SIZEACC:
                arguments.writer.write(String.format("%8s%8s", automaton.size(), automaton.pairNumber()).getBytes());
                break;
        }

        arguments.writer.close();
    }

    public static AccAutomatonInterface computeAutomaton(Formula inputFormula, AutomatonType type, Simplifier.Strategy simplify, FactoryRegistry.Backend backend,
            Set<Optimisation> opts) {
        nonsilent("Formula unsimplified: " + inputFormula);

        inputFormula = Simplifier.simplify(inputFormula, simplify);
        nonsilent("Formula simplified:" + inputFormula);

        nonsilent("Enumeration of valuations");
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(backend, inputFormula.getPropositions());
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(backend, inputFormula.getAtoms());

        DTGRARaw dtgra = new DTGRARaw(inputFormula, factory, valuationSetFactory, opts);

        switch (type) {
            case TGR:
                return new DTGRA(dtgra);

            case TR:
                return new DTRA(dtgra);

            case SR:
                return new DSRA(new DTRA(dtgra));

            default:
                errorMessageAndExit("Unsupported automaton type");
                return null;
        }
    }

    public enum AutomatonType {
        TGR, TR, SR
    }

    public enum Format {
        HOA, DOT, SIZE, SIZEACC
    }

}
