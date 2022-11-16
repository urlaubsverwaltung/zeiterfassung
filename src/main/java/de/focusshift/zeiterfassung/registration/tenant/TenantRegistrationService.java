package de.focusshift.zeiterfassung.registration.tenant;

import de.focusshift.zeiterfassung.tenant.Tenant;
import de.focusshift.zeiterfassung.tenant.TenantService;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.focusshift.zeiterfassung.tenant.TenantConfigurationProperties.MULTI;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
public class TenantRegistrationService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantService tenantService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TenantRegistrationService(TenantService tenantService,
                                     ApplicationEventPublisher applicationEventPublisher) {
        this.tenantService = tenantService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void registerNewTenant(TenantRegistration tenantRegistration) {
        final String tenantId = tenantRegistration.tenantId();

        if (tenantService.getTenantByTenantId(tenantId).isPresent()) {
            LOG.info("skip registering new tenant with tenantId={} - already exists!", tenantId);
            return;
        }

        LOG.info("Registering new tenant with tenantId={} ...!", tenantId);
        final Tenant tenant = tenantService.create(tenantId);
        applicationEventPublisher.publishEvent(new TenantRegisteredEvent(tenant, tenantRegistration.oidcClientSecret()));
        LOG.info("Finished registering new tenant with tenantId={} ...!", tenantId);
    }

    public List<String> findAll() {
        return tenantService.findAllTenants().stream().map(Tenant::tenantId).toList();
    }

    public void disableTenant(String tenantId) {
        tenantService.disable(tenantId);
    }
}
