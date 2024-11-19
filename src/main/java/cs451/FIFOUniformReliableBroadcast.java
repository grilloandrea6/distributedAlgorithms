package cs451;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FIFOUniformReliableBroadcast {

    static Integer hostNumber;
    static Integer myId;

    static Integer seqNumber = 0;

    // pending
    static HashMap<FIFOUrbPacket, FIFOUrbPacket> pending = new HashMap<>();

    static final int MAX_WINDOW_SIZE = 5;
    static int windowSize = 0;

    // delivered
    // static HashSet<FIFOUrbPacket> delivered = new HashSet<>(); //ToDo improve using hashmap of ackkeeper
    static List<FIFOKeeper> delivered;

    // acked
    static HashMap<FIFOUrbPacket, Integer> acked = new HashMap<>();


    static BlockingQueue<FIFOUrbPacket> deliveryQueue = new LinkedBlockingQueue<>();

    static void begin(Parser p) {
        PerfectLinks.begin(p);

        hostNumber = p.hosts().size();
        myId = p.myId();

        delivered = new ArrayList<>(hostNumber);
        for(int i = 0; i < hostNumber; i++) {
            delivered.add(new FIFOKeeper());
        }

        new Thread(FIFOUniformReliableBroadcast::processReceivedPackets).start();
    }

    static void broadcast(List<Byte> data) {
        // System.out.println("FIFOUniformReliableBroadcast - Broadcasting message");

        OutputLogger.logBroadcast(data);

        int seq = seqNumber++;

        FIFOUrbPacket packet = new FIFOUrbPacket(myId, seq, data);

        synchronized(FIFOUniformReliableBroadcast.class) {
            while(windowSize >= MAX_WINDOW_SIZE) {
                try {
                    // System.out.println("FIFOUniformReliableBroadcast - window full, waiting");
                    FIFOUniformReliableBroadcast.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            windowSize++;
        }

        // mando a tutti
        internalBroadcast(packet);
        
        // aggiungo ad acked
        acked.put(packet, 1); //myself

        // aggiungo a pending
        pending.put(packet, packet);
    }

    static void internalBroadcast(FIFOUrbPacket packet) {
        List<Byte> serialized = packet.serialize();

        for(int deliveryHost = 1; deliveryHost <= hostNumber; deliveryHost++) {
            // do not send to myself
            if(deliveryHost == myId)
                continue;
        
            try{
                PerfectLinks.perfectSend(serialized, deliveryHost);
            } catch (Exception e) {
                System.out.println("Error sending to host " + deliveryHost);
                e.printStackTrace();
            } 
        }
    }

    public static void processReceivedPackets() {
        while (Main.running) {
            try {
                FIFOUrbPacket packet = deliveryQueue.take();
                //System.out.println("Received packet from " + packet.sender + " with orig sender " + packet.origSender + " seq: " + packet.seq);

                if(!delivered.get(packet.origSender - 1).isDelivered(packet)) {
                //if(!delivered.contains(packet)) {
                    acked.put(packet, acked.getOrDefault(packet, 0) + 1);
                    // System.out.println("\tacked for packet is " + acked.get(packet));

                    if(!pending.containsKey(packet)) {
                        // System.out.println("\tAdding to pending and broadcasting");
                        pending.put(packet, packet);
                        internalBroadcast(packet);

                    } else if(acked.get(packet) >= hostNumber/2) {
                        
                        delivered.get(packet.origSender - 1).addMessage(packet);

                        // // System.out.println("\tDelivering packet");
                        // OutputLogger.logDeliver(packet.origSender, packet.data);
                        // delivered.add(packet);
                        
                        acked.remove(packet);
                        pending.remove(packet);
                        if(packet.origSender == myId) {
                            synchronized(FIFOUniformReliableBroadcast.class) {
                                windowSize--;
                                FIFOUniformReliableBroadcast.class.notify();
                            }
                        }
                    }

                } // else System.out.println("\tAlready delivered, not doing anything");
                Thread.yield();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("FIFOUniformReliableBroadcast - Exiting processReceivedPackets thread");
    } 


    static void receivePacket(int senderId, List<Byte> data) {        
        try{
            // System.out.println("received packet, enqueuing");
            FIFOUrbPacket packet = FIFOUrbPacket.deserialize(data, senderId);

            deliveryQueue.put(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
