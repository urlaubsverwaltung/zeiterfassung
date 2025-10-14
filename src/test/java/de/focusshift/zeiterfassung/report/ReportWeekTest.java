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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
            new ReportDay(date, false, Map.of(userIdComposite, zeroHoursDay(date)), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(1), false, Map.of(userIdComposite, zeroHoursDay(date.plusDays(1))), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(2), false, Map.of(userIdComposite, zeroHoursDay(date.plusDays(2))), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(3), false, Map.of(userIdComposite, zeroHoursDay(date.plusDays(3))), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(4), false, Map.of(userIdComposite, zeroHoursDay(date.plusDays(4))), Map.of(userIdComposite, List.of()), Map.of()),
            new ReportDay(date.plusDays(5), false, Map.of(userIdComposite, zeroHoursDay(date.plusDays(5))), Map.of(userIdComposite, List.of()), Map.of())
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
        final WorkDuration workDuration = new WorkDuration(Duration.ofHours(8));

        final LocalDate monday = LocalDate.of(2023, 2, 20);
        final LocalDate tuesday = monday.plusDays(1);
        final LocalDate wednesday = monday.plusDays(2);
        final LocalDate thursday = monday.plusDays(3);
        final LocalDate friday = monday.plusDays(4);
        final LocalDate saturday = monday.plusDays(5);
        final LocalDate sunday = monday.plusDays(6);

        final ReportWeek sut = new ReportWeek(monday, List.of(
            new ReportDay(monday, false, Map.of(user.userIdComposite(), eightHoursDay(monday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(null, user, "", ZonedDateTime.of(LocalDateTime.of(monday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(monday, timeEnd), UTC), workDuration, false)
            )), Map.of()),
            new ReportDay(tuesday, false, Map.of(user.userIdComposite(), eightHoursDay(tuesday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(null, user, "", ZonedDateTime.of(LocalDateTime.of(tuesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(tuesday, timeEnd), UTC), workDuration, false)
            )), Map.of()),
            new ReportDay(wednesday, false, Map.of(user.userIdComposite(), eightHoursDay(wednesday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(null, user, "", ZonedDateTime.of(LocalDateTime.of(wednesday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(wednesday, timeEnd), UTC), workDuration, false)
            )), Map.of()),
            new ReportDay(thursday, false, Map.of(user.userIdComposite(), eightHoursDay(thursday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(null, user, "", ZonedDateTime.of(LocalDateTime.of(thursday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(thursday, timeEnd), UTC), workDuration, false)
            )), Map.of()),
            new ReportDay(friday, false, Map.of(user.userIdComposite(), eightHoursDay(friday)), Map.of(user.userIdComposite(), List.of(
                new ReportDayEntry(null, user, "", ZonedDateTime.of(LocalDateTime.of(friday, timeStart), UTC), ZonedDateTime.of(LocalDateTime.of(friday, timeEnd), UTC), workDuration, false)
            )), Map.of()),
            new ReportDay(saturday, false, Map.of(user.userIdComposite(), zeroHoursDay(saturday)), Map.of(user.userIdComposite(), List.of()), Map.of()),
            new ReportDay(sunday, false, Map.of(user.userIdComposite(), zeroHoursDay(sunday)), Map.of(user.userIdComposite(), List.of()), Map.of())
        ));

        final WorkDuration actual = sut.averageDayWorkDuration();

        assertThat(actual).isEqualTo(new WorkDuration(Duration.ofHours(8)));
    }

    @ParameterizedTest
    @CsvSource({
        "2021-12-27,52",
        "2022-01-03,1",
        "2022-09-26,39",
        "2022-12-26,52",
        "2023-01-02,1",
        "2024-12-23,52",
        "2024-12-30,1",
        "2025-01-06,2",
        "2025-12-22,52",
        "2025-12-29,1",
        "2026-01-05,2",
        "2026-12-28,53",
        "2027-01-04,1",
        "2027-12-27,52",
        "2028-01-03,1",
    })
    void ensureCalendarWeek(String date, int week) {
        final ReportWeek timeEntryWeek = new ReportWeek(LocalDate.parse(date), List.of());
        assertThat(timeEntryWeek.calenderWeek()).isEqualTo(week);
    }


    private WorkingTimeCalendar zeroHoursDay(LocalDate date) {
        return new WorkingTimeCalendar(Map.of(date, PlannedWorkingHours.ZERO), Map.of(date, List.of()));
    }

    private WorkingTimeCalendar eightHoursDay(LocalDate date) {
        return new WorkingTimeCalendar(Map.of(date, PlannedWorkingHours.EIGHT), Map.of(date, List.of()));
    }
}
