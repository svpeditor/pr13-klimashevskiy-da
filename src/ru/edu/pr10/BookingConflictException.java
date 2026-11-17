package ru.edu.pr10;

// новое бронирование пересекается с уже существующим
public class BookingConflictException extends RuntimeException {
    private final String conflictId;

    public BookingConflictException(String conflictId, String message) {
        super(message);
        this.conflictId = conflictId;
    }

    public String getConflictId() {
        return conflictId;
    }
}
