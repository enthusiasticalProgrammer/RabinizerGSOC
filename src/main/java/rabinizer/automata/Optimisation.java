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

package rabinizer.automata;

public enum Optimisation {
    /* Common */
    EAGER, SKELETON,

    /* DetLimit */
    COVER, SCC, IMPATIENT,

    /* Rabinizer */
    ONLY_RELEVANT_SLAVES, SLAVE_SUSPENSION,

    /* additionnaly for DTGRARaw: */
    COMPUTE_ACC_CONDITION, SINKS, OPTIMISE_INITIAL_STATE, COMPLETE, EMPTINESS_CHECK
}
