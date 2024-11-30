package cs451;

import java.util.Set;

import javax.imageio.IIOException;

import java.io.IOException;
import java.util.HashSet;
import java.util.PriorityQueue;

public class FIFOKeeper {
    private int maximumInOrderMessage;
    private PriorityQueue<FIFOUrbPacket> pending;

    static int maxSetSize = 0;

    public FIFOKeeper() {
        this.maximumInOrderMessage = -1;
        this.pending = new PriorityQueue<>((p1,p2) -> p1.seq - p2.seq);
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
        FIFOUrbPacket packet = this.pending.peek();

        while(packet != null) {
            if (packet.seq != this.maximumInOrderMessage + 1)
                return;

            this.maximumInOrderMessage++;
            OutputLogger.logDeliver(packet.origSender, packet.data);

            this.pending.poll();
        
            packet = this.pending.peek();
        }
    }

    public boolean isDelivered(FIFOUrbPacket packet) {
        if(packet.seq <= this.maximumInOrderMessage)
            return true;
        
        return this.pending.contains(packet);
    }
}
