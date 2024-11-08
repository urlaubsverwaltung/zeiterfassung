package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class ReportWeekTest {

    @Test
    void ensureAverageDayWorkDurationIsEmptyWhenNoReportDays() {
        final ReportWeek sut = new ReportWeek(LocalDate.of(2023, 2, 13), List.of());
        assertThat(sut.averageDayWorkDuration()).isEqualTo(WorkDuration.ZERO);
    }

    @Test
    void ensureAverageDayWorkDurationIsEmptyWhenAllReportDaysHasNotPlannedWorkingHours() {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        LocalDate date = LocalDate.of(2023, 2, 13);
        final ReportWeek sut = new ReportWeek(date, List.of(
            new ReportDay(date, Map.of(userIdComposite, zeroHoursDay(date)), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(1), Map.of(userIdComposite, zeroHoursDay(date.plusDays(1))), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(2), Map.of(userIdComposite, zeroHoursDay(date.plusDays(2))), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(3), Map.of(userIdComposite, zeroHoursDay(date.plusDays(3))), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(4), Map.of(userIdComposite, zeroHoursDay(date.plusDays(4))), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(5), Map.of(userIdComposite, zeroHoursDay(date.plusDays(5))), Map.of(userIdComposite, List.of()), Map.of())
        ));

        assertThat(sut.averageDayWorkDuration()).isEqualTo(WorkDuration.ZERO);
    }

    @Test
    void ensureAverageDayWorkDuration() {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

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
            new ReportDay(monday, Map.of(user.userIdComposite(), eightHoursDay(monday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(monday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(monday, timeEnd), UTC), false)
            )), Map.of()),
            new ReportDay(tuesday, Map.of(user.userIdComposite(), eightHoursDay(tuesday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(tuesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(tuesday, timeEnd), UTC), false)
            )), Map.of()),
            new ReportDay(wednesday, Map.of(user.userIdComposite(), eightHoursDay(wednesday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(wednesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(wednesday, timeEnd), UTC), false)
            )), Map.of()),
            new ReportDay(thursday, Map.of(user.userIdComposite(), eightHoursDay(thursday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(thursday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(thursday, timeEnd), UTC), false)
            )), Map.of()),
            new ReportDay(friday, Map.of(user.userIdComposite(), eightHoursDay(friday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(user, "", ZonedDateTime.of(LocalDateTime.of(friday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(friday, timeEnd), UTC), false)
            )), Map.of()),
            new ReportDay(saturday, Map.of(user.userIdComposite(), zeroHoursDay(saturday)), Map.of(user.userIdComposite(), List.of()), Map.of()),
            new ReportDay(sunday, Map.of(user.userIdComposite(), zeroHoursDay(sunday)), Map.of(user.userIdComposite(), List.of()), Map.of())
        ));

        final WorkDuration actual = sut.averageDayWorkDuration();

        assertThat(actual).isEqualTo(new WorkDuration(Duration.ofHours(8)));
    }


    private WorkingTimeCalendar zeroHoursDay(LocalDate date) {
        return new WorkingTimeCalendar(Map.of(date, PlannedWorkingHours.ZERO), Map.of(date, List.of()));
    }

    private WorkingTimeCalendar eightHoursDay(LocalDate date) {
        return new WorkingTimeCalendar(Map.of(date, PlannedWorkingHours.EIGHT), Map.of(date, List.of()));
    }
}
