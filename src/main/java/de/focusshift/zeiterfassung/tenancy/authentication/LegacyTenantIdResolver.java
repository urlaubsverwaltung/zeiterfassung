package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.regex.Pattern;

@Order(20)
class LegacyTenantIdResolver implements TenantIdResolver {

    private final Pattern tenantIdPattern;
    private final String issuerMarker;

    LegacyTenantIdResolver(LegacyTenantIdResolverProperties properties) {
        this.tenantIdPattern = Pattern.compile(properties.getTenantIdPattern());
        this.issuerMarker = properties.getIssuerTenantMarker();
    }

    @Override
    public Optional<TenantId> resolve(OAuth2AuthenticationToken token) {
        return toTenantId(token.getAuthorizedClientRegistrationId());
    }

    @Override
    public Optional<TenantId> resolve(OAuth2LoginAuthenticationToken token) {
        return toTenantId(token.getClientRegistration().getRegistrationId());
    }

    @Override
    public Optional<TenantId> resolve(OidcUserAuthority authority) {
        final String iss = authority.getIdToken().getClaimAsString(IdTokenClaimNames.ISS);
        return extractFromIssuer(iss).flatMap(this::toTenantId);
    }

    private Optional<TenantId> toTenantId(String candidate) {
        if (candidate == null || !tenantIdPattern.matcher(candidate).matches()) {
            return Optional.empty();
        }
        final TenantId tenantId = new TenantId(candidate);
        return tenantId.valid() ? Optional.of(tenantId) : Optional.empty();
    }

    private Optional<String> extractFromIssuer(String iss) {
        if (iss == null) {
            return Optional.empty();
        }
        final String[] segments = iss.split("/");
        for (int i = 0; i <= segments.length - 2; i++) {
            if (issuerMarker.equals(segments[i]) && StringUtils.hasText(segments[i + 1])) {
                return Optional.of(segments[i + 1]);
            }
        }
        return Optional.empty();
    }
}
