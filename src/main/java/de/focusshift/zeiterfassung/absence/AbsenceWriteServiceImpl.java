package de.focusshift.zeiterfassung.absence;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public void addAbsence(AbsenceWrite absence) {

        final List<AbsenceWriteEntity> existing = findExistingAbsenceMatches(absence);

        if (existing.isEmpty()) {
            final AbsenceWriteEntity entity = new AbsenceWriteEntity();
            setEntityFields(entity, absence);
            repository.save(entity);
            LOG.debug("successfully persisted absence in database.");
        } else {
            LOG.info("did not persist absence because it seems to exist already in database.");
        }
    }

    @Override
    @Transactional
    public void updateAbsence(AbsenceWrite absence) {

        final List<AbsenceWriteEntity> existing = findExistingAbsenceMatches(absence);

        if (existing.size() == 1) {
            final AbsenceWriteEntity entity = existing.get(0);
            setEntityFields(entity, absence);
            repository.save(entity);
            LOG.debug("successfully updated absence in database.");
        } else  if (existing.isEmpty()) {
            LOG.info("no absence found that could be updated.");
        } else {
            LOG.info("multiple absences found that could be updated. aborting update.");
        }
    }

    @Override
    @Transactional
    public void deleteAbsence(AbsenceWrite absence) {

        repository.deleteAllByTenantIdAndUserIdAndStartDateAndEndDateAndDayLengthAndType(
            absence.tenantId().tenantId(),
            absence.userId().value(),
            absence.startDate(),
            absence.endDate(),
            absence.dayLength(),
            absence.type()
        );
    }

    private List<AbsenceWriteEntity> findExistingAbsenceMatches(AbsenceWrite absence) {
        return repository.findAllByTenantIdAndUserIdAndStartDateAndEndDateAndDayLengthAndType(
            absence.tenantId().tenantId(),
            absence.userId().value(),
            absence.startDate(),
            absence.endDate(),
            absence.dayLength(),
            absence.type()
        );
    }

    private static void setEntityFields(AbsenceWriteEntity entity, AbsenceWrite absence) {
        entity.setTenantId(absence.tenantId().tenantId());
        entity.setUserId(absence.userId().value());
        entity.setStartDate(absence.startDate());
        entity.setEndDate(absence.endDate());
        entity.setDayLength(absence.dayLength());
        entity.setType(absence.type());
        entity.setColor(absence.color());
    }
}
