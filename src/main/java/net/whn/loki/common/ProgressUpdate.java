package net.whn.loki.common;

public class ProgressUpdate {

    private final int max;
    private final int done;

    /**
     * Constructor for grunt's values
     * @param total
     * @param remaining
     */
    public ProgressUpdate(long total, long remaining) {
        double ratio = ((double) (total - remaining) / (double) total);
        max = 100;
        done = (int) (ratio * max);
    }

    /**
     * Constructor for straight values
     * @param m
     * @param d
     */
    public ProgressUpdate(int m, int d) {
        max = m;
        done = d;
    }

    public int getDone() {
        return done;
    }

    public int getMax() {
        return max;
    }
}
