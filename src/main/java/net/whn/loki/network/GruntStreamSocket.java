package net.whn.loki.network;

import net.whn.loki.common.ProgressUpdate;
import net.whn.loki.grunt.GruntEQCaller;
import net.whn.loki.grunt.GruntForm;
import net.whn.loki.io.IOHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class GruntStreamSocket extends StreamSocketA {

    private ArrayBlockingQueue<Integer> sentPercentQueue = new ArrayBlockingQueue<>(1);

    /**
     *
     * @param masterAddress
     * @param connectPort
     * @param filesReceivePort
     * @param updateMachinePort
     * @throws IOException - if socket creation fails
     */
    public GruntStreamSocket(InetAddress masterAddress, int connectPort, int filesReceivePort, int updateMachinePort) throws IOException {

        socket = new Socket(masterAddress, connectPort);
        fileReceiveSocket = new Socket(masterAddress, filesReceivePort);
        machineSocket = new Socket(masterAddress, updateMachinePort);
        initStream();
    }

    public void receiveFileFromBroker(File projectFile,
                                      long projectFileSize,
                                      GruntForm gruntForm) throws IOException {

        long remaining = projectFileSize;
        byte[] buffer = new byte[IOHelper.BUFFER_SIZE];
        int amountRead;
        long totalRead = 0;

        try (OutputStream outFile = new FileOutputStream(projectFile)) {
            while (remaining > 0) {

                amountRead = remaining > IOHelper.BUFFER_SIZE ? sockIn.read(buffer) : sockIn.read(buffer, 0, (int) remaining);
                outFile.write(buffer, 0, amountRead);
                remaining -= amountRead;
                totalRead += amountRead;
                Integer percentValue = (int) (totalRead * 100 / projectFileSize);
                sentPercentQueue.offer(percentValue);

                if (gruntForm != null)
                    GruntEQCaller.invokeGruntUpdatePBar(gruntForm, new ProgressUpdate(projectFileSize, remaining));
            }
        }
        finishFillingPercentQueue();
        sendHeader(new Header(HeaderType.BUSY));
    }

    /**
     * UI scope
     */
    public void finishFillingPercentQueue() {
        try {
            Thread.sleep(100);
            sentPercentQueue.poll(100, TimeUnit.MILLISECONDS);
            sentPercentQueue.offer(100);
            Thread.sleep(500);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param machineHeader: should be of type: MACHINE_INFO or MACHINE_UPDATE
     * @throws IOException
     */
    public synchronized void sendMachineHeader(Header machineHeader) throws IOException {
        machineObjectOutputStream.writeObject(machineHeader);
        machineObjectOutputStream.flush();
    }

    /**
     * puts the specified Header object into the stream. trailing file should be
     * sent with the sendFile method
     * synchronized because both machine update and taskhandler thread want to
     * send on socket
     *
     * @param header
     * @throws IOException if socket interaction fails; probably lost connection
     */
    @Override
    public synchronized void sendHeader(Header header) throws IOException {
        objectOut.writeObject(header);
        objectOut.flush();
        //objectOut.reset();
    }

    public boolean wasFileReceived() {
        try {
            //we'll block on this call until we receive a header from socket
            Header header = (Header) fileReceiveObjectInputStream.readObject();
            return header.getHeaderType().equals(HeaderType.FILE_RECEIVED);
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getMasterLokiVersion() throws IOException, ClassNotFoundException {
        try {
            //we'll block on this call until we receive a header from socket
            Header header = (Header) machineObjectInputStream.readObject();
            HeaderType headerType = header.getHeaderType();
            if (headerType.equals(HeaderType.TELL_LOKI_VERSION)) {
                return header.getLokiVersion();
            } else {
                throw new RuntimeException("GruntStreamSocket should receive HeaderType.DIFFERENT_LOKI_VERSION, instead of: HeaderType." + headerType);
            }
        } catch (ClassNotFoundException | IOException e) {
            throw e;
        }
    }

    public void waitUntilInformationReceived() throws IOException, ClassNotFoundException {
        try {
            //we'll block on this call until we receive a header from socket
            Header header = (Header) fileReceiveObjectInputStream.readObject();
            HeaderType headerType = header.getHeaderType();
            if (!headerType.equals(HeaderType.DIFFERENT_LOKI_VERSION)) {
                throw new RuntimeException("GruntStreamSocket should receive HeaderType.DIFFERENT_LOKI_VERSION, instead of: HeaderType." + headerType);
            }
        } catch (ClassNotFoundException | IOException e) {
            throw e;
        }
    }

    public Integer pollTotalReceivedPercent() throws InterruptedException {
        return sentPercentQueue.poll(50, TimeUnit.MILLISECONDS);
    }

    public void clearTotalReceivedPercentQueue() {
        sentPercentQueue.clear();
    }

    public void updatePercentQueue(Integer value) {
        sentPercentQueue.offer(value);
    }
}
