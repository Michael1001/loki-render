package net.whn.loki.network;

import net.whn.loki.common.ICommon;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Provides functionality needed by the broker to communicate with the grunt
 */
public class BrokerStreamSocket extends StreamSocketA {

    /**
     * @param gruntSocket
     * @param updateMachineSocket
     * @throws SocketException if we have underlying protocol problem
     * @throws IOException     if we failed to setup the streams on socket
     */
    public BrokerStreamSocket(Socket gruntSocket, Socket fileReceiveSocket, Socket updateMachineSocket) throws IOException {
        socket = gruntSocket;
        this.fileReceiveSocket = fileReceiveSocket;
        this.machineSocket = updateMachineSocket;
        initStream();
    }

    public synchronized void sendFileReceiveHeader() throws IOException {
        sendObjectHeader(new Header(ICommon.HeaderType.FILE_RECEIVED));
    }

    public synchronized void confirmInformationReceived() throws IOException {
        sendObjectHeader(new Header(ICommon.HeaderType.DIFFERENT_LOKI_VERSION));
    }

    public synchronized void sendObjectHeader(Header header) throws IOException {
        fileReceiveObjectOutputStream.writeObject(header);
        fileReceiveObjectOutputStream.flush();
    }

    public void sendMasterLokiVersion(String lokiVersion) throws IOException {
        machineObjectOutputStream.writeObject(new Header(lokiVersion, HeaderType.TELL_LOKI_VERSION));
        machineObjectOutputStream.flush();
    }
}
