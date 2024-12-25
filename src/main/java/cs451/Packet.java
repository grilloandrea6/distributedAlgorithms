package cs451;

import java.nio.ByteBuffer;

public class Packet {
    static private int idCounter = 0;

    static private final Long INITIAL_TIMEOUT = 200L;
    
    int id, senderID, targetID;
    byte[] data;
    boolean isAckPacket;
    Long timeout;

    Packet() {}
    
    Packet(byte[] data, int senderID, int targetID) {
        this.data = data;
        this.senderID = senderID;
        this.targetID = targetID;
        this.id = idCounter++;
    }

    public void setTimeout() {
        timeout = System.currentTimeMillis() + INITIAL_TIMEOUT;
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(9 + ((data != null) ? data.length : 0));

        // Serialize fields
        buffer.putInt(id);                            // 4 bytes
        buffer.putInt(senderID);                      // 4 bytes
        buffer.put((byte) (isAckPacket ? 1 : 0));     // 1 byte for boolean
        
        if(data != null) {
            for (byte b : data) {
                buffer.put(b);
            }
        }
        
        return buffer.array();
    }

    public static Packet deserialize(byte[] byteArray, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);

        Packet packet = new Packet();
        packet.data = new byte[length - 9];
        
        // Deserialize fields
        packet.id = buffer.getInt();                         // 4 bytes
        packet.senderID = buffer.getInt();                   // 4 bytes
        packet.isAckPacket = buffer.get() == 1;              // 1 byte for boolean
        packet.targetID = PerfectLinks.myId;
        
        for(int i = 0; i < length - 9; i++) {
            packet.data[i] = buffer.get();
        }
        
        return packet;
    }

    public static Packet createAckPacket(Packet p) {
        Packet packet = new Packet();
        packet.isAckPacket = true;
        packet.data = NetworkInterface.intToBytes(p.id);
        packet.senderID = PerfectLinks.myId;
        packet.targetID = p.senderID;
        return packet;
    }

    @Override
    public int hashCode() {
        return id * 961 + targetID;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Packet) {
            Packet p = (Packet) obj;
            return p.id == id && p.targetID == targetID;
        }
        return false;
    }

}
