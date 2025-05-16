package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.timeentry.DayLockedEvent;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccount;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@RecordApplicationEvents
class OvertimePublisherIT extends SingleTenantTestContainersBase {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoBean
    private OvertimeService overtimeService;
    @MockitoBean
    private OvertimeAccountService overtimeAccountService;

    @Test
    void ensureUserHasMadeOvertimePublished() {

        final LocalDate date = LocalDate.now();

        final UserIdComposite userId1 = new UserIdComposite(new UserId("batman"), new UserLocalId(1L));
        final UserIdComposite userId2 = new UserIdComposite(new UserId("robin"), new UserLocalId(2L));

        when(overtimeService.getOvertimeForDate(date)).thenReturn(Map.of(
            userId1, OvertimeHours.EIGHT_POSITIVE,
            userId2, OvertimeHours.EIGHT_NEGATIVE
        ));

        when(overtimeAccountService.getAllOvertimeAccounts()).thenReturn(Map.of(
            userId1, new OvertimeAccount(userId1, true),
            userId2, new OvertimeAccount(userId2, true)
        ));

        final DayLockedEvent dayLockedEvent = new DayLockedEvent(date);
        applicationEventPublisher.publishEvent(dayLockedEvent);

        final List<UserHasMadeOvertimeEvent> actualEvents = applicationEvents.stream(UserHasMadeOvertimeEvent.class).toList();
        assertThat(actualEvents).contains(
            new UserHasMadeOvertimeEvent(userId1, date, OvertimeHours.EIGHT_POSITIVE),
            new UserHasMadeOvertimeEvent(userId2, date, OvertimeHours.EIGHT_NEGATIVE)
        );
    }

    @Test
    void ensureUserHasMadeOvertimeIsNotPublishedWhenOvertimeIsZero() {

        final LocalDate date = LocalDate.now();

        final UserIdComposite userId = new UserIdComposite(new UserId("batman"), new UserLocalId(1L));
        final UserIdComposite userId2= new UserIdComposite(new UserId("robin"), new UserLocalId(2L));

        when(overtimeService.getOvertimeForDate(date)).thenReturn(Map.of(
            userId, OvertimeHours.ZERO,
            userId2, new OvertimeHours(Duration.ofSeconds(1))
        ));

        when(overtimeAccountService.getAllOvertimeAccounts()).thenReturn(Map.of(
            userId, new OvertimeAccount(userId, true),
            userId2, new OvertimeAccount(userId, true)
        ));

        final DayLockedEvent dayLockedEvent = new DayLockedEvent(date);
        applicationEventPublisher.publishEvent(dayLockedEvent);

        final List<UserHasMadeOvertimeEvent> actualEvents = applicationEvents.stream(UserHasMadeOvertimeEvent.class).toList();
        assertThat(actualEvents).contains(
            new UserHasMadeOvertimeEvent(userId2, date, new OvertimeHours(Duration.ofSeconds(1)))
        );
    }

    @Test
    void ensureUserHasMadeOvertimeIsNotPublishedWhenIsNotAllowedToMakOvertime() {

        final LocalDate date = LocalDate.now();

        final UserIdComposite userId = new UserIdComposite(new UserId("batman"), new UserLocalId(1L));

        when(overtimeService.getOvertimeForDate(date)).thenReturn(Map.of(
            userId, OvertimeHours.EIGHT_POSITIVE
        ));

        when(overtimeAccountService.getAllOvertimeAccounts()).thenReturn(Map.of(
            userId, new OvertimeAccount(userId, false)
        ));

        final DayLockedEvent dayLockedEvent = new DayLockedEvent(date);
        applicationEventPublisher.publishEvent(dayLockedEvent);

        final List<UserHasMadeOvertimeEvent> actualEvents = applicationEvents.stream(UserHasMadeOvertimeEvent.class).toList();
        assertThat(actualEvents).isEmpty();
    }

    @Nested
    class EnsureUpdatedOvertime {

        @Test
        void ensureUserHasUpdatedOvertimeEventIsNotPublishedWhenNotLocked() {

            final TimeEntryUpdatedEvent timeEntryUpdatedEvent = new TimeEntryUpdatedEvent(new TimeEntryId(1L), false, null, null);
            applicationEventPublisher.publishEvent(timeEntryUpdatedEvent);

            final Stream<UserHasUpdatedOvertimeEvent> actualEvents = applicationEvents.stream(UserHasUpdatedOvertimeEvent.class);
            assertThat(actualEvents).isEmpty();
        }

        @Test
        void ensureUserHasUpdatedOvertimeEventIsNotPublishedWhenNotAllowed() {

            final UserId userId = new UserId("uuid");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            when(overtimeAccountService.getOvertimeAccount(userLocalId)).thenReturn(new OvertimeAccount(userIdComposite, false));

            final TimeEntryUpdatedEvent timeEntryUpdatedEvent = new TimeEntryUpdatedEvent(new TimeEntryId(1L), false, userIdComposite, null);
            applicationEventPublisher.publishEvent(timeEntryUpdatedEvent);

            final Stream<UserHasUpdatedOvertimeEvent> actualEvents = applicationEvents.stream(UserHasUpdatedOvertimeEvent.class);
            assertThat(actualEvents).isEmpty();
        }

        @Test
        void ensureUserHasUpdatedOvertimeEvent() {

            final UserId userId = new UserId("uuid");
            final UserLocalId userLocalId = new UserLocalId(1L);
            final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

            when(overtimeAccountService.getOvertimeAccount(userLocalId)).thenReturn(new OvertimeAccount(userIdComposite, true));

            final TimeEntryId timeEntryId = new TimeEntryId(1L);
            final ZonedDateTime date = ZonedDateTime.parse("2025-05-16T13:37:00Z");

            when(overtimeService.getOvertimeForDateAndUser(date.toLocalDate(), userLocalId)).thenReturn(OvertimeHours.ZERO);

            final TimeEntryUpdatedEvent timeEntryUpdatedEvent = new TimeEntryUpdatedEvent(timeEntryId, true, userIdComposite, date);
            applicationEventPublisher.publishEvent(timeEntryUpdatedEvent);

            final Stream<UserHasUpdatedOvertimeEvent> actualEvents = applicationEvents.stream(UserHasUpdatedOvertimeEvent.class);
            assertThat(actualEvents).containsExactly(
                new UserHasUpdatedOvertimeEvent(userIdComposite, date.toLocalDate(), OvertimeHours.ZERO)
            );
        }
    }
}
