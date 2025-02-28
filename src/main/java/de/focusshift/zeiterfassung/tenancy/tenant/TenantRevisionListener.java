package de.focusshift.zeiterfassung.tenancy.tenant;

import de.focusshift.zeiterfassung.security.AuthenticationFacade;
import de.focusshift.zeiterfassung.user.UserId;
import org.hibernate.envers.RevisionListener;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
class TenantRevisionListener implements RevisionListener {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantContextHolder tenantContextHolder;
    private final AuthenticationFacade authenticationFacade;

    TenantRevisionListener(TenantContextHolder tenantContextHolder, AuthenticationFacade authenticationFacade) {
        this.tenantContextHolder = tenantContextHolder;
        this.authenticationFacade = authenticationFacade;
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
        try {
            final UserId userId = authenticationFacade.getCurrentUserIdComposite().id();
            entity.setUpdatedBy(userId.value());
        } catch (Exception exception) {
            LOG.info("Skip setting updated_by information on audited entity. UserId could not be recognised in current Authentication", exception);
        }
    }
}
