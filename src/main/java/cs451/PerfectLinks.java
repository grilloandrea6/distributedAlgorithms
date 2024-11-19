package cs451;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PerfectLinks {
    static Parser parser;

    private static List<AtomicInteger> windowSize;
    private static List<Queue<Packet>> sendingQueue;

    public final static int WINDOW_MAX_SIZE = 10; // ToDo mmmmm

    private static int[] hosts;

    private static PriorityBlockingQueue<Packet> waitingForAck;

    static void begin(Parser p) {
        parser = p;

        hosts = parser.hosts().stream().map(Host::getId).mapToInt(Integer::intValue).toArray();

        waitingForAck = new PriorityBlockingQueue<Packet>(hosts.length * WINDOW_MAX_SIZE, (p1, p2) -> p1.getTimeout().compareTo(p2.getTimeout()));

        windowSize = new ArrayList<>(hosts.length);
        sendingQueue = new ArrayList<>(hosts.length);

        for(int i = 0; i < hosts.length; i++) {
            windowSize.add(new AtomicInteger(0));
            sendingQueue.add(new ConcurrentLinkedQueue<>());
        }

        new Thread(PerfectLinks::retransmitThread).start();

        new Thread(PerfectLinks::sendingThread).start();
    }

    private static void retransmitThread() {
        // System.out.println("PerfectLinksSender retransmitThread started");
        while (Main.running) {
            try {
                Packet packet = waitingForAck.take();
                
                Long actualTime = System.currentTimeMillis();
                if (packet.getTimeout() <= actualTime) {
                    System.out.println("retransmit Thread - Retransmitting packet with id: " + packet.getId() + "to target " + packet.getTargetID());

                    NetworkInterface.sendPacket(packet);
                    packet.backoff();
                    waitingForAck.put(packet);

                    Thread.yield();

                } else {
                    // System.out.println("retransmit Thread - Not retransmitting packet with id: " + packet.getId());

                    waitingForAck.put(packet);
                    Thread.sleep(packet.getTimeout() - actualTime); 
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Exiting retransmit thread");
    }

    static int maxQueueSize = 0;

    public static void perfectSend(List<Byte> data, int deliveryHost) {   
        Packet p = new Packet(data, parser.myId(), deliveryHost);

        sendingQueue.get(deliveryHost - 1).add(p);
        if(sendingQueue.get(deliveryHost - 1).size() > maxQueueSize) {
            maxQueueSize = sendingQueue.get(deliveryHost - 1).size();
        }
    }

    private static void sendingThread() {
        while(Main.running) {
            try {
                for(int hostId : hosts) {
                    Queue<Packet> currentSendingQueue = sendingQueue.get(hostId - 1);

                    if(windowSize.get(hostId - 1).get() <= WINDOW_MAX_SIZE && currentSendingQueue.size() > 0) {
                        Packet p = currentSendingQueue.poll();
                        windowSize.get(hostId - 1).incrementAndGet();
                    
                        NetworkInterface.sendPacket(p);
                        p.setTimeout();
                        waitingForAck.put(p);
                    }
                }
                Thread.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Exiting sending thread");
    }


    static double timeForLockAckReceived = 0.;
    static double maximumTimeForLockAckReceived = 0.;
    static int nTimesOverMillisecond = 0;
    static final double ALPHA = 0.25;

    public static void ackReceived(Packet packet) {
        int senderId = packet.getSenderID();

        int ackedId = packet.getAckedIds();

        for( Packet p : waitingForAck) {
            if(p.getTargetID() == senderId && p.getId() == ackedId) {
                waitingForAck.remove(p);
                break;
            }
        }
        windowSize.get(senderId - 1).decrementAndGet();
    }

    // receiver part
    static private Map<Integer, AckKeeper> ackKeeperMap = new HashMap<>();

    public static void receivedPacket(Packet packet) {
        // System.out.println("PerfectLinks receivedPacket called");

        int senderId = packet.getSenderID();

        if(!ackKeeperMap.containsKey(senderId))
            ackKeeperMap.put(senderId, new AckKeeper());

        AckKeeper ackKeeper = ackKeeperMap.get(senderId);

        Packet ackPacket = Packet.createAckPacket(packet);

        //System.out.println("PERFECT_LINKS - sending ack packet for packet id " + packet.getId() + " to host " + packet.getSenderID());
        NetworkInterface.sendPacket(ackPacket);

        if(ackKeeper.isAcked(packet.getId())) {
            // System.out.println("Already acked");
        } else {
            // System.out.println("Not acked");
            ackKeeper.addAck(packet.getId());
            
            //OutputLogger.logDeliver(packet.getSenderID(), packet.getData());
            FIFOUniformReliableBroadcast.receivePacket(packet.getSenderID(), packet.getData());
        }

    }
}
