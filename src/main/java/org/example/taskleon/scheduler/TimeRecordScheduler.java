package org.example.taskleon.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.taskleon.Service.TimeRecordService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimeRecordScheduler {

    private final TimeRecordService timeRecordService;

    // Раз в секунду кладём текущее время в буфер
    @Scheduled(fixedRateString = "${app.schedule.producer-ms:1000}")
    public void produceCurrentTime() {
        timeRecordService.bufferCurrentTime();
    }

    // Раз в секунду пытаемся выгрузить буфер в БД
    @Scheduled(fixedDelayString = "${app.schedule.flush-ms:1000}")
    public void flushBuffer() {
        timeRecordService.flushBufferToDatabase();
    }

    // Раз в 5 секунд проверяем, не восстановилась ли БД
    @Scheduled(fixedDelayString = "${app.schedule.reconnect-ms:5000}")
    public void checkDatabase() {
        timeRecordService.checkDatabaseAvailability();
    }
}
