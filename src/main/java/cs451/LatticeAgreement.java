package cs451;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LatticeAgreement {
    static int myId, hostNumber, fPlusOne;

    static int lastShotNumber = 0;

    static final Map<Integer, LatticeInstance> instances = new ConcurrentHashMap<>(); 

    static final int MAX_WINDOW_SIZE = 150;
    static int windowSize = 0;

    static Set<Integer> removedShots = ConcurrentHashMap.newKeySet();
    
    public static void begin(Parser p) {
        myId = p.myId();        
        hostNumber = p.hosts().size();
        fPlusOne = (hostNumber - 1) / 2 + 1;
        if(hostNumber % 2 == 0) {
            fPlusOne++;
        }

        PerfectLinks.begin(p);
    }

    public static void propose(Set<Integer> proposal) throws Exception {
        lastShotNumber++;
        synchronized(LatticeAgreement.class) {
            while(windowSize >= MAX_WINDOW_SIZE ) {
                LatticeAgreement.class.wait();  
            }
            windowSize++;
        }            
        instances.putIfAbsent(lastShotNumber, new LatticeInstance());
        LatticeInstance instance = instances.get(lastShotNumber);
        synchronized(instance) 
        {
            instance.addProposal(proposal);
            instance.receivedFrom.add((byte) myId);
        }    
        LatticePacket packet = new LatticePacket(lastShotNumber, LatticePacket.Type.PROPOSAL, instance.activeProposalNumber, proposal);       
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

        if(removedShots.contains(packet.shotNumber)) return;

        instances.putIfAbsent(packet.shotNumber, new LatticeInstance());
        LatticeInstance instance = instances.get(packet.shotNumber);

        synchronized(instance) 
        {
            switch (packet.getType()) {
                case PROPOSAL:
                    // if accepted_value ⊆ packet.getSetValue()
                    if(packet.setValues.containsAll(instance.acceptedValues)) {
                        instance.acceptedValues.addAll(packet.setValues);
                        packet.type = LatticePacket.Type.ACK;
                        packet.setValues = null;
                    } else {
                        instance.acceptedValues.addAll(packet.setValues);
                        packet.type = LatticePacket.Type.NACK;
                        packet.setValues = instance.acceptedValues;
                    }
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
                        instance.proposedValues.addAll(packet.setValues);
                    }
                    break;
                case CLEAN:
                    instance.clean.add((byte) senderID);
                    if(instance.clean.size() == hostNumber) {
                        instances.remove(packet.shotNumber);
                        removedShots.add(packet.shotNumber);
                    }
                    break;
                default:
                    System.err.println("Unknown packet type");
                    break;
            }

            if(instance.active && instance.receivedFrom.size() == hostNumber) {
                // System.err.println("Received from all hosts, deciding shot number " + packet.shotNumber);
                instance.proposedValues.addAll(instance.acceptedValues);
                decide(packet.shotNumber, instance);
            }

            if(instance.active && (packet.getType() == LatticePacket.Type.ACK || packet.getType() == LatticePacket.Type.NACK)) {
                // System.out.print("Active and ack/nack received, checking. " + instance.ackCount + " " + instance.nackCount + " " + fPlusOne + " - ");
                if(instance.ackCount >= fPlusOne) {
                    decide(packet.shotNumber, instance);
                } 
                
                if(instance.nackCount > 0 && (instance.ackCount + instance.nackCount) >= fPlusOne) {
                    // System.out.println("Incrementing proposal number and sending new proposal");

                    if(instance.proposedValues.containsAll(instance.acceptedValues)) {
                        instance.nackCount = 0; instance.ackCount = 1;
                    } else {
                        instance.ackCount = 0; instance.nackCount = 1;
                        instance.proposedValues.addAll(instance.acceptedValues);
                    }
                    instance.acceptedValues.addAll(instance.proposedValues);

                    instance.activeProposalNumber++;
                    packet.type = LatticePacket.Type.PROPOSAL;
                    packet.integerValue = instance.activeProposalNumber;
                    packet.setValues = instance.proposedValues;
                    internalBroadcast(packet);
                }
            } 
        }
    }

    private static void decide(int shotNumber, LatticeInstance instance) throws Exception{
        FIFOKeeper.addDecision(shotNumber,instance.proposedValues);
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
        }
    }
}