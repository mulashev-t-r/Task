package org.example.taskleon.Service;

import org.example.taskleon.Model.TimeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TimeRecordService {

    void bufferCurrentTime();

    void flushBufferToDatabase();

    void checkDatabaseAvailability();

    Page<TimeRecord> getTimeRecords(Pageable pageable);
}
