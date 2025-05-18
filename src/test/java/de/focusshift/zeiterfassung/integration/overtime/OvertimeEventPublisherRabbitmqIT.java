package de.focusshift.zeiterfassung.integration.overtime;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.overtime.OvertimeHours;
import de.focusshift.zeiterfassung.overtime.events.UserHasMadeOvertimeEvent;
import de.focusshift.zeiterfassung.overtime.events.UserHasUpdatedOvertimeEvent;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
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
      "zeiterfassung.integration.overtime.enabled=true"
    }
)
class OvertimeEventPublisherRabbitmqIT extends SingleTenantTestContainersBase {

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
    void ensureOvertimeEventIsNotPublishedWhenThereIsNoTenantId() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.empty());

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final UserHasMadeOvertimeEvent userHasMadeOvertimeEvent = new UserHasMadeOvertimeEvent(userIdComposite, LocalDate.parse("2025-05-09"), OvertimeHours.EIGHT_POSITIVE);
        applicationEventPublisher.publishEvent(userHasMadeOvertimeEvent);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void ensureOvertimeEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final UserHasMadeOvertimeEvent userHasMadeOvertimeEvent = new UserHasMadeOvertimeEvent(userIdComposite, LocalDate.parse("2025-05-09"), OvertimeHours.EIGHT_POSITIVE);
        applicationEventPublisher.publishEvent(userHasMadeOvertimeEvent);

        final ArgumentCaptor<OvertimeEvent> captor = ArgumentCaptor.forClass(OvertimeEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("overtime.topic"), eq("entered"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.date()).isEqualTo(LocalDate.parse("2025-05-09"));
            assertThat(actual.duration()).isEqualTo(Duration.ofHours(8));
        });
    }

    @Test
    void ensureOvertimeUpdatedEventIsNotPublishedWhenThereIsNoTenantId() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.empty());

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final LocalDate date = LocalDate.now();

        final UserHasUpdatedOvertimeEvent event = new UserHasUpdatedOvertimeEvent(userIdComposite, date, OvertimeHours.ZERO);
        applicationEventPublisher.publishEvent(event);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void ensureOvertimeUpdatedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final LocalDate date = LocalDate.now();

        final UserHasUpdatedOvertimeEvent event = new UserHasUpdatedOvertimeEvent(userIdComposite, date, OvertimeHours.ZERO);
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<OvertimeUpdatedEvent> captor = ArgumentCaptor.forClass(OvertimeUpdatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("overtime.topic"), eq("updated"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.date()).isEqualTo(date);
            assertThat(actual.duration()).isEqualTo(Duration.ZERO);
        });
    }
}
