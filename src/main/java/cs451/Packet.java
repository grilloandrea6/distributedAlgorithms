package cs451;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Packet {

    private int targetID;
    private byte[] data;

    static private int idCounter = 0;

    static private final Long INITIAL_TIMEOUT = 1000L;

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

    public byte[] serialize() { //TODO check if something can be removed
        // Determine the size of the final byte array based on the sizes of all fields
        int dataLength = data != null ? data.length : 0;
        
        // Allocate ByteBuffer to hold all the fields
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + dataLength + 1 + 4 + 4 + 8 + 8);

        // Serialize the fields
        buffer.putInt(targetID);                      // 4 bytes
        buffer.putInt(dataLength);                    // 4 bytes for data length
        if (dataLength > 0) {
            buffer.put(data);                         // variable size
        }
        buffer.put((byte) (isAckPacket ? 1 : 0));     // 1 byte for boolean
        buffer.putInt(id);                            // 4 bytes
        buffer.putInt(senderID);                      // 4 bytes
        buffer.putLong(interval != null ? interval : 0L);   // 8 bytes
        buffer.putLong(timeout != null ? timeout : 0L);     // 8 bytes

        return buffer.array(); // Get the final byte array
    }

    public static Packet deserialize(byte[] byteArray) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        
        Packet packet = new Packet();
        
        // Deserialize fields
        packet.targetID = buffer.getInt();                   // 4 bytes
        int dataLength = buffer.getInt();                    // 4 bytes
        if (dataLength > 0) {
            packet.data = new byte[dataLength];
            buffer.get(packet.data, 0, dataLength);          // variable size
        }
        packet.isAckPacket = buffer.get() == 1;              // 1 byte for boolean
        packet.id = buffer.getInt();                         // 4 bytes
        packet.senderID = buffer.getInt();                   // 4 bytes
        packet.interval = buffer.getLong();                  // 8 bytes
        packet.timeout = buffer.getLong();                   // 8 bytes

        return packet;
    }

}
