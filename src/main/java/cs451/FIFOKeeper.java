package cs451;

import java.io.IOException;
import java.util.TreeSet;

public class FIFOKeeper {
    private int maximumInOrderMessage;
    private TreeSet<FIFOUrbPacket> pending;

    public FIFOKeeper() {
        this.maximumInOrderMessage = -1;
        this.pending = new TreeSet<>((p1,p2) -> p1.seq - p2.seq);
    }

    public void addMessage(FIFOUrbPacket packet) throws IOException {
        if (packet.seq == this.maximumInOrderMessage + 1) {
            OutputLogger.logDeliver(packet.origSender, packet.data);

            this.maximumInOrderMessage++;
            removeMessages();
        } else {
            this.pending.add(packet);
        }
    }

    private void removeMessages() throws IOException {
        while (!pending.isEmpty() && pending.first().seq == this.maximumInOrderMessage + 1) {
            this.maximumInOrderMessage++;
            FIFOUrbPacket p = this.pending.pollFirst();
            OutputLogger.logDeliver(p.origSender, p.data);
        } 
    }
    
    public boolean isDelivered(FIFOUrbPacket packet) {
        return packet.seq <= this.maximumInOrderMessage || this.pending.contains(packet);
    }
}
