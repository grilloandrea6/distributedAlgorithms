package cs451;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Packet {

    private int targetID;
    private byte[] data;

    static private int idCounter = 0;

    static private final Long INITIAL_TIMEOUT = 100L;

    private boolean isAckPacket;

    private int id;
    private int senderID;

    private Long interval;

    private Long timeout;
    

    Packet() {}

    Packet(byte[] data, int senderID, int targetID) {
        this.data = data;
        this.senderID = senderID;
        this.targetID = targetID;
        this.id = idCounter++;


        interval = INITIAL_TIMEOUT;
        timeout = System.currentTimeMillis() + interval;
    }

    public void backoff() {
        System.out.println("Backoff called - interval: " + interval);
        interval *= 2;
        timeout = System.currentTimeMillis() + interval;
    }

    public Long getTimeout() {
        return timeout;
    }

    public boolean isAckPacket() {
        return isAckPacket;
    }

    public int[] getAckedIds() {
        if(!isAckPacket)
            return null;
        
        IntBuffer intBuf = ByteBuffer.wrap(data).asIntBuffer();
        int[] array = new int[intBuf.remaining()];
        intBuf.get(array);
        return array;
    }

    public int getId() {
        return id;
    }

    public int getTargetID() {
        return targetID;
    }

    public byte[] serialize() {
        int dataLength = data != null ? data.length : 0;
        
        // Allocate ByteBuffer to hold all the fields
        ByteBuffer buffer = ByteBuffer.allocate(13 + dataLength);

        // Serialize fields
        buffer.putInt(id);                            // 4 bytes
        buffer.putInt(senderID);                      // 4 bytes
        buffer.put((byte) (isAckPacket ? 1 : 0));     // 1 byte for boolean
        buffer.putInt(dataLength);                    // 4 bytes for data length
        if (dataLength > 0) {
            buffer.put(data);                         // variable size
        }
        
        return buffer.array(); // Get the final byte array
    }

    public static Packet deserialize(byte[] byteArray, int length) {
        System.out.println("Deserializing packet");
        try{
            if(length < 13) {
                throw new Exception("Invalid packet size");
            }

            ByteBuffer buffer = ByteBuffer.wrap(byteArray);
            
            Packet packet = new Packet();
            
            // Deserialize fields
            packet.id = buffer.getInt();                         // 4 bytes
            packet.senderID = buffer.getInt();                   // 4 bytes
            packet.isAckPacket = buffer.get() == 1;              // 1 byte for boolean
            int dataLength = buffer.getInt();                    // 4 bytes
            if (dataLength > 0) {
                packet.data = new byte[dataLength];
                buffer.get(packet.data, 0, dataLength);          // variable size
            }
            
            return packet;

        } catch (Exception e) {
            System.out.println("Failed to deserialize packet.");
            return null;
        }
        
    }

    public static Packet createAckPacket(Packet p) {
        Packet packet = new Packet();
        packet.isAckPacket = true;
        packet.data = ByteBuffer.allocate(4).putInt(p.id).array();
        packet.targetID = p.senderID;
        return packet;
    }

    public byte[] getData() {
        return data;
    }

    public int getSenderID() {
        return senderID;
    }

}
