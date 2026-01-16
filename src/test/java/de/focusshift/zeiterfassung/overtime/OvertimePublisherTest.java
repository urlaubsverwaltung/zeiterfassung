package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.absence.AbsenceAddedEvent;
import de.focusshift.zeiterfassung.absence.AbsenceDeletedEvent;
import de.focusshift.zeiterfassung.absence.AbsenceUpdatedEvent;
import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeEvent;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.TimeEntryLockService;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent.UpdatedValueCandidate;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccount;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendar;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimePublisherTest {

    private OvertimePublisher sut;

    @Mock
    private OvertimeService overtimeService;
    @Mock
    private OvertimeAccountService overtimeAccountService;
    @Mock
    private TimeEntryLockService timeEntryLockService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private WorkingTimeCalendarService workingTimeCalendarService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        sut = new OvertimePublisher(
            overtimeService,
            overtimeAccountService,
            timeEntryLockService,
            userManagementService,
            workingTimeCalendarService,
            applicationEventPublisher);
    }

    @Nested
    class TimeEntryCreated {

        @Test
        void ensureOvertimeUpdateNotPublishedWhenNotLocked() {

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UserIdComposite userIdComposite = anyUserIdComposite();

            final TimeEntryCreatedEvent event = new TimeEntryCreatedEvent(
                timeEntryId,
                userIdComposite,
                false,
                LocalDate.now(),
                WorkDuration.EIGHT
            );
            sut.publishOvertimeUpdated(event);

            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        void ensureOvertimeUpdatedNotPublishedWhenNotAllowed() {

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UserIdComposite userIdComposite = anyUserIdComposite();

            when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                .thenReturn(new OvertimeAccount(userIdComposite, false));

            final TimeEntryCreatedEvent event = new TimeEntryCreatedEvent(
                timeEntryId,
                userIdComposite,
                true,
                LocalDate.now(),
                WorkDuration.EIGHT
            );
            sut.publishOvertimeUpdated(event);

            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        void ensureOvertimeUpdatedNotPublishedWhenWorkDurationIsZero() {

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UserIdComposite userIdComposite = anyUserIdComposite();

            final TimeEntryCreatedEvent event = new TimeEntryCreatedEvent(
                timeEntryId,
                userIdComposite,
                true,
                LocalDate.now(),
                WorkDuration.ZERO
            );
            sut.publishOvertimeUpdated(event);

            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        void ensureOvertimeUpdated() {

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UserIdComposite userIdComposite = anyUserIdComposite();
            final LocalDate date = LocalDate.now();

            when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                .thenReturn(new OvertimeAccount(userIdComposite, true));

            when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId()))
                .thenReturn(OvertimeHours.EIGHT_POSITIVE);

            final TimeEntryCreatedEvent event = new TimeEntryCreatedEvent(
                timeEntryId,
                userIdComposite,
                true,
                date,
                WorkDuration.EIGHT
            );
            sut.publishOvertimeUpdated(event);

            final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());

            final List<Object> publishedEvents = captor.getAllValues();
            assertThat(publishedEvents).containsExactly(
                new UserHasWorkedOvertimeEvent(userIdComposite, date, OvertimeHours.EIGHT_POSITIVE)
            );
        }
    }

    @Nested
    class TimeEntryUpdated {

        @ParameterizedTest
        @CsvSource({
            // date changed must not have an impact because not-locked is relevant
            "2025-05-18, 2025-05-18",
            "2025-05-18, 2025-05-17",
            "2025-05-18, 2025-05-19",
        })
        void ensureOvertimeUpdatedNotPublishedWhenNotLocked(String prevDateString, String curDateString) {

            final LocalDate prevDate = LocalDate.parse(prevDateString);
            final LocalDate curDate = LocalDate.parse(curDateString);

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UpdatedValueCandidate<Boolean> lockedCandidate = new UpdatedValueCandidate<>(false, false);
            final UpdatedValueCandidate<LocalDate> dateCandidate = new UpdatedValueCandidate<>(prevDate, curDate);
            final UpdatedValueCandidate<WorkDuration> workDurationCandidate = new UpdatedValueCandidate<>(WorkDuration.EIGHT, WorkDuration.EIGHT);

            final UserIdComposite userIdComposite = anyUserIdComposite();

            final TimeEntryUpdatedEvent event = new TimeEntryUpdatedEvent(timeEntryId, userIdComposite, lockedCandidate, dateCandidate, workDurationCandidate);
            sut.publishOvertimeUpdated(event);

            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        void ensureOvertimeUpdateNotPublishedWhenLockedButDateAndWorkDurationDidNotChange() {
            // e.g. timeEntry comment has been updated by a privileged person

            final LocalDate date = LocalDate.now();

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UpdatedValueCandidate<Boolean> lockedCandidate = new UpdatedValueCandidate<>(true, true);
            final UpdatedValueCandidate<LocalDate> dateCandidate = new UpdatedValueCandidate<>(date, date);
            final UpdatedValueCandidate<WorkDuration> workDurationCandidate = new UpdatedValueCandidate<>(WorkDuration.EIGHT, WorkDuration.EIGHT);

            final UserIdComposite userIdComposite = anyUserIdComposite();

            when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId())).thenReturn(new OvertimeAccount(userIdComposite, true));

            final TimeEntryUpdatedEvent event = new TimeEntryUpdatedEvent(timeEntryId, userIdComposite, lockedCandidate, dateCandidate, workDurationCandidate);
            sut.publishOvertimeUpdated(event);

            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        void ensureOvertimeUpdateWhenLockedIsToggledFromTrueToFalse() {

            final LocalDate prevDate = LocalDate.parse("2025-05-18");
            final LocalDate curDate = prevDate.plusDays(1);

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UpdatedValueCandidate<Boolean> lockedCandidate = new UpdatedValueCandidate<>(true, false);
            final UpdatedValueCandidate<LocalDate> dateCandidate = new UpdatedValueCandidate<>(prevDate, curDate);
            final UpdatedValueCandidate<WorkDuration> workDurationCandidate = new UpdatedValueCandidate<>(WorkDuration.EIGHT, WorkDuration.EIGHT);

            final UserIdComposite userIdComposite = anyUserIdComposite();

            when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId())).thenReturn(new OvertimeAccount(userIdComposite, true));
            when(overtimeService.getOvertimeForDateAndUser(prevDate, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);

            final TimeEntryUpdatedEvent event = new TimeEntryUpdatedEvent(timeEntryId, userIdComposite, lockedCandidate, dateCandidate, workDurationCandidate);
            sut.publishOvertimeUpdated(event);

            final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());

            final List<Object> publishedEvents = captor.getAllValues();
            assertThat(publishedEvents).containsExactly(
                new UserHasWorkedOvertimeEvent(userIdComposite, prevDate, OvertimeHours.EIGHT_POSITIVE)
            );
        }

        @Test
        void ensureOvertimeUpdateWhenLockedAndDateChanged() {

            final LocalDate prevDate = LocalDate.now();
            final LocalDate curDate = prevDate.plusDays(1);

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UpdatedValueCandidate<Boolean> lockedCandidate = new UpdatedValueCandidate<>(true, true);
            final UpdatedValueCandidate<LocalDate> dateCandidate = new UpdatedValueCandidate<>(prevDate, curDate);
            final UpdatedValueCandidate<WorkDuration> workDurationCandidate = new UpdatedValueCandidate<>(WorkDuration.EIGHT, WorkDuration.EIGHT);

            final UserIdComposite userIdComposite = anyUserIdComposite();

            when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId())).thenReturn(new OvertimeAccount(userIdComposite, true));
            when(overtimeService.getOvertimeForDateAndUser(prevDate, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
            when(overtimeService.getOvertimeForDateAndUser(curDate, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_NEGATIVE);

            final TimeEntryUpdatedEvent event = new TimeEntryUpdatedEvent(timeEntryId, userIdComposite, lockedCandidate, dateCandidate, workDurationCandidate);
            sut.publishOvertimeUpdated(event);

            final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());

            final List<Object> publishedEvents = captor.getAllValues();
            assertThat(publishedEvents).containsExactly(
                new UserHasWorkedOvertimeEvent(userIdComposite, prevDate, OvertimeHours.EIGHT_POSITIVE),
                new UserHasWorkedOvertimeEvent(userIdComposite, curDate, OvertimeHours.EIGHT_NEGATIVE)
            );
        }

        @Test
        void ensureOvertimeUpdateWhenLockedToggledToTrue() {
            // and therefore the date since locking timeEntries is time related currently.

            final LocalDate prevDate = LocalDate.parse("2025-05-18");
            final LocalDate curDate = prevDate.minusDays(1);

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UpdatedValueCandidate<Boolean> lockedCandidate = new UpdatedValueCandidate<>(false,true);
            final UpdatedValueCandidate<LocalDate> dateCandidate = new UpdatedValueCandidate<>(prevDate, curDate);
            final UpdatedValueCandidate<WorkDuration> workDurationCandidate = new UpdatedValueCandidate<>(WorkDuration.EIGHT, WorkDuration.EIGHT);

            final UserIdComposite userIdComposite = anyUserIdComposite();

            when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId())).thenReturn(new OvertimeAccount(userIdComposite, true));
            when(overtimeService.getOvertimeForDateAndUser(curDate, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);

            final TimeEntryUpdatedEvent event = new TimeEntryUpdatedEvent(timeEntryId, userIdComposite, lockedCandidate, dateCandidate, workDurationCandidate);
            sut.publishOvertimeUpdated(event);

            final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());

            final List<Object> publishedEvents = captor.getAllValues();
            assertThat(publishedEvents).containsExactly(
                new UserHasWorkedOvertimeEvent(userIdComposite, curDate, OvertimeHours.EIGHT_POSITIVE)
            );
        }

        @Test
        void ensureOvertimeUpdateWhenLockedAndNotDateButWorkDurationChanged() {

            final LocalDate date = LocalDate.parse("2025-05-18");

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final UpdatedValueCandidate<Boolean> lockedCandidate = new UpdatedValueCandidate<>(true,true);
            final UpdatedValueCandidate<LocalDate> dateCandidate = new UpdatedValueCandidate<>(date, date);
            final UpdatedValueCandidate<WorkDuration> workDurationCandidate = new UpdatedValueCandidate<>(WorkDuration.EIGHT, WorkDuration.ZERO);

            final UserIdComposite userIdComposite = anyUserIdComposite();

            when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId())).thenReturn(new OvertimeAccount(userIdComposite, true));
            when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.ZERO);

            final TimeEntryUpdatedEvent event = new TimeEntryUpdatedEvent(timeEntryId, userIdComposite, lockedCandidate, dateCandidate, workDurationCandidate);
            sut.publishOvertimeUpdated(event);

            final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());

            final List<Object> publishedEvents = captor.getAllValues();
            assertThat(publishedEvents).containsExactly(
                new UserHasWorkedOvertimeEvent(userIdComposite, date, OvertimeHours.ZERO)
            );
        }
    }

    @Nested
    class AbsenceEvents {

        @Nested
        class AbsenceAdded {

            @Test
            void ensureOvertimeUpdatedNotPublishedForUnknownUser() {
                final UserId userId = new UserId("unknown-user-id");
                final DateRange dateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-03"));

                when(userManagementService.findUserById(userId)).thenReturn(Optional.empty());

                final AbsenceAddedEvent event = new AbsenceAddedEvent(userId, dateRange);
                sut.publishOvertimeUpdated(event);

                verifyNoInteractions(applicationEventPublisher);
            }

            @Test
            void ensureOvertimeUpdatedNotPublishedWhenOvertimeNotAllowed() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange dateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-03"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, false));

                final AbsenceAddedEvent event = new AbsenceAddedEvent(userId, dateRange);
                sut.publishOvertimeUpdated(event);

                verifyNoInteractions(applicationEventPublisher);
            }

            @Test
            void ensureOvertimeUpdatedNotPublishedForUnlockedDate() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange dateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-03"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
                when(timeEntryLockService.isLocked(LocalDate.parse("2025-01-01"), lockTimeEntriesSettings)).thenReturn(false);

                final AbsenceAddedEvent event = new AbsenceAddedEvent(userId, dateRange);
                sut.publishOvertimeUpdated(event);

                verifyNoInteractions(applicationEventPublisher);

                // first date is not locked, therefore the following days are not locked, too
                verifyNoMoreInteractions(timeEntryLockService);
            }

            @Test
            void ensureOvertimeUpdatedNotPublishedForZeroPlannedWorkingHours() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final LocalDate date = LocalDate.parse("2025-01-01");

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
                when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);

                when(workingTimeCalendarService.getWorkingTimeCalender(date, date.plusDays(1), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(
                        Map.of(date, PlannedWorkingHours.ZERO),
                        Map.of()
                    ));

                final AbsenceAddedEvent event = new AbsenceAddedEvent(userId, new DateRange(date, date));
                sut.publishOvertimeUpdated(event);

                verifyNoInteractions(applicationEventPublisher);
            }

            @Test
            void ensureOvertimeUpdatedSkipsDatesWithZeroPlannedHours() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final LocalDate date1 = LocalDate.parse("2025-01-01");
                final LocalDate date2 = LocalDate.parse("2025-01-02");
                final LocalDate date3 = LocalDate.parse("2025-01-03");

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
                when(timeEntryLockService.isLocked(date1, lockTimeEntriesSettings)).thenReturn(true);
                when(timeEntryLockService.isLocked(date2, lockTimeEntriesSettings)).thenReturn(true);
                when(timeEntryLockService.isLocked(date3, lockTimeEntriesSettings)).thenReturn(true);

                when(workingTimeCalendarService.getWorkingTimeCalender(date1, date3.plusDays(1), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(
                        Map.of(
                            date1, PlannedWorkingHours.EIGHT,
                            date2, PlannedWorkingHours.ZERO,
                            date3, PlannedWorkingHours.EIGHT
                        ),
                        Map.of()
                    ));

                when(overtimeService.getOvertimeForDateAndUser(date1, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                when(overtimeService.getOvertimeForDateAndUser(date3, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);

                sut.publishOvertimeUpdated(new AbsenceAddedEvent(userId, new DateRange(date1, date3)));

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());

                final List<Object> publishedEvents = captor.getAllValues();
                assertThat(publishedEvents).containsExactly(
                    new UserHasWorkedOvertimeEvent(userIdComposite, date1, OvertimeHours.EIGHT_POSITIVE),
                    new UserHasWorkedOvertimeEvent(userIdComposite, date3, OvertimeHours.EIGHT_POSITIVE)
                );
            }

            @Test
            void ensureOvertimeUpdatedPublishedForSingleLockedDate() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final LocalDate date = LocalDate.parse("2025-01-01");

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
                when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);

                when(workingTimeCalendarService.getWorkingTimeCalender(date, date.plusDays(1), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(
                        Map.of(date, PlannedWorkingHours.EIGHT),
                        Map.of()
                    ));

                when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId()))
                    .thenReturn(OvertimeHours.EIGHT_POSITIVE);

                sut.publishOvertimeUpdated(new AbsenceAddedEvent(userId, new DateRange(date, date)));

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher).publishEvent(captor.capture());

                final List<Object> publishedEvents = captor.getAllValues();
                assertThat(publishedEvents).containsExactly(
                    new UserHasWorkedOvertimeEvent(userIdComposite, date, OvertimeHours.EIGHT_POSITIVE)
                );
            }

            @Test
            void ensureOvertimeUpdatedPublishedForMultipleLockedDates() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final LocalDate date1 = LocalDate.parse("2025-01-01");
                final LocalDate date2 = LocalDate.parse("2025-01-02");
                final LocalDate date3 = LocalDate.parse("2025-01-03");
                final DateRange dateRange = new DateRange(date1, date3);

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
                when(timeEntryLockService.isLocked(date1, lockTimeEntriesSettings)).thenReturn(true);
                when(timeEntryLockService.isLocked(date2, lockTimeEntriesSettings)).thenReturn(true);
                when(timeEntryLockService.isLocked(date3, lockTimeEntriesSettings)).thenReturn(true);

                when(workingTimeCalendarService.getWorkingTimeCalender(date1, date3.plusDays(1), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(
                        Map.of(
                            date1, PlannedWorkingHours.EIGHT,
                            date2, PlannedWorkingHours.EIGHT,
                            date3, PlannedWorkingHours.EIGHT
                        ),
                        Map.of()
                    ));

                when(overtimeService.getOvertimeForDateAndUser(date1, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                when(overtimeService.getOvertimeForDateAndUser(date2, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                when(overtimeService.getOvertimeForDateAndUser(date3, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);

                final AbsenceAddedEvent event = new AbsenceAddedEvent(userId, dateRange);
                sut.publishOvertimeUpdated(event);

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(3)).publishEvent(captor.capture());

                final List<Object> publishedEvents = captor.getAllValues();
                assertThat(publishedEvents).containsExactly(
                    new UserHasWorkedOvertimeEvent(userIdComposite, date1, OvertimeHours.EIGHT_POSITIVE),
                    new UserHasWorkedOvertimeEvent(userIdComposite, date2, OvertimeHours.EIGHT_POSITIVE),
                    new UserHasWorkedOvertimeEvent(userIdComposite, date3, OvertimeHours.EIGHT_POSITIVE)
                );
            }

            @Test
            void ensureOvertimeUpdatedBreaksOnFirstUnlockedDate() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final LocalDate date1 = LocalDate.parse("2025-01-01");
                final LocalDate date2 = LocalDate.parse("2025-01-02");
                final LocalDate date3 = LocalDate.parse("2025-01-03");

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
                when(timeEntryLockService.isLocked(date1, lockTimeEntriesSettings)).thenReturn(true);
                when(timeEntryLockService.isLocked(date2, lockTimeEntriesSettings)).thenReturn(false);

                when(workingTimeCalendarService.getWorkingTimeCalender(date1, date3.plusDays(1), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(
                        Map.of(
                            date1, PlannedWorkingHours.EIGHT,
                            date2, PlannedWorkingHours.EIGHT,
                            date3, PlannedWorkingHours.EIGHT
                        ),
                        Map.of()
                    ));

                when(overtimeService.getOvertimeForDateAndUser(date1, userIdComposite.localId()))
                    .thenReturn(OvertimeHours.EIGHT_POSITIVE);

                sut.publishOvertimeUpdated(new AbsenceAddedEvent(userId, new DateRange(date1, date3)));

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher).publishEvent(captor.capture());

                final List<Object> publishedEvents = captor.getAllValues();
                assertThat(publishedEvents).containsExactly(
                    new UserHasWorkedOvertimeEvent(userIdComposite, date1, OvertimeHours.EIGHT_POSITIVE)
                );

                // Verify that isLocked was only called for date1 and date2, but not date3 due to break
                verifyNoMoreInteractions(timeEntryLockService);
            }
        }

        @Nested
        class AbsenceDeleted {

            @Test
            void ensureOvertimeUpdatedNotPublishedForUnknownUser() {
                final UserId userId = new UserId("unknown-user-id");
                final DateRange dateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-03"));

                when(userManagementService.findUserById(userId)).thenReturn(Optional.empty());

                sut.publishOvertimeUpdated(new AbsenceDeletedEvent(userId, dateRange));

                verifyNoInteractions(applicationEventPublisher);
            }

            @Test
            void ensureOvertimeUpdatedNotPublishedWhenOvertimeNotAllowed() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange dateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-03"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, false));

                sut.publishOvertimeUpdated(new AbsenceDeletedEvent(userId, dateRange));

                verifyNoInteractions(applicationEventPublisher);
            }

            @Test
            void ensureOvertimeUpdatedPublishedForLockedDate() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final LocalDate date = LocalDate.parse("2025-01-01");

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
                when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);

                when(workingTimeCalendarService.getWorkingTimeCalender(date, date.plusDays(1), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(
                        Map.of(date, PlannedWorkingHours.EIGHT),
                        Map.of()
                    ));

                when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);

                sut.publishOvertimeUpdated(new AbsenceDeletedEvent(userId, new DateRange(date, date)));

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher).publishEvent(captor.capture());

                final List<Object> publishedEvents = captor.getAllValues();
                assertThat(publishedEvents).containsExactly(
                    new UserHasWorkedOvertimeEvent(userIdComposite, date, OvertimeHours.EIGHT_POSITIVE)
                );
            }

            @Test
            void ensureOvertimeUpdatedNotPublishedForNotLockedDate() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final LocalDate date = LocalDate.parse("2025-01-01");

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);
                when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(false);

                when(workingTimeCalendarService.getWorkingTimeCalender(date, date.plusDays(1), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(
                        Map.of(date, PlannedWorkingHours.EIGHT),
                        Map.of()
                    ));

                sut.publishOvertimeUpdated(new AbsenceDeletedEvent(userId, new DateRange(date, date)));

                verifyNoInteractions(applicationEventPublisher);
            }
        }

        @Nested
        class AbsenceUpdated {

            @Test
            void ensureOvertimeUpdatedForOverlappingRanges_OldRangeStartsFirst() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange oldDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05"));
                final DateRange newDateRange = new DateRange(LocalDate.parse("2025-01-03"), LocalDate.parse("2025-01-07"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = IntStream.rangeClosed(1, 7)
                    .mapToObj(nr -> LocalDate.parse("2025-01-%02d".formatted(nr)))
                    .collect(toMap(identity(), unused -> PlannedWorkingHours.EIGHT));

                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-06"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));
                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-03"), LocalDate.parse("2025-01-08"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

                // Mock all dates in union (2025-01-01 to 2025-01-07) as locked with planned hours
                for (int i = 1; i <= 7; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-0" + i);
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                final AbsenceUpdatedEvent event = new AbsenceUpdatedEvent(userId, oldDateRange, newDateRange);
                sut.publishOvertimeUpdated(event);

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(10)).publishEvent(captor.capture());
            }

            @Test
            void ensureOvertimeUpdatedForOverlappingRanges_NewRangeStartsFirst() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange oldDateRange = new DateRange(LocalDate.parse("2025-01-05"), LocalDate.parse("2025-01-10"));
                final DateRange newDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-07"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = IntStream.rangeClosed(1, 10)
                    .mapToObj(nr -> LocalDate.parse("2025-01-%02d".formatted(nr)))
                    .collect(toMap(identity(), unused -> PlannedWorkingHours.EIGHT));

                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-05"), LocalDate.parse("2025-01-11"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));
                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-08"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

                // Mock all dates in union (2025-01-01 to 2025-01-10) as locked with planned hours
                for (int i = 1; i <= 10; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-" + String.format("%02d", i));
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                final AbsenceUpdatedEvent event = new AbsenceUpdatedEvent(userId, oldDateRange, newDateRange);
                sut.publishOvertimeUpdated(event);

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(13)).publishEvent(captor.capture());
            }

            @Test
            void ensureOvertimeUpdatedForOverlappingRanges_IdenticalRanges() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange oldDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05"));
                final DateRange newDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = IntStream.rangeClosed(1, 5)
                    .mapToObj(nr -> LocalDate.parse("2025-01-%02d".formatted(nr)))
                    .collect(toMap(identity(), unused -> PlannedWorkingHours.EIGHT));

                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-06"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

                // Mock all dates in union (2025-01-01 to 2025-01-05) as locked with planned hours
                for (int i = 1; i <= 5; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-0" + i);
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                final AbsenceUpdatedEvent event = new AbsenceUpdatedEvent(userId, oldDateRange, newDateRange);
                sut.publishOvertimeUpdated(event);

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(10)).publishEvent(captor.capture());
            }

            @Test
            void ensureOvertimeUpdatedForOverlappingRanges_OneRangeContainsOther() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange oldDateRange = new DateRange(LocalDate.parse("2025-01-03"), LocalDate.parse("2025-01-05"));
                final DateRange newDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-10"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = IntStream.rangeClosed(1, 10)
                    .mapToObj(nr -> LocalDate.parse("2025-01-%02d".formatted(nr)))
                    .collect(toMap(identity(), unused -> PlannedWorkingHours.EIGHT));

                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-03"), LocalDate.parse("2025-01-06"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));
                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-11"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

                // Mock all dates in union (2025-01-01 to 2025-01-10) as locked with planned hours
                for (int i = 1; i <= 10; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-" + String.format("%02d", i));
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                sut.publishOvertimeUpdated(new AbsenceUpdatedEvent(userId, oldDateRange, newDateRange));

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(13)).publishEvent(captor.capture());
            }

            @Test
            void ensureOvertimeUpdatedForNonOverlappingRanges() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange oldDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05"));
                final DateRange newDateRange = new DateRange(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = IntStream.rangeClosed(1, 15)
                    .mapToObj(nr -> LocalDate.parse("2025-01-%02d".formatted(nr)))
                    .collect(toMap(identity(), unused -> PlannedWorkingHours.EIGHT));

                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-06"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));
                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-16"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

                // Mock dates in old range (2025-01-01 to 2025-01-05)
                for (int i = 1; i <= 5; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-0" + i);
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                // Mock dates in new range (2025-01-10 to 2025-01-15)
                for (int i = 10; i <= 15; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-" + i);
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                final AbsenceUpdatedEvent event = new AbsenceUpdatedEvent(userId, oldDateRange, newDateRange);
                sut.publishOvertimeUpdated(event);

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(11)).publishEvent(captor.capture());
            }

            @Test
            void ensureOvertimeUpdatedForNonOverlappingRangesTODO_NAME_ME() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange oldDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05"));
                final DateRange newDateRange = new DateRange(LocalDate.parse("2025-01-06"), LocalDate.parse("2025-01-10"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = IntStream.rangeClosed(1, 15)
                    .mapToObj(nr -> LocalDate.parse("2025-01-%02d".formatted(nr)))
                    .collect(toMap(identity(), unused -> PlannedWorkingHours.EIGHT));

                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-06"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));
                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-06"), LocalDate.parse("2025-01-11"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

                // Mock dates in old range (2025-01-01 to 2025-01-05)
                for (int i = 1; i <= 5; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-0" + i);
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                when(timeEntryLockService.isLocked(LocalDate.parse("2025-01-06"), lockTimeEntriesSettings)).thenReturn(true);
                when(overtimeService.getOvertimeForDateAndUser(LocalDate.parse("2025-01-06"), userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);

                when(timeEntryLockService.isLocked(LocalDate.parse("2025-01-07"), lockTimeEntriesSettings)).thenReturn(false);

                final AbsenceUpdatedEvent event = new AbsenceUpdatedEvent(userId, oldDateRange, newDateRange);
                sut.publishOvertimeUpdated(event);

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(6)).publishEvent(captor.capture());
            }

            @Test
            void ensureOvertimeUpdatedForNonOverlappingRanges_ReverseOrder() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange oldDateRange = new DateRange(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15"));
                final DateRange newDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, true));

                final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = IntStream.rangeClosed(1, 15)
                    .mapToObj(nr -> LocalDate.parse("2025-01-%02d".formatted(nr)))
                    .collect(toMap(identity(), unused -> PlannedWorkingHours.EIGHT));

                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-16"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));
                when(workingTimeCalendarService.getWorkingTimeCalender(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-06"), userIdComposite.localId()))
                    .thenReturn(new WorkingTimeCalendar(plannedWorkingHoursByDate, Map.of()));

                final LockTimeEntriesSettings lockTimeEntriesSettings = anyLockTimeEntriesSettings();
                when(timeEntryLockService.getLockTimeEntriesSettings()).thenReturn(lockTimeEntriesSettings);

                // Mock dates in old range (2025-01-10 to 2025-01-15)
                for (int i = 10; i <= 15; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-" + i);
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId()))
                        .thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                // Mock dates in new range (2025-01-01 to 2025-01-05)
                for (int i = 1; i <= 5; i++) {
                    final LocalDate date = LocalDate.parse("2025-01-0" + i);
                    when(timeEntryLockService.isLocked(date, lockTimeEntriesSettings)).thenReturn(true);
                    when(overtimeService.getOvertimeForDateAndUser(date, userIdComposite.localId())).thenReturn(OvertimeHours.EIGHT_POSITIVE);
                }

                final AbsenceUpdatedEvent event = new AbsenceUpdatedEvent(userId, oldDateRange, newDateRange);
                sut.publishOvertimeUpdated(event);

                final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(applicationEventPublisher, times(11)).publishEvent(captor.capture());
            }

            @Test
            void ensureOvertimeUpdatedNotPublishedWhenOvertimeNotAllowed() {
                final UserId userId = new UserId("user-id");
                final UserIdComposite userIdComposite = anyUserIdComposite();
                final DateRange oldDateRange = new DateRange(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05"));
                final DateRange newDateRange = new DateRange(LocalDate.parse("2025-01-03"), LocalDate.parse("2025-01-07"));

                final User user = new User(userIdComposite, "John", "Doe", new EMailAddress("john@example.com"), Set.of());
                when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));
                when(overtimeAccountService.getOvertimeAccount(userIdComposite.localId()))
                    .thenReturn(new OvertimeAccount(userIdComposite, false));

                final AbsenceUpdatedEvent event = new AbsenceUpdatedEvent(userId, oldDateRange, newDateRange);
                sut.publishOvertimeUpdated(event);

                verifyNoInteractions(applicationEventPublisher);
            }
        }
    }

    private LockTimeEntriesSettings anyLockTimeEntriesSettings() {
        return new LockTimeEntriesSettings(true, 42);
    }

    private static UserIdComposite anyUserIdComposite() {
        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(1L);
        return new UserIdComposite(userId, userLocalId);
    }
}
