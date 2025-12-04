package org.example.taskleon.service;

import com.github.dockerjava.api.DockerClient;
import org.example.taskleon.Model.TimeRecord;
import org.example.taskleon.Repository.TimeRecordRepository;
import org.example.taskleon.Service.TimeRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TimeAppIntegrationTest {

    @Autowired
    private TimeRecordRepository repository;

    @Autowired
    private TimeRecordService timeRecordService;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("time_db_test")
                    .withUsername("app_test")
                    .withPassword("app_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("app.schedule.producer-ms", () -> "100");
        registry.add("app.schedule.flush-ms", () -> "100");
        registry.add("app.schedule.reconnect-ms", () -> "500");
    }

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void schedulerWritesRecordsToDatabase() throws Exception {
        Thread.sleep(1500);

        long count = repository.count();
        assertThat(count).isGreaterThan(0L);
    }

    @Test
    void dbPauseAndResume_bufferIsFlushedAndOrderIsAscending() throws Exception {
        Thread.sleep(2000);
        List<TimeRecord> beforePause = repository.findAll();
        int beforeCount = beforePause.size();
        assertThat(beforeCount).isGreaterThan(0);

        DockerClient dockerClient = DockerClientFactory.instance().client();
        dockerClient.pauseContainerCmd(postgres.getContainerId()).exec();

        timeRecordService.bufferCurrentTime();
        Thread.sleep(10);
        timeRecordService.bufferCurrentTime();

        Thread.sleep(3000);

        dockerClient.unpauseContainerCmd(postgres.getContainerId()).exec();

        Thread.sleep(4000);

        List<TimeRecord> afterResume = repository.findAll();
        assertThat(afterResume.size())
                .as("после восстановления БД записей должно стать больше, данные не должны теряться")
                .isGreaterThan(beforeCount);

        LocalDateTime prev = null;
        for (TimeRecord record : afterResume) {
            if (prev != null) {
                assertThat(record.getCreatedAt())
                        .as("created_at должен быть неубывающим")
                        .isAfterOrEqualTo(prev);
            }
            prev = record.getCreatedAt();
        }
    }
}