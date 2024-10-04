package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// TODO: i don't think the sender thread is needed
// have one thread that receives packets and adds them to a blocking queue? or have a reference to a upper layer to deliver them
// a method to add a packet 

public class NetworkInterface {
    private Parser parser;

    private DatagramSocket socket;

    private final BlockingQueue<DatagramPacket> datagramsToSend = new LinkedBlockingQueue<>();

    public NetworkInterface(Parser parser) throws SocketException {
        this.parser = parser;

        System.out.println("My ID: " + parser.myId() + " - Port:" + parser.hosts().get(parser.myId()).getPort());
        socket = new DatagramSocket(parser.hosts().get(parser.myId()).getPort());
        System.out.println("Socket created on port " + parser.hosts().get(parser.myId()).getPort());
        ExecutorService workers = Executors.newFixedThreadPool(2);
        workers.execute(this::sendPackets);
        workers.execute(this::receivePackets);
    }


    public void receivePackets() {
        while (true) { // TODO NetworkInterface.running.get()) find way to stop gracefully 
            try {
                DatagramPacket datagramPacket  = new DatagramPacket(new byte[10], 10); // TODO ASSUME WE JUST SEND INTS
                socket.receive(datagramPacket);
                System.out.println("Received packet.\n. Data: " + new String(datagramPacket.getData()) + " - Length: " + datagramPacket.getLength());
                // ToDo what do i do with the packet? - StubbornLink.stubbornDeliver(new Packet(datagramPacket.getData()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } 
    
    public void addPacket(int packet, int targetID) { // TODO ASSUME WE JUST SEND INTS
        Host targetHost = parser.hosts().get(targetID);
        InetSocketAddress targetAddress = new InetSocketAddress(targetHost.getIp(), targetHost.getPort());
        byte[] buffer = intToByteArray(packet);
        try {
            datagramsToSend.put(new DatagramPacket(Arrays.copyOf(buffer, buffer.length), buffer.length, targetAddress));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } 
    }

    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >>  8),
                (byte)(value)
            };
    }

    public void sendPackets() {
        while (true) { //NetworkInterface.running.get()) {// TODO find a way to stop gracefully
            try {
                socket.send(datagramsToSend.take());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
