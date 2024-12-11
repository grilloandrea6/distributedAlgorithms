package cs451;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LatticeAgreement {
    static int myId, hostNumber;

    // ToDo forse non serve, tanto saranno per forza in ordine
    static final FinishedInstancesKeeper finishedInstances = new FinishedInstancesKeeper();

    static final Map<Integer, LatticeInstance> instances = new HashMap<>();

    public static void begin(Parser p) {
        myId = p.myId();
        hostNumber = p.hosts().size();
    }

    public static void propose(int shotNumber, Set<Integer> proposal) throws InterruptedException {
        System.out.println("Proposal " + shotNumber + " received: " + proposal);
        LatticePacket packet = new LatticePacket(shotNumber, LatticePacket.Type.PROPOSAL, 0, proposal);

        // set variables - hypothesys: have a class for each instance of agreement

        LatticeInstance instance;
        if(instances.containsKey(shotNumber)) {
            instance = instances.get(shotNumber);
        } else {
            instance = new LatticeInstance();
            instances.put(shotNumber, instance);
        }

        instance.addStartProposal(proposal);

        internalBroadcast(packet);
        
    }

    static void internalBroadcast(LatticePacket packet) throws InterruptedException {
        byte[] serialized = packet.serialize();

        for(int deliveryHost = 1; deliveryHost <= hostNumber; deliveryHost++) {
            // do not send to myself
            if(deliveryHost == myId) {
                internalReceive(myId, packet);
                continue;
            }
                
        
            PerfectLinks.perfectSend(serialized, deliveryHost);
        }
    }

    public static void receivePacket(int senderID, byte[] data) throws InterruptedException {
        LatticePacket packet = LatticePacket.deserialize(data);

        System.out.println("Packet received from " + senderID + ": " + packet.getType() + " " + packet.shotNumber + " " + packet.integerValue + " " + packet.setValues);

        internalReceive(senderID, packet);
    }
    public static void internalReceive(int senderID, LatticePacket packet) throws InterruptedException {
        if(finishedInstances.contains(packet.shotNumber))
            return;

        LatticeInstance instance;
        if(instances.containsKey(packet.shotNumber)) {
            instance = instances.get(packet.shotNumber);
        } else { // ToDo, tanto non sarà attiva e sarà resettata al propose, quindi posso discard il packet, non creare una nuova istanza
            instance = new LatticeInstance(); // ToDo anything to initialize?

            instances.put(packet.shotNumber, instance);
        }


        switch (packet.getType()) {
            case PROPOSAL:
                // if accepted_value ⊆ packet.getSetValue()
                if(instance.acceptedValue.addAll(packet.setValues)) {
                    // we added something, send nack
                    packet.type = LatticePacket.Type.NACK;
                    packet.setValues = instance.acceptedValue; // ToDo be careful - can anyone modify this set in the mean time
                } else {
                    // no modification, send ack
                    packet.type = LatticePacket.Type.ACK;
                    packet.setValues = null;
                }
                PerfectLinks.perfectSend(packet.serialize(), senderID);
                
                break;
            case ACK:
                if(packet.integerValue == instance.activeProposalNumber)
                    instance.ackCount++;

                // check if n+1 etcetc

                break;
            case NACK:
                if(packet.integerValue == instance.activeProposalNumber)
                    instance.nackCount++;



                break;
            default:
                break;
        }
         

    }
}