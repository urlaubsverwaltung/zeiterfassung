package de.focusshift.zeiterfassung.integration.workingtime;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeCreatedEvent;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeDeletedEvent;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeId;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static de.focusshift.zeiterfassung.publicholiday.FederalState.GERMANY_BADEN_WUERTTEMBERG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "zeiterfassung.integration.workingtime.enabled=true"
    }
)
class WorkingTimeRabbitEventPublisherIT extends SingleTenantTestContainersBase {

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
    void ensureWorkingTimeCreatedEventIsNotPublishedWhenThereIsNoTenantId() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.empty());

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            DayOfWeek.MONDAY, Duration.ofHours(8)
        ));

        final WorkingTimeCreatedEvent event = new WorkingTimeCreatedEvent(
            userIdComposite, new WorkingTimeId(UUID.randomUUID()), LocalDate.parse("2025-01-01"),
            GERMANY_BADEN_WUERTTEMBERG, true, workdays
        );
        applicationEventPublisher.publishEvent(event);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void ensureWorkingTimeCreatedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            DayOfWeek.MONDAY, Duration.ofHours(8),
            DayOfWeek.TUESDAY, Duration.ofHours(8),
            DayOfWeek.WEDNESDAY, Duration.ofHours(8),
            DayOfWeek.THURSDAY, Duration.ofHours(8),
            DayOfWeek.FRIDAY, Duration.ofHours(8),
            DayOfWeek.SATURDAY, Duration.ZERO,
            DayOfWeek.SUNDAY, Duration.ZERO
        ));

        final WorkingTimeCreatedEvent event = new WorkingTimeCreatedEvent(
            userIdComposite, new WorkingTimeId(UUID.randomUUID()), LocalDate.parse("2025-01-01"),
            GERMANY_BADEN_WUERTTEMBERG, true, workdays
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<WorkingTimeCreatedRabbitEvent> captor = ArgumentCaptor.forClass(WorkingTimeCreatedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.WORKINGTIME.CREATED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.validFrom()).isEqualTo(LocalDate.parse("2025-01-01"));
            assertThat(actual.federalState()).isEqualTo("GERMANY_BADEN_WUERTTEMBERG");
            assertThat(actual.worksOnPublicHoliday()).isTrue();
            assertThat(actual.monday()).isEqualTo(Duration.ofHours(8));
            assertThat(actual.friday()).isEqualTo(Duration.ofHours(8));
            assertThat(actual.saturday()).isEqualTo(Duration.ZERO);
        });
    }

    @Test
    void ensureWorkingTimeUpdatedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final EnumMap<DayOfWeek, Duration> workdays = new EnumMap<>(Map.of(
            DayOfWeek.MONDAY, Duration.ofHours(4),
            DayOfWeek.TUESDAY, Duration.ofHours(4),
            DayOfWeek.WEDNESDAY, Duration.ofHours(4),
            DayOfWeek.THURSDAY, Duration.ofHours(4),
            DayOfWeek.FRIDAY, Duration.ofHours(4),
            DayOfWeek.SATURDAY, Duration.ZERO,
            DayOfWeek.SUNDAY, Duration.ZERO
        ));

        final WorkingTimeUpdatedEvent event = new WorkingTimeUpdatedEvent(
            userIdComposite, new WorkingTimeId(UUID.randomUUID()), LocalDate.parse("2025-06-01"),
            GERMANY_BADEN_WUERTTEMBERG, false, workdays
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<WorkingTimeUpdatedRabbitEvent> captor = ArgumentCaptor.forClass(WorkingTimeUpdatedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.WORKINGTIME.UPDATED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.validFrom()).isEqualTo(LocalDate.parse("2025-06-01"));
            assertThat(actual.federalState()).isEqualTo("GERMANY_BADEN_WUERTTEMBERG");
            assertThat(actual.worksOnPublicHoliday()).isFalse();
            assertThat(actual.monday()).isEqualTo(Duration.ofHours(4));
        });
    }

    @Test
    void ensureWorkingTimeDeletedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final WorkingTimeDeletedEvent event = new WorkingTimeDeletedEvent(
            userIdComposite, new WorkingTimeId(UUID.randomUUID()), LocalDate.parse("2025-01-01")
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<WorkingTimeDeletedRabbitEvent> captor = ArgumentCaptor.forClass(WorkingTimeDeletedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.WORKINGTIME.DELETED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.validFrom()).isEqualTo(LocalDate.parse("2025-01-01"));
        });
    }
}
