package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class ReportMonthTest {

    @Test
    void ensureAverageDayWorkDuration() {

        final User user = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final LocalTime timeStart = LocalTime.of(8, 0);
        final LocalTime timeEnd = timeStart.plusHours(8);

        final ReportMonth sut = new ReportMonth(YearMonth.of(2023, 2), List.of(
            firstWeekFebruary2023(user, LocalDate.of(2023, 1, 30), timeStart, timeEnd),
            nthWeekFebruary2023(user, LocalDate.of(2023, 2, 6), timeStart, timeEnd),
            nthWeekFebruary2023(user, LocalDate.of(2023, 2, 13), timeStart, timeEnd),
            nthWeekFebruary2023(user, LocalDate.of(2023, 2, 20), timeStart, timeEnd),
            lastWeekOfFebruary2023(user, LocalDate.of(2023, 2, 27), timeStart, timeEnd)
        ));

        final WorkDuration actual = sut.averageDayWorkDuration();

        assertThat(actual).isEqualTo(new WorkDuration(Duration.ofHours(8)));
    }

    private ReportWeek firstWeekFebruary2023(User user, LocalDate firstDateOfWeek, LocalTime timeStart, LocalTime timeEnd) {

        final LocalDate tuesday = firstDateOfWeek.plusDays(1);
        final LocalDate wednesday = firstDateOfWeek.plusDays(2);
        final LocalDate thursday = firstDateOfWeek.plusDays(3);
        final LocalDate friday = firstDateOfWeek.plusDays(4);
        final LocalDate saturday = firstDateOfWeek.plusDays(5);
        final LocalDate sunday = firstDateOfWeek.plusDays(6);

        return new ReportWeek(firstDateOfWeek, List.of(
            // january
            new ReportDay(firstDateOfWeek, List.of()),
            new ReportDay(tuesday, List.of()),
            // february
            new ReportDay(wednesday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(wednesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(wednesday, timeEnd), UTC), false)
            )),
            new ReportDay(thursday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(thursday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(thursday, timeEnd), UTC), false)
            )),
            new ReportDay(friday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(friday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(friday, timeEnd), UTC), false)
            )),
            new ReportDay(saturday, List.of()),
            new ReportDay(sunday, List.of())
        ));
    }

    private ReportWeek nthWeekFebruary2023(User user, LocalDate firstDateOfWeek, LocalTime timeStart, LocalTime timeEnd) {

        final LocalDate tuesday = firstDateOfWeek.plusDays(1);
        final LocalDate wednesday = firstDateOfWeek.plusDays(2);
        final LocalDate thursday = firstDateOfWeek.plusDays(3);
        final LocalDate friday = firstDateOfWeek.plusDays(4);
        final LocalDate saturday = firstDateOfWeek.plusDays(5);
        final LocalDate sunday = firstDateOfWeek.plusDays(6);

        return new ReportWeek(firstDateOfWeek, List.of(
            new ReportDay(firstDateOfWeek, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(firstDateOfWeek, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(firstDateOfWeek, timeEnd), UTC), false)
            )),
            new ReportDay(tuesday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(tuesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(tuesday, timeEnd), UTC), false)
            )),
            new ReportDay(wednesday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(wednesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(wednesday, timeEnd), UTC), false)
            )),
            new ReportDay(thursday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(thursday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(thursday, timeEnd), UTC), false)
            )),
            new ReportDay(friday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(friday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(friday, timeEnd), UTC), false)
            )),
            new ReportDay(saturday, List.of()),
            new ReportDay(sunday, List.of())
        ));
    }

    private ReportWeek lastWeekOfFebruary2023(User user, LocalDate firstDateOfWeek, LocalTime timeStart, LocalTime timeEnd) {

        final LocalDate tuesday = firstDateOfWeek.plusDays(1);
        final LocalDate wednesday = firstDateOfWeek.plusDays(2);
        final LocalDate thursday = firstDateOfWeek.plusDays(3);
        final LocalDate friday = firstDateOfWeek.plusDays(4);
        final LocalDate saturday = firstDateOfWeek.plusDays(5);
        final LocalDate sunday = firstDateOfWeek.plusDays(6);

        return new ReportWeek(firstDateOfWeek, List.of(
            // february
            new ReportDay(firstDateOfWeek, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(wednesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(wednesday, timeEnd), UTC), false)
            )),
            new ReportDay(tuesday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(wednesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(wednesday, timeEnd), UTC), false)
            )),
            // march
            new ReportDay(wednesday, List.of()),
            new ReportDay(thursday, List.of()),
            new ReportDay(friday, List.of()),
            new ReportDay(saturday, List.of()),
            new ReportDay(sunday, List.of())
        ));
    }
}
