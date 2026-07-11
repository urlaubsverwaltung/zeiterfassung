package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Optional;

public interface TenantIdResolver {

    default Optional<TenantId> resolve(OAuth2AuthenticationToken token) {
        return Optional.empty();
    }

    default Optional<TenantId> resolve(OAuth2LoginAuthenticationToken token) {
        return Optional.empty();
    }

    default Optional<TenantId> resolve(OidcUserAuthority authority) {
        return Optional.empty();
    }
}
