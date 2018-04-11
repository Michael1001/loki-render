package net.whn.loki.common;

import com.sun.management.OperatingSystemMXBean;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Machine implements Serializable {

    private String hostname;
    private String osName;
    private String osVersion;
    private String osArchitecture;
    private String userName;
    private String userHome;
    private String currentWorkingDir;
    private int processors;
    private long totalMemory;
    private long totalSwap;
    private transient OperatingSystemMXBean osMxbean;

    public Machine() {
        getOperationSystemInfo();
        getUserInfo();
        getHardwareInfo();
    }

    /**
     * Dummy constructor to pass to broker until it gets a Machine object
     * with real values from the grunt
     * @param value
     */
    public Machine(String value) {
        initializeAllWithEmptyValues(value);
    }

    private void initializeAllWithEmptyValues(String value) {
        hostname = value;
        osMxbean = null;

        //get OS info
        osName = "";
        osVersion = "";
        osArchitecture = "";

        //get user info
        userName = "";
        userHome = "";
        currentWorkingDir = "";

        //get number of processors
        processors = -1;

        //get total memory, total swap from mxbean
        totalMemory = -1;
        totalSwap = -1;
    }


    private void getHardwareInfo() {

        hostname = getHostName();
        processors = Runtime.getRuntime().availableProcessors();

        //get total memory, total swap from mxbean
        osMxbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        totalMemory = osMxbean.getTotalPhysicalMemorySize();
        totalSwap = osMxbean.getTotalSwapSpaceSize();
    }

    private void getUserInfo() {
        userName = System.getProperty("user.name");
        userHome = System.getProperty("user.home");
        currentWorkingDir = System.getProperty("user.dir");
    }

    private void getOperationSystemInfo() {
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
        osArchitecture = System.getProperty("os.arch");
    }

    private String getHostName() {
        String hostname;
        String tmpHostname = "unknown";
        //first try to get local hostname
        try {
            InetAddress localMachine = InetAddress.getLocalHost();
            tmpHostname = localMachine.getHostName();
        } catch (UnknownHostException ex) {
            //don't do anything .. we'll just keep "unknown".
        }
        hostname = tmpHostname;
        return hostname;
    }

    public static int getSystemCpuLoad() {

        OperatingSystemMXBean mbean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double systemCpuLoad = mbean.getSystemCpuLoad();
        return (int) ((int) (systemCpuLoad * 1000.0) / 10.0);
    }

    public MachineUpdate getMachineUpdate() {
        return new MachineUpdate(
                osMxbean.getSystemLoadAverage(),
                totalMemory,
                osMxbean.getFreePhysicalMemorySize(),
                osMxbean.getFreeSwapSpaceSize(),
                getSystemCpuLoad()
        );
    }

    public String getCurrentWorkingDir() {
        return currentWorkingDir;
    }

    public String getHostname() {
        return hostname;
    }

    public String getOsArchitecture() {
        return osArchitecture;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public int getProcessors() {
        return processors;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public long getTotalSwap() {
        return totalSwap;
    }

    public String getUserHome() {
        return userHome;
    }

    public String getUserName() {
        return userName;
    }
}
