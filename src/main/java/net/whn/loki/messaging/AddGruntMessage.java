package net.whn.loki.messaging;

import java.net.Socket;

public class AddGruntMessage extends Message {

    private Socket gruntSocket;
    private Socket fileReceiveSocket;
    private final Socket updateMachineSocket;

    public AddGruntMessage(Socket gSock, Socket fileReceiveSocket, Socket updateMachineSocket) {

        super(MessageType.ADD_GRUNT);
        gruntSocket = gSock;
        this.fileReceiveSocket = fileReceiveSocket;
        this.updateMachineSocket = updateMachineSocket;
    }

    public Socket getGruntSocket() {
        return gruntSocket;
    }

    public Socket getFileReceiveSocket() {
        return fileReceiveSocket;
    }

    public Socket getUpdateMachineSocket() {
        return updateMachineSocket;
    }
}
