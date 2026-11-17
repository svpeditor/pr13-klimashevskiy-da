package ru.edu.pr10;

import java.time.Duration;
import java.time.LocalDateTime;

public record Booking(String id, String roomId, LocalDateTime start, LocalDateTime end) {
    public Duration duration() {
        return Duration.between(start, end);
    }
}
