package cs451;

import java.util.TreeSet;

public class AckKeeper {
    private int maximumInOrderAck = -1;
    private TreeSet<Integer> acks = new TreeSet<>();

    static int maxAcksSize = 0;

    public AckKeeper() {}
    
    public boolean addAck(int ack) {
        if (isAcked(ack)) {
            return false;
        }

        if (ack == this.maximumInOrderAck + 1) {
            this.maximumInOrderAck++;
            removeAcks();
        } else this.acks.add(ack);

        // maxAcksSize = Math.max(maxAcksSize, this.acks.size());
        if(acks.size() > maxAcksSize) {
            maxAcksSize = acks.size();
        }
        
        return true;
    }

    private void removeAcks() {
        while (!acks.isEmpty() && acks.first() == this.maximumInOrderAck + 1) {
            this.maximumInOrderAck++;
            this.acks.pollFirst();
        }
    }

    public boolean isAcked(int ack) {
                return ack <= this.maximumInOrderAck || this.acks.contains(ack);
    }
}