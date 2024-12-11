package cs451;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Packet {

    static private final Long INITIAL_TIMEOUT = 600L;

    static public final int  MAX_PACKET_SIZE = 50;
    
    int targetID;
    
    private byte[] data;

    static private int idCounter = 0;

    private boolean isAckPacket;

    int id;
    private int senderID;

    private Long interval;

    private Long timeout;

    Long creationTime;

    Packet() {
        creationTime = System.currentTimeMillis();
    }

    Packet(byte[] data, int senderID, int targetID) {
        this.data = data;
        this.senderID = senderID;
        this.targetID = targetID;
        this.id = idCounter++;

        creationTime = System.currentTimeMillis();
    }

    public void setTimeout() {
        interval = INITIAL_TIMEOUT;
        timeout = System.currentTimeMillis() + interval;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void backoff() {
        // System.out.println("Backoff called - interval: " + interval);
        //interval = Math.min(interval * 2, 1000L);
        timeout = System.currentTimeMillis() + interval;
    }

    public Long getTimeout() {
        return timeout;
    }

    public boolean isAckPacket() {
        return isAckPacket;
    }

    public int getAckedIds() {
        // // Make sure the byteList size is a multiple of 4
        // if (data.size() % 4 != 0) {
        //     throw new IllegalArgumentException("Byte list size must be a multiple of 4");
        // }

        return NetworkInterface.bytesToInt(data);
    }

    public int getId() {
        return id;
    }

    public int getTargetID() {
        return targetID;
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
        packet.data = new byte[byteArray.length - 9];
        
        // Deserialize fields
        packet.id = buffer.getInt();                         // 4 bytes
        packet.senderID = buffer.getInt();                   // 4 bytes
        packet.isAckPacket = buffer.get() == 1;              // 1 byte for boolean
        packet.targetID = NetworkInterface.parser.myId();
        
        for(int i = 0; i < length - 9; i++) {
            packet.data[i] = buffer.get();
        }
        
        return packet;
    }

    public static Packet createAckPacket(Packet p) {
        Packet packet = new Packet();
        packet.isAckPacket = true;
        packet.data = NetworkInterface.intToBytes(p.id);
        packet.senderID = NetworkInterface.parser.myId();
        packet.targetID = p.senderID;
        return packet;
    }

    public byte[] getData() {
        return data;
    }

    public int getSenderID() {
        return senderID;
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
