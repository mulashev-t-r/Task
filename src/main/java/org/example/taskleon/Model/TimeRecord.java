package org.example.taskleon.Model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "time_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TimeRecord(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
