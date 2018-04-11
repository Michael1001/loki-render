package net.whn.loki.grunt;

import net.whn.loki.common.ICommon.HeaderType;
import net.whn.loki.common.Machine;
import net.whn.loki.common.MachineUpdate;
import net.whn.loki.network.GruntStreamSocket;
import net.whn.loki.network.Header;

import java.io.IOException;
import java.util.logging.Logger;

public class MachineUpdateR implements Runnable {

    private static final Logger log = Logger.getLogger(MachineUpdateR.class.toString());
    private static final Machine machine = new Machine();
    private final GruntStreamSocket gruntStreamSocket;

    MachineUpdateR(GruntStreamSocket gruntStreamSocket) throws IOException {

        log.finest("constructor called");
        this.gruntStreamSocket = gruntStreamSocket;
        this.gruntStreamSocket.sendMachineHeader(new Header(HeaderType.MACHINE_INFO, machine));
    }

    @Override
    public void run() {
        if (!gruntStreamSocket.isClosed()) {
            //throws IOE if socket has problem
            try {
                MachineUpdate machineUpdate = machine.getMachineUpdate();
                Integer percent = gruntStreamSocket.pollTotalReceivedPercent();
                machineUpdate.setSentPercent(percent);
                gruntStreamSocket.sendMachineHeader(new Header(HeaderType.MACHINE_UPDATE, machineUpdate));
                if (percent != null && percent == 100) {
                    gruntStreamSocket.clearTotalReceivedPercentQueue();
                }
            } catch (IOException | InterruptedException ex) {
                log.info("lost connection: " + ex.getMessage());
                gruntStreamSocket.tryClose();
            }
        }
    }
}
