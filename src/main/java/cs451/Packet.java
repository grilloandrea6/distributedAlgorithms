package cs451;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Packet {

    static private final Long INITIAL_TIMEOUT = 600L;

    static public final int  MAX_PACKET_SIZE = 50;
    
    int targetID;
    
    private List<Byte> data;

    static private int idCounter = 0;

    private int numberOfMessages = 0;

    private boolean isAckPacket;

    int id;
    private int senderID;

    private Long interval;

    private Long timeout;

    Long creationTime;

    boolean hasBeenRetransmitted = false;

    Packet() {
        creationTime = System.currentTimeMillis();
    }

    Packet(List<Byte> data, int senderID, int targetID) {
        this.data = new ArrayList<>(MAX_PACKET_SIZE);

        //this.data.add((byte)data.size());
        this.data.addAll(data);

        this.numberOfMessages = 1;

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
        interval = Math.min(interval * 2, 1000L);
        //interval *= 2;
        timeout = System.currentTimeMillis() + interval;
    }

    public Long getTimeout() {
        return timeout;
    }

    public boolean isAckPacket() {
        return isAckPacket;
    }

    public int getAckedIds() { //ToDo do I really want multiple acks?
        // if(!isAckPacket)
        //     return null;

        // Make sure the byteList size is a multiple of 4
        if (data.size() % 4 != 0) {
            throw new IllegalArgumentException("Byte list size must be a multiple of 4");
        }
        
        // Create an int array where each int will represent 4 bytes
        // int[] intArray = new int[data.size() / 4];
        
        // Iterate over the byteList in steps of 4 bytes and combine them into a single int
        // for (int i = 0; i < intArray.length; i++) {
        //     intArray[i] = ((data.get(i * 4) & 0xFF) << 24) |  // First byte (most significant)
        //                   ((data.get(i * 4 + 1) & 0xFF) << 16) |  // Second byte
        //                   ((data.get(i * 4 + 2) & 0xFF) << 8) |   // Third byte
        //                   (data.get(i * 4 + 3) & 0xFF);           // Fourth byte (least significant)
        // }
        
        // return intArray;
        return NetworkInterface.bytesToInt(data);
    }

    public int getId() {
        return id;
    }

    public int getTargetID() {
        return targetID;
    }

    public byte[] serialize() {
        // Allocate ByteBuffer to hold all the fields
        ByteBuffer buffer = ByteBuffer.allocate(9 + ((data != null) ? data.size() : 0));

        // Serialize fields
        buffer.putInt(id);                            // 4 bytes
        buffer.putInt(senderID);                      // 4 bytes
        buffer.put((byte) (isAckPacket ? 1 : 0));     // 1 byte for boolean
        if(data != null) {
            data.forEach(buffer::put);
        }
        
        return buffer.array(); // Get the final byte array
    }

    public static Packet deserialize(byte[] byteArray, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);

        Packet packet = new Packet();
        packet.data = new ArrayList<>(byteArray.length - 9);
        
        // Deserialize fields
        packet.id = buffer.getInt();                         // 4 bytes
        packet.senderID = buffer.getInt();                   // 4 bytes
        packet.isAckPacket = buffer.get() == 1;              // 1 byte for boolean
        packet.targetID = NetworkInterface.parser.myId();
        
        for(length -= 9; length > 0; length--) {
            packet.data.add(buffer.get());
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

    public List<Byte> getData() {
        return data;
    }

    public int getSenderID() {
        return senderID;
    }

    public boolean spaceAvailable(int length) {
        return (data.size() + length + 9) < MAX_PACKET_SIZE && numberOfMessages < 1;
    }

    public void addData(List<Byte> data2) {
        data.add((byte)data2.size());
        data.addAll(data2);
        numberOfMessages++;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + targetID;
        return result;
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
