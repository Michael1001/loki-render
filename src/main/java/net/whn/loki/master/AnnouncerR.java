package net.whn.loki.master;

import net.whn.loki.common.LokiForm;
import net.whn.loki.common.Main;
import net.whn.loki.common.configs.Config;

import javax.swing.*;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * Sends multicast announcement so grunts can discover master's IP address
 * it doesn't require any cleanup since it's just sending UDP, so it should
 * be started as a daemon thread.
 */
public class AnnouncerR implements Runnable {

    private static final Logger log = Logger.getLogger(AnnouncerR.class.toString());
    final private Config config;
    final private LokiForm lokiForm;
    final private int announceInterval;
    final private int multicastSendPort = 53912;
    final private String masterInfo;
    final private DatagramPacket dgramPacketAnnounce;
    //final private DatagramSocket mSocket;

    public AnnouncerR(Config config, LokiForm lokiForm) throws IOException {

        this.config = config;
        this.lokiForm = lokiForm;
        announceInterval = config.getAnnounceInterval();
        InetAddress localMachine = InetAddress.getLocalHost();

        masterInfo = new StringBuilder(localMachine.getHostName())
                .append(";")
                .append(Main.LOKI_VERSION)
                .append(";")
                .append(Integer.toString(this.config.getConnectPort()))
                .append(";")
                .toString();

        //mSocket = new DatagramSocket(multicastSendPort);

        dgramPacketAnnounce = new DatagramPacket(
                masterInfo.getBytes(),
                masterInfo.length(),
                this.config.getMulticastAddress(),
                this.config.getGruntMulticastPort()
        );
    }

    @Override
    public void run() {
        int failureCount = 0;

        String masterIP = detectMaster();
        if (masterIP != null) {
            MasterEQCaller.showMessageDialog(lokiForm,
                    "Master detected",
                    "Loki master already running on system '" + masterIP + "'.",
                    JOptionPane.WARNING_MESSAGE
            );
            log.info("detected master at:" + masterIP);
        }

        while (!Thread.currentThread().isInterrupted()) {
            if (failureCount > 5) {
                //we can't continue if announce is broken
                throw new IllegalStateException();
            }

            try {
                //mSocket.send(dgramPacketAnnounce);
                sendAnnouncePackets();
                failureCount = 0;

                try {
                    Thread.sleep(announceInterval);
                } catch (InterruptedException ex) {
                    break;  //from the master -> shutdown
                }
            } catch (IOException ex) {
                failureCount++;
            }
        }
    }

    private void sendAnnouncePackets() throws SocketException, IOException {

        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        
        for (NetworkInterface netint : Collections.list(nets)){
            Enumeration<InetAddress> addresses = netint.getInetAddresses();
            if(!netint.isLoopback()) {

                for (InetAddress inetAddress : Collections.list(addresses)) {
                    if(inetAddress instanceof Inet4Address) {
                        DatagramSocket sock = new DatagramSocket(multicastSendPort, inetAddress);
                        sock.send(dgramPacketAnnounce);
                        sock.close();
                    } 
                }
            }
        }
    }

    private String detectMaster() {

        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            MulticastSocket mSock = new MulticastSocket(config.getGruntMulticastPort());
            mSock.joinGroup(config.getMulticastAddress());
            mSock.setSoTimeout(5_000);
            mSock.receive(packet);
        } catch (SocketTimeoutException ex) {
            return null;
        } catch (IOException ex) {
        }
        String remoteMasterInfo = new String(packet.getData());
        StringTokenizer st = new StringTokenizer(remoteMasterInfo, ";");
        return st.nextToken();
    }
}
