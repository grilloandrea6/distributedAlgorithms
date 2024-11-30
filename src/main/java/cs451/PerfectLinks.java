package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PerfectLinks {
    static Parser parser;

    private static List<AtomicInteger> windowSize;
    private static List<BlockingQueue<Packet>> sendingQueue;

    public final static int WINDOW_MAX_SIZE = 5; // ToDo mmmmm

    private static int[] hosts;

    private static PriorityBlockingQueue<Packet> waitingForAck;
    private static ConcurrentHashMap<Packet, Integer> acked = new ConcurrentHashMap<>();

    static private List<AckKeeper> ackKeeperList;

    static int nRetrasmissions = 0;

    private static final Object lock = new Object();

    static void begin(Parser p) {
        parser = p;

        hosts = parser.hosts().stream().map(Host::getId).mapToInt(Integer::intValue).toArray();

        waitingForAck = new PriorityBlockingQueue<Packet>(hosts.length * WINDOW_MAX_SIZE, (p1, p2) -> p1.getTimeout().compareTo(p2.getTimeout()));

        windowSize = new ArrayList<>(hosts.length);
        sendingQueue = new ArrayList<>(hosts.length);
        ackKeeperList = new ArrayList<>(hosts.length);

        for(int i = 0; i < hosts.length; i++) {
            windowSize.add(new AtomicInteger(0));
            sendingQueue.add(new ArrayBlockingQueue<>(800));
            ackKeeperList.add(new AckKeeper());
        }

        try {
            NetworkInterface.begin(parser);
        } catch (SocketException e) {
            System.err.println("Error starting NetworkInterface: " + e.getMessage());
        }


        new Thread(PerfectLinks::retransmitThread).start();

        new Thread(PerfectLinks::sendingThread).start();

        //new Thread(PerfectLinks::ackSenderThread).start();
    }

    private static void retransmitThread() {
        // System.out.println("PerfectLinksSender retransmitThread started");
        try {
            while (Main.running) {
                Packet packet = waitingForAck.take();

                if(acked.remove(packet) != null) {
                    continue;
                }
                
                Long actualTime = System.currentTimeMillis();
                if (packet.getTimeout() <= actualTime) {
                    // System.out.println("retransmit Thread - Retransmitting packet with id: " + packet.getId() + "to target " + packet.getTargetID());
                    nRetrasmissions++;

                    NetworkInterface.sendPacket(packet);  
                    packet.backoff();
                    waitingForAck.put(packet);


                } else {
                    // System.out.println("retransmit Thread - Not retransmitting packet with id: " + packet.getId() + "to target " + packet.getTargetID());

                    waitingForAck.put(packet);
                    Thread.sleep(packet.getTimeout() - actualTime); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Exiting retransmit thread");
    }

    static int maxQueueSize = 0;

    public static void perfectSend(List<Byte> data, int deliveryHost) throws InterruptedException {   
        Packet p = new Packet(data, parser.myId(), deliveryHost);

        sendingQueue.get(deliveryHost - 1).put(p);
        
        if(sendingQueue.get(deliveryHost - 1).size() > maxQueueSize) {
            maxQueueSize = sendingQueue.get(deliveryHost - 1).size();
        }
    }

    private static void sendingThread() {
        try {
            while(Main.running) {
                Boolean allEmpty = true;
                for(int hostId : hosts) {
                    Queue<Packet> currentSendingQueue = sendingQueue.get(hostId - 1);

                    if(windowSize.get(hostId - 1).get() <= WINDOW_MAX_SIZE) {
                        Packet p = currentSendingQueue.poll();
                        if(p == null)
                            continue;

                        windowSize.get(hostId - 1).incrementAndGet();
                    
                        allEmpty = false;
                        NetworkInterface.sendPacket(p);
                        p.setTimeout();
                        waitingForAck.put(p);
                    }
                }
                if(allEmpty) {
                    Thread.sleep(10);
                    // synchronized(lock) {
                    //     lock.wait();
                    // }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Exiting sending thread");
    }

    public static void ackReceived(Packet packet) {
        int senderId = packet.getSenderID();

        packet.targetID = packet.getSenderID();
        packet.id = packet.getAckedIds();

        acked.put(packet, 0);

        windowSize.get(senderId - 1).decrementAndGet();
    }

    static double timeForLockAckReceived = 0.;
    static double maximumTimeForLockAckReceived = 0.;
    static int nTimesOverMillisecond = 0;

    static double timeFor = 0.;
    static double maximumTimeFor = 0.;
    static int nTimesFor = 0;

    public static void receivedPacket(Packet packet) throws InterruptedException, IOException {
        // System.out.println("PerfectLinks receivedPacket called");
        int senderId = packet.getSenderID();

        Packet ackPacket = Packet.createAckPacket(packet);

        long time = System.nanoTime();

        NetworkInterface.sendPacket(ackPacket);

        time = System.nanoTime() - time;
        timeFor = NetworkInterface.ALPHA * time + (1 - NetworkInterface.ALPHA) * timeFor;
        if(time > maximumTimeFor) {
            maximumTimeFor = time;
        }
        if(time > 1000000) {
            nTimesFor++;
        }

        if(ackKeeperList.get(senderId - 1).addAck(packet.getId())) {
            time = System.nanoTime();

            FIFOUniformReliableBroadcast.receivePacket(packet.getSenderID(), packet.getData());

            time = System.nanoTime() - time;
            timeForLockAckReceived = NetworkInterface.ALPHA * time + (1 - NetworkInterface.ALPHA) * timeForLockAckReceived;
            if(time > maximumTimeForLockAckReceived) {
                maximumTimeForLockAckReceived = time;
            }
            if(time > 1000000) {
                nTimesOverMillisecond++;
            }
        }



    }
}
