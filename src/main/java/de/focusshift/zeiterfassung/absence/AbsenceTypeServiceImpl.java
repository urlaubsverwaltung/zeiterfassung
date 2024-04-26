package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Service
class AbsenceTypeServiceImpl implements AbsenceTypeService {

    private final AbsenceTypeRepository repository;
    private final TenantContextHolder tenantContextHolder;

    AbsenceTypeServiceImpl(AbsenceTypeRepository repository, TenantContextHolder tenantContextHolder) {
        this.repository = repository;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public void updateAbsenceType(AbsenceTypeUpdate absenceTypeUpdate) {

        final String tenantId = absenceTypeUpdate.tenantId().tenantId();
        final Long sourceId = absenceTypeUpdate.sourceId();

        final AbsenceTypeEntity entity = repository.findByTenantIdAndSourceId(tenantId, sourceId)
            .orElseGet(AbsenceTypeEntity::new);

        entity.setTenantId(tenantId);
        entity.setSourceId(sourceId);
        entity.setCategory(absenceTypeUpdate.category());
        entity.setColor(absenceTypeUpdate.color());
        entity.setLabelByLocale(new HashMap<>(absenceTypeUpdate.labelByLocale()));

        repository.save(entity);
    }

    @Override
    public List<AbsenceType> findAllByAbsenceTypeSourceIds(Collection<Long> absenceTypeSourceIds) {
        return repository.findByTenantIdAndSourceIdIsIn(tenantId(), absenceTypeSourceIds).stream()
            .map(AbsenceTypeServiceImpl::toAbsenceType)
            .toList();
    }

    private String tenantId() {
        return tenantContextHolder.getCurrentTenantId()
            .orElseThrow(() -> new IllegalStateException("expected tenantId to exist."))
            .tenantId();
    }

    private static AbsenceType toAbsenceType(AbsenceTypeEntity entity) {
        return new AbsenceType(entity.getCategory(), entity.getSourceId(), entity.getLabelByLocale());
    }
}
