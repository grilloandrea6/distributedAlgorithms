package cs451;

import java.util.TreeSet;

public class FinishedInstancesKeeper {
    private int maximumInOrder = -1;
    private TreeSet<Integer> instances = new TreeSet<>();

    public FinishedInstancesKeeper() {}
    
    public boolean add(int value) {

        if (value == this.maximumInOrder + 1) {
            this.maximumInOrder++;
            if(!instances.isEmpty()) removeInstances();
        } else this.instances.add(value);
        return true;
    }


    private void removeInstances() {
        while (!instances.isEmpty() && instances.first() == this.maximumInOrder + 1) {
            this.maximumInOrder++;
            this.instances.pollFirst();
        }
    }

    public boolean contains(int value) {
                return value <= this.maximumInOrder || this.instances.contains(value);
    }
}