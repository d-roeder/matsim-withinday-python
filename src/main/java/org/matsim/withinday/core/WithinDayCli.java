package org.matsim.withinday.core;

/**
 * <h2>WithinDayCli</h2>
 * <p>
 * Bridges the two command line styles used in this project: the within-day runners derived from
 * {@link RunWithinDay} expect the config file as the first positional argument, while runners
 * derived from MATSimApplication (e.g. the Open Berlin scenario) expect it behind {@code --config}.
 * </p>
 */
public final class WithinDayCli {

    private WithinDayCli() {
    }

    /**
     * Rewrites a leading positional config path into an explicit {@code --config <path>} option so
     * that the same launcher can drive both runner styles.
     *
     * @param args Raw command-line arguments.
     * @return Arguments a MATSimApplication based runner understands.
     */
    public static String[] normalize(String[] args) {
        if (args == null || args.length == 0) {
            return new String[0];
        }

        String first = args[0];
        if (first == null || first.isBlank() || first.startsWith("-") || first.equals("run")) {
            return args;
        }

        String[] normalized = new String[args.length + 1];
        normalized[0] = "--config";
        normalized[1] = first;
        System.arraycopy(args, 1, normalized, 2, args.length - 1);
        return normalized;
    }
}
