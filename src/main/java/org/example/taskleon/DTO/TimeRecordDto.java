package org.example.taskleon.DTO;

import org.example.taskleon.Model.TimeRecord;

import java.time.LocalDateTime;

public record TimeRecordDto(Long id, LocalDateTime createdAt) {
    public static TimeRecordDto fromEntity(TimeRecord entity) {
        return new TimeRecordDto(entity.getId(), entity.getCreatedAt());
    }
}
