package de.focusshift.zeiterfassung.registration.oidc;

import de.focusshift.zeiterfassung.registration.tenant.TenantRegisteredEvent;
import de.focusshift.zeiterfassung.tenant.TenantDisabledEvent;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static de.focusshift.zeiterfassung.tenant.TenantConfigurationProperties.MULTI;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
public class TenantLifecycleEventHandlerOidcClient {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private final EditableClientRegistrationRepository editableClientRegistrationRepository;

    public TenantLifecycleEventHandlerOidcClient(EditableClientRegistrationRepository editableClientRegistrationRepository) {
        this.editableClientRegistrationRepository = editableClientRegistrationRepository;
    }

    @Async
    @EventListener
    void handleTenantDisabledEvent(TenantDisabledEvent tenantDisabledEvent) {
        String tenantId = tenantDisabledEvent.tenant().tenantId();

        if (!editableClientRegistrationRepository.existsClient(tenantId)) {
            LOG.info("skip deleting oidc client for tenantId={} - already gone!", tenantId);
            return;
        }

        LOG.info("Deleting oidc client for tenantId={} ...!", tenantId);
        editableClientRegistrationRepository.deleteExistingClient(tenantId);
        LOG.info("Finished deleting oidc client for tenantId={} ...!", tenantId);
    }

    @Async
    @EventListener
    void handleTenantRegisteredEvent(TenantRegisteredEvent tenantRegisteredEvent) {

        final String tenantId = tenantRegisteredEvent.tenant().tenantId();
        if (editableClientRegistrationRepository.existsClient(tenantId)) {
            LOG.info("skip registering oidc client registration for tenantId={} - already exists!", tenantId);
            return;
        }

        LOG.info("Registering new oidc client for tenantId={} ...!", tenantId);
        editableClientRegistrationRepository.addNewClient(tenantId, tenantRegisteredEvent.oidcClientSecret());
        LOG.info("Finished registering new oidc client for tenantId={} ...!", tenantId);
    }
}
