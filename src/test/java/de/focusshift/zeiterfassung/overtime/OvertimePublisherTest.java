package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeEvent;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent.UpdatedValueCandidate;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccount;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimePublisherTest {

    private OvertimePublisher sut;

    @Mock
    private OvertimeService overtimeService;
    @Mock
    private OvertimeAccountService overtimeAccountService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        sut = new OvertimePublisher(overtimeService, overtimeAccountService, applicationEventPublisher);
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

    private static UserIdComposite anyUserIdComposite() {
        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(1L);
        return new UserIdComposite(userId, userLocalId);
    }
}
