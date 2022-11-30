package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import de.focusshift.zeiterfassung.tenancy.registration.web.TenantRegisteredEvent;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantDisabledEvent;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
public class TenantLifecycleEventHandlerOidcClient {

    private static final Logger LOG = getLogger(lookup().lookupClass());
    private final JdbcClientRegistrationRepository jdbcClientRegistrationRepository;

    TenantLifecycleEventHandlerOidcClient(JdbcClientRegistrationRepository jdbcClientRegistrationRepository) {
        this.jdbcClientRegistrationRepository = jdbcClientRegistrationRepository;
    }

    @Async
    @EventListener
    void handleTenantDisabledEvent(TenantDisabledEvent tenantDisabledEvent) {
        String tenantId = tenantDisabledEvent.tenant().tenantId();

        if (!jdbcClientRegistrationRepository.existsClient(tenantId)) {
            LOG.info("skip deleting oidc client for tenantId={} - already gone!", tenantId);
            return;
        }

        LOG.info("Deleting oidc client for tenantId={} ...!", tenantId);
        jdbcClientRegistrationRepository.deleteExistingClient(tenantId);
        LOG.info("Finished deleting oidc client for tenantId={} ...!", tenantId);
    }

    @Async
    @EventListener
    void handleTenantRegisteredEvent(TenantRegisteredEvent tenantRegisteredEvent) {

        final String tenantId = tenantRegisteredEvent.tenant().tenantId();
        if (jdbcClientRegistrationRepository.existsClient(tenantId)) {
            LOG.info("skip registering oidc client registration for tenantId={} - already exists!", tenantId);
            return;
        }

        LOG.info("Registering new oidc client for tenantId={} ...!", tenantId);
        jdbcClientRegistrationRepository.addNewClient(tenantId, tenantRegisteredEvent.oidcClientSecret());
        LOG.info("Finished registering new oidc client for tenantId={} ...!", tenantId);
    }
}
