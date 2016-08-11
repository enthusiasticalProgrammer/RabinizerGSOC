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
}

