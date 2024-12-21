package cs451;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LatticeAgreement {
    static int myId, hostNumber, fPlusOne;

    static int lastShotNumber = 0;

    static final Map<Integer, LatticeInstance> instances = new HashMap<>(); 
    // ToDo capire se è possibile togliere istances, quando sono finite? l'acceptor continua a girare anche dopo il decide...

    public static void begin(Parser p) {
        myId = p.myId();        
        hostNumber = p.hosts().size();
        fPlusOne = (hostNumber - 1) / 2 + 1;
        System.out.println("f+1: " + fPlusOne);

        PerfectLinks.begin(p);
    }

    public static void propose(Set<Integer> proposal) throws Exception {
        lastShotNumber++;
        System.out.println("Proposal " + lastShotNumber + " received: " + proposal);
        LatticePacket packet = new LatticePacket(lastShotNumber, LatticePacket.Type.PROPOSAL, 1 , proposal);

        LatticeInstance instance = instances.get(packet.shotNumber);
        if(instance == null) {
            instance = new LatticeInstance();
            instances.put(packet.shotNumber, instance);
        }

        instance.addProposal(proposal);
                
        internalBroadcast(packet);
    }

    static void internalBroadcast(LatticePacket packet) throws Exception {
        byte[] serialized = packet.serialize();

        for(int deliveryHost = 1; deliveryHost <= hostNumber; deliveryHost++) {
            // todo do not send to myself
            // if(deliveryHost == myId) {
            //     internalReceive(myId, packet);
            //     continue;
            // }
        
            PerfectLinks.perfectSend(serialized, deliveryHost);
        }
    }

    public static void receivePacket(int senderID, byte[] data) throws Exception {
        LatticePacket packet = LatticePacket.deserialize(data);

        System.out.println("Packet received from " + senderID + ": " + packet.getType() + " " + packet.shotNumber + " " + packet.integerValue + " " + packet.setValues);

        synchronized(LatticeAgreement.class) {
            internalReceive(senderID, packet);
        }
    }
    public static void internalReceive(int senderID, LatticePacket packet) throws Exception {
        // if(lastShotNumber < packet.shotNumber)
        //     return;

        LatticeInstance instance = instances.get(packet.shotNumber);
        if(instance == null) {
            instance = new LatticeInstance();
            instances.put(packet.shotNumber, instance);
        }

        switch (packet.getType()) {
            case PROPOSAL:
                // if accepted_value ⊆ packet.getSetValue()
                if(packet.setValues.containsAll(instance.acceptedValue)) {
                    instance.acceptedValue.addAll(packet.setValues);
                    packet.type = LatticePacket.Type.ACK;
                    packet.setValues = null; // ToDo be careful - can anyone modify this set in the mean time
                } else {
                    // no modification, send ack
                    instance.acceptedValue.addAll(packet.setValues);
                    packet.type = LatticePacket.Type.ACK;
                    packet.setValues = instance.acceptedValue;
                }
                System.out.println("Sending packet to " + senderID + ": " + packet.getType() + " " + packet.shotNumber + " " + packet.integerValue + " " + packet.setValues);
                PerfectLinks.perfectSend(packet.serialize(), senderID);
                return;
            case ACK:
                if(packet.integerValue == instance.activeProposalNumber)
                    instance.ackCount++;

                break;
            case NACK:
                if(packet.integerValue == instance.activeProposalNumber) {
                    instance.nackCount++;
                    instance.proposedValue.addAll(packet.setValues);
                }

                break;
            default:
                System.err.println("Unknown packet type");
                break;
        }

        if(instance.active && (packet.getType() == LatticePacket.Type.ACK || packet.getType() == LatticePacket.Type.NACK)) {
            System.out.print("Active and ack/nack received, checking. " + instance.ackCount + " " + instance.nackCount + " " + fPlusOne + " - ");
            if(instance.ackCount >= fPlusOne) {
                System.out.println("Decided on " + instance.proposedValue);
                FIFOKeeper.addDecision(packet.shotNumber,instance.proposedValue);
                instance.active = false;
            } 
            
            if(instance.nackCount > 0 && (instance.ackCount + instance.nackCount) >= fPlusOne) {
                System.out.println("Incrementing proposal number and sending new proposal");
                instance.ackCount = 0;
                instance.nackCount = 0;
                instance.activeProposalNumber++;
                packet.type = LatticePacket.Type.PROPOSAL;
                packet.integerValue = instance.activeProposalNumber;
                packet.setValues = instance.proposedValue;
                internalBroadcast(packet);
            }
        } 
         

    }
}