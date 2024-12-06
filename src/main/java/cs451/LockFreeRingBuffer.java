package cs451;

import java.util.concurrent.atomic.AtomicInteger;

public class LockFreeRingBuffer<T> {
    private final Object[] buffer;            // Fixed-size array to store elements
    private final int capacity;              // Capacity of the ring buffer
    private final AtomicInteger head = new AtomicInteger(0); // Tracks where to consume
    private final AtomicInteger tail = new AtomicInteger(0); // Tracks where to produce

    public LockFreeRingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    // Add an element to the buffer
    public boolean offer(T value) {
        while (true) {
            int currentTail = tail.get();
            int nextTail = (currentTail + 1) % capacity;

            // If the buffer is full, return false
            if (nextTail == head.get()) {
                return false;
            }

            // Try to claim the slot atomically
            if (tail.compareAndSet(currentTail, nextTail)) {
                buffer[currentTail] = value;
                return true;
            }
        }
    }

    // Retrieve and remove an element from the buffer
    @SuppressWarnings("unchecked")
    public T poll() {
        while (true) {
            int currentHead = head.get();

            // If the buffer is empty, return null
            if (currentHead == tail.get()) {
                return null;
            }

            // Try to claim the slot atomically
            if (head.compareAndSet(currentHead, (currentHead + 1) % capacity)) {
                T value = (T) buffer[currentHead];
                buffer[currentHead] = null; // Help garbage collection
                return value;
            }
        }
    }

    // Check if the buffer is empty
    public boolean isEmpty() {
        return head.get() == tail.get();
    }

    // Check if the buffer is full
    public boolean isFull() {
        return (tail.get() + 1) % capacity == head.get();
    }

    // Get the current size of the buffer
    public int size() {
        int currentTail = tail.get();
        int currentHead = head.get();
        return (currentTail - currentHead + capacity) % capacity;
    }
}