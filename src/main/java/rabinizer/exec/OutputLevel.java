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

public enum OutputLevel {
    SILENT, NORMAL, VERBOSE;

    /**
     * @param level
     *            0 for silent 1 for normal 2 for verbose anything else for
     *            IllegalArgumentException
     */
    public static OutputLevel getOutputLevel(int level) {
        if (level < 0 || level > 2) {
            throw new IllegalArgumentException("The output level " + level + " is not a valid output level");
        }
        if (level == 0) {
            return SILENT;
        }
        if (level == 1) {
            return NORMAL;
        }
        return VERBOSE;
    }

    private static OutputLevel output = OutputLevel.SILENT;

    /**
     * This sets the desired output level, 0 for silent, 1 for normal or 2 for
     * verbose
     */
    public static void setOutputLevel(OutputLevel outputLevel) {
        output = outputLevel;
    }

    public static boolean isVerbose() {
        return output == OutputLevel.VERBOSE;
    }

    public static boolean isSilent() {
        return output == OutputLevel.SILENT;
    }

    public static void verboseln(String s) {
        if (isVerbose()) {
            System.out.println(s);
        }
    }

    public static void nonsilent(String s) {
        if (!isSilent()) {
            System.out.println(s);
        }
    }
}
