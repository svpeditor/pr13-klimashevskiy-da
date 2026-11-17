package ru.edu.pr10;

import java.time.Duration;
import java.time.LocalDateTime;

public class BookingService {
    public static final Duration MIN_DURATION = Duration.ofMinutes(15);
    public static final Duration MAX_DURATION = Duration.ofHours(8);

    private final BookingRepository repo;
    // промежуток, который должен оставаться между бронированиями одной комнаты
    private final Duration buffer;

    public BookingService(BookingRepository r) {
        this(r, Duration.ZERO);
    }

    public BookingService(BookingRepository r, Duration buffer) {
        if (buffer == null || buffer.isNegative()) {
            throw new IllegalArgumentException("Буфер не может быть отрицательным");
        }
        this.repo = r;
        this.buffer = buffer;
    }

    public Booking book(String id, String room, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (id == null || id.isBlank() || room == null || room.isBlank()) {
            throw new IllegalArgumentException("Нужны id бронирования и комната");
        }
        checkInterval(start, end);
        if (now == null || start.isBefore(now)) {
            throw new IllegalArgumentException("Нельзя бронировать в прошлом: начало " + start + ", сейчас " + now);
        }
        if (repo.findById(id) != null) {
            throw new IllegalArgumentException("Бронирование с id " + id + " уже есть");
        }
        for (Booking b : repo.findByRoom(room)) {
            if (overlaps(b, start, end)) {
                throw new BookingConflictException(b.id(), "Комната " + room + " занята: пересечение с бронированием "
                        + b.id() + " (" + b.start() + " - " + b.end() + ")");
            }
        }
        Booking booking = new Booking(id, room, start, end);
        repo.save(booking);
        return booking;
    }

    // только проверка, в репозиторий ничего не добавляется
    public boolean isAvailable(String room, LocalDateTime start, LocalDateTime end) {
        checkInterval(start, end);
        for (Booking b : repo.findByRoom(room)) {
            if (overlaps(b, start, end)) {
                return false;
            }
        }
        return true;
    }

    // конфликт, если startA < endB и startB < endA, касание границ не конфликт
    boolean overlaps(Booking b, LocalDateTime start, LocalDateTime end) {
        return start.isBefore(b.end().plus(buffer)) && b.start().isBefore(end.plus(buffer));
    }

    // сравниваются полные дата и время, поэтому переход через полночь считается правильно
    private void checkInterval(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Не указано начало или окончание");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Окончание " + end + " должно быть позже начала " + start);
        }
        Duration d = Duration.between(start, end);
        if (d.compareTo(MIN_DURATION) < 0 || d.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException("Длительность " + d.toMinutes() + " мин, можно от "
                    + MIN_DURATION.toMinutes() + " до " + MAX_DURATION.toMinutes() + " мин");
        }
    }
}
