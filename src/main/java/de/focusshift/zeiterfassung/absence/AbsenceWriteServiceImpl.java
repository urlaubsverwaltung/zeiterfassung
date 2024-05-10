package de.focusshift.zeiterfassung.absence;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class AbsenceWriteServiceImpl implements AbsenceWriteService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AbsenceRepository repository;

    AbsenceWriteServiceImpl(AbsenceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void addAbsence(AbsenceWrite absence) {

        final Optional<AbsenceWriteEntity> existing = findEntity(absence);

        final String tenantId = absence.tenantId().tenantId();
        final Long sourceId = absence.sourceId();
        final AbsenceTypeSourceId typeId = absence.absenceTypeSourceId();

        if (existing.isEmpty()) {
            final AbsenceWriteEntity entity = new AbsenceWriteEntity();
            setEntityFields(entity, absence);
            repository.save(entity);
            LOG.info("successfully persisted absence in database. tenantId={} sourceId={} type={}", tenantId, sourceId, typeId);
        } else {
            LOG.info("did not persist absence because it exists already. tenantId={} sourceId={} type={}", tenantId, sourceId, typeId);
        }
    }

    @Override
    @Transactional
    public void updateAbsence(AbsenceWrite absence) {

        final Optional<AbsenceWriteEntity> existing = findEntity(absence);

        final String tenantId = absence.tenantId().tenantId();
        final Long sourceId = absence.sourceId();
        final AbsenceTypeSourceId typeId = absence.absenceTypeSourceId();

        if (existing.isPresent()) {
            final AbsenceWriteEntity entity = existing.get();
            setEntityFields(entity, absence);
            repository.save(entity);
            LOG.info("successfully updated absence in database. tenantId={} sourceId={} type={}", tenantId, sourceId, typeId);
        } else {
            LOG.info("no absence found that could be updated. tenantId={} sourceId={} type={}", tenantId, sourceId, typeId);
        }
    }

    @Override
    @Transactional
    public void deleteAbsence(AbsenceWrite absence) {

        final String tenantId = absence.tenantId().tenantId();
        final Long sourceId = absence.sourceId();
        final AbsenceTypeSourceId typeSourceId = absence.absenceTypeSourceId();
        final AbsenceTypeCategory category = absence.absenceTypeCategory();

        final int countOfDeletedAbsences = repository.deleteByTenantIdAndSourceIdAndType_Category(tenantId, sourceId, category);

        if (countOfDeletedAbsences >= 1) {
            LOG.info("successfully deleted {} absences. tenantId={} sourceId={} typeSourceId={} typeCategory={}", countOfDeletedAbsences, tenantId, sourceId, typeSourceId, category);
        } else {
            LOG.info("did not delete absence. tenantId={} sourceId={} typeSourceId={} typeCategory={}", tenantId, sourceId, typeSourceId, category);
        }
    }

    private Optional<AbsenceWriteEntity> findEntity(AbsenceWrite absence) {
        return repository.findByTenantIdAndSourceIdAndType_Category(absence.tenantId().tenantId(), absence.sourceId(), absence.absenceTypeCategory());
    }

    private static void setEntityFields(AbsenceWriteEntity entity, AbsenceWrite absence) {
        entity.setTenantId(absence.tenantId().tenantId());
        entity.setSourceId(absence.sourceId());
        entity.setUserId(absence.userId().value());
        entity.setStartDate(absence.startDate());
        entity.setEndDate(absence.endDate());
        entity.setDayLength(absence.dayLength());
        entity.setType(setTypeEntityFields(absence));
    }

    private static AbsenceTypeEntityEmbeddable setTypeEntityFields(AbsenceWrite absence) {

        final AbsenceTypeEntityEmbeddable absenceTypeEntity = new AbsenceTypeEntityEmbeddable();
        absenceTypeEntity.setCategory(absence.absenceTypeCategory());

        if (absence.absenceTypeSourceId() != null) {
            absenceTypeEntity.setSourceId(absence.absenceTypeSourceId().value());
        }

        return absenceTypeEntity;
    }
}
