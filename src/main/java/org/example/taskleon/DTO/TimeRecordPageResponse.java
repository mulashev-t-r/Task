package org.example.taskleon.DTO;

import java.util.List;

public record TimeRecordPageResponse(
        List<TimeRecordDto> records,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
