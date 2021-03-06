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
import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.consumer.HOAIntermediateStoreAndManipulate;
import jhoafparser.transformations.ToStateAcceptance;
import rabinizer.automata.*;
import rabinizer.exec.CLIParser.AutomatonType;
import omega_automaton.Automaton;
import omega_automaton.collections.valuationset.BDDValuationSetFactory;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import ltl.Formula;
import ltl.Literal;
import ltl.equivalence.EquivalenceClassFactory;
import ltl.simplifier.Simplifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Main {

    private static long clock2;

    public static double stopwatchLocal() {
        long start = clock2;
        clock2 = System.currentTimeMillis();
        return (clock2 - start) / 1000.0;
    }

    // Parsing arguments
    public static void main(String... args) throws IOException {

        CLIParser.CmdArguments arguments;

        try {
            arguments = CLIParser.parseArgs(args);
        } catch (ParserWrapperException e1) {
            System.err.println("the following Exception occurred during parsing: ");
            System.err.println(e1.getMessage());
            System.err.println("Rabinizer aborted");
            return;
        }
        OutputLevel.setOutputLevel(OutputLevel.getOutputLevel(arguments.outputLevel));

        OutputLevel.nonsilent("\n******************************************************************************\n"
                + "* Rabinizer 3.1.0 by Jan Kretinsky                                           *\n"
                + "******************************************************************************\n"
                + "* Translator of LTL to deterministic automata                                *\n"
                + "* Release: July 21, 2015                                                     *\n"
                + "* Thanks for reused pieces of code from previous versions of Rabinizer       *\n"
                + "* Version 1 by Andreas Gaiser                                                *\n"
                + "* Version 2 by Ruslan Ledesma-Garza                                          *\n"
                + "* Version 3 by Zuzana Komarkova and Jan Kretinsky                            *\n"
                + "* Version 3.2. by Salomon Sickert and Christopher Ziegler                    *\n"
                + "******************************************************************************");

        Automaton<?, ?> automaton = computeAutomaton(arguments.inputFormula, arguments.autType, arguments.simplification, arguments.backend, arguments.optimisations,
                arguments.mapping);

        OutputLevel.nonsilent("Done!");

        HOAConsumer outputPipeline;
        FileOutputStream ops = null;
        if (arguments.outputFile == null) {
            outputPipeline = arguments.format == CLIParser.Format.DOT
                    ? new omega_automaton.output.DotPrinter(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) : new HOAConsumerPrint(System.out);
        } else {
            ops = new FileOutputStream(arguments.outputFile);
            if (arguments.format == CLIParser.Format.DOT)
                outputPipeline = new omega_automaton.output.DotPrinter(new PrintWriter(new OutputStreamWriter(ops, StandardCharsets.UTF_8)));
            else
                outputPipeline = new HOAConsumerPrint(ops);
        }

        if (arguments.autType == CLIParser.AutomatonType.SGR || arguments.autType == CLIParser.AutomatonType.SR) {
            outputPipeline = new HOAIntermediateStoreAndManipulate(outputPipeline, new ToStateAcceptance());
        }

        automaton.toHOA(outputPipeline, arguments.mapping);
        if (ops != null) {
            ops.close();
        }
    }

    private static Automaton<?, ?> computeAutomaton(Formula inputFormula, CLIParser.AutomatonType type, Simplifier.Strategy simplify,
            ltl.equivalence.FactoryRegistry.Backend backend, Set<Optimisation> opts, BiMap<String, Integer> mapping) {

        Literal.mapping = mapping;
        OutputLevel.nonsilent("Formula unsimplified: " + inputFormula);

        inputFormula = Simplifier.simplify(inputFormula, simplify);
        OutputLevel.nonsilent("Formula simplified:" + inputFormula);

        EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(backend, inputFormula);

        ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(mapping.values().size());

        AbstractAutomatonFactory<?, ?, ?> automataFactory;

        if (type == AutomatonType.MDP) {
            automataFactory = new DTGRMAFactory(inputFormula, factory, valuationSetFactory, opts);
        } else {
            automataFactory = new DTGRAFactory(inputFormula, factory, valuationSetFactory, opts);
        }

        Product<?> dtgra = automataFactory.constructAutomaton();

        switch (type) {
            case SR:
            case TR:
                return new DTRA((ProductRabinizer) dtgra);

            case SGR:
            case TGR:
            default:
                return dtgra;
        }
    }
}
