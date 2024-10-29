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

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
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

        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batmanIdComposite, PlannedWorkingHours.EIGHT), Map.of(batmanIdComposite, List.of(reportDayEntry)), Map.of());

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

        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batmanIdComposite, PlannedWorkingHours.EIGHT), Map.of(batmanIdComposite, List.of()), Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence))));

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

        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), Map.of(batmanIdComposite, PlannedWorkingHours.EIGHT), Map.of(batmanIdComposite, List.of()), Map.of(batmanIdComposite, List.of(new ReportDayAbsence(batman, absence))));

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
        Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser = Map.of(batmanIdComposite, PlannedWorkingHours.EIGHT, robinIdComposite, new PlannedWorkingHours(Duration.ofHours(4L)));
        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), plannedWorkingHoursByUser, entriesByUser, absencesByUser);

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
        Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser = Map.of(batmanIdComposite, PlannedWorkingHours.EIGHT, robinIdComposite, new PlannedWorkingHours(Duration.ofHours(4L)));
        final ReportDay reportDay = new ReportDay(LocalDate.of(2021, 1, 4), plannedWorkingHoursByUser, entriesByUser, absencesByUser);

        assertThat(reportDay.shouldWorkingHours()).isEqualTo(new ShouldWorkingHours(Duration.ofHours(8L)));
    }
}
