package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.DateRange;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.time.ZoneOffset.UTC;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class AbsenceWriteServiceImpl implements AbsenceWriteService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final AbsenceRepository repository;
    private final ApplicationEventPublisher applicationEventPublisher;

    AbsenceWriteServiceImpl(AbsenceRepository repository, ApplicationEventPublisher applicationEventPublisher) {
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional
    public void addAbsence(AbsenceWrite absence) {

        final Optional<AbsenceWriteEntity> existing = findEntity(absence);

        final Long sourceId = absence.sourceId();
        final AbsenceTypeSourceId typeId = absence.absenceTypeSourceId();

        if (existing.isEmpty()) {
            final AbsenceWriteEntity entity = new AbsenceWriteEntity();
            setEntityFields(entity, absence);
            repository.save(entity);
            LOG.info("successfully persisted absence in database. sourceId={} type={}", sourceId, typeId);

            // AbsenceWriteService only knows a "technical" layer (messaging with urlaubsverwaltung)
            // and the incoming date is an Instant. Therefore, we can use UTC "safely" here...
            // The web layer NEVER calls this service -> no user specific timezone knowledge required
            final LocalDate startDate = LocalDate.ofInstant(absence.startDate(), UTC);
            final LocalDate endDate = LocalDate.ofInstant(absence.endDate(), UTC);
            final DateRange dateRange = new DateRange(startDate, endDate);
            applicationEventPublisher.publishEvent(new AbsenceAddedEvent(absence.userId(), dateRange));
        } else {
            LOG.info("did not persist absence because it exists already. sourceId={} type={}", sourceId, typeId);
        }
    }

    @Override
    @Transactional
    public void updateAbsence(AbsenceWrite absence) {

        final Optional<AbsenceWriteEntity> existing = findEntity(absence);

        final Long sourceId = absence.sourceId();
        final AbsenceTypeSourceId typeId = absence.absenceTypeSourceId();

        if (existing.isPresent()) {
            final AbsenceWriteEntity entity = existing.get();
            setEntityFields(entity, absence);
            repository.save(entity);
            LOG.info("successfully updated absence in database. sourceId={} type={}", sourceId, typeId);
            applicationEventPublisher.publishEvent(new AbsenceUpdatedEvent(absence.userId(), new DateRange(LocalDate.ofInstant(existing.get().getStartDate(), UTC), LocalDate.ofInstant(existing.get().getEndDate(), UTC)), new DateRange(LocalDate.ofInstant(absence.startDate(), UTC), LocalDate.ofInstant(absence.endDate(), UTC))));
        } else {
            LOG.info("no absence found that could be updated, sourceId={} type={}", sourceId, typeId);
        }
    }

    @Override
    @Transactional
    public void deleteAbsence(AbsenceWrite absence) {

        final Long sourceId = absence.sourceId();
        final AbsenceTypeSourceId typeSourceId = absence.absenceTypeSourceId();
        final AbsenceTypeCategory category = absence.absenceTypeCategory();

        final int countOfDeletedAbsences = repository.deleteBySourceIdAndType_Category(sourceId, category);

        if (countOfDeletedAbsences >= 1) {
            LOG.info("successfully deleted {} absences. sourceId={} typeSourceId={} typeCategory={}", countOfDeletedAbsences, sourceId, typeSourceId, category);
            applicationEventPublisher.publishEvent(new AbsenceDeletedEvent(absence.userId(), new DateRange(LocalDate.ofInstant(absence.startDate(), UTC), LocalDate.ofInstant(absence.endDate(), UTC))));
        } else {
            LOG.info("did not delete absence. sourceId={} typeSourceId={} typeCategory={}", sourceId, typeSourceId, category);
        }
    }

    private Optional<AbsenceWriteEntity> findEntity(AbsenceWrite absence) {
        return repository.findBySourceId(absence.sourceId());
    }

    private static void setEntityFields(AbsenceWriteEntity entity, AbsenceWrite absence) {
        entity.setSourceId(absence.sourceId());
        entity.setUserId(absence.userId().value());
        entity.setStartDate(absence.startDate());
        entity.setEndDate(absence.endDate());
        entity.setDayLength(absence.dayLength());
        entity.setType(setTypeEntityFields(absence));
        entity.setOvertimeHours(absence.overtimeHours());
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
