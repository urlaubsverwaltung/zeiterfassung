package de.focusshift.zeiterfassung.integration.timeclock;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.timeclock.TimeClockStartedEvent;
import de.focusshift.zeiterfassung.timeclock.TimeClockStoppedEvent;
import de.focusshift.zeiterfassung.timeclock.TimeClockUpdatedEvent;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "zeiterfassung.integration.timeclock.enabled=true"
    }
)
class TimeClockRabbitEventPublisherIT extends SingleTenantTestContainersBase {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;
    // RabbitMessagingTemplate has to be mocked to ensure noInteractions on rabbitTemplate in test below
    @MockitoBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;
    @MockitoBean
    private TenantContextHolder tenantContextHolder;

    @Test
    void ensureTimeClockStartedEventIsNotPublishedWhenThereIsNoTenantId() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.empty());

        final TimeClockStartedEvent event = new TimeClockStartedEvent(
            new UserId("username"), ZonedDateTime.now(ZoneId.of("UTC")), "comment", false
        );
        applicationEventPublisher.publishEvent(event);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void ensureTimeClockStartedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final ZonedDateTime startedAt = ZonedDateTime.of(2025, 5, 9, 10, 0, 0, 0, ZoneId.of("UTC"));

        final TimeClockStartedEvent event = new TimeClockStartedEvent(
            new UserId("username"), startedAt, "my comment", true
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<TimeClockStartedRabbitEvent> captor = ArgumentCaptor.forClass(TimeClockStartedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.TIMECLOCK.STARTED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.startedAt()).isEqualTo(startedAt.toInstant());
            assertThat(actual.comment()).isEqualTo("my comment");
            assertThat(actual.isBreak()).isTrue();
        });
    }

    @Test
    void ensureTimeClockUpdatedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final ZonedDateTime startedAt = ZonedDateTime.of(2025, 5, 9, 10, 0, 0, 0, ZoneId.of("UTC"));

        final TimeClockUpdatedEvent event = new TimeClockUpdatedEvent(
            new UserId("username"), startedAt, "updated comment", false
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<TimeClockUpdatedRabbitEvent> captor = ArgumentCaptor.forClass(TimeClockUpdatedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.TIMECLOCK.UPDATED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.startedAt()).isEqualTo(startedAt.toInstant());
            assertThat(actual.comment()).isEqualTo("updated comment");
            assertThat(actual.isBreak()).isFalse();
        });
    }

    @Test
    void ensureTimeClockStoppedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final ZonedDateTime startedAt = ZonedDateTime.of(2025, 5, 9, 10, 0, 0, 0, ZoneId.of("UTC"));
        final ZonedDateTime stoppedAt = ZonedDateTime.of(2025, 5, 9, 18, 0, 0, 0, ZoneId.of("UTC"));

        final TimeClockStoppedEvent event = new TimeClockStoppedEvent(
            new UserId("username"), startedAt, stoppedAt, "done", true
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<TimeClockStoppedRabbitEvent> captor = ArgumentCaptor.forClass(TimeClockStoppedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.TIMECLOCK.STOPPED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.startedAt()).isEqualTo(startedAt.toInstant());
            assertThat(actual.stoppedAt()).isEqualTo(stoppedAt.toInstant());
            assertThat(actual.comment()).isEqualTo("done");
            assertThat(actual.isBreak()).isTrue();
        });
    }
}
