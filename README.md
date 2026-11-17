# Практическая работа №13. Date Time API и проверка пересечения интервалов

Климашевский Даниил Александрович, группа ЭФБО-07-25, вариант 4.

Бронирование переговорных: проверка интервала (окончание позже начала, длительность, не в прошлом), пересечение одной формулой startA < endB и startB < endA, касание границ не конфликт, буфер между бронированиями задается в конструкторе. По варианту 4 максимальная длительность 8 ч + 25 % = 10 ч.

## Сборка и запуск

Нужна Java 21 или новее.

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr10.SelfCheck
```

Или скриптом: `./run_selfcheck.sh` (macOS/Linux), `run_selfcheck.bat` (Windows).

Сначала выполняются проверки, потом обрабатывается `data/sample.csv` (формат `room;start;end`, первая строка - заголовок).
Свой файл: `java -cp out ru.edu.pr10.SelfCheck data/boundaries.csv`. Вторым аргументом можно передать "сейчас": `java -cp out ru.edu.pr10.SelfCheck data/my.csv 2026-10-01T08:00`.

Пакет называется `ru.edu.pr10`, как в исходном проекте.

## Что где лежит

- `src/ru/edu/pr10/BookingService.java` - проверка интервала, overlaps, isAvailable, book
- `src/ru/edu/pr10/BookingRepository.java` - хранение бронирований
- `src/ru/edu/pr10/Booking.java` (и BookingConflictException.java) - бронирование и конфликт с id
- `src/ru/edu/pr10/SelfCheck.java` - проверки и бронирования из файла
- `data/sample.csv` - открытый набор из задания, `data/boundaries.csv` - мои граничные значения
- `diagrams/` - activity-диаграмма и диаграмма классов (dot и png)
- `DECISIONS.md` - вариант, два решения, граничные проверки, ответы на вопросы задания
- `REPORT.docx` - отчет
