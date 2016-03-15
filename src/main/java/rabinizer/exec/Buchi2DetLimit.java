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

import jhoafparser.consumer.HOAConsumerPrint;
import jhoafparser.parser.HOAFParser;
import jhoafparser.parser.generated.ParseException;
import rabinizer.automata.buchi.BuchiAutomaton;
import rabinizer.automata.buchi.BuchiAutomatonBuilder;
import rabinizer.automata.buchi.SemiDeterminization;

public class Buchi2DetLimit {

    public static void main(String... args) throws ParseException {
        BuchiAutomatonBuilder builder = new BuchiAutomatonBuilder();
        HOAFParser.parseHOA(System.in, builder);

        for (BuchiAutomaton buchiAutomaton : builder.getAutomata()) {
            SemiDeterminization algorithm = new SemiDeterminization(buchiAutomaton);
            BuchiAutomaton semiAutomaton = algorithm.apply();
            semiAutomaton.toHOA(new HOAConsumerPrint(System.out));
        }
    }
}
