package cs451;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class PerfectLinksSender {
    static Parser parser;

    private static Integer windowSize = 0;

    public final static int WINDOW_MAX_SIZE = 5; 

    private static Packet currentPacket = null;

    private static Lock lock = new ReentrantLock();

    private static PriorityBlockingQueue<Packet> waitingForAck = new PriorityBlockingQueue<Packet>(WINDOW_MAX_SIZE, (p1, p2) -> p1.getTimeout().compareTo(p2.getTimeout()));

    static void begin(Parser p) {
        parser = p;

        System.out.println("PerfectLinksSender constructor called");

        new Thread(PerfectLinksSender::retransmitThread).start();

        sendTimer();
    }

    private static void sendTimer() {
        // Create a new Timer instance
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if(currentPacket == null) return;
                    
                    if(currentPacket.getCreationTime() < System.currentTimeMillis() - 100) { // ToDo parametrize
                        internalSend(currentPacket);
                        // System.out.println("Timer - sending packet with id: " + currentPacket.getId());
                        currentPacket = null;
                    }
                }
                finally {
                    lock.unlock();
                }
                
            }
        };

        int delay = 500;       // Initial delay (in milliseconds)
        int period = 500;   // Time between executions (in milliseconds) => 5 seconds

        // Schedule the task to run every 'period' milliseconds after an initial delay
        timer.scheduleAtFixedRate(task, delay, period);

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



    public static void perfectSend(List<Byte> data, int deliveryHost) throws InterruptedException {        
        lock.lock();
        try {

            if(currentPacket == null) { // if null i just create a new packet with data
                currentPacket = new Packet(data, parser.myId(), deliveryHost);
            } else if(currentPacket.spaceAvailable(data.size())) { // if existing and I can add to it, just add
                currentPacket.addData(data);
            } else { // if existing and full, send it and create a new one - this could block if windows is full
                //System.out.println("packet full - sending and creating new");
                internalSend(currentPacket);
                currentPacket = new Packet(data, parser.myId(), deliveryHost);
            }
            OutputLogger.logBroadcast(data);
        }
        finally {
            lock.unlock();
        }
        
    }

    private static void internalSend(Packet packet) {
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

        NetworkInterface.sendPacket(packet);
        packet.setTimeout();
        waitingForAck.put(packet);
    }

}
