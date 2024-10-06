package cs451;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class PerfectLinksSender {
    Parser parser;

    public final static int TO_BE_SENT_CAPACITY = 10; //TODO

    BlockingQueue<Packet> toBeSent = new ArrayBlockingQueue<Packet>(TO_BE_SENT_CAPACITY);

    PriorityBlockingQueue<Packet> waitingForAck = new PriorityBlockingQueue<Packet>(TO_BE_SENT_CAPACITY, (p1, p2) -> p1.getTimeout().compareTo(p2.getTimeout()));
    


    PerfectLinksSender(Parser parser, OutputLogger outputLogger) {
        this.parser = parser;

        System.out.println("PerfectLinksSender constructor called");

        new Thread(this::sendersThread).start();
        new Thread(this::retransmitThread).start();

    }

    private void sendersThread() {
        System.out.println("PerfectLinksSender sendersThread started");
        while (true) {
            try {
                Packet packet = toBeSent.take();
                // TODO set timeout at actual time + initial timeout
                NetworkInterface.addPacket(packet);
                waitingForAck.put(packet);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void retransmitThread() {
        System.out.println("PerfectLinksSender retransmitThread started");
        while (true) {
            try {
                Packet packet = waitingForAck.take();
                
                Long actualTime = System.currentTimeMillis();
                if (packet.getTimeout() <= actualTime) {
                    System.out.println("retransmit Thread - Retransmitting packet with id: " + packet.getId());
                    NetworkInterface.addPacket(packet);
                    packet.backoff();
                    // TODO set timeout at actual time + initial timeout + backoff
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
    }

    public void ackReceived(Packet packet) {
        if(!packet.isAckPacket())
            return;

        for (int id : packet.getAckedIds()) {
            waitingForAck.removeIf(p -> p.getId() == id);
        }
    }



    public void perfectSend(byte[] data, int deliveryHost) throws InterruptedException {
        // blocking if queue is full
        toBeSent.put(new Packet(data, parser.myId(), deliveryHost));
    }

}
