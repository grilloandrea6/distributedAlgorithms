package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;


public class NetworkInterface {
    public static Parser parser;

    private static DatagramSocket socketReceive, socketSend;

    private static InetSocketAddress[] targetAddress;

    public static void begin(Parser p) throws SocketException {
        parser = p;

        int port = parser.hosts().get(parser.myId() - 1).getPort();

        // System.out.println("My ID: " + parser.myId() + " - Port:" + port);
        socketReceive = new DatagramSocket(port);
        // System.out.println("Listening socket created on port " + port);
        socketSend = new DatagramSocket();

        targetAddress = new InetSocketAddress[parser.hosts().size()];
        for(Host host : parser.hosts()) {
            targetAddress[host.getId() - 1] = new InetSocketAddress(host.getIp(), host.getPort());
        }

        new Thread(NetworkInterface::receivePackets).start();
    }

    public static void receivePackets() {
        DatagramPacket datagramPacket  = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        try {
            while (Main.running) {
                socketReceive.receive(datagramPacket);
                //System.out.println("Received packet.\n. Data: " + new String(datagramPacket.getData()) + " - Length: " + datagramPacket.getLength());
                Packet p = Packet.deserialize(datagramPacket.getData(), datagramPacket.getLength());

                if(p.isAckPacket()) {
                    PerfectLinks.ackReceived(p);
                }
                else {
                    PerfectLinks.receivedPacket(p);
                }
            }   
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        // System.out.println("NetworkInterface - Exiting receivePackets thread");
    } 

    public static void sendPacket(Packet packet) throws IOException {
        byte[] data = packet.serialize();
        socketSend.send(new DatagramPacket(data, data.length, targetAddress[packet.getTargetID() - 1]));
    }

    public static final byte[] intToBytes(int number) {
        byte[] byteList = new byte[4];

        // Extract bytes from int
        byteList[0] = (byte) ((number >> 24) & 0xFF); // Most significant byte
        byteList[1] = (byte) ((number >> 16) & 0xFF);
        byteList[2] = (byte) ((number >> 8) & 0xFF);
        byteList[3] = (byte) (number & 0xFF);          // Least significant byte

        return byteList;    
    }

    public static final int bytesToInt(byte[] byteList) {
        if (byteList == null || byteList.length < 4) {
            throw new IllegalArgumentException("The list must contain at least 4 bytes.");
        }

        // Combine the bytes back into an int
        int result = 0;
        result |= (byteList[0] & 0xFF) << 24; // Most significant byte
        result |= (byteList[1] & 0xFF) << 16;
        result |= (byteList[2] & 0xFF) << 8;
        result |= (byteList[3] & 0xFF);       // Least significant byte

        return result;
    }
}
