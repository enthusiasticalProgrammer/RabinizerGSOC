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

import com.google.common.collect.BiMap;
import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.consumer.HOAIntermediateStoreAndManipulate;
import jhoafparser.transformations.ToStateAcceptance;
import rabinizer.automata.*;
import rabinizer.automata.output.DotPrinter;
import rabinizer.collections.valuationset.ValuationSetFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.equivalence.EquivalenceClassFactory;
import rabinizer.ltl.parser.ParseException;
import rabinizer.ltl.simplifier.Simplifier;

import java.io.IOException;
import java.util.Set;

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

        Automaton<?> automaton = computeAutomaton(arguments.inputFormula, arguments.autType, arguments.simplification, arguments.backend, arguments.optimisations, arguments.mapping);

        nonsilent("Done!");

        HOAConsumer outputPipeline = arguments.format == CLIParser.Format.DOT
                ? new DotPrinter(arguments.writer)
                : new HOAConsumerPrint(arguments.writer);

        if (arguments.autType == CLIParser.AutomatonType.SGR || arguments.autType == CLIParser.AutomatonType.SR) {
            outputPipeline = new HOAIntermediateStoreAndManipulate(outputPipeline, new ToStateAcceptance());
        }

        automaton.toHOA(outputPipeline);
        arguments.writer.close();
    }

    public static Automaton<?> computeAutomaton(Formula inputFormula, CLIParser.AutomatonType type, Simplifier.Strategy simplify, FactoryRegistry.Backend backend,
                                                Set<Optimisation> opts, BiMap<String, Integer> mapping) {
        nonsilent("Formula unsimplified: " + inputFormula);
        if (opts.contains(Optimisation.SLAVE_SUSPENSION)) {
            inputFormula = Simplifier.simplify(inputFormula, Simplifier.Strategy.PULLUP_X);
        }
        inputFormula = Simplifier.simplify(inputFormula, simplify);
        nonsilent("Formula simplified:" + inputFormula);

        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(backend, inputFormula);
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(backend, inputFormula, mapping);

        DTGRA dtgra = DTGRAFactory.constructDTGRA(inputFormula, factory, valuationSetFactory, opts);

        switch (type) {
            case SR:
            case TR:
                return new DTRA(dtgra);

            case SGR:
            case TGR:
            default:
                return dtgra;
        }
    }
}
