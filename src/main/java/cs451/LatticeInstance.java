package cs451;

import java.util.HashSet;
import java.util.Set;

public class LatticeInstance {
    boolean active = false;
    int ackCount, nackCount, activeProposalNumber;

    Set<Integer> acceptedValue = new HashSet<>(), // ToDo capire se serve istanziare
                 proposedValue; 

    void addStartProposal(Set<Integer> proposedValue) {
        if(active) {
            System.err.println("ERROR - multiple proposals for same instance.");
            return;
        }

        active = true;
        this.proposedValue = proposedValue;
        ackCount = 0;
        nackCount = 0;
        activeProposalNumber = 1;
    }


}
