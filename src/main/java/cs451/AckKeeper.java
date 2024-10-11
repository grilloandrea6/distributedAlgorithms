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

    public void addAck(int ack) {
        if(nacks.contains(ack)) {
            nacks.remove(ack);
        } else if (ack == this.maximumInOrderAck + 1) {
            this.maximumInOrderAck++;
            removeAcks();
        } else if(acks.size() >= MAXIMUM_ACK_SIZE) {
            this.nacks.add(++this.maximumInOrderAck);
            removeAcks();
            addAck(ack);
        } else {
            this.acks.add(ack);
        }
    }

    private void removeAcks() {
        for (int ack : acks) {
            if (ack == this.maximumInOrderAck + 1) {
                this.maximumInOrderAck++;
                this.acks.remove(ack);
                removeAcks();
                return;
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
