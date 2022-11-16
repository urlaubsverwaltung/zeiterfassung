package de.focusshift.zeiterfassung.registration.oidc.persistent;

import de.focusshift.zeiterfassung.registration.tenant.TenantRegistration;
import de.focusshift.zeiterfassung.registration.tenant.TenantRegistrationService;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnBean(TenantRegistrationService.class)
@ConditionalOnProperty(value = "zeiterfassung.tenant.registration.enabled", havingValue = "true")
class OidcClientFromPropertiesImporterService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final OAuth2ClientProperties oAuth2ClientProperties;
    private final TenantRegistrationService tenantRegistrationService;

    OidcClientFromPropertiesImporterService(OAuth2ClientProperties oAuth2ClientProperties,
                                            TenantRegistrationService tenantRegistrationService) {
        this.oAuth2ClientProperties = oAuth2ClientProperties;
        this.tenantRegistrationService = tenantRegistrationService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void importTenantsFromProperties() {

        if (oAuth2ClientProperties.getRegistration().isEmpty()) {
            LOG.warn("No registrations in oAuth2ClientProperties - going to skip oidc client import!");
            return;
        }

        LOG.info("going to import tenants from oAuth2ClientProperties to database!");
        oAuth2ClientProperties.getRegistration()
            .values()
            .forEach(fromProperties -> {
                final String tenantId = fromProperties.getProvider();
                final String clientSecret = fromProperties.getClientSecret();
                tenantRegistrationService.registerNewTenant(new TenantRegistration(tenantId, clientSecret));
            });
        LOG.info("imported tenants from oAuth2ClientProperties to database!");
    }
}
