package cs451;

import java.util.HashMap;
import java.util.Map;

public class PerfectLinksReceiver {
    
    static private Map<Integer, AckKeeper> ackKeeperMap = new HashMap<>();

    public static void receivedPacket(Packet packet) {
        // System.out.println("PerfectLinksReceiver receivedPacket called");

        int senderId = packet.getSenderID();

        if(!ackKeeperMap.containsKey(senderId))
            ackKeeperMap.put(senderId, new AckKeeper());

        AckKeeper ackKeeper = ackKeeperMap.get(senderId);

        if(ackKeeper.isAcked(packet.getId())) {
            // System.out.println("Already acked");
        } else {
            // System.out.println("Not acked");
            ackKeeper.addAck(packet.getId());
            OutputLogger.logDeliver(packet.getSenderID(), packet.getData());
        }

        Packet ackPacket = Packet.createAckPacket(packet);

        NetworkInterface.sendPacket(ackPacket);
    }
}
