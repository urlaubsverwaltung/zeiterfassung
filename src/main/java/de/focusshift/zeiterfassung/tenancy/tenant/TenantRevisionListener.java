package de.focusshift.zeiterfassung.tenancy.tenant;

import org.hibernate.envers.RevisionListener;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
class TenantRevisionListener implements RevisionListener {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantContextHolder tenantContextHolder;

    TenantRevisionListener(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public void newRevision(Object object) {
        if (object instanceof TenantAwareRevisionEntity tenantAware) {
            setTenantId(tenantAware);
            setUpdatedBy(tenantAware);
        }
    }

    private void setTenantId(TenantAwareRevisionEntity entity) {

        final String tenantId = tenantContextHolder.getCurrentTenantId()
            .map(TenantId::tenantId)
            .orElseThrow(() -> new MissingTenantException("No tenant found in security context"));

        entity.setTenantId(tenantId);
    }

    private void setUpdatedBy(TenantAwareRevisionEntity entity) {

        // TODO use AuthenticationService to get userId?
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof OAuth2AuthenticationToken token) {
            final OAuth2User principal = token.getPrincipal();
            if (principal instanceof OidcUser oidcUser) {
                final String userIdValue = oidcUser.getSubject();
                entity.setUpdatedBy(userIdValue);
            } else {
                LOG.info("Skip setting updated_by information on audited entity. Unexpected principal class {}", principal.getClass());
            }
        } else {
            LOG.info("Skip setting updated_by information on audited entity. Unexpected authentication: {}", auth);
        }
    }
}
