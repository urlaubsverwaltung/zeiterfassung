package de.focusshift.zeiterfassung.integration.overtime;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.overtime.OvertimeHours;
import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeEvent;
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
class OvertimeRabbitEventPublisherIT extends SingleTenantTestContainersBase {

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

        final UserHasWorkedOvertimeEvent userHasWorkedOvertimeEvent = new UserHasWorkedOvertimeEvent(userIdComposite, LocalDate.parse("2025-05-09"), OvertimeHours.EIGHT_POSITIVE);
        applicationEventPublisher.publishEvent(userHasWorkedOvertimeEvent);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void ensureOvertimeEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final UserHasWorkedOvertimeEvent userHasWorkedOvertimeEvent = new UserHasWorkedOvertimeEvent(userIdComposite, LocalDate.parse("2025-05-09"), OvertimeHours.EIGHT_POSITIVE);
        applicationEventPublisher.publishEvent(userHasWorkedOvertimeEvent);

        final ArgumentCaptor<OvertimeRabbitEvent> captor = ArgumentCaptor.forClass(OvertimeRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.OVERTIME.ENTERED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.date()).isEqualTo(LocalDate.parse("2025-05-09"));
            assertThat(actual.duration()).isEqualTo(Duration.ofHours(8));
        });
    }
}
