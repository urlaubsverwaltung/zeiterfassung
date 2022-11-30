package de.focusshift.zeiterfassung.security.oidc.clientregistration.property;

import de.focusshift.zeiterfassung.tenancy.registration.web.TenantRegistration;
import de.focusshift.zeiterfassung.tenancy.registration.web.TenantRegistrationService;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnBean(TenantRegistrationService.class)
@ConditionalOnProperty(value = "zeiterfassung.security.oidc.client.registration.property.enabled", havingValue = "true")
@EnableConfigurationProperties(OidcClientRegistrationPropertyConfigurationProperties.class)
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
    void importOIDCClientsFromProperties() {

        if (oAuth2ClientProperties.getRegistration().isEmpty()) {
            LOG.warn("No registrations in oAuth2ClientProperties - going to skip oidc client import!");
            return;
        }

        LOG.info("going to import tenants from oAuth2ClientProperties to database!");
        oAuth2ClientProperties.getRegistration()
            .values()
            .forEach(registration -> {
                final String tenantId = registration.getProvider();
                final String clientSecret = registration.getClientSecret();
                tenantRegistrationService.registerNewTenant(new TenantRegistration(tenantId, clientSecret));
            });
        LOG.info("imported tenants from oAuth2ClientProperties to database!");
    }
}
