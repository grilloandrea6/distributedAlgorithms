package cs451;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class FIFOUniformReliableBroadcast {

    static Integer hostNumber;
    static Integer myId;

    static Integer seqNumber = 0;

    static final int MAX_WINDOW_SIZE = 5;
    static int windowSize = 0;

    static List<FIFOKeeper> delivered;

    // pending
    static ConcurrentHashMap<FIFOUrbPacket, FIFOUrbPacket> pending = new ConcurrentHashMap<>();


    static void begin(Parser p) {
        hostNumber = p.hosts().size();
        myId = p.myId();

        delivered = new ArrayList<>(hostNumber);
        for(int i = 0; i < hostNumber; i++) {
            delivered.add(new FIFOKeeper());
        }
        PerfectLinks.begin(p);
    }

    static void broadcast(List<Byte> data) throws Exception {
        // System.out.println("FIFOUniformReliableBroadcast - Broadcasting message");

        OutputLogger.logBroadcast(data);

        int seq = seqNumber++;

        FIFOUrbPacket packet = new FIFOUrbPacket(myId, seq, data);

        synchronized(FIFOUniformReliableBroadcast.class) {
            while(windowSize >= MAX_WINDOW_SIZE) {
                // System.out.println("FIFOUniformReliableBroadcast - window full, waiting");
                FIFOUniformReliableBroadcast.class.wait();
            }
            windowSize++;
        }

        // aggiungo ad pending
        pending.put(packet, packet);

        // mando a tutti
        internalBroadcast(packet);
        
    }

    static void internalBroadcast(FIFOUrbPacket packet) throws InterruptedException {
        List<Byte> serialized = packet.serialize();

        for(int deliveryHost = 1; deliveryHost <= hostNumber; deliveryHost++) {
            // do not send to myself
            if(deliveryHost == myId)
                continue;
        
            PerfectLinks.perfectSend(serialized, deliveryHost);
        }
    }


    static void receivePacket(int senderId, List<Byte> data) throws InterruptedException, IOException {        
        FIFOUrbPacket packet = FIFOUrbPacket.deserialize(data, senderId);

        if(!delivered.get(packet.origSender - 1).isDelivered(packet)) {

            if(!pending.containsKey(packet)) {
                // System.out.println("\tAdding to pending and broadcasting");
                pending.put(packet, packet);
                internalBroadcast(packet);

            } else {
                FIFOUrbPacket p = pending.get(packet);
                p.ackReceived++;

                if(p.ackReceived >= hostNumber/2) {
                    delivered.get(packet.origSender - 1).addMessage(packet);
                    pending.remove(packet);

                    if(packet.origSender == myId) {
                        synchronized(FIFOUniformReliableBroadcast.class) {
                            windowSize--;
                            FIFOUniformReliableBroadcast.class.notify();
                        }
                    }
                }
            }
        } 
    }

}
