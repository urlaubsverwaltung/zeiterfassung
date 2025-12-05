package de.focusshift.zeiterfassung.tenancy.registration.property;

import de.focusshift.zeiterfassung.tenancy.registration.TenantRegistration;
import de.focusshift.zeiterfassung.tenancy.registration.TenantRegistrationService;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnBean(TenantRegistrationService.class)
@ConditionalOnProperty(value = "zeiterfassung.tenant.registration.property.oauth.enabled", havingValue = "true")
@EnableConfigurationProperties(TenantRegistryFromOAuthConfigurationProperties.class)
@AutoConfiguration(after = {OAuth2ClientAutoConfiguration.class})
class TenantRegistryFromOAuthPropertiesImporterService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final OAuth2ClientProperties oAuth2ClientProperties;
    private final TenantRegistrationService tenantRegistrationService;

    TenantRegistryFromOAuthPropertiesImporterService(OAuth2ClientProperties oAuth2ClientProperties,
                                                     TenantRegistrationService tenantRegistrationService) {
        this.oAuth2ClientProperties = oAuth2ClientProperties;
        this.tenantRegistrationService = tenantRegistrationService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void importOIDCClientsFromProperties() {

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
