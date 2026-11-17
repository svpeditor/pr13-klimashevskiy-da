package ru.edu.pr10;

import java.util.ArrayList;
import java.util.List;

public class BookingRepository {
    private final List<Booking> data = new ArrayList<>();

    public void save(Booking b) {
        data.add(b);
    }

    public List<Booking> findByRoom(String r) {
        return data.stream().filter(x -> x.roomId().equals(r)).toList();
    }

    public Booking findById(String id) {
        for (Booking b : data) {
            if (b.id().equals(id)) {
                return b;
            }
        }
        return null;
    }

    public int size() {
        return data.size();
    }
}
