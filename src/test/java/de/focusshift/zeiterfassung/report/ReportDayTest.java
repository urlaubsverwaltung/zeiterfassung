package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
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
import java.time.Instant;
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
        final ReportDayEntry reportDayEntry = new ReportDayEntry(null, batman, "hard work", from, to, WorkDuration.ZERO, true);

        final LocalDate reportDate = LocalDate.of(2024, 11, 13);
        final ReportDay sut = new ReportDay(reportDate, false, Map.of(), Map.of(batmanIdComposite, List.of(reportDayEntry)), Map.of());

        final List<ReportDayEntry> reportDayEntries = sut.reportDayEntries();
        assertThat(reportDayEntries)
            .hasSize(1)
            .containsExactly(reportDayEntry);
    }

    @Test
    void plannedWorkingHours() {
        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);

        final UserId robinId = new UserId("uuid2");
        final UserLocalId robinLocalId = new UserLocalId(1337L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);

        final LocalDate reportDate = LocalDate.of(2024, 11, 13);

        final ReportDay sut = new ReportDay(
            reportDate,
            false, Map.of(
            batmanIdComposite, new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of()),
            robinIdComposite, new WorkingTimeCalendar(Map.of(reportDate, new PlannedWorkingHours(Duration.ofHours(4))), Map.of())
        ),
            Map.of(),
            Map.of());

        assertThat(sut.plannedWorkingHours().duration()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void ensurePlannedWorkingHoursByUser() {

        final UserIdComposite batmanIdComposite = new UserIdComposite(new UserId("uuid"), new UserLocalId(1L));
        final UserIdComposite robinIdComposite = new UserIdComposite(new UserId("uuid2"), new UserLocalId(2L));
        final UserIdComposite jokerIdComposite = new UserIdComposite(new UserId("uuid3"), new UserLocalId(3L));

        final LocalDate reportDate = LocalDate.of(2024, 11, 13);

        final ReportDay sut = new ReportDay(
            reportDate,
            false, Map.of(
            batmanIdComposite, new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of()),
            robinIdComposite, new WorkingTimeCalendar(Map.of(reportDate, new PlannedWorkingHours(Duration.ofHours(4))), Map.of()),
            // enforce empty optional value while calculating planned working hours
            // which then use the fallback value of ZERO
            jokerIdComposite, new WorkingTimeCalendar(Map.of(reportDate.plusDays(1), PlannedWorkingHours.EIGHT), Map.of())
        ),
            Map.of(),
            Map.of());

        assertThat(sut.plannedWorkingHoursByUser()).isEqualTo(Map.of(
            batmanIdComposite, PlannedWorkingHours.EIGHT,
            robinIdComposite, new PlannedWorkingHours(Duration.ofHours(4)),
            jokerIdComposite, PlannedWorkingHours.ZERO)
        );
    }

    @Test
    void ensureToRemoveBreaks() {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final ReportDayEntry reportDayEntry = new ReportDayEntry(null, batman, "hard work", from, to, WorkDuration.ZERO, true);

        final LocalDate reportDate = LocalDate.of(2021, 1, 4);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of());

        final ReportDay sut = new ReportDay(reportDate, false, Map.of(batmanIdComposite, workingTimeCalendar), Map.of(batmanIdComposite, List.of(reportDayEntry)), Map.of());
        assertThat(sut.workDuration().duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void shouldWorkingHoursReturnsZeroForFullyAbsentUser() {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final Absence absence = new Absence(batmanId, from.toInstant(), to.toInstant(), FULL, locale -> "foo", RED, HOLIDAY);

        final LocalDate reportDate = LocalDate.of(2021, 1, 4);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of(reportDate, List.of(absence)));

        final ReportDay sut = new ReportDay(reportDate, false, Map.of(batmanIdComposite, workingTimeCalendar), Map.of(batmanIdComposite, List.of()), Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence))));
        assertThat(sut.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ZERO));
    }

    @ParameterizedTest
    @EnumSource(value = DayLength.class, names = {"MORNING", "NOON"})
    void shouldWorkingHoursReturnsHalfShouldWorkingHoursForHalfDayAbsentUser(DayLength dayLength) {

        final UserId batmanId = new UserId("uuid");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final Instant from = dateTime(2021, 1, 4, 1, 0).toInstant();
        final Instant to = dateTime(2021, 1, 4, 2, 0).toInstant();
        final Absence absence = new Absence(batmanId, from, to, dayLength, locale -> "foo", RED, HOLIDAY);

        final LocalDate reportDate = LocalDate.of(2021, 1, 4);
        final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(reportDate, PlannedWorkingHours.EIGHT), Map.of(reportDate, List.of(absence)));

        final ReportDay sut = new ReportDay(LocalDate.of(2021, 1, 4), false, Map.of(batmanIdComposite, workingTimeCalendar), Map.of(batmanIdComposite, List.of()), Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence))));
        assertThat(sut.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ofHours(4)));
    }

    @Test
    void shouldWorkingHoursReturnsShouldHoursForOneAbsentUserAndOneWorkingUser() {

        final UserId batmanId = new UserId("uuid1");
        final UserLocalId batmanLocalId = new UserLocalId(1337L);
        final UserIdComposite batmanIdComposite = new UserIdComposite(batmanId, batmanLocalId);
        final User batman = new User(batmanIdComposite, "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of());

        final ZonedDateTime from = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime to = dateTime(2021, 1, 4, 2, 0);
        final Absence absence = new Absence(batmanId, from.toInstant(), to.toInstant(), FULL, locale -> "foo", RED, HOLIDAY);

        final UserId robinId = new UserId("uuid2");
        final UserLocalId robinLocalId = new UserLocalId(1337L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);

        final Map<UserIdComposite, List<ReportDayEntry>> entriesByUser = Map.of(batmanIdComposite, List.of(), robinIdComposite, List.of());
        final Map<UserIdComposite, List<ReportDayAbsence>> absencesByUser = Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence)), robinIdComposite, List.of());

        final LocalDate reportDate = LocalDate.of(2021, 1, 4);
        final Map<UserIdComposite, WorkingTimeCalendar> plannedWorkingHoursByUser = Map.of(
            batmanIdComposite, new WorkingTimeCalendar(
                Map.of(reportDate, PlannedWorkingHours.EIGHT),
                Map.of(reportDate, List.of(absence))),
            robinIdComposite, new WorkingTimeCalendar(
                Map.of(reportDate, new PlannedWorkingHours(Duration.ofHours(4))),
                Map.of(reportDate, List.of()))
        );

        final ReportDay sut = new ReportDay(reportDate, false, plannedWorkingHoursByUser, entriesByUser, absencesByUser);
        assertThat(sut.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ofHours(4L)));
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
        final Absence absence = new Absence(batmanId, from.toInstant(), to.toInstant(), dayLength, locale -> "foo", RED, HOLIDAY);

        final UserId robinId = new UserId("uuid2");
        final UserLocalId robinLocalId = new UserLocalId(1337L);
        final UserIdComposite robinIdComposite = new UserIdComposite(robinId, robinLocalId);

        final Map<UserIdComposite, List<ReportDayEntry>> entriesByUser = Map.of(batmanIdComposite, List.of(), robinIdComposite, List.of());
        final Map<UserIdComposite, List<ReportDayAbsence>> absencesByUser = Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence)), robinIdComposite, List.of());
        final LocalDate reportDate = LocalDate.of(2021, 1, 4);
        final Map<UserIdComposite, WorkingTimeCalendar> plannedWorkingHoursByUser = Map.of(
            batmanIdComposite, new WorkingTimeCalendar(
                Map.of(reportDate, PlannedWorkingHours.EIGHT),
                Map.of(reportDate, List.of(absence))),
            robinIdComposite, new WorkingTimeCalendar(
                Map.of(reportDate, new PlannedWorkingHours(Duration.ofHours(4))),
                Map.of(reportDate, List.of()))
        );

        final ReportDay sut = new ReportDay(reportDate, false, plannedWorkingHoursByUser, entriesByUser, absencesByUser);
        assertThat(sut.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ofHours(8L)));
    }

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
    }
}
