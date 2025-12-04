package org.example.taskleon.service;

import org.example.taskleon.Buffer.InMemoryTimeBuffer;
import org.example.taskleon.Config.AppProperties;
import org.example.taskleon.Model.TimeRecord;
import org.example.taskleon.Repository.TimeRecordRepository;
import org.example.taskleon.Service.TimeRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    void bufferCurrentTimeAddsTimestampToBuffer() {
        assertThat(buffer.size()).isZero();

        service.bufferCurrentTime();

        assertThat(buffer.size()).isEqualTo(1);
        assertThat(buffer.peek()).isNotNull();
    }

    @Test
    void flushBufferKeepsDataWhenDbUnavailable() {
        buffer.add(LocalDateTime.now());

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(repository).save(any(TimeRecord.class));

        service.flushBufferToDatabase();

        assertThat(buffer.size()).isEqualTo(1);
        verify(repository, times(1)).save(any(TimeRecord.class));
    }

    @Test
    void whenDbIsStillUnavailableAfterCheckBufferIsNotFlushed() {
        LocalDateTime ts1 = LocalDateTime.now();
        LocalDateTime ts2 = ts1.plusSeconds(1);
        buffer.add(ts1);
        buffer.add(ts2);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(repository).save(any(TimeRecord.class));
        service.flushBufferToDatabase();

        assertThat(buffer.size()).isEqualTo(2);

        doThrow(new DataAccessResourceFailureException("Still down"))
                .when(repository).count();
        service.checkDatabaseAvailability();

        service.flushBufferToDatabase();

        verify(repository, times(1)).save(any(TimeRecord.class));
        assertThat(buffer.size()).isEqualTo(2);
    }

    @Test
    void whenDbRestoredBufferedEventsAreFlushedInOrder() {
        LocalDateTime ts1 = LocalDateTime.now();
        LocalDateTime ts2 = ts1.plusSeconds(1);
        buffer.add(ts1);
        buffer.add(ts2);

        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(repository).save(any(TimeRecord.class));
        service.flushBufferToDatabase();

        assertThat(buffer.size()).isEqualTo(2);

        reset(repository);

        when(repository.count()).thenReturn(0L);
        service.checkDatabaseAvailability();

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);

        service.flushBufferToDatabase();

        verify(repository, times(2)).save(captor.capture());
        assertThat(buffer.size()).isEqualTo(0);

        assertThat(captor.getAllValues())
                .extracting(TimeRecord::getCreatedAt)
                .containsExactly(ts1, ts2);
    }
}