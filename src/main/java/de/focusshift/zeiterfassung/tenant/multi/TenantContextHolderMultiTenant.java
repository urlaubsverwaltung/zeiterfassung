package de.focusshift.zeiterfassung.tenant.multi;

import de.focusshift.zeiterfassung.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenant.TenantId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static de.focusshift.zeiterfassung.tenant.TenantConfigurationProperties.MULTI;

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
