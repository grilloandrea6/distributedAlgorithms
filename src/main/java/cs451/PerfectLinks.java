package cs451;

import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PerfectLinks {
    static Parser parser;

    private static AtomicInteger[] windowSize;
    private static LockFreeRingBuffer<Packet>[] sendingQueue;
    private static ConcurrentLinkedQueue<Packet>[] sendingOverflow;
    private static AckKeeper[] ackKeeperList;

    public final static int WINDOW_MAX_SIZE = 5; // ToDo mmmmm

    private static int nHosts;

    private static PriorityBlockingQueue<Packet> waitingForAck;

    static void begin(Parser p) {
        parser = p;

        nHosts = parser.hosts().size();

        waitingForAck = new PriorityBlockingQueue<Packet>(nHosts * WINDOW_MAX_SIZE, (p1, p2) -> p1.getTimeout().compareTo(p2.getTimeout()));

        windowSize = new AtomicInteger[nHosts];
        sendingQueue = new LockFreeRingBuffer[nHosts];
        sendingOverflow = new ConcurrentLinkedQueue[nHosts];
        ackKeeperList = new AckKeeper[nHosts];

        for(int i = 0; i < nHosts; i++) {
            windowSize[i] = new AtomicInteger(0);
            sendingQueue[i] = new LockFreeRingBuffer<>(1200);
            sendingOverflow[i] = new ConcurrentLinkedQueue<>();
            ackKeeperList[i] = new AckKeeper();
        }

        try {
            NetworkInterface.begin(parser);
        } catch (SocketException e) {
            System.err.println("Error starting NetworkInterface: " + e.getMessage());
        }


        new Thread(PerfectLinks::retransmitThread).start();

        new Thread(PerfectLinks::sendingThread).start();
    }

    private static void retransmitThread() {
        try {
            while (Main.running) {
                Long actualTime = System.currentTimeMillis();
                Packet packet = waitingForAck.take();

                if (packet.getTimeout() <= actualTime) {
                    // System.out.println("retransmit Thread - Retransmitting packet with id: " + packet.getId() + "to target " + packet.getTargetID());

                    packet.backoff();
                    waitingForAck.put(packet);
                    NetworkInterface.sendPacket(packet);  
                } else {
                    // System.out.println("retransmit Thread - Not retransmitting packet with id: " + packet.getId() + "to target " + packet.getTargetID());

                    waitingForAck.put(packet);
                    Thread.sleep(packet.getTimeout() - actualTime); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void perfectSend(byte[] data, int deliveryHost) throws InterruptedException {   
        Packet p = new Packet(data, parser.myId(), deliveryHost);

        if (!sendingQueue[deliveryHost - 1].offer(p)) {
            sendingOverflow[deliveryHost - 1].add(p);
        }
    }

    private static void sendingThread() {
        try {
            while(Main.running) {
                Boolean allEmpty = true;
                for(int hostId = 1; hostId <= nHosts; hostId++) {
                    if(windowSize[hostId - 1].get() <= WINDOW_MAX_SIZE) {
                        Packet p = sendingQueue[hostId - 1].poll();
                        if(p == null)
                            p = sendingOverflow[hostId - 1].poll();
                        if(p == null)
                            continue;

                        windowSize[hostId - 1].incrementAndGet();
                    
                        allEmpty = false;
                        NetworkInterface.sendPacket(p);
                        p.setTimeout();
                        waitingForAck.put(p);
                    }
                }
                if(allEmpty) {
                    Thread.sleep(10);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ackReceived(Packet packet) {
        int senderId = packet.getSenderID();

        packet.targetID = packet.getSenderID();
        packet.id = packet.getAckedIds();

        if(waitingForAck.remove(packet))
            windowSize[senderId - 1].decrementAndGet();
    }

    public static void receivedPacket(Packet packet) throws Exception {
        int senderId = packet.getSenderID();

        Packet ackPacket = Packet.createAckPacket(packet);

        NetworkInterface.sendPacket(ackPacket);

        if(ackKeeperList[senderId - 1].addAck(packet.getId())) {
            LatticeAgreement.receivePacket(packet.getSenderID(), packet.getData());
        }
    }
}
