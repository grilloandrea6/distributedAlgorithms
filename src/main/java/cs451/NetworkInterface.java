package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class NetworkInterface {
    public static Parser parser;

    private static DatagramSocket socketReceive, socketSend;
    static double timeForAckReceived = 0.;
    static double timeForProcessPacket = 0.;
    static double maximumTimeForAckReceived = 0.;
    static double maximumTimeForProcessPacket = 0.;
    static int timesOverMillisecondAckReceived = 0;
    static int timesOverMillisecondProcessPacket = 0;
    static double ALPHA = 0.25;

    public static void begin(Parser p) throws SocketException {
        parser = p;

        int port = parser.hosts().get(parser.myId() - 1).getPort();

        System.out.println("My ID: " + parser.myId() + " - Port:" + port);
        socketReceive = new DatagramSocket(port);
        System.out.println("Listening socket created on port " + port);
        socketSend = new DatagramSocket();

        new Thread(NetworkInterface::receivePackets).start();
    }

    public static void receivePackets() {
        DatagramPacket datagramPacket  = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        while (Main.running) {
            try {
                socketReceive.receive(datagramPacket);
                //System.out.println("Received packet.\n. Data: " + new String(datagramPacket.getData()) + " - Length: " + datagramPacket.getLength());
                Packet p = Packet.deserialize(datagramPacket.getData(), datagramPacket.getLength());
                if (p != null) {
                    if(p.isAckPacket()) {
                        long time = System.nanoTime();
                        PerfectLinks.ackReceived(p);
                        time = System.nanoTime() - time;
                        timeForAckReceived = ALPHA * time + (1 - ALPHA) * timeForAckReceived;
                        if(time > maximumTimeForAckReceived) {
                            maximumTimeForAckReceived = time;
                        }
                        if(time > 1000000) {
                            timesOverMillisecondAckReceived++;
                        }
                    }
                    else {
                        long time = System.nanoTime();
                        PerfectLinks.receivedPacket(p);
                        time = System.nanoTime() - time;
                        timeForProcessPacket = ALPHA * time + (1 - ALPHA) * timeForProcessPacket;
                        if(time > maximumTimeForProcessPacket) {
                            maximumTimeForProcessPacket = time;
                        }
                        if(time > 1000000) {
                            timesOverMillisecondProcessPacket++;
                        }
                    }
                } else System.err.println("Invalid packet received!");
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("NetworkInterface - Exiting receivePackets thread");
    } 

    public static void sendPacket(Packet packet) {
        Host targetHost = parser.hosts().get(packet.getTargetID() - 1);
        InetSocketAddress targetAddress = new InetSocketAddress(targetHost.getIp(), targetHost.getPort());
        try {
            // System.out.println("network interface - Sending packet to id " + packet.getTargetID() + " port " + targetHost.getPort());
            
            byte[] data = packet.serialize();
            socketSend.send(new DatagramPacket(Arrays.copyOf(data, data.length), data.length, targetAddress)); //ToDo do i really need the copy?
        } catch (IOException e) {
            // System.out.println("Failed to send packet to " + packet.getTargetID());
            e.printStackTrace();
        } 
    }

    public static final List<Byte> intToBytes(int number) {
        List<Byte> byteList = new ArrayList<>(4);

        // Extract bytes from int
        byteList.add((byte) ((number >> 24) & 0xFF)); // Most significant byte
        byteList.add((byte) ((number >> 16) & 0xFF));
        byteList.add((byte) ((number >> 8) & 0xFF));
        byteList.add((byte) (number & 0xFF));          // Least significant byte

        return byteList;    
    }

    public static final int bytesToInt(List<Byte> byteList) {
        if (byteList == null || byteList.size() < 4) {
            throw new IllegalArgumentException("The list must contain at least 4 bytes.");
        }

        // Combine the bytes back into an int
        int result = 0;
        result |= (byteList.get(0) & 0xFF) << 24; // Most significant byte
        result |= (byteList.get(1) & 0xFF) << 16;
        result |= (byteList.get(2) & 0xFF) << 8;
        result |= (byteList.get(3) & 0xFF);       // Least significant byte

        return result;
    }
}
