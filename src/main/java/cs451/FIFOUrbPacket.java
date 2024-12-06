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
        List<Byte> buffer = new ArrayList<>(8 + ((data != null) ? data.size() : 0));

        // Serialize fields
        buffer.addAll(NetworkInterface.intToBytes(origSender));         // 4 bytes
        buffer.addAll(NetworkInterface.intToBytes(seq));                // 4 bytes
        if(data != null)
            buffer.addAll(data);
        
        // System.out.println("Serialized packet: size " + buffer.size() + " - " + origSender + " " + seq + " " + data);
        return buffer;
    }

    public static FIFOUrbPacket deserialize(List<Byte> data, int sender)  {
        FIFOUrbPacket packet = new FIFOUrbPacket();

        packet.origSender = NetworkInterface.bytesToInt(data.subList(0, 4));
        packet.seq = NetworkInterface.bytesToInt(data.subList(4, 8));
        packet.data = data.subList(8, data.size());

        packet.sender = sender;
        return packet;        
    }

    @Override
    public int hashCode() {
        return seq * 961 + origSender;
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
