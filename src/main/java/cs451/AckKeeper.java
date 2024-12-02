package cs451;

import java.util.Set;
import java.util.HashSet;

public class AckKeeper {
    private static final int MAXIMUM_ACK_SIZE = 20; // ToDo tune value

    private int maximumInOrderAck;
    private Set<Integer> acks;
    private Set<Integer> nacks;

    public AckKeeper() {
        this.maximumInOrderAck = -1;
        this.acks = new HashSet<>();
        this.nacks = new HashSet<>();
    }

    public boolean addAck(int ack) {
        if (isAcked(ack)) {
            return false;
        }
    
        if (nacks.contains(ack)) {
            nacks.remove(ack);
            return true;
        }

        while (true) {
            if (ack == this.maximumInOrderAck + 1) {
                this.maximumInOrderAck++;
                removeAcks();
                return true;
            }


            if (acks.size() >= MAXIMUM_ACK_SIZE) {
                this.nacks.add(++this.maximumInOrderAck);
                removeAcks();
                continue; 
            }
            
            this.acks.add(ack);
            return true;
        }
    }

    private void removeAcks() {
        while (true) {
            boolean ackFound = false;
            for (int ack : acks) {
                if (ack == this.maximumInOrderAck + 1) {
                    this.maximumInOrderAck++;
                    this.acks.remove(ack);
                    ackFound = true;
                    break; // Restart the loop to check for the next ack
                }
            }
            if (!ackFound) {
                break; // Exit the loop if no further acks are found
            }
        }
    }

    public boolean isAcked(int ack) {
        if(nacks.contains(ack))
            return false;
        
        if(ack <= this.maximumInOrderAck)
            return true;
        
        return this.acks.contains(ack);
    }
}