package cs451;

import java.util.HashSet;
import java.util.Set;

public class LatticeInstance {
    boolean active = false;
    int ackCount = 0, nackCount = 0, activeProposalNumber = 0;

    Set<Integer> acceptedValue = new HashSet<>(),
                 proposedValue; 

    LatticeInstance() {
        ackCount = 0;
        nackCount = 0;
        activeProposalNumber = 0;
    }

    void addProposal(Set<Integer> proposedValue) {
        active = true;
        this.proposedValue = proposedValue;
        activeProposalNumber++;
        ackCount = 0;
        nackCount = 0;
    }

}
