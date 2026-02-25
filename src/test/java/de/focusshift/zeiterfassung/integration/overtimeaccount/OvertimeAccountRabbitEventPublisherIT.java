package de.focusshift.zeiterfassung.integration.overtimeaccount;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountUpdatedEvent;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "zeiterfassung.integration.overtime-account.enabled=true"
    }
)
class OvertimeAccountRabbitEventPublisherIT extends SingleTenantTestContainersBase {

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
    void ensureOvertimeAccountUpdatedEventIsNotPublishedWhenThereIsNoTenantId() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.empty());

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final OvertimeAccountUpdatedEvent event = new OvertimeAccountUpdatedEvent(
            userIdComposite, true, Duration.ofHours(10)
        );
        applicationEventPublisher.publishEvent(event);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void ensureOvertimeAccountUpdatedEventIsPublishedOnRabbit() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final OvertimeAccountUpdatedEvent event = new OvertimeAccountUpdatedEvent(
            userIdComposite, true, Duration.ofHours(10)
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<OvertimeAccountUpdatedRabbitEvent> captor = ArgumentCaptor.forClass(OvertimeAccountUpdatedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.OVERTIMEACCOUNT.UPDATED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.isOvertimeAllowed()).isTrue();
            assertThat(actual.maxAllowedOvertime()).isEqualTo(Duration.ofHours(10));
        });
    }

    @Test
    void ensureOvertimeAccountUpdatedEventIsPublishedOnRabbitWithoutMaxAllowedOvertime() {

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant-id")));

        final UserId userId = new UserId("username");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final OvertimeAccountUpdatedEvent event = new OvertimeAccountUpdatedEvent(
            userIdComposite, false, null
        );
        applicationEventPublisher.publishEvent(event);

        final ArgumentCaptor<OvertimeAccountUpdatedRabbitEvent> captor = ArgumentCaptor.forClass(OvertimeAccountUpdatedRabbitEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("zeiterfassung.topic"), eq("ZE.EVENT.tenant-id.OVERTIMEACCOUNT.UPDATED"), captor.capture());

        assertThat(captor.getValue()).satisfies(actual -> {
            assertThat(actual.id()).isNotNull();
            assertThat(actual.tenantId()).isEqualTo("tenant-id");
            assertThat(actual.username()).isEqualTo("username");
            assertThat(actual.isOvertimeAllowed()).isFalse();
            assertThat(actual.maxAllowedOvertime()).isNull();
        });
    }
}
