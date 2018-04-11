package net.whn.loki.master;

import net.whn.loki.common.ICommon;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.error.MasterFrozenException;
import net.whn.loki.messaging.AddGruntMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class ListenerR implements Runnable, ICommon {

    private static final Logger log = Logger.getLogger(ListenerR.class.toString());
    private ServerSocket listenSocket;
    private ServerSocket fileRecieveServerSocket;
    private ServerSocket updateMachineServerSocket;
    private MasterR master;

    ListenerR(int gPort, int connectPort2, int updateMachinePort, MasterR masterR) throws InterruptedException, IOException {

        listenSocket = new ServerSocket(gPort);
        listenSocket.setSoTimeout(1000);

        fileRecieveServerSocket = new ServerSocket(connectPort2);
        fileRecieveServerSocket.setSoTimeout(1000);

        updateMachineServerSocket = new ServerSocket(updateMachinePort);
        updateMachineServerSocket.setSoTimeout(1000);

        master = masterR;
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                try {
                    Socket gruntSocket = listenSocket.accept();
                    Socket fileRecieveSocket = fileRecieveServerSocket.accept();
                    Socket updateMachineSocket = updateMachineServerSocket.accept();

                    master.deliverMessage(new AddGruntMessage(gruntSocket, fileRecieveSocket, updateMachineSocket));
                } catch (InterruptedException ex) {
                    //master says shutdown
                    break;
                } catch (SocketTimeoutException ex) {
                    //don't do anything. we just wanted to get
                    //unblocked so we can periodically check the interrupt
                } catch (MasterFrozenException mfe) {
                    ErrorHelper.outputToLogMsgAndKill(null, false, log, "fatal error. exiting.", mfe.getCause());
                }
            } catch (IOException ex) {
                //hopefully next time succeeds!
                System.out.println("failed to accept new grunt.");
            }
        }
        //we received a shutdown signal
        if (listenSocket.isBound()) {
            try {
                listenSocket.close();
            } catch (IOException ex) {
                //do nothing...we're exiting
            }
        }
    }
}
