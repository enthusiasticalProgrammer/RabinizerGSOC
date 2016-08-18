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
import ltl.visitors.RestrictToFGXU;
import ltl.visitors.predicates.ContainsPredicate;

import org.apache.commons.cli.*;
import rabinizer.automata.Optimisation;
import rabinizer.frequencyLTL.MojmirOperatorVisitor;
import rabinizer.frequencyLTL.TopMostOperatorVisitor;
import ltl.Formula;
import ltl.FrequencyG;
import ltl.GOperator;
import ltl.UOperator;
import ltl.UnaryModalOperator;
import ltl.equivalence.FactoryRegistry;
import ltl.parser.Parser;
import ltl.parser.ParseException;
import ltl.simplifier.Simplifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

class CLIParser {

    private static final Options opts = makeOptions();

    private static Options makeOptions() {
        Options result = new Options();
        result.addOption("h", "help", false, "prints this help and exits.");
        result.addOption("o", "output-level", true,
                "Here you can give the output level. Possible values are 0 (for silent), 1 (default, print only important messages), and 2 (for verbose)");
        result.addOption("a", "automatonType", true,
                "This option determines the acceptance type of the output automaton. Possible values are tgr (default) for transition based generalized Rabin acceptance, tr for transition based Rabin acceptance, sgr for state based generalized Rabin acceptance, and sr for state based Rabin acceptance.");
        result.addOption("m", "format", true, "The format in which the automaton is stored. Possible values are hoa (default), dot for the dot syntax.");
        result.addOption("p", "optimisations", true, "This option defines, if optimisations are to be used or not. Possible values are on and off. The default is on");
        result.addOption("e", "eager", false, "This option defines, if eager unfolding is done. Per default it is on if the optimisation flag is on.");
        result.addOption("s", "skeleton", false,
                "This option defines, if only the acceptance condition of the skeleton of the G-formulae is used. This reduces the amount of acceptance conditions. Per default it is on if the optimisation flag is on. It is recommended to use it only for a simplify-formula level of at least 1.");
        result.addOption("r", "relevant-slaves-only", false,
                "This option defines, if only the relevant slaves of a state are to be computed. Per default it is on if the optimisation flag is on.");
        result.addOption("i", "optimise-initial-state", false,
                "This option defines, if the initial state of the Rabin-slaves is to be optimised. Per default it is on if the optimisation flag is set to on.");
        result.addOption("t", "emptiness-check", false,
                "This option defines, if at the end of the construction, and emptiness-check is done and all nonaccepting SCCs are removed, and some acceptance conditions are diminished. Per default it is enabled if the optimisation flag is set to on.");
        result.addOption("y", "simplify-formula", true,
                "This defines the level of simplification which is used on the formula. Possible values are 0 for only propositional simplification, 1 for modal simplification, and 2 for aggressive simplification. The default is 0, if optimisation is off , and 2 if optimisation is on.");
        result.addOption("u", "output-file", true, "The name of the file, in which the automaton has to be printed. Per default the automaton gets printed on the terminal");
        result.addOption("n", "input-file", true, "The name of the file, in which the input formula is written");
        result.addOption("f", "formula", true,
                "The input formula in spot-syntax. We can recognize the modal operators F,G,U,V,W,R, and propositional negations, ands, and ors. Either use the formula option or the input-file option");
        result.addOption("z", "acc-condition", false, "This flag prohibits computing the acceptance condition. It can be used for benchmarking.");
        result.addOption("U", "use-controller-syntheses-for-MDP-algorithm", false,
                "This flag should be used if it is desired to employ the algorithm called controller synthesis for MDPs and Frequency LTL\\GU");
        return result;
    }

    static CmdArguments parseArgs(String... args) throws ParserWrapperException {
        CommandLineParser lvParser = new DefaultParser();
        CommandLine cmd;

        int outputLevel = 1;
        AutomatonType autType = AutomatonType.TGR;
        Format format = Format.HOA;

        Set<Optimisation> optimisations = EnumSet.allOf(Optimisation.class);

        Simplifier.Strategy simplification = Simplifier.Strategy.NONE;
        File outputFile = null;
        Formula inputFormula;

        try {
            cmd = lvParser.parse(opts, args);
        } catch (org.apache.commons.cli.ParseException pvException) {
            throw new ParserWrapperException(pvException);
        }

        if (cmd.hasOption('h')) {
            printHelp();
        }

        adjustOptimisations(cmd, optimisations);

        simplification = obtainSimplificationLevel(cmd);

        autType = getAutomatonType(cmd);

        outputLevel = getOutputLevel(cmd);

        format = getOutputFormat(cmd);

        if (cmd.hasOption('u')) {
            String file = cmd.getOptionValue('u');
            outputFile = new File(file);
        }

        Parser parser = getParser(cmd);
        try {
            inputFormula = parser.formula().accept(new RestrictToFGXU());
        } catch (ParseException e) {
            throw new ParserWrapperException(e);
        }

        if (cmd.hasOption('z')) {
            optimisations.remove(Optimisation.COMPUTE_ACC_CONDITION);
        } else {
            optimisations.add(Optimisation.COMPUTE_ACC_CONDITION);
        }

        if (cmd.hasOption('U')) {
            // prepare for controller synthesis

            optimisations.retainAll(Collections.singleton(Optimisation.COMPUTE_ACC_CONDITION));
            simplification = Simplifier.Strategy.NONE;
            if (outputLevel != 0) {
                System.err.println("Warning: Optimisations and simplification have been disabled.");
            }
            inputFormula = inputFormula.accept(new MojmirOperatorVisitor());
            Set<UnaryModalOperator> gSubformulae = inputFormula.accept(new TopMostOperatorVisitor());
            if (gSubformulae.stream().filter(op -> op instanceof GOperator).anyMatch(g -> g.accept(new ContainsPredicate(UOperator.class)))) {
                throw new ParserWrapperException("The controller synthesis construction works only for fLTL\\GU."
                        + "If your formula contains no frequency-G, then maybe you want to drop the -U option to use the Rabinizer construction, which can cope with LTL");
            }

        } else {
            if (inputFormula.accept(new ContainsPredicate(FrequencyG.class))) {
                throw new ParserWrapperException("The Rabinizer-construction does not work with FrequencyG operator. "
                        + "Maybe you want to use the -U option to do the Controller synthesis construction for fLTL\\GU");
            }
        }

        return new CmdArguments(outputLevel, autType, format, optimisations, simplification, outputFile, inputFormula, FactoryRegistry.Backend.BDD, parser.map);
    }

    private static Parser getParser(CommandLine cmd) throws ParserWrapperException {
        Parser parser;
        if (cmd.hasOption('f') && !cmd.hasOption('n')) {
            parser = new Parser(new StringReader(cmd.getOptionValue('f')));
        } else if (!cmd.hasOption('f') && cmd.hasOption('n')) {
            try (InputStream in = new FileInputStream(new File(cmd.getOptionValue('n')));
                    Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                    BufferedReader bReader = new BufferedReader(reader)) {
                String form = bReader.readLine();
                if (form == null) {
                    throw new ParserWrapperException("Error: input file contains no line");
                }
                parser = new Parser(new StringReader(form));
            } catch (FileNotFoundException e) {
                throw new ParserWrapperException(e);
            } catch (IOException e) {
                throw new ParserWrapperException(e);
            }
        } else {
            System.out.println(
                    "Error: either you gave me a formula in a file and one via command line and I don't know which one to choose, or you gave me neither nor. Maybe you want to take a look at the --help options below");
            printHelp();
            throw new ParserWrapperException();
        }
        return parser;
    }

    private static void adjustOptimisations(CommandLine cmd, Set<Optimisation> optimisations) throws ParserWrapperException {

        if (cmd.hasOption('p')) {
            switch (cmd.getOptionValue('p')) {
                case "off":
                    optimisations.clear();
                    break;
                case "on":
                    break;
                default:
                    System.out.println("wrong optimisations-argument. Look at the help printed below.");
                    printHelp();
                    throw new ParserWrapperException();
            }
        }

        if (cmd.hasOption('e')) {
            optimisations.add(Optimisation.EAGER);
        }

        if (cmd.hasOption('s')) {
            optimisations.add(Optimisation.SKELETON);
        }

        if (cmd.hasOption('r')) {
            optimisations.add(Optimisation.ONLY_RELEVANT_SLAVES);
        }

        if (cmd.hasOption('i')) {
            optimisations.add(Optimisation.OPTIMISE_INITIAL_STATE);
        }

        if (cmd.hasOption('t')) {
            optimisations.add(Optimisation.EMPTINESS_CHECK);
        }
    }

    private static Format getOutputFormat(CommandLine cmd) throws ParserWrapperException {
        if (cmd.hasOption('m')) {
            String m = cmd.getOptionValue('m');
            switch (m) {
                case "dot":
                    return Format.DOT;
                case "hoa":
                    return Format.HOA;
                default:
                    System.out.println("wrong format-argument. Look at the help printed below.");
                    printHelp();
                    throw new ParserWrapperException();
            }
        }
        return Format.HOA;
    }

    private static Simplifier.Strategy obtainSimplificationLevel(CommandLine cmd) throws ParserWrapperException {
        Simplifier.Strategy defaultStrategy;
        if (!cmd.hasOption('p') || "on".equals(cmd.getOptionValue('p'))) {
            defaultStrategy = Simplifier.Strategy.AGGRESSIVELY;
        } else {
            defaultStrategy = Simplifier.Strategy.NONE;
        }

        if (cmd.hasOption('y')) {
            try {
                switch (Integer.parseInt(cmd.getOptionValue('y'))) {
                    case 0:
                        return Simplifier.Strategy.NONE;
                    case 1:
                        return Simplifier.Strategy.MODAL;
                    case 2:
                        return Simplifier.Strategy.AGGRESSIVELY;
                    default:
                        System.out.println("Wrong number for simplify-formula option. Look at the help printed below.");
                        printHelp();
                        throw new ParserWrapperException();
                }
            } catch (NumberFormatException e) {
                System.out.println("Wrong format for simplify-formula option. Look at the help printed below.");
                printHelp();
                throw new ParserWrapperException(e);
            }
        }
        return defaultStrategy;
    }

    private static int getOutputLevel(CommandLine cmd) throws ParserWrapperException {
        int result = 0;
        if (cmd.hasOption('o')) {
            try {
                result = Integer.parseInt(cmd.getOptionValue('o'));
            } catch (NumberFormatException e) {
                System.out.println("Wrong format for output-level option. Look at the help printed below.");
                printHelp();
                throw new ParserWrapperException(e);
            }
            if (result < 0 || result > 2) {
                System.out.println("Wrong number for output-level option. Look at the help printed below.");
                printHelp();
                throw new ParserWrapperException();
            }
        }
        return result;
    }

    private static AutomatonType getAutomatonType(CommandLine cmd) throws ParserWrapperException {
        if (cmd.hasOption('a') && cmd.hasOption('U')) {
            throw new ParserWrapperException("The 'U' and 'a' option do not go well with each other");
        }
        if (cmd.hasOption('a')) {
            String s = cmd.getOptionValue('a');
            switch (s) {
                case "tr":
                    return AutomatonType.TR;
                case "sr":
                    return AutomatonType.SR;
                default:
                    System.out.println("Wrong Automaton Type. Look at the help printed below.");
                    printHelp();
                    throw new ParserWrapperException();
            }
        } else if (cmd.hasOption('U')) {
            return AutomatonType.MDP;
        }
        return AutomatonType.TGR;
    }

    private static void printHelp() {
        HelpFormatter helper = new HelpFormatter();
        helper.printHelp("CLIParser", opts);
    }

    public enum AutomatonType {
        TGR, TR, SGR, SR, MDP
    }

    public enum Format {
        HOA, DOT
    }

    static final class CmdArguments {
        // 0: silent, 1: neither silent nor verbose, 2: verbose
        final int outputLevel;
        final AutomatonType autType;
        final Format format;
        final Set<Optimisation> optimisations;
        final Simplifier.Strategy simplification;
        final File outputFile;
        final Formula inputFormula;
        final FactoryRegistry.Backend backend;
        final BiMap<String, Integer> mapping;

        private CmdArguments(int outputLevel, AutomatonType autType, Format format, Set<Optimisation> optimisations, Simplifier.Strategy strat, File outputFile,
                Formula inputFormula, FactoryRegistry.Backend backend, BiMap<String, Integer> mapping) {
            this.outputLevel = outputLevel;
            this.autType = autType;
            this.format = format;
            this.optimisations = optimisations;
            this.simplification = strat;
            this.outputFile = outputFile;
            this.inputFormula = inputFormula;
            this.backend = backend;
            this.mapping = mapping;
        }
    }

}
