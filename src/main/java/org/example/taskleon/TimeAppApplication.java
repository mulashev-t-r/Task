package org.example.taskleon;

import org.example.taskleon.Config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class TimeAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeAppApplication.class, args);
    }
}
