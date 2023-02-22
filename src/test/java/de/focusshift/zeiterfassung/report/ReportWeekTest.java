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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class ReportWeekTest {

    @Test
    void ensureAverageDayWorkDuration() {

        final User user = new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final LocalTime timeStart = LocalTime.of(8, 0);
        final LocalTime timeEnd = timeStart.plusHours(8);

        final LocalDate monday = LocalDate.of(2023, 2, 20);
        final LocalDate tuesday = monday.plusDays(1);
        final LocalDate wednesday = monday.plusDays(2);
        final LocalDate thursday = monday.plusDays(3);
        final LocalDate friday = monday.plusDays(4);
        final LocalDate saturday = monday.plusDays(5);
        final LocalDate sunday = monday.plusDays(6);

        final ReportWeek sut = new ReportWeek(monday, List.of(
            new ReportDay(monday, List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(monday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(monday, timeEnd), UTC), false)
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

        final WorkDuration actual = sut.averageDayWorkDuration();

        assertThat(actual).isEqualTo(new WorkDuration(Duration.ofHours(8)));
    }
}
