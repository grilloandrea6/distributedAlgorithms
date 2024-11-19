package cs451;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class PerfectLinks {
    static Parser parser;

    private static List<AtomicInteger> windowSize;
    private static List<List<Packet>> sendingQueue;

    public final static int WINDOW_MAX_SIZE = 10; // ToDo mmmmm

    private static List<Lock> locks;
    private static int[] hosts;


    private static PriorityBlockingQueue<Packet> waitingForAck;

    static void begin(Parser p) {
        parser = p;


        hosts = parser.hosts().stream().map(Host::getId).mapToInt(Integer::intValue).toArray();

        waitingForAck = new PriorityBlockingQueue<Packet>(hosts.length * WINDOW_MAX_SIZE, (p1, p2) -> p1.getTimeout().compareTo(p2.getTimeout()));

        locks = new ArrayList<>(hosts.length);
        windowSize = new ArrayList<>(hosts.length);
        sendingQueue = new ArrayList<>(hosts.length);

        for(int i = 0; i < hosts.length; i++) {
            locks.add(new ReentrantLock());
            windowSize.add(new AtomicInteger(0));
            sendingQueue.add(new LinkedList<Packet>());
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

    public static void perfectSend(List<Byte> data, int deliveryHost) {   
        Packet p = new Packet(data, parser.myId(), deliveryHost);

        Lock lock = locks.get(deliveryHost - 1);
        lock.lock();
        try {
            sendingQueue.get(deliveryHost - 1).add(p);
        } finally {
            lock.unlock();
        }
    }

    private static void sendingThread() {
        while(Main.running) {
            try {
                Lock lock;
                for(int hostId : hosts) {
                    Packet p = null;

                    lock = locks.get(hostId - 1);
                    if(lock.tryLock()) {
                        try{
                            List<Packet> currentSendingQueue = sendingQueue.get(hostId - 1);

                            if(windowSize.get(hostId - 1).get() <= WINDOW_MAX_SIZE && currentSendingQueue.size() > 0) {
                                p = currentSendingQueue.remove(0);
                                windowSize.get(hostId - 1).incrementAndGet();
                            }
                        }
                        finally {
                            lock.unlock();
                        }

                        if(p != null) {
                            NetworkInterface.sendPacket(p);
                            p.setTimeout();
                            waitingForAck.put(p);
                        }
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
        // just checked before calling this function
        // if(!packet.isAckPacket())
        //     return;

        int senderId = packet.getSenderID();
        
        // int ackedId[] = packet.getAckedIds(); // ToDo do I really want multiple acks?
        // for (int id : ackedIds) {
        //     waitingForAck.removeIf(p -> p.getTargetID() == senderId && p.getId() == id);
        // }

        // synchronized(PerfectLinks.class) {
        //     windowSize.put(senderId, windowSize.get(senderId) - ackedIds.length);
        //     PerfectLinks.class.notify();
        // }

        int ackedId = packet.getAckedIds();


        long time = System.nanoTime();

        //waitingForAck.removeIf(p -> p.getTargetID() == senderId && p.getId() == ackedId);
        for( Packet p : waitingForAck) {
            if(p.getTargetID() == senderId && p.getId() == ackedId) {
                waitingForAck.remove(p);
                break;
            }
        }
        
        time = System.nanoTime() - time;
        timeForLockAckReceived = ALPHA * time + (1 - ALPHA) * timeForLockAckReceived;
        if(time > maximumTimeForLockAckReceived) {
            maximumTimeForLockAckReceived = time;
        }
        if(time > 1000000) {
            nTimesOverMillisecond++;
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
