package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;

@Component
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
public class TenantContextHolderMultiTenant implements TenantContextHolder {

    public Optional<TenantId> getCurrentTenantId() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof final OAuth2AuthenticationToken oauthToken) {
            final TenantId tenantId = new TenantId(oauthToken.getAuthorizedClientRegistrationId());
            if (tenantId.valid()) {
                return Optional.of(tenantId);
            }
        }
        return Optional.empty();
    }
}
