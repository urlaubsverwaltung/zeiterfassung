package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.absence.AbsenceService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.user.UserDateService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.Month.OCTOBER;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private AbsenceService absenceService;

    @BeforeEach
    void setUp() {
        sut = new ReportServiceRaw(timeEntryService, userManagementService, userDateService, workingTimeCalendarService, absenceService);
    }

    // ------------------------------------------------------------
    // WEEK report
    // ------------------------------------------------------------

    @Test
    void ensureReportWeekFirstDateOfWeekIsMonday() {

        final User user = anyUser();
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userId());
        assertThat(actualReportWeek.firstDateOfWeek().getDayOfWeek()).isEqualTo(MONDAY);
    }

    @Test
    void ensureReportWeekWithoutTimeEntriesFirstWeekOfYear() {

        final User user = anyUser();
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), new UserId("batman")))
            .thenReturn(List.of());

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userId());

        assertThat(actualReportWeek.reportDays()).hasSize(7);

        for (ReportDay reportDay : actualReportWeek.reportDays()) {
            assertThat(reportDay.workDuration().duration()).isZero();
        }
    }

    @Test
    void ensureReportWeekWithOneTimeEntryADay() {

        final User user = anyUser();
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        final ZonedDateTime firstFrom = dateTime(2021, 1, 4, 10, 0);
        final ZonedDateTime firstTo = dateTime(2021, 1, 4, 11, 0);
        final TimeEntry firstTimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work", firstFrom, firstTo, false);

        final ZonedDateTime secondFrom = dateTime(2021, 1, 7, 8, 0);
        final ZonedDateTime secondTo = dateTime(2021, 1, 7, 11, 0);
        final TimeEntry secondTimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work", secondFrom, secondTo, false);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), user.userId()))
            .thenReturn(List.of(firstTimeEntry, secondTimeEntry));

        when(userManagementService.findAllUsersByIds(List.of(user.userId()))).thenReturn(List.of(user));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userId());

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
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        final ZonedDateTime morningFrom = dateTime(2021, 1, 5, 10, 0);
        final ZonedDateTime morningTo = dateTime(2021, 1, 5, 11, 0);
        final TimeEntry morningTimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work in the morning", morningFrom, morningTo, false);

        final ZonedDateTime noonFrom = dateTime(2021, 1, 5, 15, 0);
        final ZonedDateTime noonTo = dateTime(2021, 1, 5, 19, 0);
        final TimeEntry noonTimeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work in the noon", noonFrom, noonTo, false);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), user.userId()))
            .thenReturn(List.of(morningTimeEntry, noonTimeEntry));

        when(userManagementService.findAllUsersByIds(List.of(user.userId()))).thenReturn(List.of(user));

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

        final User user = anyUser();
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        final ZonedDateTime from = dateTime(2021, 1, 4, 22, 0);
        final ZonedDateTime to = dateTime(2021, 1, 5, 3, 0);
        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), user.userIdComposite(), "hard work in the night", from, to, false);

        when(userDateService.firstDayOfWeek(Year.of(2021), 1))
            .thenReturn(LocalDate.of(2021, 1, 4));

        when(timeEntryService.getEntries(LocalDate.of(2021, 1, 4), LocalDate.of(2021, 1, 11), user.userId()))
            .thenReturn(List.of(timeEntry));

        when(userManagementService.findAllUsersByIds(List.of(user.userId()))).thenReturn(List.of(user));

        final ReportWeek actualReportWeek = sut.getReportWeek(Year.of(2021), 1, user.userId());

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

        final User user = anyUser();
        when(userManagementService.findUserById(user.userId())).thenReturn(Optional.of(user));

        when(userDateService.localDateToFirstDateOfWeek(LocalDate.of(2021, 1, 1)))
            .thenReturn(LocalDate.of(2020, 12, 28));

        when(timeEntryService.getEntriesByUserLocalIds(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1), List.of(user.userLocalId())))
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

        when(timeEntryService.getEntriesByUserLocalIds(LocalDate.of(2021, 12, 1), LocalDate.of(2022, 1, 1), List.of(user.userLocalId())))
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

        when(timeEntryService.getEntriesByUserLocalIds(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1), List.of(user.userLocalId())))
            .thenReturn(Map.of(user.userIdComposite(), List.of(w1_d1_TimeEntry, w1_d2_TimeEntry, w2_d1_TimeEntry, w2_d2_TimeEntry, w3_d1_TimeEntry, w3_d2_TimeEntry, w4_d1_TimeEntry, w4_d2_TimeEntry)));

        when(userManagementService.findAllUsersByIds(List.of(user.userId()))).thenReturn(List.of(user));

        final ReportMonth actualReportMonth = sut.getReportMonth(YearMonth.of(2021, 1), user.userId());

        assertThat(actualReportMonth.yearMonth()).isEqualTo(YearMonth.of(2021, 1));
        assertThat(actualReportMonth.weeks()).hasSize(5);

        assertThat(actualReportMonth.weeks().get(0).workDuration().duration()).isEqualTo(Duration.ZERO);
        assertThat(actualReportMonth.weeks().get(1).workDuration().duration()).isEqualTo(Duration.ofHours(2));
        assertThat(actualReportMonth.weeks().get(2).workDuration().duration()).isEqualTo(Duration.ofHours(4));
        assertThat(actualReportMonth.weeks().get(3).workDuration().duration()).isEqualTo(Duration.ofHours(6));
        assertThat(actualReportMonth.weeks().get(4).workDuration().duration()).isEqualTo(Duration.ofHours(8));
    }

    @Nested
    class GetWeekSummary {

        @Test
        void ensureGetWeekSummaryForOneUserThrowsWhenUserIdIsUnknown() {

            final Year year = Year.of(2024);
            final UserId userId = new UserId("user-id");

            when(userManagementService.findUserById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.getWeekSummary(year, 42, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("could not find user id=UserId[value=user-id]");
        }

        @Test
        void ensureGetWeekSummaryForOneUserWithOvertime() {

            final Year year = Year.of(2024);
            final int week = 42;

            final UserId userId = new UserId("user-id");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            when(userManagementService.findUserById(userId)).thenReturn(Optional.of(anyUser(userIdComposite)));

            final LocalDate firstDayOfWeek = LocalDate.of(2024, 10, 14);
            final LocalDate firstDayOfNextWeek = LocalDate.of(2024, 10, 21);

            when(userDateService.firstDayOfWeek(year, week)).thenReturn(firstDayOfWeek);

            final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(
                firstDayOfWeek, PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(1), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(2), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(3), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(4), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(5), PlannedWorkingHours.ZERO,
                firstDayOfWeek.plusDays(6), PlannedWorkingHours.ZERO
            ));

            when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(firstDayOfWeek, firstDayOfNextWeek, List.of(userLocalId)))
                .thenReturn(Map.of(userIdComposite, workingTimeCalendar));

            final LocalTime time9am = LocalTime.of(9, 0);
            final LocalTime time5pm = LocalTime.of(17, 0);

            when(timeEntryService.getEntriesByUserLocalIds(firstDayOfWeek, firstDayOfNextWeek, List.of(userLocalId)))
                .thenReturn(Map.of(
                    userIdComposite, List.of(
                        timeEntry(userIdComposite, ZonedDateTime.of(firstDayOfWeek, time9am, UTC), ZonedDateTime.of(firstDayOfWeek, time5pm, UTC)),
                        timeEntry(userIdComposite, ZonedDateTime.of(firstDayOfWeek.plusDays(1), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(1), time5pm, UTC)),
                        timeEntry(userIdComposite, ZonedDateTime.of(firstDayOfWeek.plusDays(2), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(2), time5pm, UTC)),
                        timeEntry(userIdComposite, ZonedDateTime.of(firstDayOfWeek.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(3), time5pm, UTC)),
                        timeEntry(userIdComposite, ZonedDateTime.of(firstDayOfWeek.plusDays(4), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(4), time5pm, UTC)),
                        timeEntry(userIdComposite, ZonedDateTime.of(firstDayOfWeek.plusDays(5), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(5), time5pm, UTC)),
                        timeEntry(userIdComposite, ZonedDateTime.of(firstDayOfWeek.plusDays(6), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(6), time5pm, UTC))
                    )
                ));

            final ReportSummary actual = sut.getWeekSummary(year, week, userId);

            assertThat(actual.hoursWorked()).isEqualTo(Duration.ofHours(56));
            assertThat(actual.plannedWorkingHours()).isEqualTo(Duration.ofHours(40));
            assertThat(actual.hoursDelta()).isEqualTo(Duration.ofHours(16));
        }

        @Test
        void ensureGetWeekSummaryForOneUserWithNegativeDelta() {

            final Year year = Year.of(2024);
            final int week = 42;

            final UserId userId = new UserId("user-id");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            when(userManagementService.findUserById(userId)).thenReturn(Optional.of(anyUser(userIdComposite)));

            final LocalDate firstDayOfWeek = LocalDate.of(2024, 10, 14);
            final LocalDate firstDayOfNextWeek = LocalDate.of(2024, 10, 21);

            when(userDateService.firstDayOfWeek(year, week)).thenReturn(firstDayOfWeek);

            final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(
                firstDayOfWeek, PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(1), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(2), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(3), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(4), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(5), PlannedWorkingHours.ZERO,
                firstDayOfWeek.plusDays(6), PlannedWorkingHours.ZERO
            ));

            when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(firstDayOfWeek, firstDayOfNextWeek, List.of(userLocalId)))
                .thenReturn(Map.of(userIdComposite, workingTimeCalendar));

            final LocalTime time9am = LocalTime.of(9, 0);
            final LocalTime time5pm = LocalTime.of(17, 0);

            when(timeEntryService.getEntriesByUserLocalIds(firstDayOfWeek, firstDayOfNextWeek, List.of(userLocalId)))
                .thenReturn(Map.of(
                    userIdComposite, List.of(
                        timeEntry(userIdComposite, ZonedDateTime.of(firstDayOfWeek, time9am, UTC), ZonedDateTime.of(firstDayOfWeek, time5pm, UTC))
                    )
                ));

            final ReportSummary actual = sut.getWeekSummary(year, week, userId);

            assertThat(actual.hoursWorked()).isEqualTo(Duration.ofHours(8));
            assertThat(actual.plannedWorkingHours()).isEqualTo(Duration.ofHours(40));
            assertThat(actual.hoursDelta()).isEqualTo(Duration.ofHours(32).negated());
        }

        @Test
        void ensureGetWeekSummaryForUserLocalIds() {

            final Year year = Year.of(2024);
            final int week = 42;

            final UserLocalId userLocalId1 = new UserLocalId(1L);
            final UserLocalId userLocalId2 = new UserLocalId(2L);
            final UserIdComposite userIdComposite1 = new UserIdComposite(new UserId("1111"), userLocalId1);
            final UserIdComposite userIdComposite2 = new UserIdComposite(new UserId("2222"), userLocalId2);

            final LocalDate firstDayOfWeek = LocalDate.of(2024, 10, 14);
            final LocalDate firstDayOfNextWeek = LocalDate.of(2024, 10, 21);

            when(userDateService.firstDayOfWeek(year, week)).thenReturn(firstDayOfWeek);

            final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(
                firstDayOfWeek, PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(1), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(2), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(3), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(4), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(5), PlannedWorkingHours.ZERO,
                firstDayOfWeek.plusDays(6), PlannedWorkingHours.ZERO
            ));

            when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(firstDayOfWeek, firstDayOfNextWeek, List.of(userLocalId1, userLocalId2)))
                .thenReturn(Map.of(
                    userIdComposite1, workingTimeCalendar,
                    userIdComposite2, workingTimeCalendar
                ));

            final LocalTime time9am = LocalTime.of(9, 0);
            final LocalTime time5pm = LocalTime.of(17, 0);

            when(timeEntryService.getEntriesByUserLocalIds(firstDayOfWeek, firstDayOfNextWeek, List.of(userLocalId1, userLocalId2)))
                .thenReturn(Map.of(
                    userIdComposite1, List.of(
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek, time9am, UTC), ZonedDateTime.of(firstDayOfWeek, time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(1), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(1), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(2), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(2), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(3), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(4), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(4), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(5), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(5), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(6), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(6), time5pm, UTC))
                    ),
                    userIdComposite2, List.of(
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDayOfWeek, time9am, UTC), ZonedDateTime.of(firstDayOfWeek, time5pm, UTC)),
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDayOfWeek.plusDays(1), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(1), time5pm, UTC)),
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDayOfWeek.plusDays(2), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(2), time5pm, UTC)),
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDayOfWeek.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(3), time5pm, UTC)),
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDayOfWeek.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(3), time5pm, UTC))
                    )
                ));

            final ReportSummary actual = sut.getWeekSummary(year, week, List.of(userLocalId1, userLocalId2));

            assertThat(actual.hoursWorked()).isEqualTo(Duration.ofHours(96));
            assertThat(actual.plannedWorkingHours()).isEqualTo(Duration.ofHours(80));
            assertThat(actual.hoursDelta()).isEqualTo(Duration.ofHours(16));
        }

        @Test
        void ensureGetWeekSummaryForUserLocalIdsWithNegativeDelta() {

            final Year year = Year.of(2024);
            final int week = 42;

            final UserLocalId userLocalId1 = new UserLocalId(1L);
            final UserLocalId userLocalId2 = new UserLocalId(2L);
            final UserIdComposite userIdComposite1 = new UserIdComposite(new UserId("1111"), userLocalId1);
            final UserIdComposite userIdComposite2 = new UserIdComposite(new UserId("2222"), userLocalId2);

            final LocalDate firstDayOfWeek = LocalDate.of(2024, 10, 14);
            final LocalDate firstDayOfNextWeek = LocalDate.of(2024, 10, 21);

            when(userDateService.firstDayOfWeek(year, week)).thenReturn(firstDayOfWeek);

            final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(
                firstDayOfWeek, PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(1), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(2), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(3), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(4), PlannedWorkingHours.EIGHT,
                firstDayOfWeek.plusDays(5), PlannedWorkingHours.ZERO,
                firstDayOfWeek.plusDays(6), PlannedWorkingHours.ZERO
            ));

            when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(firstDayOfWeek, firstDayOfNextWeek, List.of(userLocalId1, userLocalId2)))
                .thenReturn(Map.of(
                    userIdComposite1, workingTimeCalendar,
                    userIdComposite2, workingTimeCalendar
                ));

            final LocalTime time9am = LocalTime.of(9, 0);
            final LocalTime time5pm = LocalTime.of(17, 0);

            when(timeEntryService.getEntriesByUserLocalIds(firstDayOfWeek, firstDayOfNextWeek, List.of(userLocalId1, userLocalId2)))
                .thenReturn(Map.of(
                    userIdComposite1, List.of(
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek, time9am, UTC), ZonedDateTime.of(firstDayOfWeek, time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(1), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(1), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(2), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(2), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(3), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDayOfWeek.plusDays(4), time9am, UTC), ZonedDateTime.of(firstDayOfWeek.plusDays(4), time5pm, UTC))
                    ),
                    userIdComposite2, List.of()
                ));

            final ReportSummary actual = sut.getWeekSummary(year, week, List.of(userLocalId1, userLocalId2));

            assertThat(actual.hoursWorked()).isEqualTo(Duration.ofHours(40));
            assertThat(actual.plannedWorkingHours()).isEqualTo(Duration.ofHours(80));
            assertThat(actual.hoursDelta()).isEqualTo(Duration.ofHours(40).negated());
        }
    }

    @Nested
    class GetMonthSummary {

        @Test
        void ensureGetMonthSummaryForOneUserThrowsWhenUserIdIsUnknown() {

            final YearMonth yearMonth = YearMonth.of(2024, OCTOBER);
            final UserId userId = new UserId("user-id");

            when(userManagementService.findUserById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.getMonthSummary(yearMonth, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("could not find user id=UserId[value=user-id]");
        }

        @Test
        void ensureGetMonthSummaryForOneUserWithOvertime() {

            final YearMonth yearMonth = YearMonth.of(2024, OCTOBER);

            final UserId userId = new UserId("user-id");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            when(userManagementService.findUserById(userId)).thenReturn(Optional.of(anyUser(userIdComposite)));

            final LocalDate firstDay = LocalDate.of(2024, 10, 1);
            final LocalDate lastDay = LocalDate.of(2024, 10, 31);

            final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(buildPlannedWorkingHoursByDate(firstDay, lastDay, date -> {
                if (date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY) {
                    return PlannedWorkingHours.ZERO;
                } else {
                    return PlannedWorkingHours.EIGHT;
                }
            }));

            when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(firstDay, lastDay.plusDays(1), List.of(userLocalId)))
                .thenReturn(Map.of(userIdComposite, workingTimeCalendar));

            final LocalTime time9am = LocalTime.of(9, 0);
            final LocalTime time5pm = LocalTime.of(17, 0);

            final List<TimeEntry> timeEntries = buildTimeEntries(firstDay, lastDay, date ->
                List.of(timeEntry(userIdComposite, ZonedDateTime.of(date, time9am, UTC), ZonedDateTime.of(date, time5pm, UTC)))
            );

            when(timeEntryService.getEntriesByUserLocalIds(firstDay, lastDay.plusDays(1), List.of(userLocalId)))
                .thenReturn(Map.of(userIdComposite, timeEntries));

            final ReportSummary actual = sut.getMonthSummary(yearMonth, userId);

            final int weekendDays = 8;
            assertThat(actual.hoursWorked()).isEqualTo(Duration.ofHours(31 * 8));
            assertThat(actual.plannedWorkingHours()).isEqualTo(Duration.ofHours((31 - weekendDays) * 8));
            assertThat(actual.hoursDelta()).isEqualTo(Duration.ofHours(weekendDays * 8));
        }

        @Test
        void ensureGetMonthSummaryForOneUserWithNegativeDelta() {

            final YearMonth yearMonth = YearMonth.of(2024, OCTOBER);

            final UserId userId = new UserId("user-id");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            when(userManagementService.findUserById(userId)).thenReturn(Optional.of(anyUser(userIdComposite)));

            final LocalDate firstDay = LocalDate.of(2024, 10, 1);
            final LocalDate lastDay = LocalDate.of(2024, 10, 31);

            final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(buildPlannedWorkingHoursByDate(firstDay, lastDay, date -> {
                if (date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY) {
                    return PlannedWorkingHours.ZERO;
                } else {
                    return PlannedWorkingHours.EIGHT;
                }
            }));

            when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(firstDay, lastDay.plusDays(1), List.of(userLocalId)))
                .thenReturn(Map.of(userIdComposite, workingTimeCalendar));

            final LocalTime time9am = LocalTime.of(9, 0);
            final LocalTime time5pm = LocalTime.of(17, 0);

            final List<TimeEntry> timeEntries = buildTimeEntries(firstDay, lastDay, date -> {
                if (date.equals(firstDay)) {
                    return List.of(timeEntry(userIdComposite, ZonedDateTime.of(date, time9am, UTC), ZonedDateTime.of(date, time5pm, UTC)));
                } else {
                    return List.of();
                }
            });

            when(timeEntryService.getEntriesByUserLocalIds(firstDay, lastDay.plusDays(1), List.of(userLocalId)))
                .thenReturn(Map.of(userIdComposite, timeEntries));

            final ReportSummary actual = sut.getMonthSummary(yearMonth, userId);

            final int weekendDays = 8;
            final Duration expectedPlannedHours = Duration.ofHours((31 - weekendDays) * 8);

            assertThat(actual.hoursWorked()).isEqualTo(Duration.ofHours(8));
            assertThat(actual.plannedWorkingHours()).isEqualTo(expectedPlannedHours);
            assertThat(actual.hoursDelta()).isEqualTo(expectedPlannedHours.minusHours(8).negated());
        }

        @Test
        void ensureGetMonthSummaryForUserLocalIds() {

            final YearMonth yearMonth = YearMonth.of(2024, OCTOBER);

            final UserLocalId userLocalId1 = new UserLocalId(1L);
            final UserLocalId userLocalId2 = new UserLocalId(2L);
            final UserIdComposite userIdComposite1 = new UserIdComposite(new UserId("user-id-1"), userLocalId1);
            final UserIdComposite userIdComposite2 = new UserIdComposite(new UserId("user-id-2"), userLocalId2);

            final LocalDate firstDay = LocalDate.of(2024, 10, 1);
            final LocalDate lastDay = LocalDate.of(2024, 10, 31);

            final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(
                firstDay, PlannedWorkingHours.EIGHT,
                firstDay.plusDays(1), PlannedWorkingHours.EIGHT,
                firstDay.plusDays(2), PlannedWorkingHours.EIGHT,
                firstDay.plusDays(3), PlannedWorkingHours.EIGHT,
                firstDay.plusDays(4), PlannedWorkingHours.EIGHT,
                firstDay.plusDays(5), PlannedWorkingHours.ZERO,
                firstDay.plusDays(6), PlannedWorkingHours.ZERO
            ));

            when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(firstDay, lastDay.plusDays(1), List.of(userLocalId1, userLocalId2)))
                .thenReturn(Map.of(
                    userIdComposite1, workingTimeCalendar,
                    userIdComposite2, workingTimeCalendar
                ));

            final LocalTime time9am = LocalTime.of(9, 0);
            final LocalTime time5pm = LocalTime.of(17, 0);

            when(timeEntryService.getEntriesByUserLocalIds(firstDay, lastDay.plusDays(1), List.of(userLocalId1, userLocalId2)))
                .thenReturn(Map.of(
                    userIdComposite1, List.of(
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay, time9am, UTC), ZonedDateTime.of(firstDay, time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(1), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(1), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(2), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(2), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(3), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(4), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(4), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(5), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(5), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(6), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(6), time5pm, UTC))
                    ),
                    userIdComposite2, List.of(
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDay, time9am, UTC), ZonedDateTime.of(firstDay, time5pm, UTC)),
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDay.plusDays(1), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(1), time5pm, UTC)),
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDay.plusDays(2), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(2), time5pm, UTC)),
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDay.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(3), time5pm, UTC)),
                        timeEntry(userIdComposite2, ZonedDateTime.of(firstDay.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(3), time5pm, UTC))
                    )
                ));


            final ReportSummary actual = sut.getMonthSummary(yearMonth, List.of(userLocalId1, userLocalId2));

            assertThat(actual.hoursWorked()).isEqualTo(Duration.ofHours(96));
            assertThat(actual.plannedWorkingHours()).isEqualTo(Duration.ofHours(80));
            assertThat(actual.hoursDelta()).isEqualTo(Duration.ofHours(16));
        }

        @Test
        void ensureGetMonthSummaryForUserLocalIdsWithNegativeDelta() {

            final YearMonth yearMonth = YearMonth.of(2024, OCTOBER);

            final UserLocalId userLocalId1 = new UserLocalId(1L);
            final UserLocalId userLocalId2 = new UserLocalId(2L);
            final UserIdComposite userIdComposite1 = new UserIdComposite(new UserId("user-id-1"), userLocalId1);
            final UserIdComposite userIdComposite2 = new UserIdComposite(new UserId("user-id-2"), userLocalId2);

            final LocalDate firstDay = LocalDate.of(2024, 10, 1);
            final LocalDate lastDay = LocalDate.of(2024, 10, 31);

            final WorkingTimeCalendar workingTimeCalendar = new WorkingTimeCalendar(Map.of(
                firstDay, PlannedWorkingHours.EIGHT,
                firstDay.plusDays(1), PlannedWorkingHours.EIGHT,
                firstDay.plusDays(2), PlannedWorkingHours.EIGHT,
                firstDay.plusDays(3), PlannedWorkingHours.EIGHT,
                firstDay.plusDays(4), PlannedWorkingHours.EIGHT,
                firstDay.plusDays(5), PlannedWorkingHours.ZERO,
                firstDay.plusDays(6), PlannedWorkingHours.ZERO
            ));

            when(workingTimeCalendarService.getWorkingTimeCalendarForUsers(firstDay, lastDay.plusDays(1), List.of(userLocalId1, userLocalId2)))
                .thenReturn(Map.of(
                    userIdComposite1, workingTimeCalendar,
                    userIdComposite2, workingTimeCalendar
                ));

            final LocalTime time9am = LocalTime.of(9, 0);
            final LocalTime time5pm = LocalTime.of(17, 0);
            when(timeEntryService.getEntriesByUserLocalIds(firstDay, lastDay.plusDays(1), List.of(userLocalId1, userLocalId2)))
                .thenReturn(Map.of(
                    userIdComposite1, List.of(
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay, time9am, UTC), ZonedDateTime.of(firstDay, time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(1), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(1), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(2), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(2), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(3), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(3), time5pm, UTC)),
                        timeEntry(userIdComposite1, ZonedDateTime.of(firstDay.plusDays(4), time9am, UTC), ZonedDateTime.of(firstDay.plusDays(4), time5pm, UTC))
                    ),
                    userIdComposite2, List.of()
                ));

            final ReportSummary actual = sut.getMonthSummary(yearMonth, List.of(userLocalId1, userLocalId2));

            assertThat(actual.hoursWorked()).isEqualTo(Duration.ofHours(40));
            assertThat(actual.plannedWorkingHours()).isEqualTo(Duration.ofHours(80));
            assertThat(actual.hoursDelta()).isEqualTo(Duration.ofHours(40).negated());
        }
    }

    private List<TimeEntry> buildTimeEntries(LocalDate from, LocalDate to, Function<LocalDate, List<TimeEntry>> timeEntryProvider) {
        final List<TimeEntry> timeEntries = new ArrayList<>();
        for (LocalDate date : new DateRange(from, to)) {
            timeEntries.addAll(timeEntryProvider.apply(date));
        }
        return timeEntries;
    }

    private Map<LocalDate, PlannedWorkingHours> buildPlannedWorkingHoursByDate(LocalDate from, LocalDate to, Function<LocalDate, PlannedWorkingHours> plannedWorkingHoursProvider) {
        final Map<LocalDate, PlannedWorkingHours> map = new HashMap<>();
        for (LocalDate date : new DateRange(from, to)) {
            map.put(date, plannedWorkingHoursProvider.apply(date));
        }
        return map;
    }

    private TimeEntry timeEntry(UserIdComposite userIdComposite, ZonedDateTime start, ZonedDateTime end) {
        final TimeEntryId id = new TimeEntryId(1L);
        return new TimeEntry(id, userIdComposite, "", start, end, false);
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
