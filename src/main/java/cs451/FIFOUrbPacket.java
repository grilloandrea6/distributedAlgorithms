package cs451;

import java.util.ArrayList;
import java.util.List;

public class FIFOUrbPacket {
    int origSender;
    int seq;
    List<Byte> data;
    int sender;

    int ackReceived = 1;

    public FIFOUrbPacket() {}

    public FIFOUrbPacket(Integer myId, int seq2, List<Byte> data2) {
        origSender = myId;
        seq = seq2;
        data = data2;
    }

    public List<Byte> serialize() {
        List<Byte> buffer = new ArrayList<>(5 + ((data != null) ? data.size() : 0));

        // Serialize fields
        buffer.add((byte)origSender);                    // 1 byte
        buffer.addAll(NetworkInterface.intToBytes(seq)); // 4 bytes
        if(data != null)
            buffer.addAll(data);
        
        // System.out.println("Serialized packet: size " + buffer.size() + " - " + origSender + " " + seq + " " + data);
        return buffer;
    }

    public static FIFOUrbPacket deserialize(List<Byte> data, int sender)  {
        // System.out.println("deserialize packet: size " + data.size());

        // try{
            // if(data.size() < 5) {
            //     throw new Exception("Invalid packet size");
            // }

            FIFOUrbPacket packet = new FIFOUrbPacket();

            //data.remove(0); //length of the message

            packet.origSender = data.get(0);
            packet.seq = NetworkInterface.bytesToInt(data.subList(1, 5));
            // packet.data = List.copyOf(data.subList(5, data.size()));
            packet.data = data.subList(5, data.size());

            packet.sender = sender;
            // System.out.println("Deserialized packet: " + packet.origSender + " " + packet.seq + " " + packet.data);

            
            return packet;
        // } catch (Exception e) {
        //     System.out.println("Failed to deserialize packet.");
        //     e.printStackTrace();
        //     return null;
        // }
        
    }

    @Override
    public int hashCode() {
        return origSender ^ seq;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FIFOUrbPacket other = (FIFOUrbPacket) obj;
        return origSender == other.origSender && seq == other.seq;
    }
    
}
