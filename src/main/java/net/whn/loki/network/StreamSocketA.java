package net.whn.loki.network;

import net.whn.loki.common.ICommon;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * Provides common socket and stream functionality specific to communication
 * between the broker and grunt.
 * notes on behavior:
 */
public abstract class StreamSocketA implements ICommon {

    private static final String className = StreamSocketA.class.toString();
    private static final Logger log = Logger.getLogger(className);

    protected Socket socket;
    protected InputStream sockIn;
    protected OutputStream sockOut;
    protected ObjectInputStream objectIn;
    protected ObjectOutputStream objectOut;

    protected Socket fileReceiveSocket;
    protected ObjectInputStream fileReceiveObjectInputStream;
    protected ObjectOutputStream fileReceiveObjectOutputStream;

    protected Socket machineSocket;
    protected ObjectInputStream machineObjectInputStream;
    protected ObjectOutputStream machineObjectOutputStream;

    /**
     * configures the socket and initializes stream objects for interacting
     * with the socket.  must be called before the socket read/write!
     *
     * @throws SocketException if underlying protocol has a problem
     * @throws IOException     if socket is not connected or we get an I/O from
     *                         attempting to create input/outputstream
     */
    public void initStream() throws IOException {
        sockOut = socket.getOutputStream();
        sockIn = socket.getInputStream();
        objectOut = new ObjectOutputStream(sockOut);
        objectOut.flush();
        objectIn = new ObjectInputStream(sockIn);

        buildFileReceiveObjectOutputStream();
        buildFileReceiveObjectInputStream();

        buildMachineObjectOutputStream();
        buildMachineObjectInputStream();
    }

    private void buildMachineObjectOutputStream() throws IOException {
        OutputStream machineOutputStream = machineSocket.getOutputStream();
        machineObjectOutputStream = new ObjectOutputStream(machineOutputStream);
        machineObjectOutputStream.flush();
    }

    private void buildMachineObjectInputStream() throws IOException {
        InputStream machineInputStream = machineSocket.getInputStream();
        machineObjectInputStream = new ObjectInputStream(machineInputStream);
    }

    private void buildFileReceiveObjectOutputStream() throws IOException {
        OutputStream fileReceiveOutputStream = fileReceiveSocket.getOutputStream();
        fileReceiveObjectOutputStream = new ObjectOutputStream(fileReceiveOutputStream);
        fileReceiveObjectOutputStream.flush();
    }

    private void buildFileReceiveObjectInputStream() throws IOException {
        InputStream fileReceiveInputStream = fileReceiveSocket.getInputStream();
        fileReceiveObjectInputStream = new ObjectInputStream(fileReceiveInputStream);
    }

    public InputStream getSockIn() {
        return sockIn;
    }

    /**
     * puts the specified Header object into the stream. trailing file should be
     * sent with the sendFile method
     *
     * @param h
     * @throws IOException if socket interaction fails; probably lost connection
     */
    public void sendHeader(Header h) throws IOException {
        objectOut.writeObject(h);
        objectOut.flush();
    }

    public void sendProjectFileChunk(byte[] buffer, int amountToSend) throws IOException {
        sockOut.write(buffer, 0, amountToSend);
    }

    public void sendFileChunk(byte[] buffer, int amountToSend) throws IOException {
        sockOut.write(buffer, 0, amountToSend);
    }

    public void flushSockOut() throws IOException {
        sockOut.flush();
    }

    /**
     * grabs the next object from the stream, assumes it will be of type Header
     *
     * @return Header object taken from the stream, unless we have an Exception:-)
     * @throws IOException            if we have a socket problem
     * @throws ClassNotFoundException if we don't recognize the received class
     */
    public Header receiveDelivery() throws ClassNotFoundException, IOException {
        return (Header) objectIn.readObject();
    }

    /**
     * @return Header of type: MACHINE_INFO or MACHINE_UPDATE
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public Header receiveMachineDelivery() throws ClassNotFoundException, IOException {
        return (Header) machineObjectInputStream.readObject();
    }

    /**
     * attempts to close the socket if it isn't already closed
     */
    public void tryClose() {
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ex) {
                log.throwing(className, "tryClose()", ex);
            }
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public ObjectOutputStream getObjectOut() {
        return objectOut;
    }

    public OutputStream getSockOut() {
        return sockOut;
    }
}
