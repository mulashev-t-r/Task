package org.example.taskleon.Buffer;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class InMemoryTimeBuffer implements TimeBuffer {

    private final Queue<LocalDateTime> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void add(LocalDateTime timestamp) {
        queue.add(timestamp);
    }

    @Override
    public LocalDateTime peek() {
        return queue.peek();
    }

    @Override
    public LocalDateTime poll() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}