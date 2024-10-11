package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

// TODO: i don't think the sender thread is needed
// have one thread that receives packets and adds them to a blocking queue? or have a reference to a upper layer to deliver them
// a method to add a packet 

public class NetworkInterface {
    private static Parser parser;

    private static DatagramSocket socket;
    
    private static boolean senderOrReceiver;

    public static void begin(Parser p, boolean sendRec) throws SocketException {
        parser = p;
        senderOrReceiver = sendRec;

        int port = parser.hosts().get(parser.myId() - 1).getPort();

        System.out.println("My ID: " + parser.myId() + " - Port:" + port);
        socket = new DatagramSocket(port);
        System.out.println("Listening socket created on port " + port);

        new Thread(NetworkInterface::receivePackets).start();
    }

    public static void receivePackets() {
        while (Main.running) { // TODO NetworkInterface.running.get()) find way to stop gracefully 
            try {
                DatagramPacket datagramPacket  = new DatagramPacket(new byte[1000], 1000); // TODO 
                socket.receive(datagramPacket);
                //System.out.println("Received packet.\n. Data: " + new String(datagramPacket.getData()) + " - Length: " + datagramPacket.getLength());
                Packet p = Packet.deserialize(datagramPacket.getData(), datagramPacket.getLength());
                if (p != null) {
                    if(senderOrReceiver) PerfectLinksSender.ackReceived(p);
                    else PerfectLinksReceiver.receivedPacket(p);
                } else System.err.println("Invalid packet received!");
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.err.println("Exiting receivePackets thread");
    } 

    public static void sendPacket(Packet packet) {
        Host targetHost = parser.hosts().get(packet.getTargetID() - 1);
        InetSocketAddress targetAddress = new InetSocketAddress(targetHost.getIp(), targetHost.getPort());
        try {
            System.out.println("network interface - Sending packet to id " + packet.getTargetID() + " port " + targetHost.getPort());
            
            byte[] data = packet.serialize();
            socket.send(new DatagramPacket(Arrays.copyOf(data, data.length), data.length, targetAddress));
        } catch (IOException e) {
            System.out.println("Failed to send packet to " + packet.getTargetID());
            e.printStackTrace();
        } 
    }

    public static final byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static final int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
}
