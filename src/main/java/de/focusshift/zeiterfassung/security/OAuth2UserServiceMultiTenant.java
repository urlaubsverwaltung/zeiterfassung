package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.authentication.TenantIdProvider;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;

class OAuth2UserServiceMultiTenant extends OAuth2TenantUserService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final TenantUserService tenantUserService;
    private final TenantContextHolder tenantContextHolder;
    private final TenantIdProvider tenantIdProvider;

    OAuth2UserServiceMultiTenant(OAuth2UserService<OidcUserRequest, OidcUser> delegate, TenantUserService tenantUserService, TenantContextHolder tenantContextHolder, TenantIdProvider tenantIdProvider) {
        super(delegate);
        this.tenantUserService = tenantUserService;
        this.tenantContextHolder = tenantContextHolder;
        this.tenantIdProvider = tenantIdProvider;
    }

    @Override
    protected Optional<TenantUser> loadTenantUser(OidcUserRequest oidcUserRequest, OidcUser oidcUser) {
        try {
            // we need to fill the tenantContextHolder by ourselves because
            // the interceptor based approach will work for authenticated users only
            return prepareTenantContext(oidcUser)
                .map(tenantId -> findUserByOidcSubject(oidcUser))
                .orElseGet(() -> {
                    LOG.warn("detected invalid tenantId in oidcUserRequest - returning empty tenantUser");
                    return Optional.empty();
                });
        } finally {
            cleanupTenantContext();
        }
    }

    private Optional<TenantUser> findUserByOidcSubject(OidcUser oidcUser) {
        return tenantUserService.findById(new UserId(oidcUser.getUserInfo().getSubject()));
    }

    private void cleanupTenantContext() {
        tenantContextHolder.clear();
    }

    private Optional<TenantId> prepareTenantContext(OidcUser oidcUser) {
        final OidcUserAuthority authority = new OidcUserAuthority(oidcUser.getIdToken(), oidcUser.getUserInfo());
        final Optional<TenantId> tenantId = tenantIdProvider.resolve(authority);
        tenantId.ifPresent(tenantContextHolder::setTenantId);
        return tenantId;
    }

}
