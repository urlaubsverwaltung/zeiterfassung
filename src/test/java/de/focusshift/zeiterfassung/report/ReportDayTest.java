package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.RED;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static org.assertj.core.api.Assertions.assertThat;

class ReportDayTest {

    private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void reportDayEntries() {
        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2024, 11, 13, 1, 0);
        final ZonedDateTime to = dateTime(2024, 11, 13, 2, 0);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(batman, "hard work", from, to, true);

        LocalDate reportDate = LocalDate.of(2024, 11, 13);
        ReportDay reportDay = new ReportDay(reportDate, Map.of(), Map.of(batmanIdComposite, List.of(reportDayEntry)), Map.of());

        List<ReportDayEntry> reportDayEntries = reportDay.reportDayEntries();

        assertThat(reportDayEntries).hasSize(1).containsExactly(reportDayEntry);
    }

    @Test
    void plannedWorkingHours() {
        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId robinId = new UserId("uuid2");
        final UserLocalId robinLocalId = new UserLocalId(1337L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);

        LocalDate reportDate = LocalDate.of(2024, 11, 13);
        ReportDay reportDay = new ReportDay(
                reportDate,
                Map.of(
                        batmanIdComposite, new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of()),
                        robinIdComposite, new WorkingTimeCalendar(Map.of(reportDate, new PlannedWorkingHours(Duration.ofHours(4))), Map.of())
                ),
                Map.of(),
                Map.of());

        assertThat(reportDay.plannedWorkingHours().duration()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void ensureToRemoveBreaks() {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(batman, "hard work", from, to, true);

        LocalDate reportDate = LocalDate.of(2021, 1, 4);
        WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of());
        final ReportDay reportDay = new ReportDay(reportDate, Map.of(batmanIdComposite, workingTimeCalendar), Map.of(batmanIdComposite, List.of(reportDayEntry)), Map.of());

        assertThat(reportDay.workDuration().duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void shouldWorkingHoursReturnsZeroForFullyAbsentUser() {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        Absence absence = new Absence(batmanId, from, to, FULL, locale -> "foo", RED, HOLIDAY);

        LocalDate reportDate = LocalDate.of(2021, 1, 4);
        WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of(reportDate, List.of(absence)));
        final ReportDay reportDay = new ReportDay(reportDate, Map.of(batmanIdComposite, workingTimeCalendar), Map.of(batmanIdComposite, List.of()), Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence))));

        assertThat(reportDay.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ZERO));
    }

    @ParameterizedTest
    @EnumSource(value = DayLength.class, names = {"MORNING", "NOON"})
    void shouldWorkingHoursReturnsHalfShouldWorkingHoursForHalfDayAbsentUser(DayLength dayLength) {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        Absence absence = new Absence(batmanId, from, to, dayLength, locale -> "foo", RED, HOLIDAY);

        LocalDate reportDate = LocalDate.of(2021, 1, 4);
        WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of(reportDate, List.of(absence)));
        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batmanIdComposite, workingTimeCalendar), Map.of(batmanIdComposite, List.of()), Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence))));

        assertThat(reportDay.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ofHours(4)));
    }

    @Test
    void shouldWorkingHoursReturnsShouldHoursForOneAbsentUserAndOneWorkingUser() {

        final UserId batmanId = new UserId("uuid1");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        Absence absence = new Absence(batmanId, from, to, FULL, locale -> "foo", RED, HOLIDAY);

        final UserId robinId = new UserId("uuid2");
        final UserLocalId robinLocalId = new UserLocalId(1337L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);

        Map<UserIdComposite, List<ReportDayEntry>> entriesByUser = Map.of(batmanIdComposite, List.of(), robinIdComposite, List.of());
        Map<UserIdComposite, List<ReportDayAbsence>> absencesByUser = Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence)), robinIdComposite, List.of());

        LocalDate reportDate = LocalDate.of(2021, 1, 4);
        Map<UserIdComposite, WorkingTimeCalendar> plannedWorkingHoursByUser = Map.of(
                batmanIdComposite, new WorkingTimeCalendar(
                        Map.of(reportDate, PlannedWorkingHours.EIGHT),
                        Map.of(reportDate, List.of(absence))),
                robinIdComposite, new WorkingTimeCalendar(
                        Map.of(reportDate, new PlannedWorkingHours(Duration.ofHours(4))),
                        Map.of(reportDate, List.of()))
        );
        final ReportDay reportDay = new ReportDay(reportDate, plannedWorkingHoursByUser, entriesByUser, absencesByUser);

        assertThat(reportDay.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ofHours(4L)));
    }

    @ParameterizedTest
    @EnumSource(value = DayLength.class, names = {"MORNING", "NOON"})
    void shouldWorkingHoursReturnsSummedShouldHoursForOneHalfAbsentUserAndOneWorkingUser(DayLength dayLength) {

        final UserId batmanId = new UserId("uuid1");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        Absence absence = new Absence(batmanId, from, to, dayLength, locale -> "foo", RED, HOLIDAY);

        final UserId robinId = new UserId("uuid2");
        final UserLocalId robinLocalId = new UserLocalId(1337L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);

        Map<UserIdComposite, List<ReportDayEntry>> entriesByUser = Map.of(batmanIdComposite, List.of(), robinIdComposite, List.of());
        Map<UserIdComposite, List<ReportDayAbsence>> absencesByUser = Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence)), robinIdComposite, List.of());
        LocalDate reportDate = LocalDate.of(2021, 1, 4);
        Map<UserIdComposite, WorkingTimeCalendar> plannedWorkingHoursByUser = Map.of(
                batmanIdComposite, new WorkingTimeCalendar(
                        Map.of(reportDate, PlannedWorkingHours.EIGHT),
                        Map.of(reportDate, List.of(absence))),
                robinIdComposite, new WorkingTimeCalendar(
                        Map.of(reportDate, new PlannedWorkingHours(Duration.ofHours(4))),
                        Map.of(reportDate, List.of()))
        );
        final ReportDay reportDay = new ReportDay(reportDate, plannedWorkingHoursByUser, entriesByUser, absencesByUser);

        assertThat(reportDay.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ofHours(8L)));
    }

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
    }
}
