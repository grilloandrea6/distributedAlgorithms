package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;


public class NetworkInterface {
    public static Parser parser;

    private static DatagramSocket socketReceive, socketSend;

    public static void begin(Parser p) throws SocketException {
        parser = p;

        int port = parser.hosts().get(parser.myId() - 1).getPort();

        // System.out.println("My ID: " + parser.myId() + " - Port:" + port);
        socketReceive = new DatagramSocket(port);
        // System.out.println("Listening socket created on port " + port);
        socketSend = new DatagramSocket();

        new Thread(NetworkInterface::receivePackets).start();
    }

    public static void receivePackets() {
        DatagramPacket datagramPacket  = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
        try {
            while (Main.running) {
                socketReceive.receive(datagramPacket);
                //System.out.println("Received packet.\n. Data: " + new String(datagramPacket.getData()) + " - Length: " + datagramPacket.getLength());
                Packet p = Packet.deserialize(datagramPacket.getData(), datagramPacket.getLength());
                if (p != null) {
                    if(p.isAckPacket()) {
                        PerfectLinks.ackReceived(p);
                    }
                    else {
                        PerfectLinks.receivedPacket(p);
                    }
                } else System.err.println("Invalid packet received!");
            }   
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        // System.out.println("NetworkInterface - Exiting receivePackets thread");
    } 

    public static void sendPacket(Packet packet) throws IOException {
        Host targetHost = parser.hosts().get(packet.getTargetID() - 1);
        InetSocketAddress targetAddress = new InetSocketAddress(targetHost.getIp(), targetHost.getPort());
  
        byte[] data = packet.serialize();
        socketSend.send(new DatagramPacket(data, data.length, targetAddress));
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
