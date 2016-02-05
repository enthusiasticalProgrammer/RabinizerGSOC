package rabinizer.exec;

import java.io.BufferedReader;
import rabinizer.ltl.*;
import rabinizer.ltl.Simplifier.Strategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
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
import rabinizer.ltl.ValuationSetFactory;
import rabinizer.ltl.bdd.BDDEquivalenceClassFactory;
import rabinizer.ltl.bdd.BDDValuationSetFactory;
import rabinizer.ltl.z3.Z3EquivalenceClassFactory;
import rabinizer.ltl.z3.Z3ValuationSetFactory;
import rabinizer.parser.LTLParser;
import rabinizer.parser.ParseException;

/**
 * @author jkretinsky
 */
public class Main {

    public static boolean verbose = false;
    public static boolean silent = false;
    private static long clock = 0L, clock2;

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

    public static void printUsage() {
        System.out.println("\n" + "Usage: rabinizer [options] <ltlfile>\n" + "Options:\n"
                + "   -help                       : print this help and exit\n"
                + "   -silent                     : silent terminal output, no messages, only the result\n\n"
                // + " -verbose : verbose terminal output for debugging\n"
                + "   -auto=tgr                   : creates generalized Rabin automaton with condition on transitions (default)\n"
                + "   -auto=sgr                   : creates generalized Rabin automaton with condition on states\n"
                + "   -auto=tr                    : creates Rabin automaton with condition on transitions\n"
                + "   -auto=sr                    : creates Rabin automaton with condition on states\n"
                // + " -auto=buchi : creates Rabin automaton with condition on
                // states\n"
                + "   -format=hoa                 : HOA (Hanoi omega-automata) format \n"
                + "   -format=dot                 : dotty format (default)\n"
                + "   -format=size                : print only size of automaton\n"
                + "   -how=isabelle               : stick to mechanically proven construction\n"
                + "   -how=optimize               : use optimizations (default) \n"
                + "   -eager                      : use eager optimizations, default if optimize\n"
                + "   -not-eager                  : do not use eager optimizations, default if isabelle \n"
                + "   -simplify-formula           : simplify the formula, default \n"
                + "   -not-simplify-formula       : do not simplify the formula \n"
                + "   -relevant-slaves-only       : computes only relevant slaves, default if optimize \n"
                + "   -all-slaves                 : computes all slaves, default if isabelle \n"
                + "   -slave-suspension           : suspends the slaves, whenever possible \n"
                + "   -sinks-on                   : uses the sink-optimization for Mojmir slaves, default if optimize\n"
                + "   -sinks-off                  : does not use the sink-optimization, default if isabelle \n"
                + "   -optimize-initial-state     : remove transient pair in Rabin Slave, default if optimize\n"
                + "   -g-skeleton                 : uses the G-Skeleton optimisation (not default)"
                + "   -not-optimize-initial-state : does not remove transient pair in Rabin Slave, default if isabelle \n"
                + "   -z3                         : use z3 as backend\n"
                + "   -bdd                        : use bdd as backend (default)\n"
                + "   -emptiness-check            : perform an emptiness check and remove non-accepting sinks, per default off\n"
                + "   -complete                   : return always a complete automaton, per default off (except for emptiness-check)"
                + "   -in=formula                 : formula is input as an argument (default)\n"
                + "   -in=file                    : batch processing of one or more formula per line in the file passed as an argument\n"
                + "   -out=file                   : print automaton to file(s) (default)\n"
                + "   -out=std                    : print automaton to terminal\n\n");

    }

    public static double stopwatch() {
        long start = clock;
        clock = System.currentTimeMillis();
        return (clock - start) / 1000.0;
    }

    public static double stopwatchLocal() {
        long start = clock2;
        clock2 = System.currentTimeMillis();
        return (clock2 - start) / 1000.0;
    }

    public static void main(String[] args) throws IOException {
        // Parsing arguments
        AutomatonType type = AutomatonType.TGR;
        Format format = Format.DOT;
        boolean inFormula = true;
        boolean outFile = true;
        String argument = null;
        boolean simplifyFormula = false; // NULL Pointer
        boolean z3 = false;


        Set<Optimisation> opts = new HashSet<>();
        opts.add(Optimisation.OPTIMISE_INITIAL_STATE);
        opts.add(Optimisation.EAGER);
        opts.add(Optimisation.SINKS);
        opts.add(Optimisation.ONLY_RELEVANT_SLAVES);

        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--h") || arg.equals("-help") || arg.equals("--help")) {
                printUsage();
                System.exit(0);
            } else if (arg.equals("-v") || arg.equals("--v") || arg.equals("-verbose") || arg.equals("--verbose")) {
                Main.verbose = true;
            } else if (arg.equals("-silent") || arg.equals("--silent")) {
                Main.silent = true;
            } else if (arg.equals("-auto=tgr") || arg.equals("--auto=tgr")) {
                type = AutomatonType.TGR;
            } else if (arg.equals("-auto=sgr") || arg.equals("--auto=sgr")) {
                type = AutomatonType.SGR;
            } else if (arg.equals("-auto=tr") || arg.equals("--auto=tr")) {
                type = AutomatonType.TR;
            } else if (arg.equals("-auto=sr") || arg.equals("--auto=sr")) {
                type = AutomatonType.SR;
            } else if (arg.equals("-auto=buchi") || arg.equals("--auto=buchi")) {
                type = AutomatonType.BUCHI;
            } else if (arg.equals("-format=hoa") || arg.equals("--format=hoa")) {
                format = Format.HOA;
            } else if (arg.equals("-format=dot") || arg.equals("--format=dot")) {
                format = Format.DOT;
            } else if (arg.equals("-format=size") || arg.equals("--format=size")) {
                format = Format.SIZE;
            } else if (arg.equals("-format=sizeacc") || arg.equals("--format=sizeacc")) {
                format = Format.SIZEACC;
            } else if (arg.equals("-how=isabelle") || arg.equals("--how=isabelle")) {
                opts.remove(Optimisation.EAGER);
                opts.remove(Optimisation.SINKS);
                opts.remove(Optimisation.ONLY_RELEVANT_SLAVES);
                opts.remove(Optimisation.OPTIMISE_INITIAL_STATE);
            } else if (arg.equals("-how=optimize") || arg.equals("--how=optimize")) {
                opts.add(Optimisation.EAGER);
            } else if (arg.equals("-in=formula") || arg.equals("--in=formula")) {
                inFormula = true;
            } else if (arg.equals("-in=file") || arg.equals("--in=file")) {
                inFormula = false;
            } else if (arg.equals("-out=file") || arg.equals("--out=file")) {
                outFile = true;
            } else if (arg.equals("-out=std") || arg.equals("--out=std")) {
                outFile = false;
            } else if (arg.equals("-simplify-formula") || arg.equals("--simplify-formula")) {
                simplifyFormula = true;
            } else if (arg.equals("-not-simplify-formula") || arg.equals("--not-simplify-formula")) {
                simplifyFormula = false;
            } else if (arg.equals("-eager") || arg.equals("--eager")) {
                opts.add(Optimisation.EAGER);
            } else if (arg.equals("-not-eager") || arg.equals("--not-eager")) {
                opts.remove(Optimisation.EAGER);
            } else if (arg.equals("-relevant-slaves-only") || arg.equals("--relevant-slaves-only")) {
                opts.add(Optimisation.ONLY_RELEVANT_SLAVES);
            } else if (arg.equals("-all-slaves") || arg.equals("--all-slaves")) {
                opts.remove(Optimisation.ONLY_RELEVANT_SLAVES);
            } else if (arg.equals("-sinks-on") || arg.equals("--sinks-on")) {
                opts.add(Optimisation.SINKS);
            } else if (arg.equals("-sinks-off") || arg.equals("--sinks-off")) {
                opts.remove(Optimisation.SINKS);
            } else if (arg.equals("-optimize-initial-state") || arg.equals("--optimize-initial-state")) {
                opts.add(Optimisation.OPTIMISE_INITIAL_STATE);
            } else if (arg.equals("-not-optimize-initial-state") || arg.equals("--not-optimize-initial-state")) {
                opts.remove(Optimisation.OPTIMISE_INITIAL_STATE);
            } else if (arg.equals("-z3") || arg.equals("--z3")) {
                z3 = true;
            } else if (arg.equals("-emptiness-check") || arg.equals("--emptiness-check")) {
                opts.add(Optimisation.EMPTINESS_CHECK);
                opts.add(Optimisation.COMPLETE);
            }else if(arg.equals("-complete")||arg.equals("--complete")){
                opts.add(Optimisation.COMPLETE);
            } else if (arg.equals("-slave-suspension") || arg.equals("--slave-suspension")) {
                opts.add(Optimisation.SLAVE_SUSPENSION);
            } else if (arg.equals("-g-skeleton") || arg.equals("--g-skeleton")) {
                opts.add(Optimisation.SKELETON);
            } else if (arg.substring(0, 1).equals("-")) {
                System.out.println("\n\nERROR: unknown option " + arg);
                printUsage();
                System.exit(1);
            } else {
                argument = arg;
            }
        }

        if (argument == null) {
            errorMessageAndExit("No input given.");
        }

        if (opts.contains(Optimisation.EAGER) && opts.contains(Optimisation.SLAVE_SUSPENSION)) {
            System.err.println("Please do not use eager together with slave suspension, since eagerness will negate the slave suspensions effect.");
            System.err.println("Warning (in this context): eager unfolding is deactivated.");
            opts.remove(Optimisation.EAGER);
        }


        if (format != Format.SIZE || type != AutomatonType.TGR)
            opts.add(Optimisation.COMPUTE_ACC_CONDITION);

        opts.add(Optimisation.NOT_ISABELLE_ACC);

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

        Reader reader = null;
        if (inFormula) {
            reader = new StringReader(argument);
        } else { // input from file
            try {
                reader = new FileReader(new File(argument));
            } catch (FileNotFoundException e) {
                errorMessageAndExit("Exception when opening the input file: " + e.getLocalizedMessage());
            }
        }
        BufferedReader bReader = new BufferedReader(reader);

        OutputStream writer;
        FileOutputStream fw = null;
        if (outFile) {
            String file;
            if (!inFormula) {
                file = argument;
            } else {
                file = "output";
            }
            switch (format) {
            case HOA:
                file += ".hoa";
                break;
            case DOT:
                file += ".dot";
                break;
            case SIZE:
            case SIZEACC:
                file += ".txt";
                break;
            }
            try {
                fw = new FileOutputStream(new File(file));
            } catch (IOException e) {
                errorMessageAndExit("IO exception when creating file " + file + e.getMessage());
            }
            writer = fw;
        } else { // standard output
            writer = System.out;
        }

        String input, output;
        HOAConsumer hoa = new HOAConsumerPrint(writer);
        while ((input = bReader.readLine()) != null) { // TODO possible
            // IOException
            stopwatch();

            AccAutomatonInterface automaton = computeAutomaton(input, type, simplifyFormula, z3, opts);

            nonsilent("Time for construction: " + stopwatch() + " s");
            nonsilent("Outputting DGRA");
            switch (format) {
            case HOA:
                try {
                    automaton.toHOANew(hoa);
                } catch (HOAConsumerException e) {
                    e.printStackTrace();
                }
                output = "";
                break;
            case DOT:
                output = automaton.toDotty();
                output += "\n" + automaton.acc();
                break;
            case SIZE:
                output = String.format("%8s", automaton.size());
                break;
            case SIZEACC:
                output = String.format("%8s%8s", automaton.size(), automaton.pairNumber());
                // throw new UnsupportedOperationException("Not supported
                // yet.");
                break;
            default:
                output = null;
            }
            if (format != Format.HOA) {
                writer.write(output.getBytes());
            }
            writer.flush();
        }
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException e) {
                errorMessageAndExit("IO exception when closing the file: " + e.getMessage());
            }

        }
        nonsilent("Done!");
        reader.close();
        bReader.close();
        writer.close();
    }

    public static AccAutomatonInterface computeAutomaton(String input, AutomatonType type, boolean simplify, boolean z3,
            Set<Optimisation> opts) {
        LTLParser parser = new LTLParser(new StringReader(input));

        Formula formula = null;

        try {
            formula = parser.parse();
        } catch (ParseException e) {
            errorMessageAndExit("Exception when parsing: " + e.getLocalizedMessage());
        }

        if (simplify) {
            nonsilent("Formula unsimplified: " + input);
            Simplifier.simplify(formula, Strategy.AGGRESSIVELY);
            nonsilent("Formula simplified:" + formula);
        } else {
            nonsilent("Input formula: " + formula);
        }

        nonsilent("Input formula in NNF: " + formula);
        nonsilent("Enumeration of valuations");

        FactoryRegistry.Backend backend = z3 ? FactoryRegistry.Backend.Z3 : FactoryRegistry.Backend.BDD;
        EquivalenceClassFactory factory = FactoryRegistry.createEquivalenceClassFactory(backend, formula.getPropositions());
        ValuationSetFactory valuationSetFactory = FactoryRegistry.createValuationSetFactory(backend, formula.getAtoms());

        // DGRA dgra = new DTGRA(phi); for optimized
        DTGRARaw dtgra = new DTGRARaw(formula, factory, valuationSetFactory, opts);
        switch (type) {
        case TGR:
            return new DTGRA(dtgra);
            // case SGR:
            // return new DSGRA(dtgra);
        case TR:
            return new DTRA(dtgra, valuationSetFactory);
        case SR:
            return new DSRA(new DTRA(dtgra, valuationSetFactory), valuationSetFactory);
        case BUCHI:
        }
        errorMessageAndExit("Unsupported automaton type");
        return null;
    }

    public enum AutomatonType {
        TGR, TR, SGR, SR, BUCHI
    }

    public enum Format {
        HOA, DOT, SIZE, SIZEACC
    }

}
