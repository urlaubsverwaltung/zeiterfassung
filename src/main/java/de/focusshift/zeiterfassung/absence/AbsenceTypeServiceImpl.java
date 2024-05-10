package de.focusshift.zeiterfassung.absence;

import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
class AbsenceTypeServiceImpl implements AbsenceTypeService {

    private final AbsenceTypeRepository repository;

    AbsenceTypeServiceImpl(AbsenceTypeRepository repository) {
        this.repository = repository;
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
}
