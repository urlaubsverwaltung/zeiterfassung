package de.focusshift.zeiterfassung.tenant;

import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class TenantServiceImpl implements TenantService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantRepository tenantRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    TenantServiceImpl(TenantRepository tenantRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.tenantRepository = tenantRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Optional<Tenant> getTenantByTenantId(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
            .map(TenantServiceImpl::toTenant);
    }

    @Override
    public Tenant create(String tenantId) {
        LOG.info("going to add new tenant with tenantId={}", tenantId);
        final Instant now = Instant.now();
        final TenantEntity update = new TenantEntity(null, tenantId, now, now, TenantEntity.TenantStatusEntity.ACTIVE);
        Tenant tenant = toTenant(tenantRepository.save(update));
        LOG.info("added new tenant with tenantId={}", tenantId);
        return tenant;
    }

    @Override
    public Tenant disable(String tenantId) {

        LOG.info("going to disable tenant with tenantId={}", tenantId);

        if (getTenantByTenantId(tenantId).isEmpty()) {
            LOG.info("skip disabling tenant with tenantId={} - this tenant doesn't use this application", tenantId);
            return null;
        }

        final Tenant update = update(new Tenant(tenantId, null, null, TenantStatus.DISABLED));

        applicationEventPublisher.publishEvent(new TenantDisabledEvent(update));
        LOG.info("disabled tenant with tenantId={}", tenantId);
        return update;
    }

    @Override
    public List<Tenant> findAllTenants() {
        return tenantRepository.findAll().stream().map(TenantServiceImpl::toTenant).toList();
    }

    @Override
    public void delete(String tenantId) {
        tenantRepository.deleteByTenantId(tenantId);
    }

    private static Tenant toTenant(TenantEntity entity) {
        return new Tenant(entity.getTenantId(), entity.getCreatedAt(), entity.getUpdatedAt(), TenantStatus.valueOf(entity.getStatus().name()));
    }

    private Tenant update(Tenant tenant) {
        final String tenantId = tenant.tenantId();
        final TenantEntity existing = tenantRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("could not find tenant with tenantId=%s".formatted(tenantId)));

        final TenantEntity update = new TenantEntity(
            existing.id,
            tenantId,
            existing.getCreatedAt(),
            Instant.now(),
            TenantEntity.TenantStatusEntity.valueOf(tenant.status().name())
        );
        return toTenant(tenantRepository.save(update));
    }
}
