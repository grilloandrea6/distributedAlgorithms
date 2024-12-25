package cs451;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

public class LatticeInstance {
    boolean active = false;
    int ackCount = 0, nackCount = 0, activeProposalNumber = 0;

    final Set<Integer> proposedValues = new HashSet<>(); 
    final Set<Integer> acceptedValues = new HashSet<>(); 

    final BitSet receivedFrom = new BitSet(LatticeAgreement.hostNumber);
    int clean = 0;

    LatticeInstance() {
        ackCount = 0;
        nackCount = 0;
        activeProposalNumber = 0;
    }

    void addProposal(Set<Integer> proposedValue) {
        active = true;
        activeProposalNumber++;
        proposedValues.addAll(proposedValue);

        if(proposedValue.containsAll(acceptedValues)) {
            nackCount = 0; ackCount = 1;
        } else {
            ackCount = 0; nackCount = 1;
            proposedValues.addAll(acceptedValues);
        }
        acceptedValues.addAll(proposedValue);
    }

}
