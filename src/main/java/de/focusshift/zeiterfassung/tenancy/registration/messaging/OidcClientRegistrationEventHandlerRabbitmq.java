package de.focusshift.zeiterfassung.tenancy.registration.messaging;

import de.focusshift.zeiterfassung.tenancy.registration.TenantRegistration;
import de.focusshift.zeiterfassung.tenancy.registration.TenantRegistrationService;
import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import static de.focusshift.zeiterfassung.tenancy.registration.messaging.OidcClientRegistrationRabbitmqConfiguration.ZEITERFASSUNG_QUEUE_OIDC_CLIENT_CREATED_CONSUMER;
import static de.focusshift.zeiterfassung.tenancy.registration.messaging.OidcClientRegistrationRabbitmqConfiguration.ZEITERFASSUNG_QUEUE_OIDC_CLIENT_DELETED_CONSUMER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

class OidcClientRegistrationEventHandlerRabbitmq {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantRegistrationService tenantRegistrationService;

    OidcClientRegistrationEventHandlerRabbitmq(TenantRegistrationService tenantRegistrationService) {
        this.tenantRegistrationService = tenantRegistrationService;
    }

    @RabbitListener(queues = {ZEITERFASSUNG_QUEUE_OIDC_CLIENT_CREATED_CONSUMER})
    public void handleEvent(OidcClientCreatedEventDTO event) {
        LOG.info("Received oidcClientCreatedEvent for oidcClient={} and tenantId={}", event.clientId(), event.tenantId());

        tenantRegistrationService.registerNewTenant(new TenantRegistration(event.tenantId(), event.clientSecret()));
    }

    @RabbitListener(queues = {ZEITERFASSUNG_QUEUE_OIDC_CLIENT_DELETED_CONSUMER})
    public void handleEvent(OidcClientDeletedEventDTO event) {
        LOG.info("Received oidcClientDeletedEvent for oidcClient={} and tenantId={}", event.clientId(), event.tenantId());

        tenantRegistrationService.disableTenant(event.tenantId());
    }
}
