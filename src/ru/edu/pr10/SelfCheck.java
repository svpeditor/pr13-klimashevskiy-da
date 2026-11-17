package ru.edu.pr10;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class SelfCheck {
    // "сейчас" для проверок, чтобы результат не зависел от дня запуска
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 8, 0);
    private static int passed = 0;
    private static int count = 0;

    public static void main(String[] args) {
        checkInterval();
        checkOverlaps();
        checkBook();
        checkMidnight();
        checkBuffer();
        System.out.println("Unit: " + passed + "/" + count + " passed");
        // свой файл и свое "сейчас": java -cp out ru.edu.pr10.SelfCheck data/my.csv 2026-10-01T08:00
        LocalDateTime now = args.length > 1 ? LocalDateTime.parse(args[1]) : NOW;
        runFile(args.length > 0 ? args[0] : "data/sample.csv", now);
    }

    private static void checkInterval() {
        check("end equals start", !tryBook("R1", t(10, 0), t(10, 0)));
        check("end before start", !tryBook("R1", t(11, 0), t(10, 0)));
        // от 15 минут
        check("14 minutes", !tryBook("R1", t(10, 0), t(10, 14)));
        check("15 minutes", tryBook("R1", t(10, 0), t(10, 15)));
        Duration max = BookingService.MAX_DURATION;
        check("max duration", tryBook("R1", t(9, 0), t(9, 0).plus(max)));
        check("max plus 1 minute", !tryBook("R1", t(9, 0), t(9, 0).plus(max).plusMinutes(1)));
        // прошлое относительно clockNow
        check("1 minute in past", !tryBook("R1", NOW.minusMinutes(1), t(9, 0)));
        check("start equals now", tryBook("R1", NOW, t(9, 0)));
        check("null end", !tryBook("R1", t(10, 0), null));
    }

    private static void checkOverlaps() {
        BookingService s = serviceWith10to11();
        // касание границ не конфликт
        check("touch after", s.isAvailable("R1", t(11, 0), t(12, 0)));
        check("touch before", s.isAvailable("R1", t(9, 0), t(10, 0)));
        check("overlap 1 minute", !s.isAvailable("R1", t(10, 59), t(12, 0)));
        check("inside", !s.isAvailable("R1", t(10, 15), t(10, 45)));
        check("outside", !s.isAvailable("R1", t(9, 0), t(12, 0)));
        check("same interval", !s.isAvailable("R1", t(10, 0), t(11, 0)));
        check("other room", s.isAvailable("R2", t(10, 0), t(11, 0)));
    }

    private static void checkBook() {
        BookingRepository repo = new BookingRepository();
        BookingService s = new BookingService(repo);
        s.book("B1", "R1", t(10, 0), t(11, 0), NOW);
        // isAvailable ничего не сохраняет
        s.isAvailable("R1", t(12, 0), t(13, 0));
        check("isAvailable no save", repo.size() == 1);
        String conflict = "";
        try {
            s.book("B2", "R1", t(10, 30), t(11, 30), NOW);
        } catch (BookingConflictException e) {
            conflict = e.getConflictId();
        }
        check("conflict id", conflict.equals("B1") && repo.size() == 1);
        check("book after touch", tryBookIn(s, "B3", "R1", t(11, 0), t(12, 0)) && repo.size() == 2);
        check("duplicate id", !tryBookIn(s, "B1", "R2", t(10, 0), t(11, 0)));
    }

    private static void checkMidnight() {
        BookingService s = new BookingService(new BookingRepository());
        // 22:00 - 06:00 следующего дня, 8 часов
        check("over midnight", tryBookIn(s, "N1", "R1", t(22, 0), t(22, 0).plusHours(8)));
        // утро следующего дня пересекается, утро этого дня (те же часы) нет
        check("next morning conflict", !s.isAvailable("R1", t(5, 0).plusDays(1), t(7, 0).plusDays(1)));
        check("same hours other day", s.isAvailable("R1", t(5, 0).plusDays(2), t(7, 0).plusDays(2)));
        check("after night", s.isAvailable("R1", t(6, 0).plusDays(1), t(7, 0).plusDays(1)));
    }

    private static void checkBuffer() {
        // буфер 15 минут: вплотную уже нельзя
        BookingRepository repo = new BookingRepository();
        BookingService s = new BookingService(repo, Duration.ofMinutes(15));
        s.book("B1", "R1", t(10, 0), t(11, 0), NOW);
        check("buffer touch", !s.isAvailable("R1", t(11, 0), t(12, 0)));
        check("buffer 14 min gap", !s.isAvailable("R1", t(11, 14), t(12, 0)));
        check("buffer 15 min gap", s.isAvailable("R1", t(11, 15), t(12, 0)));
        check("buffer before", s.isAvailable("R1", t(9, 0), t(9, 45)) && !s.isAvailable("R1", t(9, 0), t(9, 46)));
        check("negative buffer", !tryBuffer(Duration.ofMinutes(-5)));
    }

    // ---------- вспомогательные методы ----------

    private static LocalDateTime t(int hour, int minute) {
        return LocalDateTime.of(2026, 10, 1, hour, minute);
    }

    private static BookingService serviceWith10to11() {
        BookingService s = new BookingService(new BookingRepository());
        s.book("B1", "R1", t(10, 0), t(11, 0), NOW);
        return s;
    }

    private static boolean tryBook(String room, LocalDateTime start, LocalDateTime end) {
        return tryBookIn(new BookingService(new BookingRepository()), "B", room, start, end);
    }

    private static boolean tryBookIn(BookingService s, String id, String room, LocalDateTime start, LocalDateTime end) {
        try {
            s.book(id, room, start, end, NOW);
            return true;
        } catch (IllegalArgumentException | BookingConflictException e) {
            return false;
        }
    }

    private static boolean tryBuffer(Duration d) {
        try {
            new BookingService(new BookingRepository(), d);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void check(String name, boolean ok) {
        count++;
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            System.out.println("FAIL " + name);
        }
    }

    // ---------- бронирования из файла room;start;end ----------

    private static void runFile(String fileName, LocalDateTime now) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(fileName));
        } catch (IOException e) {
            System.out.println("Не удалось прочитать " + fileName);
            return;
        }
        System.out.println();
        System.out.println("Бронирования из файла " + fileName + ", сейчас " + now + ":");
        BookingService service = new BookingService(new BookingRepository());
        for (int i = 1; i < lines.size(); i++) {   // первая строка - заголовок
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String id = "B" + i;
            String[] p = line.split(";", -1);
            try {
                if (p.length != 3) {
                    throw new IllegalArgumentException("нужно 3 значения через ;");
                }
                Booking b = service.book(id, p[0].strip(), LocalDateTime.parse(p[1].strip()), LocalDateTime.parse(p[2].strip()), now);
                System.out.println(id + " " + line + " -> создано, " + b.duration().toMinutes() + " мин");
            } catch (DateTimeParseException e) {
                System.out.println(id + " " + line + " -> ошибка: дата не в формате ГГГГ-ММ-ДДTчч:мм");
            } catch (IllegalArgumentException | BookingConflictException e) {
                System.out.println(id + " " + line + " -> " + e.getMessage());
            }
        }
    }
}
