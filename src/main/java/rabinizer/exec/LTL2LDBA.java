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

import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import rabinizer.automata.nxt.DetLimitAutomaton;
import rabinizer.automata.nxt.DetLimitAutomatonFactory;
import rabinizer.ltl.Formula;
import rabinizer.ltl.parser.LTLParser;

import java.io.StringReader;

public class LTL2LDBA {

    public static void main(String... args) throws rabinizer.ltl.parser.ParseException, HOAConsumerException {
        LTLParser parser = new LTLParser(new StringReader(args[0]));
        Formula formula = parser.parse();
        DetLimitAutomaton automaton = DetLimitAutomatonFactory.createDetLimitAutomaton(formula);
        automaton.toHOA(new HOAConsumerPrint(System.out));
    }
}
