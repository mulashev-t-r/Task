package org.example.taskleon.service;

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

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    void schedulerWritesRecordsToDatabase() {
        await()
                .atMost(ofSeconds(5))
                .untilAsserted(() -> assertThat(repository.count()).isGreaterThan(0L));
    }

    @Test
    void dbPauseAndResume_bufferIsFlushedAndOrderIsAscending() {
        await()
                .atMost(ofSeconds(5))
                .untilAsserted(() -> assertThat(repository.count()).isGreaterThan(0L));

        long beforeCount = repository.count();

        pauseDb();

        timeRecordService.bufferCurrentTime();
        timeRecordService.bufferCurrentTime();

        await()
                .during(ofSeconds(2))
                .atMost(ofSeconds(3))
                .untilAsserted(() -> assertThat(repository.count()).isEqualTo(beforeCount));

        unpauseDb();

        await()
                .atMost(ofSeconds(8))
                .untilAsserted(() -> assertThat(repository.count()).isGreaterThan(beforeCount));

        List<TimeRecord> afterResume = repository.findAll();
        assertThat(isNonDecreasing(afterResume)).isTrue();
    }

    @Test
    void dbDownAtStartup_recordsBufferedAndWrittenAfterRestore() {
        pauseDb();

        await()
                .during(ofSeconds(3))
                .atMost(ofSeconds(4))
                .untilAsserted(() -> assertThat(repository.count()).isZero());

        unpauseDb();

        await()
                .atMost(ofSeconds(10))
                .untilAsserted(() -> assertThat(repository.count()).isGreaterThan(0L));

        List<TimeRecord> records = repository.findAll();
        assertThat(isNonDecreasing(records)).isTrue();
    }

    @Test
    void dbUnavailableForAWhile_thenAllBufferedRecordsPersisted() {
        await()
                .atMost(ofSeconds(5))
                .untilAsserted(() -> assertThat(repository.count()).isGreaterThan(0L));

        long beforeCount = repository.count();
        pauseDb();

        timeRecordService.bufferCurrentTime();
        timeRecordService.bufferCurrentTime();
        timeRecordService.bufferCurrentTime();

        await()
                .during(ofSeconds(2))
                .atMost(ofSeconds(3))
                .untilAsserted(() -> assertThat(repository.count()).isEqualTo(beforeCount));

        unpauseDb();

        await()
                .atMost(ofSeconds(10))
                .untilAsserted(() -> assertThat(repository.count()).isGreaterThan(beforeCount + 1));

        List<TimeRecord> records = repository.findAll();
        assertThat(isNonDecreasing(records)).isTrue();
    }

    private void pauseDb() {
        var dockerClient = DockerClientFactory.instance().client();
        dockerClient.pauseContainerCmd(postgres.getContainerId()).exec();
    }

    private void unpauseDb() {
        var dockerClient = DockerClientFactory.instance().client();
        dockerClient.unpauseContainerCmd(postgres.getContainerId()).exec();
    }

    private boolean isNonDecreasing(List<TimeRecord> records) {
        LocalDateTime prev = null;
        for (TimeRecord record : records) {
            if (prev != null && record.getCreatedAt().isBefore(prev)) {
                return false;
            }
            prev = record.getCreatedAt();
        }
        return true;
    }
}
