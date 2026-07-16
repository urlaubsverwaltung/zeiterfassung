package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.configuration.single.ConditionalOnSingleTenantMode;
import de.focusshift.zeiterfassung.tenancy.configuration.single.SingleTenantConfigurationProperties;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the tenant id in single-tenant mode. There is exactly one tenant, so the
 * configured {@link SingleTenantConfigurationProperties#getDefaultTenantId() default tenant id}
 * is returned for every authentication shape - the call sites only need a non-empty result
 * to proceed. The resolved value mirrors the tenant reported by the single-tenant
 * {@code TenantContextHolder}, but the two collaborators are independent and only share the
 * {@link SingleTenantConfigurationProperties}.
 */
@Component
@ConditionalOnSingleTenantMode
class DefaultTenantIdResolver implements TenantIdResolver {

    private final TenantId defaultTenantId;

    DefaultTenantIdResolver(SingleTenantConfigurationProperties properties) {
        this.defaultTenantId = new TenantId(properties.getDefaultTenantId());
    }

    @Override
    public Optional<TenantId> resolve(OAuth2AuthenticationToken token) {
        return Optional.of(defaultTenantId);
    }

    @Override
    public Optional<TenantId> resolve(OAuth2LoginAuthenticationToken token) {
        return Optional.of(defaultTenantId);
    }

    @Override
    public Optional<TenantId> resolve(OidcUserAuthority authority) {
        return Optional.of(defaultTenantId);
    }
}
