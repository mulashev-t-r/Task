package org.example.taskleon.service;

import org.example.taskleon.Buffer.InMemoryTimeBuffer;
import org.example.taskleon.Config.AppProperties;
import org.example.taskleon.Repository.TimeRecordRepository;
import org.example.taskleon.Service.TimeRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TimeRecordServiceImplTest {

    private TimeRecordRepository repository;
    private InMemoryTimeBuffer buffer;
    private AppProperties properties;
    private TimeRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(TimeRecordRepository.class);
        buffer = new InMemoryTimeBuffer();
        properties = new AppProperties();
        properties.setSlowWriteThresholdMs(1_000);

        service = new TimeRecordServiceImpl(repository, buffer, properties);
    }

    @Test
    void flushBufferKeepsDataWhenDbUnavailable() {
        buffer.add(LocalDateTime.now());

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(repository).save(any());

        service.flushBufferToDatabase();

        assertThat(buffer.size()).isEqualTo(1);
        verify(repository, times(1)).save(any());
    }
}
