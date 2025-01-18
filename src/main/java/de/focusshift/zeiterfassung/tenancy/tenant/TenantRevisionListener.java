package de.focusshift.zeiterfassung.tenancy.tenant;

import org.hibernate.envers.RevisionListener;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            LOG.info("No authentication found. Skip setting updated_by information on audited entity");
        } else {
            final String name = auth.getName();
            entity.setUpdatedBy(name);
        }
    }
}
