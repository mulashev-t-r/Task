package org.example.taskleon.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.taskleon.Buffer.TimeBuffer;
import org.example.taskleon.Config.AppProperties;
import org.example.taskleon.Model.TimeRecord;
import org.example.taskleon.Repository.TimeRecordRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeRecordServiceImpl implements TimeRecordService {

    private final TimeRecordRepository repository;
    private final TimeBuffer buffer;
    private final AppProperties properties;

    // true – БД доступна, false – считаем, что БД недоступна и работаем только с буфером
    private final AtomicBoolean dbAvailable = new AtomicBoolean(true);

    @Override
    public void bufferCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        buffer.add(now);
        if (log.isTraceEnabled()) {
            log.trace("Добавлена временная метка в буфер: {}", now);
        }
    }

    @Override
    @Transactional
    public void flushBufferToDatabase() {
        if (!dbAvailable.get() || buffer.isEmpty()) {
            return;
        }

        long start = System.currentTimeMillis();
        int processed = 0;

        while (!buffer.isEmpty()) {
            LocalDateTime ts = buffer.peek();
            if (ts == null) {
                break;
            }

            try {
                repository.save(new TimeRecord(ts));
                // удаляем только после успешной записи
                buffer.poll();
                processed++;
            } catch (DataAccessException ex) {
                dbAvailable.set(false);
                log.error(
                        "Не удалось записать метку времени {} в БД. Помечаем БД как недоступную.",
                        ts, ex
                );
                // текущий элемент останется в начале очереди и будет записан после восстановления
                break;
            }
        }

        long duration = System.currentTimeMillis() - start;
        if (processed > 0) {
            log.debug(
                    "Сброшено {} записей в БД за {} мс. Текущий размер буфера={}",
                    processed, duration, buffer.size()
            );
            if (duration > properties.getSlowWriteThresholdMs()) {
                log.warn(
                        "Медленная запись в БД: {} записей за {} мс (порог {} мс). " +
                                "Возможна перегрузка БД или медленное соединение. Текущий размер буфера={}",
                        processed, duration, properties.getSlowWriteThresholdMs(), buffer.size()
                );
            }
        }
    }

    @Override
    public void checkDatabaseAvailability() {
        if (dbAvailable.get()) {
            return;
        }
        try {
            repository.count();
            dbAvailable.set(true);
            log.info(
                    "Соединение с БД восстановлено. Незаписанных записей в буфере: {}",
                    buffer.size()
            );
        } catch (DataAccessException ex) {
            log.warn("БД по-прежнему недоступна: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimeRecord> getTimeRecords(Pageable pageable) {
        return repository.findAll(pageable);
    }
}