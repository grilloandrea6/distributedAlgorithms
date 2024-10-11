package cs451;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class PerfectLinksSender {
    static Parser parser;

    private static Integer windowSize = 0;

    public final static int WINDOW_MAX_SIZE = 5; 

    private static PriorityBlockingQueue<Packet> waitingForAck = new PriorityBlockingQueue<Packet>(WINDOW_MAX_SIZE, (p1, p2) -> p1.getTimeout().compareTo(p2.getTimeout()));

    static void begin(Parser p) {
        parser = p;

        System.out.println("PerfectLinksSender constructor called");

        new Thread(PerfectLinksSender::retransmitThread).start();
    }

    private static void retransmitThread() {
        System.out.println("PerfectLinksSender retransmitThread started");
        while (Main.running) { // ToDo if it is okay to stop this way
            try {
                Packet packet = waitingForAck.take();
                
                Long actualTime = System.currentTimeMillis();
                if (packet.getTimeout() <= actualTime) {
                    System.out.println("retransmit Thread - Retransmitting packet with id: " + packet.getId());

                    NetworkInterface.sendPacket(packet);
                    packet.backoff();
                    waitingForAck.put(packet);

                } else {
                    System.out.println("retransmit Thread - Not retransmitting packet with id: " + packet.getId());

                    waitingForAck.put(packet);
                    Thread.sleep(packet.getTimeout() - actualTime); 
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.err.println("Exiting retransmit thread");
    }

    public static void ackReceived(Packet packet) {
        if(!packet.isAckPacket())
            return;
        
        int ackedIds[] = packet.getAckedIds();
        for (int id : ackedIds) {
            waitingForAck.removeIf(p -> p.getId() == id);
        }

        synchronized(PerfectLinksSender.class) {
            windowSize -= ackedIds.length;
            PerfectLinksSender.class.notify();
        }
    }



    public static void perfectSend(byte[] data, int deliveryHost) throws InterruptedException {
        // todo serialize multiple send in the same packet
        synchronized(PerfectLinksSender.class) {
            while(windowSize >= WINDOW_MAX_SIZE) {
                try {
                    System.out.println("PerfectLinksSender - window full, waiting");
                    PerfectLinksSender.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("PerfectLinksSender - window not full, sending" + windowSize);
            windowSize++;
        }

        Packet packet = new Packet(data, parser.myId(), deliveryHost);
        NetworkInterface.sendPacket(packet);
        waitingForAck.put(packet); 
        OutputLogger.logBroadcast(data);
    }

}
