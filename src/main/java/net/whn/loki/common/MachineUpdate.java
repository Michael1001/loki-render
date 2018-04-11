package net.whn.loki.common;

import java.io.Serializable;
import java.text.DecimalFormat;

public class MachineUpdate implements Serializable {

    private static final long bytesPerGB = 1073741824;
    private static DecimalFormat gb = new DecimalFormat("#0.00");

    private final double loadAvg;
    private final long totalMemory;
    private final long freeMemory;
    private final long freeSwap;
    private final String usedMemory;
    private final int systemCpuLoad;
    private volatile Integer sentPercent;

    public MachineUpdate(double loadAvg, long totalMemory, long freeMemory, long freeSwap, int systemCpuLoad) {
        this.loadAvg = loadAvg;
        this.totalMemory = totalMemory;
        this.freeMemory = freeMemory;
        this.freeSwap = freeSwap;
        usedMemory = generateMemUsage();
        this.systemCpuLoad = systemCpuLoad;
    }

    public String generateMemUsage() {
        double total = (double) totalMemory / (double) bytesPerGB;
        String tmpUsed;
        if (totalMemory == freeMemory) {
            tmpUsed = "?";
        } else {
            double used = (double) (totalMemory - freeMemory) / (double) bytesPerGB;
            tmpUsed = gb.format(used);
        }
        String tmpTotal = gb.format(total);
        return tmpUsed + "/" + tmpTotal;
    }

    public String getMemUsageStr() {
        return usedMemory;
    }

    public long getFreeSwap() {
        return freeSwap;
    }

    public double getLoadAvg() {
        return loadAvg;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public int getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public Integer getSentPercent() {
        return sentPercent;
    }

    public void setSentPercent(Integer sentPercent) {
        this.sentPercent = sentPercent;
    }
}
