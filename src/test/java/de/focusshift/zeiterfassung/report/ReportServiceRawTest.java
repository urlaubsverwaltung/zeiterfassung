package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenantuser.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceRawTest {

    private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

    private ReportServiceRaw sut;

    @Mock
    private TimeEntryService timeEntryService;

    @Mock
    private UserManagementService userManagementService;

    @Mock
    private UserDateService userDateService;

    @BeforeEach
    void setUp() {
        sut = new ReportServiceRaw(timeEntryService, userManagementService, userDateService);
    }

    // ------------------------------------------------------------
    // WEEK report
    // ------------------------------------------------------------

    @Test
    void ensureReportWeekFirstDateOfWeekIsMonday() {

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, new UserId("batman"));
        assertThat(actualReportWeek.firstDateOfWeek().getDayOfWeek()).isEqualTo(MONDAY);
    }

    @Test
    void ensureReportWeekWithoutTimeEntriesFirstWeekOfYear() {

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), new UserId("batman")))
            .thenReturn(List.of());

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, new UserId("batman"));

        assertThat(actualReportWeek.reportDays()).hasSize(7);

        for (ReportDay reportDay : actualReportWeek.reportDays()) {
            assertThat(reportDay.workDuration().duration()).isZero();
        }
    }

    @Test
    void ensureReportWeekWithOneTimeEntryADay() {

        final ZonedDateTime firstFrom = dateTime(2021, 1, 4, 10, 0);
        final ZonedDateTime firstTo = dateTime(2021, 1, 4, 11, 0);
        final TimeEntry firstTimeEntry = new TimeEntry(1L, new UserId("batman"), "hard work", firstFrom, firstTo);

        final ZonedDateTime secondFrom = dateTime(2021, 1, 7, 8, 0);
        final ZonedDateTime secondTo = dateTime(2021, 1, 7, 11, 0);
        final TimeEntry secondTimeEntry = new TimeEntry(1L, new UserId("batman"), "hard work", secondFrom, secondTo);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), new UserId("batman")))
            .thenReturn(List.of(firstTimeEntry, secondTimeEntry));

        when(userManagementService.findAllUsersByIds(List.of(new UserId("batman"))))
            .thenReturn(List.of(new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of())));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, new UserId("batman"));

        assertThat(actualReportWeek.reportDays()).hasSize(7);

        assertThat(actualReportWeek.reportDays().get(0).workDuration().duration()).isEqualTo(Duration.ofHours(1L));
        assertThat(actualReportWeek.reportDays().get(1).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(2).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(3).workDuration().duration()).isEqualTo(Duration.ofHours(3L));
        assertThat(actualReportWeek.reportDays().get(4).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(5).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(6).workDuration().duration()).isZero();
    }

    @Test
    void ensureReportWeekWithMultipleTimeEntriesADay() {
        final ZonedDateTime morningFrom = dateTime(2021, 1, 5, 10, 0);
        final ZonedDateTime morningTo = dateTime(2021, 1, 5, 11, 0);
        final TimeEntry morningTimeEntry = new TimeEntry(1L, new UserId("batman"), "hard work in the morning", morningFrom, morningTo);

        final ZonedDateTime noonFrom = dateTime(2021, 1, 5, 15, 0);
        final ZonedDateTime noonTo = dateTime(2021, 1, 5, 19, 0);
        final TimeEntry noonTimeEntry = new TimeEntry(1L, new UserId("batman"), "hard work in the noon", noonFrom, noonTo);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), new UserId("batman")))
            .thenReturn(List.of(morningTimeEntry, noonTimeEntry));

        when(userManagementService.findAllUsersByIds(List.of(new UserId("batman"))))
            .thenReturn(List.of(new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of())));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, new UserId("batman"));

        assertThat(actualReportWeek.reportDays()).hasSize(7);

        assertThat(actualReportWeek.reportDays().get(0).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(1).workDuration().duration()).isEqualTo(Duration.ofHours(5L));
        assertThat(actualReportWeek.reportDays().get(2).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(3).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(4).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(5).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(6).workDuration().duration()).isZero();
    }

    @Test
    void ensureReportWeekWithTimeEntryTouchingNextDayIsReportedForStartingDate() {
        final ZonedDateTime from = dateTime(2021, 1, 4, 22, 0);
        final ZonedDateTime to = dateTime(2021, 1, 5, 3, 0);
        final TimeEntry timeEntry = new TimeEntry(1L, new UserId("batman"), "hard work in the night", from, to);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), new UserId("batman")))
            .thenReturn(List.of(timeEntry));

        when(userManagementService.findAllUsersByIds(List.of(new UserId("batman"))))
            .thenReturn(List.of(new User(new UserId("batman"), new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of())));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, new UserId("batman"));

        assertThat(actualReportWeek.reportDays()).hasSize(7);

        assertThat(actualReportWeek.reportDays().get(0).workDuration().duration()).isEqualTo(Duration.ofHours(5L));
        assertThat(actualReportWeek.reportDays().get(1).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(2).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(3).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(4).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(5).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(6).workDuration().duration()).isZero();
    }

    // ------------------------------------------------------------
    // MONTH report
    // ------------------------------------------------------------

    @Test
    void ensureReportMonthFirstDayOfEveryWeekIsMonday() {

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2021, 1, 1)))
            .thenReturn(LocalDate.of(2020, 12, 28));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1), new UserId("batman")))
            .thenReturn(List.of());

        final ReportMonth actualReportMonth = sut.getReportMonth(YearMonth.of(2021, 1), new UserId("batman"));

        assertThat(actualReportMonth.weeks()).hasSize(5);

        for (ReportWeek reportWeek : actualReportMonth.weeks()) {
            assertThat(reportWeek.firstDateOfWeek().getDayOfWeek()).isEqualTo(MONDAY);
        }
    }

    @Test
    void ensureReportMonthDecemberWithoutTimeEntries() {

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2021, 12, 1)))
            .thenReturn(LocalDate.of(2021, 11, 29));

        when(timeEntryService.getEntries(LocalDate.of(2021, 12, 1), LocalDate.of(2022, 1, 1), new UserId("batman")))
            .thenReturn(List.of());

        final ReportMonth actualReportMonth = sut.getReportMonth(YearMonth.of(2021, 12), new UserId("batman"));

        assertThat(actualReportMonth.yearMonth()).isEqualTo(YearMonth.of(2021, 12));
        assertThat(actualReportMonth.weeks()).hasSize(5);

        for (ReportWeek reportWeek : actualReportMonth.weeks()) {

            assertThat(reportWeek.reportDays()).hasSize(7);

            for (ReportDay reportDay : reportWeek.reportDays()) {
                assertThat(reportDay.workDuration().duration()).isZero();
            }
        }
    }

    @Test
    void ensureReportMonthDecemberWithOneTimeEntryAWeek() {

        final UserId batman = new UserId("batman");

        final ZonedDateTime w1_d1_From = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime w1_d1_To = dateTime(2021, 1, 4, 2, 0);
        final TimeEntry w1_d1_TimeEntry = new TimeEntry(1L, batman, "hard work w1_d1", w1_d1_From, w1_d1_To);
        final ZonedDateTime w1_d2_From = dateTime(2021, 1, 5, 3, 0);
        final ZonedDateTime w1_d2_To = dateTime(2021, 1, 5, 4, 0);
        final TimeEntry w1_d2_TimeEntry = new TimeEntry(1L, batman, "hard work w1_d2", w1_d2_From, w1_d2_To);

        final ZonedDateTime w2_d1_From = dateTime(2021, 1, 11, 1, 0);
        final ZonedDateTime w2_d1_To = dateTime(2021, 1, 11, 3, 0);
        final TimeEntry w2_d1_TimeEntry = new TimeEntry(1L, batman, "hard work w2_d1", w2_d1_From, w2_d1_To);
        final ZonedDateTime w2_d2_From = dateTime(2021, 1, 12, 4, 0);
        final ZonedDateTime w2_d2_To = dateTime(2021, 1, 12, 6, 0);
        final TimeEntry w2_d2_TimeEntry = new TimeEntry(1L, batman, "hard work w2_d2", w2_d2_From, w2_d2_To);

        final ZonedDateTime w3_d1_From = dateTime(2021, 1, 18, 1, 0);
        final ZonedDateTime w3_d1_To = dateTime(2021, 1, 18, 4, 0);
        final TimeEntry w3_d1_TimeEntry = new TimeEntry(1L, batman, "hard work w3_d1", w3_d1_From, w3_d1_To);
        final ZonedDateTime w3_d2_From = dateTime(2021, 1, 19, 5, 0);
        final ZonedDateTime w3_d2_To = dateTime(2021, 1, 19, 8, 0);
        final TimeEntry w3_d2_TimeEntry = new TimeEntry(1L, batman, "hard work w3_d2", w3_d2_From, w3_d2_To);

        final ZonedDateTime w4_d1_From = dateTime(2021, 1, 25, 1, 0);
        final ZonedDateTime w4_d1_To = dateTime(2021, 1, 25, 5, 0);
        final TimeEntry w4_d1_TimeEntry = new TimeEntry(1L, batman, "hard work w4_d1", w4_d1_From, w4_d1_To);
        final ZonedDateTime w4_d2_From = dateTime(2021, 1, 26, 6, 0);
        final ZonedDateTime w4_d2_To = dateTime(2021, 1, 26, 10, 0);
        final TimeEntry w4_d2_TimeEntry = new TimeEntry(1L, batman, "hard work w4_d2", w4_d2_From, w4_d2_To);

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2021, 1, 1)))
            .thenReturn(LocalDate.of(2020, 12, 28));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1), batman))
            .thenReturn(List.of(w1_d1_TimeEntry, w1_d2_TimeEntry, w2_d1_TimeEntry, w2_d2_TimeEntry, w3_d1_TimeEntry, w3_d2_TimeEntry, w4_d1_TimeEntry, w4_d2_TimeEntry));

        when(userManagementService.findAllUsersByIds(List.of(batman)))
            .thenReturn(List.of(new User(batman, new UserLocalId(1L), "Bruce", "Wayne", new EMailAddress("batman@example.org"), Set.of())));

        final ReportMonth actualReportMonth = sut.getReportMonth(YearMonth.of(2021, 1), batman);

        assertThat(actualReportMonth.yearMonth()).isEqualTo(YearMonth.of(2021, 1));
        assertThat(actualReportMonth.weeks()).hasSize(5);

        assertThat(actualReportMonth.weeks().get(0).workDuration().duration()).isEqualTo(Duration.ZERO);
        assertThat(actualReportMonth.weeks().get(1).workDuration().duration()).isEqualTo(Duration.ofHours(2));
        assertThat(actualReportMonth.weeks().get(2).workDuration().duration()).isEqualTo(Duration.ofHours(4));
        assertThat(actualReportMonth.weeks().get(3).workDuration().duration()).isEqualTo(Duration.ofHours(6));
        assertThat(actualReportMonth.weeks().get(4).workDuration().duration()).isEqualTo(Duration.ofHours(8));
    }

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
    }
}
