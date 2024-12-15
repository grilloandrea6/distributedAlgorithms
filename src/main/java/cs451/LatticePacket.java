package cs451;

import java.util.HashSet;
import java.util.Set;

public class LatticePacket {
    public enum Type {
        ACK, NACK, PROPOSAL
    }

    int shotNumber;
    Type type;
    int integerValue;
    Set<Integer> setValues;


    public LatticePacket() {
    }

    public LatticePacket(int shotNumber, Type type, int integerValue, Set<Integer> setValues) {
        this.shotNumber = shotNumber;
        this.type = type;
        this.integerValue = integerValue;
        this.setValues = setValues;
    }

    public Type getType() {
        return type;
    }

    public byte[] serialize() {
        // Precompute the size of the buffer
        int size = 9 + ((setValues != null) ? (4 * setValues.size()) : 0); // 2 ints + 1 byte + 4 * set size
        byte[] buffer = new byte[size];
        int offset = 0;
    
        // Write shotNumber (4 bytes)
        offset = writeIntToBuffer(buffer, offset, shotNumber);
    
        // Write type (1 byte)
        buffer[offset++] = (byte) type.ordinal();
    
        // Write integerValue (4 bytes)
        offset = writeIntToBuffer(buffer, offset, integerValue);
    
        // Write setValues if not null
        if (setValues != null) {
            for (int value : setValues) {
                offset = writeIntToBuffer(buffer, offset, value);
            }
        }
    
        return buffer;
    }
    
    public static LatticePacket deserialize(byte[] buffer) {
        LatticePacket packet = new LatticePacket();

        int offset = 0;

        // Read shotNumber (4 bytes)
        packet.shotNumber = readIntFromBuffer(buffer, offset);
        offset += 4;

        // Read type (1 byte)
        packet.type = Type.values()[buffer[offset++] & 0xFF];

        // Read integerValue (4 bytes)
        packet.integerValue = readIntFromBuffer(buffer, offset);
        offset += 4;

        // Read setValues
        packet.setValues = new HashSet<>();
        while (offset < buffer.length) {
            packet.setValues.add(readIntFromBuffer(buffer, offset));
            offset += 4;
        }

        // Reconstruct the object
        return packet;
    }

    private static int readIntFromBuffer(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xFF) << 24) |
            ((buffer[offset + 1] & 0xFF) << 16) |
            ((buffer[offset + 2] & 0xFF) << 8) |
            (buffer[offset + 3] & 0xFF);
    }


    private int writeIntToBuffer(byte[] buffer, int offset, int value) {
        buffer[offset++] = (byte) (value >>> 24);
        buffer[offset++] = (byte) (value >>> 16);
        buffer[offset++] = (byte) (value >>> 8);
        buffer[offset++] = (byte) value;
        return offset;
    }

  

    


}
