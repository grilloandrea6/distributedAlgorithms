package cs451;

import java.util.List;
import java.util.LinkedList;

public class AckKeeper {
    private int maxAck;
    private List<Integer> acks;

    public AckKeeper() {
        this.maxAck = 0;
        this.acks = new LinkedList<>();
    }

    public void addAck(int ack) {
        System.out.println("Received ack: " + ack);
        if (ack == this.maxAck + 1) {
            System.out.println("Ack is in order, just incrementing maxAck");
            this.maxAck++;
            removeAcks();
        } else {
            System.out.println("Ack is not in order, adding to list");
            this.acks.add(ack);
        }
        System.out.println("Current maxAck: " + this.maxAck + "\n\n\n");
    }

    private void removeAcks() {
        System.out.println("Removing acks");
        this.acks.forEach(ack -> {
            if (ack == this.maxAck + 1) {
                this.maxAck++;
                this.acks.remove(ack);
                removeAcks();
                return;
            }
        });
    }

    public boolean isAcked(int ack) {
        if(ack >= this.maxAck) {
            return true;
        }
        return this.acks.contains(ack);
    }
}
