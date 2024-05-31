package de.focusshift.zeiterfassung.security;

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

import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;

class OAuth2UserServiceMultiTenant extends OAuth2TenantUserService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final TenantUserService tenantUserService;
    private final TenantContextHolder tenantContextHolder;

    OAuth2UserServiceMultiTenant(OAuth2UserService<OidcUserRequest, OidcUser> delegate, TenantUserService tenantUserService, TenantContextHolder tenantContextHolder) {
        super(delegate);
        this.tenantUserService = tenantUserService;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    protected Optional<TenantUser> loadTenantUser(OidcUserRequest oidcUserRequest, OidcUser oidcUser) {
        try {
            // we need to fill the tenantContextHolder by ourselves because
            // the interceptor based approach will work for authenticated users only
            return prepareTenantContext(oidcUserRequest)
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

    private Optional<TenantId> prepareTenantContext(OidcUserRequest oidcUserRequest) {
        TenantId tenantId = new TenantId(oidcUserRequest.getClientRegistration().getRegistrationId());
        if (tenantId.valid()) {
            tenantContextHolder.setTenantId(tenantId);
            return Optional.of(tenantId);
        } else {
            return Optional.empty();
        }
    }

}
