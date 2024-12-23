package cs451;

import java.util.HashSet;
import java.util.Set;

public class LatticeInstance {
    boolean active = false;
    int ackCount = 0, nackCount = 0, activeProposalNumber = 0;

    Set<Integer> values = new HashSet<>(); 
    Set<Byte> receivedFrom = new HashSet<>();
    Set<Byte> clean = new HashSet<>();

    LatticeInstance() {
        ackCount = 0;
        nackCount = 0;
        activeProposalNumber = 0;
    }

    void addProposal(Set<Integer> proposedValue) {
        active = true;
        values.addAll(proposedValue);
        activeProposalNumber++;
        ackCount = 1;
        nackCount = 0;
    }

}
