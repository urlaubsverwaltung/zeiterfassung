package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
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

    @Mock
    private WorkingTimeCalendarService workingTimeCalendarService;

    @Mock
    private TimeEntryLockService timeEntryLockService;

    @BeforeEach
    void setUp() {
        sut = new ReportServiceRaw(timeEntryService, userManagementService, userDateService, workingTimeCalendarService, timeEntryLockService);
    }

    // ------------------------------------------------------------
    // WEEK report
    // ------------------------------------------------------------

    @Test
    void ensureReportWeekFirstDateOfWeekIsMonday() {

        final User user = anyUser();
        when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userLocalId());
        assertThat(actualReportWeek.firstDateOfWeek().getDayOfWeek()).isEqualTo(MONDAY);
    }

    @Test
    void ensureReportWeekWithoutTimeEntriesFirstWeekOfYear() {

        final User user = anyUser();
        when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), user.userLocalId()))
            .thenReturn(List.of());

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userLocalId());

        assertThat(actualReportWeek.reportDays()).hasSize(7);

        for (ReportDay reportDay : actualReportWeek.reportDays()) {
            assertThat(reportDay.workDuration().duration()).isZero();
        }
    }

    @Test
    void ensureReportWeekWithOneTimeEntryADay() {

        final User user = anyUser();
        when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

        final ZonedDateTime firstFrom = dateTime(2021, 1, 4, 10, 0);
        final ZonedDateTime firstTo = dateTime(2021, 1, 4, 11, 0);
        final TimeEntry firstTimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work", firstFrom, firstTo, false);

        final ZonedDateTime secondFrom = dateTime(2021, 1, 7, 8, 0);
        final ZonedDateTime secondTo = dateTime(2021, 1, 7, 11, 0);
        final TimeEntry secondTimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work", secondFrom, secondTo, false);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), user.userLocalId()))
            .thenReturn(List.of(firstTimeEntry, secondTimeEntry));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userLocalId());

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

        final User user = anyUser();
        when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

        final ZonedDateTime morningFrom = dateTime(2021, 1, 5, 10, 0);
        final ZonedDateTime morningTo = dateTime(2021, 1, 5, 11, 0);
        final TimeEntry morningTimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work in the morning", morningFrom, morningTo, false);

        final ZonedDateTime noonFrom = dateTime(2021, 1, 5, 15, 0);
        final ZonedDateTime noonTo = dateTime(2021, 1, 5, 19, 0);
        final TimeEntry noonTimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work in the noon", noonFrom, noonTo, false);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), user.userLocalId()))
            .thenReturn(List.of(morningTimeEntry, noonTimeEntry));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userLocalId());

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

        final User user = anyUser();
        when(userManagementService.findUserByLocalId(user.userLocalId())).thenReturn(Optional.of(user));

        final ZonedDateTime from = dateTime(2021, 1, 4, 22, 0);
        final ZonedDateTime to = dateTime(2021, 1, 5, 3, 0);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work in the night", from, to, false);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), user.userLocalId()))
            .thenReturn(List.of(timeEntry));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userLocalId());

        assertThat(actualReportWeek.reportDays()).hasSize(7);

        assertThat(actualReportWeek.reportDays().get(0).workDuration().duration()).isEqualTo(Duration.ofHours(5L));
        assertThat(actualReportWeek.reportDays().get(1).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(2).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(3).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(4).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(5).workDuration().duration()).isZero();
        assertThat(actualReportWeek.reportDays().get(6).workDuration().duration()).isZero();
    }

    @Test
    void ensureReportWeek() {
        final User user = anyUser();
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate endExclusive = LocalDate.of(2024, 1, 8);

        when(userDateService.firstDayOfWeek(Year.of(2024), 1))
            .thenReturn(start);

        sut.getReportWeek(Year.of(2024), 1, List.of(user.userLocalId()));

        verify(timeEntryService).getEntries(start, endExclusive, List.of(user.userLocalId()));
        verify(workingTimeCalendarService).getWorkingTimeCalendarForUsers(start, endExclusive, List.of(user.userLocalId()));
    }

    @Test
    void ensureReportWeekIncludesEntriesForEveryUserDespiteNoTimeEntries() {

        final User user = anyUser(new UserIdComposite(new UserId("batman"), new UserLocalId(1L)));
        final User userTwo = anyUser(new UserIdComposite(new UserId("robin"), new UserLocalId(2L)));

        when(userManagementService.findAllUsersByLocalIds(List.of(user.userLocalId(), userTwo.userLocalId())))
            .thenReturn(List.of(user, userTwo));

        when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(any(), any(), any()))
            .thenReturn(Map.of(
                user.userIdComposite(), new WorkingTimeCalendar(Map.of(), Map.of()),
                userTwo.userIdComposite(), new WorkingTimeCalendar(Map.of(), Map.of()))
            );

        when(timeEntryService.getEntries(any(LocalDate.class), any(LocalDate.class), anyList()))
            .thenReturn(Map.of(
                user.userIdComposite(), List.of(),
                userTwo.userIdComposite(), List.of()
            ));

        LocalDate start = LocalDate.of(2024, 1, 1);
        when(userDateService.firstDayOfWeek(Year.of(2024), 1))
            .thenReturn(start);

        final ReportWeek reportWeek = sut.getReportWeek(Year.of(2024), 1, List.of(user.userLocalId(), userTwo.userLocalId()));

        for (ReportDay reportDay : reportWeek.reportDays()) {
            assertThat(reportDay.workingTimeCalendarByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
            assertThat(reportDay.reportDayEntriesByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
            assertThat(reportDay.detailDayAbsencesByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
        }
    }

    @Test
    void getReportWeekForAllUsers() {
        final LocalDate start = LocalDate.of(2024, 1, 1);
        final LocalDate endExclusive = LocalDate.of(2024, 1, 8);

        when(userDateService.firstDayOfWeek(Year.of(2024), 1))
            .thenReturn(start);

        sut.getReportWeekForAllUsers(Year.of(2024), 1);

        verify(timeEntryService).getEntriesForAllUsers(start, endExclusive);
        verify(workingTimeCalendarService).getWorkingTimeCalendarForAllUsers(start, endExclusive);
    }

    @Test
    void ensureReportWeekForAllUsersIncludesEntriesForEveryUserDespiteNoTimeEntries() {

        final User user = anyUser(new UserIdComposite(new UserId("batman"), new UserLocalId(1L)));
        final User userTwo = anyUser(new UserIdComposite(new UserId("robin"), new UserLocalId(2L)));

        when(userManagementService.findAllUsers()).thenReturn(List.of(user, userTwo));

        when(workingTimeCalendarService.getWorkingTimeCalendarForAllUsers(any(), any()))
            .thenReturn(Map.of(
                user.userIdComposite(), new WorkingTimeCalendar(Map.of(), Map.of()),
                userTwo.userIdComposite(), new WorkingTimeCalendar(Map.of(), Map.of()))
            );

        when(timeEntryService.getEntriesForAllUsers(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Map.of(
                user.userIdComposite(), List.of(),
                userTwo.userIdComposite(), List.of()
            ));

        LocalDate start = LocalDate.of(2024, 1, 1);
        when(userDateService.firstDayOfWeek(Year.of(2024), 1))
            .thenReturn(start);

        final ReportWeek reportWeek = sut.getReportWeekForAllUsers(Year.of(2024), 1);

        for (ReportDay reportDay : reportWeek.reportDays()) {
            assertThat(reportDay.workingTimeCalendarByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
            assertThat(reportDay.reportDayEntriesByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
            assertThat(reportDay.detailDayAbsencesByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
        }
    }

    // ------------------------------------------------------------
    // MONTH report
    // ------------------------------------------------------------

    @Test
    void ensureReportMonthFirstDayOfEveryWeekIsMonday() {

        final User user = anyUser();
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2021, 1, 1)))
            .thenReturn(LocalDate.of(2020, 12, 28));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1), List.of(user.userLocalId())))
            .thenReturn(Map.of());

        final ReportMonth actualReportMonth = sut.getReportMonth(YearMonth.of(2021, 1), user.userId());

        assertThat(actualReportMonth.weeks()).hasSize(5);

        for (ReportWeek reportWeek : actualReportMonth.weeks()) {
            assertThat(reportWeek.firstDateOfWeek().getDayOfWeek()).isEqualTo(MONDAY);
        }
    }

    @Test
    void ensureReportMonthDecemberWithoutTimeEntries() {

        final User user = anyUser();
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2021, 12, 1)))
            .thenReturn(LocalDate.of(2021, 11, 29));

        when(timeEntryService.getEntries(LocalDate.of(2021, 12, 1), LocalDate.of(2022, 1, 1), List.of(user.userLocalId())))
            .thenReturn(Map.of());

        final ReportMonth actualReportMonth = sut.getReportMonth(YearMonth.of(2021, 12), user.userId());

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

        final User user = anyUser();
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        final ZonedDateTime w1_d1_From = dateTime(2021, 1, 4, 1, 0);
        final ZonedDateTime w1_d1_To = dateTime(2021, 1, 4, 2, 0);
        final TimeEntry w1_d1_TimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work w1_d1", w1_d1_From, w1_d1_To, false);
        final ZonedDateTime w1_d2_From = dateTime(2021, 1, 5, 3, 0);
        final ZonedDateTime w1_d2_To = dateTime(2021, 1, 5, 4, 0);
        final TimeEntry w1_d2_TimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work w1_d2", w1_d2_From, w1_d2_To, false);

        final ZonedDateTime w2_d1_From = dateTime(2021, 1, 11, 1, 0);
        final ZonedDateTime w2_d1_To = dateTime(2021, 1, 11, 3, 0);
        final TimeEntry w2_d1_TimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work w2_d1", w2_d1_From, w2_d1_To, false);
        final ZonedDateTime w2_d2_From = dateTime(2021, 1, 12, 4, 0);
        final ZonedDateTime w2_d2_To = dateTime(2021, 1, 12, 6, 0);
        final TimeEntry w2_d2_TimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work w2_d2", w2_d2_From, w2_d2_To, false);

        final ZonedDateTime w3_d1_From = dateTime(2021, 1, 18, 1, 0);
        final ZonedDateTime w3_d1_To = dateTime(2021, 1, 18, 4, 0);
        final TimeEntry w3_d1_TimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work w3_d1", w3_d1_From, w3_d1_To, false);
        final ZonedDateTime w3_d2_From = dateTime(2021, 1, 19, 5, 0);
        final ZonedDateTime w3_d2_To = dateTime(2021, 1, 19, 8, 0);
        final TimeEntry w3_d2_TimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work w3_d2", w3_d2_From, w3_d2_To, false);

        final ZonedDateTime w4_d1_From = dateTime(2021, 1, 25, 1, 0);
        final ZonedDateTime w4_d1_To = dateTime(2021, 1, 25, 5, 0);
        final TimeEntry w4_d1_TimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work w4_d1", w4_d1_From, w4_d1_To, false);
        final ZonedDateTime w4_d2_From = dateTime(2021, 1, 26, 6, 0);
        final ZonedDateTime w4_d2_To = dateTime(2021, 1, 26, 10, 0);
        final TimeEntry w4_d2_TimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work w4_d2", w4_d2_From, w4_d2_To, false);

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2021, 1, 1)))
            .thenReturn(LocalDate.of(2020, 12, 28));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1), List.of(user.userLocalId())))
            .thenReturn(Map.of(user.userIdComposite(), List.of(w1_d1_TimeEntry, w1_d2_TimeEntry, w2_d1_TimeEntry, w2_d2_TimeEntry, w3_d1_TimeEntry, w3_d2_TimeEntry, w4_d1_TimeEntry, w4_d2_TimeEntry)));

        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        final ReportMonth actualReportMonth = sut.getReportMonth(YearMonth.of(2021, 1), user.userId());

        assertThat(actualReportMonth.yearMonth()).isEqualTo(YearMonth.of(2021, 1));
        assertThat(actualReportMonth.weeks()).hasSize(5);

        assertThat(actualReportMonth.weeks().get(0).workDuration().duration()).isEqualTo(Duration.ZERO);
        assertThat(actualReportMonth.weeks().get(1).workDuration().duration()).isEqualTo(Duration.ofHours(2));
        assertThat(actualReportMonth.weeks().get(2).workDuration().duration()).isEqualTo(Duration.ofHours(4));
        assertThat(actualReportMonth.weeks().get(3).workDuration().duration()).isEqualTo(Duration.ofHours(6));
        assertThat(actualReportMonth.weeks().get(4).workDuration().duration()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void getReportMonth() {
        final User user = anyUser();
        final YearMonth yearMonth = YearMonth.of(2024, 1);
        final LocalDate start = yearMonth.atDay(1);
        final LocalDate endExclusive = yearMonth.atEndOfMonth().plusDays(1);

        when(userDateService.localDateToFirstDateOfWeek(start))
            .thenReturn(start);

        sut.getReportMonth(yearMonth, List.of(user.userLocalId()));

        verify(timeEntryService).getEntries(start, endExclusive, List.of(user.userLocalId()));
        verify(workingTimeCalendarService).getWorkingTimeCalendarForUsers(start, endExclusive, List.of(user.userLocalId()));
    }

    @Test
    void ensureReportMonthIncludesEntriesForEveryUserDespiteNoTimeEntries() {

        final YearMonth yearMonth = YearMonth.of(2024, 1);

        final User user = anyUser(new UserIdComposite(new UserId("batman"), new UserLocalId(1L)));
        final User userTwo = anyUser(new UserIdComposite(new UserId("robin"), new UserLocalId(2L)));

        when(userManagementService.findAllUsersByLocalIds(List.of(user.userLocalId(), userTwo.userLocalId())))
            .thenReturn(List.of(user, userTwo));

        when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(any(), any(), any()))
            .thenReturn(Map.of(
                user.userIdComposite(), new WorkingTimeCalendar(Map.of(), Map.of()),
                userTwo.userIdComposite(), new WorkingTimeCalendar(Map.of(), Map.of()))
            );

        when(timeEntryService.getEntries(any(LocalDate.class), any(LocalDate.class), anyList()))
            .thenReturn(Map.of(
                user.userIdComposite(), List.of(),
                userTwo.userIdComposite(), List.of()
            ));

        final LocalDate start = LocalDate.of(2024, 1, 1);
        when(userDateService.localDateToFirstDateOfWeek(start)).thenReturn(start);

        final ReportMonth actual = sut.getReportMonth(yearMonth, List.of(user.userLocalId(), userTwo.userLocalId()));

        for (ReportWeek reportWeek : actual.weeks()) {
            for (ReportDay reportDay : reportWeek.reportDays()) {
                assertThat(reportDay.workingTimeCalendarByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
                assertThat(reportDay.reportDayEntriesByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
                assertThat(reportDay.detailDayAbsencesByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
            }
        }
    }

    @Test
    void getReportMonthForAllUsers() {
        YearMonth month = YearMonth.of(2024, 1);
        LocalDate start = month.atDay(1);
        LocalDate endExclusive = month.atEndOfMonth().plusDays(1);

        when(userDateService.localDateToFirstDateOfWeek(start))
            .thenReturn(start);

        sut.getReportMonthForAllUsers(month);

        verify(timeEntryService).getEntriesForAllUsers(start, endExclusive);
        verify(workingTimeCalendarService).getWorkingTimeCalendarForAllUsers(start, endExclusive);
    }

    @Test
    void ensureReportMonthForAllUsersIncludesEntriesForEveryUserDespiteNoTimeEntries() {

        final YearMonth yearMonth = YearMonth.of(2024, 1);

        final User user = anyUser(new UserIdComposite(new UserId("batman"), new UserLocalId(1L)));
        final User userTwo = anyUser(new UserIdComposite(new UserId("robin"), new UserLocalId(2L)));

        when(userManagementService.findAllUsers()).thenReturn(List.of(user, userTwo));

        when(workingTimeCalendarService.getWorkingTimeCalendarForAllUsers(any(), any()))
            .thenReturn(Map.of(
                user.userIdComposite(), new WorkingTimeCalendar(Map.of(), Map.of()),
                userTwo.userIdComposite(), new WorkingTimeCalendar(Map.of(), Map.of()))
            );

        when(timeEntryService.getEntriesForAllUsers(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Map.of(
                user.userIdComposite(), List.of(),
                userTwo.userIdComposite(), List.of()
            ));

        final LocalDate start = LocalDate.of(2024, 1, 1);
        when(userDateService.localDateToFirstDateOfWeek(start)).thenReturn(start);

        final ReportMonth actual = sut.getReportMonthForAllUsers(yearMonth);

        for (ReportWeek reportWeek : actual.weeks()) {
            for (ReportDay reportDay : reportWeek.reportDays()) {
                assertThat(reportDay.workingTimeCalendarByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
                assertThat(reportDay.reportDayEntriesByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
                assertThat(reportDay.detailDayAbsencesByUser()).containsOnlyKeys(user.userIdComposite(), userTwo.userIdComposite());
            }
        }
    }

    private static User anyUser() {

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        return anyUser(userIdComposite);
    }

    private static User anyUser(UserIdComposite userIdComposite) {
        return new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
    }

    private static ZonedDateTime dateTime(int year, int month, int dayOfMonth, int hour, int minute) {
        return ZonedDateTime.of(LocalDateTime.of(year, month, dayOfMonth, hour, minute), ZONE_ID_BERLIN);
    }
}
