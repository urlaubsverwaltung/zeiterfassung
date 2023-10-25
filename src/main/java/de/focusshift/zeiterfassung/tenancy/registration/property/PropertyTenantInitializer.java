package de.focusshift.zeiterfassung.tenancy.registration.property;

import de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties;
import de.focusshift.zeiterfassung.tenancy.configuration.single.SingleTenantConfigurationProperties;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.SINGLE;
import static java.lang.invoke.MethodHandles.lookup;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = SINGLE, matchIfMissing = true)
@EnableConfigurationProperties({TenantConfigurationProperties.class, SingleTenantConfigurationProperties.class})
class PropertyTenantInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final TenantService tenantService;
    private final String defaultTenantId;

    PropertyTenantInitializer(TenantService tenantService, SingleTenantConfigurationProperties singleTenantConfigurationProperties) {
        this.tenantService = tenantService;
        this.defaultTenantId = singleTenantConfigurationProperties.getDefaultTenantId();
    }

    @PostConstruct
    void init() {
        LOG.info("zeiterfassung is running in tenant mode=single");
        if (tenantService.getTenantByTenantId(defaultTenantId).isPresent()) {
            LOG.info("tenant with tenantId={} already exists - nothing todo", defaultTenantId);
            return;
        }
        LOG.info("tenant with tenantId={} doesn't exists, will be created", defaultTenantId);
        tenantService.create(defaultTenantId);
    }
}
