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

import org.apache.commons.cli.*;
import rabinizer.automata.Optimisation;
import rabinizer.ltl.Formula;
import rabinizer.ltl.parser.LTLParser;
import rabinizer.ltl.parser.ParseException;
import rabinizer.ltl.simplifier.Simplifier;

import java.io.*;
import java.util.EnumSet;
import java.util.Set;

public class CLIParser {

    public static final Options opts = makeOptions();

    public static Options makeOptions() {
        Options result = new Options();
        result.addOption("h", "help", false, "prints this help and exits.");
        result.addOption("o", "output-level", true,
                "Here you can give the output level. Possible values are 0 (for silent), 1 (default, print only important messages), and 2 (for verbose)");
        result.addOption("a", "automatonType", true,
                "This option determines the acceptance type of the output automaton. Possible values are tgr (default) for transition based generalized Rabin acceptance, tr for transition based Rabin acceptance, sgr for state based generalized Rabin acceptance, and sr for state based Rabin acceptance.");
        result.addOption("m", "format", true,
                "The format in which the automaton is stored. Possible values are hoa (default), dot for the dot syntax.");
        result.addOption("p", "optimisations", true,
                "This option defines, if optimisations are to be used or not. Possible values are on, off, and experimental (meaning that the experimental slave-suspension is used and all other optimisations too). The default is on");
        result.addOption("e", "eager", false,
                "This option defines, if eager unfolding is done. Per default it is on if the optimisation flag is on.");
        result.addOption("s", "skeleton", false,
                "This option defines, if only the acceptance condition of the skeleton of the G-formulae is used. This reduces the amount of acceptance conditions. Per default it is on if the optimisation flag is on. It is recommended to use it only for a simplify-formula level of at least 1.");
        result.addOption("r", "relevant-slaves-only", false,
                "This option defines, if only the relevant slaves of a state are to be computed. Per default it is on if the optimisation flag is on.");
        result.addOption("i", "optimise-initial-state", false,
                "This option defines, if the initial state of the Rabin-slaves is to be optimised. Per default it is on if the optimisation flag is set to on.");
        result.addOption("t", "emptiness-check", false,
                "This option defines, if at the end of the construction, and emptiness-check is done and all nonaccepting SCCs are removed, and some acceptance conditions are diminished. Per default it is enabled if the optimisation flag is set to on.");
        result.addOption("l", "slave-suspension", false,
                "This option defines, if the slaves can wait until they are useful. This feature is yet experimental and may produce errors (in around 1 out of 1000 formulae).Per default it is on if the optimisation flag is experimental. This requires a simplify-formula level of 2.");
        result.addOption("y", "simplify-formula", true,
                "This defines the level of simplification which is used on the formula. Possible values are 0 for only propositional simplification, 1 for modal simplification, and 2 for aggressive simplification. The default is 0, if optimisation is off , and 2 if optimisation is on.");
        result.addOption("u", "output-file", true, "The name of the file, in which the automaton has to be printed. Per default the automaton gets printed on the terminal");
        result.addOption("n", "input-file", true, "The name of the file, in which the input formula is written");
        result.addOption("f", "formula", true,
                "The input formula in spot-syntax. We can recognize the modal operators F,G,U,V,W,R, and propositional negations, ands, and ors. Either use the formula option or the input-file option");
        result.addOption("b", "backend", true,
                "The backend for computing state-space, and transition labels. Possible values are bdd, which is default, and z3, which is to a great extend not recommended, since it uses way too much memory (even for formulae with a syntax tree of size around 20, it can use around 50Gib!). If you want to use z3, please make sure, that you can forcefully restart your PC without losing some of your work in progress (no open files for example).");
        result.addOption("z", "acc-condition", false, "This flag prohibits computing the acceptance condition. It can be used for benchmarking.");
        return result;
    }

    public static CmdArguments parseArgs(String... args) throws ParseException {
        CommandLineParser lvParser = new DefaultParser();
        CommandLine cmd;

        int outputLevel = 1;
        AutomatonType autType = AutomatonType.TGR;
        Format format = Format.HOA;

        Set<Optimisation> optimisations = EnumSet.allOf(Optimisation.class);
        optimisations.remove(Optimisation.SLAVE_SUSPENSION);

        Simplifier.Strategy simplification = Simplifier.Strategy.NONE;
        OutputStream writer = System.out;
        Formula inputFormula;
        FactoryRegistry.Backend backend = FactoryRegistry.Backend.BDD;

        try {
            cmd = lvParser.parse(opts, args);
        } catch (org.apache.commons.cli.ParseException pvException) {
            System.out.println(pvException.getMessage());
            printHelp();
            throw new rabinizer.ltl.parser.ParseException();
        }

        if (cmd.hasOption('p') && cmd.getOptionValue('p').equals("off")) {

        } else {
            simplification = Simplifier.Strategy.AGGRESSIVELY;
        }

        if (cmd.hasOption('p')) {
            switch (cmd.getOptionValue('p')) {
                case "off":
                    optimisations.clear();
                    break;
                case "on":
                    break;
                case "experimental":
                    optimisations.add(Optimisation.SLAVE_SUSPENSION);
                    break;
                default:
                    System.out.println("wrong optimisations-argument. Look at the help printed below.");
                    printHelp();
                    throw new ParseException();
            }
        }

        if (cmd.hasOption('h')) {
            printHelp();
            System.exit(39);
        }

        if (cmd.hasOption('a')) {
            String s = cmd.getOptionValue('a');
            switch (s) {
                case "tgr":
                    autType = AutomatonType.TGR;
                    break;
                case "tr":
                    autType = AutomatonType.TR;
                    break;
                case "sr":
                    autType = AutomatonType.SR;
                    break;
                default:
                    System.out.println("Wrong Automaton Type. Look at the help printed below.");
                    printHelp();
                    throw new ParseException();
            }
        }

        if (cmd.hasOption('o')) {
            try {
                outputLevel = Integer.parseInt(cmd.getOptionValue('o'));
            } catch (NumberFormatException e) {
                System.out.println("Wrong format for output-level option. Look at the help printed below.");
                printHelp();
                throw new ParseException("");
            }
            if (outputLevel < 0 || outputLevel > 2) {
                System.out.println("Wrong number for output-level option. Look at the help printed below.");
                printHelp();
                throw new ParseException();
            }
        }

        if (cmd.hasOption('m')) {
            String m = cmd.getOptionValue('m');
            switch (m) {
                case "dot":
                    format = Format.DOT;
                    break;
                case "hoa":
                    format = Format.HOA;
                    break;
                default:
                    System.out.println("wrong format-argument. Look at the help printed below.");
                    printHelp();
                    throw new ParseException();
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

        if (cmd.hasOption('l')) {
            optimisations.add(Optimisation.SLAVE_SUSPENSION);
        }

        if (cmd.hasOption('i')) {
            optimisations.add(Optimisation.OPTIMISE_INITIAL_STATE);
        }

        if (cmd.hasOption('t')) {
            optimisations.add(Optimisation.EMPTINESS_CHECK);
        }

        if (cmd.hasOption('y')) {
            try {
                switch (Integer.parseInt(cmd.getOptionValue('y'))) {
                    case 0:
                        if (optimisations.contains(Optimisation.SLAVE_SUSPENSION)) {
                            System.out.println("You cannot slave suspension at a simplify-formula-level below 2.");
                            throw new ParseException();
                        } else if (optimisations.contains(Optimisation.SKELETON) && outputLevel != 0) {
                            System.out.println(
                                    "It is rather bad to use skeleton together with a simplify-formula level below 1. You can continue, but don't be astonished if an exception is raised during the computation.");
                        }

                        break;

                    case 1:
                        if (optimisations.contains(Optimisation.SLAVE_SUSPENSION)) {
                            System.out.println("You cannot slave suspension at a simplify-formula-level below 2.");
                            throw new ParseException();
                        }
                        simplification = Simplifier.Strategy.MODAL;
                        break;

                    case 2:
                        simplification = Simplifier.Strategy.AGGRESSIVELY;
                        break;

                    default:
                        System.out.println("Wrong number for simplify-formula option. Look at the help printed below.");
                        printHelp();
                        throw new ParseException();
                }
            } catch (NumberFormatException e) {
                System.out.println("Wrong format for simplify-formula option. Look at the help printed below.");
                printHelp();
                throw new ParseException();
            }
        }

        if (cmd.hasOption('u')) {
            String file = cmd.getOptionValue('u');
            try {
                writer = new FileOutputStream(new File(file));
            } catch (IOException e) {
                System.out.println("The following error occurred when opening the output file.");
                e.printStackTrace();
                throw new ParseException();
            }
        }

        if (cmd.hasOption('f') && !cmd.hasOption('n')) {
            LTLParser parser = new LTLParser(new StringReader(cmd.getOptionValue('f')));

            try {
                inputFormula = parser.parse();
            } catch (ParseException e) {
                System.out.println("An error occurred while parsing the formula.");
                throw new ParseException();
            }
        } else if (!cmd.hasOption('f') && cmd.hasOption('n')) {
            try (BufferedReader bReader = new BufferedReader(new FileReader(new File(cmd.getOptionValue('n'))))) {
                String form = bReader.readLine();
                LTLParser parser = new LTLParser(new StringReader(form));
                inputFormula = parser.parse();
            } catch (FileNotFoundException e) {
                System.out.println("Error: The input file has not been found.");
                throw new ParseException();
            } catch (IOException e) {
                System.out.println("the follwing IOException occurred.");
                e.printStackTrace();
                throw new ParseException();
            } catch (ParseException e) {
                System.out.println("An error occurred while parsing the formula.");
                throw new ParseException();
            }
        } else {
            System.out.println(
                    "Error: either you gave me a formula in a file and one via command line and I don't know which one to choose, or you gave me neither nor. Maybe you want to take a look at the --help options below");
            throw new ParseException();
        }

        if (cmd.hasOption('b')) {
            if (cmd.getOptionValue('b').equals("z3")) {
                backend = FactoryRegistry.Backend.Z3;
            } else if (cmd.getOptionValue('b').equals("bdd")) {
                backend = FactoryRegistry.Backend.BDD;
            }
        }

        if (cmd.hasOption('z')) {
            optimisations.remove(Optimisation.COMPUTE_ACC_CONDITION);
        } else {
            optimisations.add(Optimisation.COMPUTE_ACC_CONDITION);
        }

        return new CmdArguments(outputLevel, autType, format, optimisations, simplification, writer, inputFormula, backend);
    }

    private static void printHelp() {
        HelpFormatter helper = new HelpFormatter();
        helper.printHelp("CLIParser", opts);
    }

    public enum AutomatonType {
        TGR, TR, SGR, SR
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
        final OutputStream writer;
        final Formula inputFormula;
        final FactoryRegistry.Backend backend;

        private CmdArguments(int outputLevel, AutomatonType autType, Format format, Set<Optimisation> optimisations, Simplifier.Strategy strat,
                             OutputStream writer, Formula inputFormula, FactoryRegistry.Backend backend) {
            this.outputLevel = outputLevel;
            this.autType = autType;
            this.format = format;
            this.optimisations = optimisations;
            this.simplification = strat;
            this.writer = writer;
            this.inputFormula = inputFormula;
            this.backend = backend;
        }
    }

}
