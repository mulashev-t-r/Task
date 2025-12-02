package org.example.taskleon.Buffer;

import java.time.LocalDateTime;

public interface TimeBuffer {
    void add(LocalDateTime timestamp);
    LocalDateTime peek();
    LocalDateTime poll();
    int size();
    boolean isEmpty();
}
