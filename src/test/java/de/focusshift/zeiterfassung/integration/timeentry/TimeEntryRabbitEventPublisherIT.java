package de.focusshift.zeiterfassung.integration.timeentry;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.overtime.OvertimePublisher;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryCreatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryDeletedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent;
import de.focusshift.zeiterfassung.timeentry.events.TimeEntryUpdatedEvent.UpdatedValueCandidate;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "zeiterfassung.integration.timeentry.enabled=true"
    }
)
class TimeEntryRabbitEventPublisherIT extends SingleTenantTestContainersBase {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;
    // RabbitMessagingTemplate has to be mocked to ensure noInteractions on rabbitTemplate in test below
    @MockitoBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    @MockitoBean
    private TenantContextHolder tenantContextHolder;
    // prevent the @EventListener methods in OvertimePublisher from reacting to our domain events
    @MockitoBean
    private OvertimePublisher overtimePublisher;

    @Test
    void ensureTimeEntryCreatedEventIsNotPublishedWhenThereIsNoTenantId() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.empty());

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final TimeEntryCreatedEvent event = new TimeEntryCreatedEvent(
            new TimeEntryId(1L), userIdComposite, false, LocalDate.parse("2025-05-09"), new WorkDuration(Duration.ofHours(8))
        );
        applicationEventPublisher.publishEvent(event);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void ensureTimeEntryCreatedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final TimeEntryCreatedEvent event = new TimeEntryCreatedEvent(
            new TimeEntryId(1L), userIdComposite, false, LocalDate.parse("2025-05-09"), new WorkDuration(Duration.ofHours(8))
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<TimeEntryCreatedRabbitEvent> captor = ArgumentCaptor.forClass(TimeEntryCreatedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.TIMEENTRY.CREATED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.date()).isEqualTo(LocalDate.parse("2025-05-09"));
            assertThat(actual.locked()).isFalse();
            assertThat(actual.workDuration()).isEqualTo(Duration.ofHours(8));
        });
    }

    @Test
    void ensureTimeEntryUpdatedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final TimeEntryUpdatedEvent event = new TimeEntryUpdatedEvent(
            new TimeEntryId(1L),
            userIdComposite,
            new UpdatedValueCandidate<>(false, true),
            new UpdatedValueCandidate<>(LocalDate.parse("2025-05-09"), LocalDate.parse("2025-05-10")),
            new UpdatedValueCandidate<>(new WorkDuration(Duration.ofHours(8)), new WorkDuration(Duration.ofHours(6)))
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<TimeEntryUpdatedRabbitEvent> captor = ArgumentCaptor.forClass(TimeEntryUpdatedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.TIMEENTRY.UPDATED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.date()).isEqualTo(LocalDate.parse("2025-05-10"));
            assertThat(actual.locked()).isTrue();
            assertThat(actual.workDuration()).isEqualTo(Duration.ofHours(6));
        });
    }

    @Test
    void ensureTimeEntryDeletedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final TimeEntryDeletedEvent event = new TimeEntryDeletedEvent(
            new TimeEntryId(1L), userIdComposite, true, LocalDate.parse("2025-05-09"), new WorkDuration(Duration.ofHours(8))
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<TimeEntryDeletedRabbitEvent> captor = ArgumentCaptor.forClass(TimeEntryDeletedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.TIMEENTRY.DELETED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.date()).isEqualTo(LocalDate.parse("2025-05-09"));
            assertThat(actual.locked()).isTrue();
            assertThat(actual.workDuration()).isEqualTo(Duration.ofHours(8));
        });
    }
}
