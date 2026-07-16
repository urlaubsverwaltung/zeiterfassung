package de.focusshift.zeiterfassung.tenancy.authentication;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.slf4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Order(10)
class ClaimTenantIdResolver implements TenantIdResolver {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final String claimName;

    ClaimTenantIdResolver(ClaimTenantIdResolverProperties properties) {
        this.claimName = properties.getClaimName();
    }

    @Override
    public Optional<TenantId> resolve(OAuth2AuthenticationToken token) {
        return resolveFromOidcUser(token.getPrincipal());
    }

    @Override
    public Optional<TenantId> resolve(OAuth2LoginAuthenticationToken token) {
        return resolveFromOidcUser(token.getPrincipal());
    }

    @Override
    public Optional<TenantId> resolve(OidcUserAuthority authority) {
        final Map<String, Object> idTokenClaims = authority.getIdToken().getClaims();
        final Map<String, Object> userInfoClaims = authority.getUserInfo() != null ? authority.getUserInfo().getClaims() : null;
        return firstClaim(idTokenClaims, userInfoClaims, null);
    }

    private Optional<TenantId> resolveFromOidcUser(Object principal) {
        if (principal instanceof OidcUser oidcUser) {
            return firstClaim(
                oidcUser.getIdToken().getClaims(),
                oidcUser.getUserInfo() != null ? oidcUser.getUserInfo().getClaims() : null,
                oidcUser.getClaims());
        }
        return Optional.empty();
    }

    @SafeVarargs
    private Optional<TenantId> firstClaim(Map<String, Object>... sources) {
        for (Map<String, Object> source : sources) {
            if (source == null || !source.containsKey(claimName)) {
                continue;
            }
            final Object value = source.get(claimName);
            final Optional<String> single = asSingleString(value);
            if (single.isEmpty()) {
                LOG.warn("tenant id claim '{}' has an ambiguous or unsupported value type - ignoring", claimName);
                return Optional.empty();
            }
            final TenantId tenantId = new TenantId(single.get());
            return tenantId.valid() ? Optional.of(tenantId) : Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<String> asSingleString(Object value) {
        if (value instanceof String string) {
            return Optional.of(string);
        }
        if (value instanceof Collection<?> collection) {
            if (collection.size() == 1) {
                final Object element = collection.iterator().next();
                if (element != null) {
                    return Optional.of(String.valueOf(element));
                }
            }
            return Optional.empty();
        }
        return Optional.empty();
    }
}
