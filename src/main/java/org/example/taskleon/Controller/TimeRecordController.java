package org.example.taskleon.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.taskleon.DTO.TimeRecordDto;
import org.example.taskleon.DTO.TimeRecordPageResponse;
import org.example.taskleon.Model.TimeRecord;
import org.example.taskleon.Service.TimeRecordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
@Tag(name = "Time records", description = "API для чтения записанных времён")
public class TimeRecordController {

    private final TimeRecordService timeRecordService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Получить записи времени (JSON, пагинация)")
    public TimeRecordPageResponse getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TimeRecord> result = timeRecordService.getTimeRecords(pageable);

        List<TimeRecordDto> records = result.getContent().stream()
                .map(TimeRecordDto::fromEntity)
                .toList();

        return new TimeRecordPageResponse(
                records,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    @GetMapping(value = "/html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Получить записи времени в простом HTML")
    public String getRecordsAsHtml(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TimeRecord> result = timeRecordService.getTimeRecords(pageable);

        StringBuilder sb = new StringBuilder("<html><body><ul>");
        for (TimeRecord record : result) {
            sb.append("<li>")
                    .append(record.getId())
                    .append(" : ")
                    .append(record.getCreatedAt())
                    .append("</li>");
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }
}
