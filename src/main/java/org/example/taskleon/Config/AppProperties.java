package org.example.taskleon.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Schedule schedule = new Schedule();
    private long slowWriteThresholdMs = 200;

    @Data
    public static class Schedule {
        private long producerMs = 1000;
        private long flushMs = 1000;
        private long reconnectMs = 5000;
    }
}
