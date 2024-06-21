package de.focusshift.zeiterfassung.absence;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Service
class AbsenceTypeServiceImpl implements AbsenceTypeService {

    private final AbsenceTypeRepository repository;

    AbsenceTypeServiceImpl(AbsenceTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void updateAbsenceType(AbsenceTypeUpdate absenceTypeUpdate) {

        final Long sourceId = absenceTypeUpdate.sourceId();

        final AbsenceTypeEntity entity = repository.findBySourceId(sourceId)
            .orElseGet(AbsenceTypeEntity::new);

        entity.setSourceId(sourceId);
        entity.setCategory(absenceTypeUpdate.category());
        entity.setColor(absenceTypeUpdate.color());
        entity.setLabelByLocale(new HashMap<>(absenceTypeUpdate.labelByLocale()));

        repository.save(entity);
    }

    @Override
    public List<AbsenceType> findAllByAbsenceTypeSourceIds(Collection<Long> absenceTypeSourceIds) {
        return repository.findBySourceIdIsIn(absenceTypeSourceIds).stream()
            .map(AbsenceTypeServiceImpl::toAbsenceType)
            .toList();
    }

    private static AbsenceType toAbsenceType(AbsenceTypeEntity entity) {
        return new AbsenceType(
            entity.getCategory(),
            entity.getSourceId(),
            locale -> entity.getLabelByLocale().get(locale),
            entity.getColor()
        );
    }
}
