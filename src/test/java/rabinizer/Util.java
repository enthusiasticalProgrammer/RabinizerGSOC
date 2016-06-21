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

package rabinizer;

import com.google.common.collect.BiMap;
import ltl.Formula;
import ltl.parser.ParseException;
import ltl.parser.Parser;

import java.io.StringReader;

import static org.junit.Assert.fail;

public final class Util {

    public static Formula createFormula(String s) {
        Parser parser = new Parser(new StringReader(s));

        try {
            return parser.formula();
        } catch (ParseException e) {
            fail("Failed to construct formula from string");
            return null;
        }
    }

    public static Formula createFormula(String s, BiMap<String, Integer> mapping) {
        Parser parser = new Parser(new StringReader(s));

        if (mapping != null) {
            parser.map = mapping;
        }

        try {
            return parser.formula();
        } catch (ParseException e) {
            fail("Failed to construct formula from string: " + e.getMessage());
            return null;
        }
    }
}
