package cs451;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LatticeAgreement {
    static int myId, hostNumber, fPlusOne;

    static int lastShotNumber = 0;

    static final Map<Integer, LatticeInstance> instances = new HashMap<>(); 

    static final int MAX_WINDOW_SIZE = 5;
    static int windowSize = 0;
    
    public static void begin(Parser p) {
        myId = p.myId();        
        hostNumber = p.hosts().size();
        fPlusOne = (hostNumber - 1) / 2 + 1;
        if(hostNumber % 2 == 0) {
            fPlusOne++;
        }
        System.out.println("f+1: " + fPlusOne);

        PerfectLinks.begin(p);
    }

    public static void propose(Set<Integer> proposal) throws Exception {
        lastShotNumber++;
        // System.err.println("Proposal " + lastShotNumber + " proposing: " + proposal);
        LatticePacket packet = new LatticePacket(lastShotNumber, LatticePacket.Type.PROPOSAL, 1 , proposal);

        synchronized(LatticeAgreement.class) {
            // System.err.println("is window open - shot number " + lastShotNumber + " - " + FIFOKeeper.pending.isEmpty());
            while(windowSize >= MAX_WINDOW_SIZE && (FIFOKeeper.pending.isEmpty() || lastShotNumber - FIFOKeeper.pending.firstKey() > MAX_WINDOW_SIZE)) {
                // System.err.println("Waiting for window to open - shot number " + lastShotNumber);
                LatticeAgreement.class.wait();
            }
            windowSize++;
        }            

        LatticeInstance instance = instances.get(packet.shotNumber);
        if(instance == null) {
            instance = new LatticeInstance();
            instances.put(packet.shotNumber, instance);
        }
        synchronized(instance) 
        {
            instance.addProposal(proposal);
            instance.receivedFrom.add((byte) myId);
        }
                
        internalBroadcast(packet);
    }

    static void internalBroadcast(LatticePacket packet) throws Exception {
        byte[] serialized = packet.serialize();

        for(int deliveryHost = 1; deliveryHost <= hostNumber; deliveryHost++) {
            if(deliveryHost == myId) {
                continue;
            }
        
            PerfectLinks.perfectSend(serialized, deliveryHost);
        }
    }

    public static void receivePacket(int senderID, byte[] data) throws Exception {
        LatticePacket packet = LatticePacket.deserialize(data);

        // System.out.println("Packet received from " + senderID + ": " + packet.getType() + " " + packet.shotNumber + " " + packet.integerValue + " " + packet.setValues);

        LatticeInstance instance = instances.get(packet.shotNumber);
        if(instance == null) {
            instance = new LatticeInstance();
            instances.put(packet.shotNumber, instance);
        }
        synchronized(instance) 
        {
            switch (packet.getType()) {
                case PROPOSAL:
                    // if accepted_value ⊆ packet.getSetValue()
                    if(packet.setValues.containsAll(instance.values)) {
                        instance.values.addAll(packet.setValues);
                        packet.type = LatticePacket.Type.ACK;
                        packet.setValues = null;
                    } else {
                        instance.values.addAll(packet.setValues);
                        packet.type = LatticePacket.Type.NACK;
                        packet.setValues = instance.values;
                    }
                    // System.out.println("Sending packet to " + senderID + ": " + packet.getType() + " " + packet.shotNumber + " " + packet.integerValue + " " + packet.setValues);
                    PerfectLinks.perfectSend(packet.serialize(), senderID);
                    instance.receivedFrom.add((byte) senderID);
                    break;
                case ACK:
                    if(packet.integerValue == instance.activeProposalNumber)
                        instance.ackCount++;

                    break;
                case NACK:
                    if(packet.integerValue == instance.activeProposalNumber) {
                        instance.nackCount++;
                        instance.values.addAll(packet.setValues);
                        }
                    break;
                case CLEAN:
                    instance.clean.add((byte) senderID);
                    if(instance.clean.size() == hostNumber) {
                        instances.remove(packet.shotNumber);
                        System.err.println("removing shot number " + packet.shotNumber);
                    }
                    break;
                default:
                    System.err.println("Unknown packet type");
                    break;
            }

            if(instance.active && instance.receivedFrom.size() == hostNumber) {
                System.err.println("Received from all hosts, deciding shot number " + packet.shotNumber);
                decide(packet.shotNumber, instance);
            }

            if(instance.active && (packet.getType() == LatticePacket.Type.ACK || packet.getType() == LatticePacket.Type.NACK)) {
                // System.out.print("Active and ack/nack received, checking. " + instance.ackCount + " " + instance.nackCount + " " + fPlusOne + " - ");
                if(instance.ackCount >= fPlusOne) {
                    decide(packet.shotNumber, instance);
                } 
                
                if(instance.nackCount > 0 && (instance.ackCount + instance.nackCount) >= fPlusOne) {
                    // System.out.println("Incrementing proposal number and sending new proposal");
                    instance.ackCount = 1;
                    instance.nackCount = 0;
                    instance.activeProposalNumber++;
                    packet.type = LatticePacket.Type.PROPOSAL;
                    packet.integerValue = instance.activeProposalNumber;
                    packet.setValues = instance.values;
                    internalBroadcast(packet);
                }
            } 
        }
    }

    private static void decide(int shotNumber, LatticeInstance instance) throws Exception{
        // System.out.println("Decided on " + instance.values);
        FIFOKeeper.addDecision(shotNumber,instance.values);
        instance.active = false;
        synchronized(LatticeAgreement.class) {
            windowSize--;
            LatticeAgreement.class.notify();
        }

        LatticePacket packet = new LatticePacket(shotNumber, LatticePacket.Type.CLEAN, 0, null);
        internalBroadcast(packet);
        instance.clean.add((byte) myId);
        if(instance.clean.size() == hostNumber) {
            instances.remove(shotNumber);
            System.err.println("removing shot number " + packet.shotNumber);
        }
    }
}