package cs451;

import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;

public class FIFOKeeper {
    static private int maximumInOrderMessage = 0;
    static private TreeMap<Integer,Set<Integer>> pending = new TreeMap<>();


    public static void addDecision(int n, Set<Integer> data) throws IOException {
        // System.err.println("adding decision to shot " + n);
        if (n == maximumInOrderMessage + 1) {
            // System.err.println("logging shot " + n);
            OutputLogger.logDecide(data);

            maximumInOrderMessage++;
            removeMessages();
        } else {
            pending.put(n,data);
        }
    }

    private static void removeMessages() throws IOException {
        while (!pending.isEmpty() && pending.firstKey() == maximumInOrderMessage + 1) {
            maximumInOrderMessage++;
            // System.err.println("logging shot " + maximumInOrderMessage);
            OutputLogger.logDecide(pending.pollFirstEntry().getValue());
        } 
    }
}