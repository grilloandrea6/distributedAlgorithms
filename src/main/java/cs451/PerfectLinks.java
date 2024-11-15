package cs451;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class PerfectLinks {
    static Parser parser;

    private static Integer windowSize = 0;

    public final static int WINDOW_MAX_SIZE = 5; // ToDo mmmmm

    private static Packet currentPacket = null;

    private static Lock lock = new ReentrantLock();

    private static PriorityBlockingQueue<Packet> waitingForAck = new PriorityBlockingQueue<Packet>(WINDOW_MAX_SIZE, (p1, p2) -> p1.getTimeout().compareTo(p2.getTimeout()));

    static void begin(Parser p) {
        parser = p;

        new Thread(PerfectLinks::retransmitThread).start();

        //sendTimer(); ToDo not run
    }

    // private static void sendTimer() {
    //     // Create a new Timer instance
    //     Timer timer = new Timer();

    //     TimerTask task = new TimerTask() {
    //         @Override
    //         public void run() {
    //             lock.lock();
    //             try {
    //                 if(currentPacket == null) return;
                    
    //                 if(currentPacket.getCreationTime() < System.currentTimeMillis() - 100) { // ToDo parametrize
    //                     internalSend(currentPacket);
    //                     // System.out.println("Timer - sending packet with id: " + currentPacket.getId());
    //                     currentPacket = null;
    //                 }
    //             }
    //             finally {
    //                 lock.unlock();
    //             }
                
    //         }
    //     };

    //     int delay = 500;       // Initial delay (in milliseconds)
    //     int period = 500;   // ToDo Time between executions (in milliseconds) => 5 seconds

    //     // Schedule the task to run every 'period' milliseconds after an initial delay
    //     timer.scheduleAtFixedRate(task, delay, period);

    // }

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

                } else {
                    // System.out.println("retransmit Thread - Not retransmitting packet with id: " + packet.getId());

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
        
        int ackedIds[] = packet.getAckedIds(); // ToDo do I really want multiple acks?
        for (int id : ackedIds) {
            waitingForAck.removeIf(p -> p.getTargetID() == packet.getSenderID() && p.getId() == id);
        }

        synchronized(PerfectLinks.class) {
            windowSize -= ackedIds.length;
            PerfectLinks.class.notify();
        }
    }



    public static void perfectSend(List<Byte> data, int deliveryHost) throws InterruptedException {   
        
     
        lock.lock();
        try {

            // if(currentPacket == null) { // if null i just create a new packet with data
            // System.out.println("sending packet!");
                currentPacket = new Packet(data, parser.myId(), deliveryHost);


                //Todo Todo added this to spedup the process
                internalSend(currentPacket);
                currentPacket = null;
            // } else if(currentPacket.spaceAvailable(data.size())) { // if existing and I can add to it, just add
            //     currentPacket.addData(data);
            // } else { 
                
            //     // if existing and full, send it and create a new one - this could block if windows is full
            //     //System.out.println("packet full - sending and creating new");
            //     internalSend(currentPacket);
            //     currentPacket = new Packet(data, parser.myId(), deliveryHost);
            // }
            //OutputLogger.logBroadcast(data);
        }
        finally {
            lock.unlock();
        }
        
    }

    private static void internalSend(Packet packet) {
        synchronized(PerfectLinks.class) {
            while(windowSize >= WINDOW_MAX_SIZE) {
                try {
                    // System.out.println("PerfectLinksSender - window full, waiting");
                    PerfectLinks.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // System.out.println("PerfectLinksSender - window not full, sending" + windowSize);
            windowSize++;
        }

        NetworkInterface.sendPacket(packet);
        packet.setTimeout();
        waitingForAck.put(packet);
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
