package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.absence.DayLength;
import de.focusshift.zeiterfassung.report.ReportDay;
import de.focusshift.zeiterfassung.report.ReportDayAbsence;
import de.focusshift.zeiterfassung.report.ReportDayEntry;
import de.focusshift.zeiterfassung.report.ReportServiceRaw;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.VIOLET;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimeServiceImplTest {

    private OvertimeServiceImpl sut;

    @Mock
    private ReportServiceRaw reportServiceRaw;

    @BeforeEach
    void setUp() {
        sut = new OvertimeServiceImpl(reportServiceRaw);
    }

    @Test
    void ensureGetOvertimeForDateIsEmpty() {

        final LocalDate date = LocalDate.parse("2025-05-09");

        when(reportServiceRaw.getReportDayForAllUsers(date))
           .thenReturn(new ReportDay(date, false, Map.of(), Map.of(), Map.of()));

        final Map<UserIdComposite, OvertimeHours> actual = sut.getOvertimeForDate(date);
        assertThat(actual).isEmpty();
     }

    @Test
    void ensureGetOvertimeForDate() {

        final LocalDate date = LocalDate.parse("2025-05-09");

        final UserId userIdBatman = new UserId("batman");
        final UserLocalId userLocalIdBatman = new UserLocalId(2L);
        final UserIdComposite userIdCompositeBatman = new UserIdComposite(userIdBatman, userLocalIdBatman);
        final User batman = new User(userIdCompositeBatman, "Bruce", "Wayne", new EMailAddress("batman@batman.org"), Set.of());

        final UserId userIdRobin = new UserId("robin");
        final UserLocalId userLocalIdRobin = new UserLocalId(2L);
        final UserIdComposite userIdCompositeRobin = new UserIdComposite(userIdRobin, userLocalIdRobin);
        final User robin = new User(userIdCompositeRobin, "Dick", "Johnson", new EMailAddress("robin@batman.org"), Set.of());

        final WorkingTimeCalendar workingTimeCalendarBatman = new WorkingTimeCalendar(
            Map.of(date, PlannedWorkingHours.EIGHT),
            Map.of()
        );

        final Absence absenceRobin = new Absence(userIdRobin, Instant.now(), Instant.now(), DayLength.FULL, locale -> "", VIOLET, HOLIDAY);
        final WorkingTimeCalendar workingTimeCalendarRobin = new WorkingTimeCalendar(
            Map.of(date, PlannedWorkingHours.EIGHT),
            Map.of(date, List.of(absenceRobin))
        );

        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars = Map.of(
            userIdCompositeBatman, workingTimeCalendarBatman,
            userIdCompositeRobin, workingTimeCalendarRobin
        );

        final ReportDayEntry reportDayBatman = new ReportDayEntry(new TimeEntryId(1L), batman, "", ZonedDateTime.parse("2025-05-09T08:00:00Z"), ZonedDateTime.parse("2025-05-09T20:00:00Z"), new WorkDuration(Duration.ofHours(12)), false);
        final Map<UserIdComposite, List<ReportDayEntry>> reportDayEntries = Map.of(
            userIdCompositeBatman, List.of(reportDayBatman),
            userIdCompositeRobin, List.of()
        );

        final Map<UserIdComposite, List<ReportDayAbsence>> absences = Map.of(
            userIdCompositeBatman, List.of(),
            userIdCompositeRobin, List.of(new ReportDayAbsence(robin, absenceRobin))
        );

        when(reportServiceRaw.getReportDayForAllUsers(date))
            .thenReturn(new ReportDay(date, false, workingTimeCalendars, reportDayEntries, absences));

        final Map<UserIdComposite, OvertimeHours> actual = sut.getOvertimeForDate(date);
        assertThat(actual).contains(
            Map.entry(userIdCompositeBatman, new OvertimeHours(Duration.ofHours(4))),
            Map.entry(userIdCompositeRobin, OvertimeHours.ZERO)
        );
    }

    @Test
    void ensureGetOvertimeForDateAndUser() {

        final LocalDate date = LocalDate.parse("2025-05-09");

        final UserId userIdBatman = new UserId("batman");
        final UserLocalId userLocalIdBatman = new UserLocalId(2L);
        final UserIdComposite userIdCompositeBatman = new UserIdComposite(userIdBatman, userLocalIdBatman);
        final User batman = new User(userIdCompositeBatman, "Bruce", "Wayne", new EMailAddress("batman@batman.org"), Set.of());

        final WorkingTimeCalendar workingTimeCalendarBatman = new WorkingTimeCalendar(
            Map.of(date, PlannedWorkingHours.EIGHT),
            Map.of()
        );

        final Map<UserIdComposite, WorkingTimeCalendar> workingTimeCalendars = Map.of(
            userIdCompositeBatman, workingTimeCalendarBatman
        );

        final ReportDayEntry reportDayBatman = new ReportDayEntry(new TimeEntryId(1L), batman, "", ZonedDateTime.parse("2025-05-09T08:00:00Z"), ZonedDateTime.parse("2025-05-09T20:00:00Z"), new WorkDuration(Duration.ofHours(12)), false);
        final Map<UserIdComposite, List<ReportDayEntry>> reportDayEntries = Map.of(
            userIdCompositeBatman, List.of(reportDayBatman)
        );

        final Map<UserIdComposite, List<ReportDayAbsence>> absences = Map.of(
            userIdCompositeBatman, List.of()
        );

        when(reportServiceRaw.getReportDayForAllUsers(date))
            .thenReturn(new ReportDay(date, false, workingTimeCalendars, reportDayEntries, absences));

        final OvertimeHours actual = sut.getOvertimeForDateAndUser(date, userLocalIdBatman);
        assertThat(actual).isEqualTo(new OvertimeHours(Duration.ofHours(4)));
    }
}
