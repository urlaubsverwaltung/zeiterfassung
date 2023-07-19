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
        final AbsenceType type = absence.type();

        if (existing.isEmpty()) {
            final AbsenceWriteEntity entity = new AbsenceWriteEntity();
            setEntityFields(entity, absence);
            repository.save(entity);
            LOG.info("successfully persisted absence in database. tenantId={} sourceId={} type={}", tenantId, sourceId, type);
        } else {
            LOG.info("did not persist absence because it exists already. tenantId={} sourceId={} type={}", tenantId, sourceId, type);
        }
    }

    @Override
    @Transactional
    public void updateAbsence(AbsenceWrite absence) {

        final Optional<AbsenceWriteEntity> existing = findEntity(absence);

        final String tenantId = absence.tenantId().tenantId();
        final Long sourceId = absence.sourceId();
        final AbsenceType type = absence.type();

        if (existing.isPresent()) {
            final AbsenceWriteEntity entity = existing.get();
            setEntityFields(entity, absence);
            repository.save(entity);
            LOG.info("successfully updated absence in database. tenantId={} sourceId={} type={}", tenantId, sourceId, type);
        } else {
            LOG.info("no absence found that could be updated. tenantId={} sourceId={} type={}", tenantId, sourceId, type);
        }
    }

    @Override
    @Transactional
    public void deleteAbsence(AbsenceWrite absence) {

        final String tenantId = absence.tenantId().tenantId();
        final Long sourceId = absence.sourceId();
        final AbsenceType type = absence.type();

        final int countOfDeletedAbsences = repository.deleteByTenantIdAndSourceIdAndType(tenantId, sourceId, type);

        if (countOfDeletedAbsences >= 1) {
            LOG.info("successfully deleted {} absences. tenantId={} sourceId={} type={}", countOfDeletedAbsences, tenantId, sourceId, type);
        } else {
            LOG.info("did not delete absence. tenantId={} sourceId={} type={}", tenantId, sourceId, type);
        }
    }

    private Optional<AbsenceWriteEntity> findEntity(AbsenceWrite absence) {
        return repository.findByTenantIdAndSourceIdAndType(absence.tenantId().tenantId(), absence.sourceId(), absence.type());
    }

    private static void setEntityFields(AbsenceWriteEntity entity, AbsenceWrite absence) {
        entity.setTenantId(absence.tenantId().tenantId());
        entity.setSourceId(absence.sourceId());
        entity.setUserId(absence.userId().value());
        entity.setStartDate(absence.startDate());
        entity.setEndDate(absence.endDate());
        entity.setDayLength(absence.dayLength());
        entity.setType(absence.type());
        entity.setColor(absence.color());
    }
}
