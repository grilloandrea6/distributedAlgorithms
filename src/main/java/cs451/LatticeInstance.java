package cs451;

import java.util.HashSet;
import java.util.Set;

public class LatticeInstance {
    boolean active = true;
    int ackCount = 0, nackCount = 0, activeProposalNumber = 1;

    Set<Integer> acceptedValue = new HashSet<>(), // ToDo capire se serve istanziare
                 proposedValue; 

    LatticeInstance(Set<Integer> proposedValue) {
        this.proposedValue = proposedValue;
        ackCount = 0;
        nackCount = 0;
        activeProposalNumber = 1;
    }

}
