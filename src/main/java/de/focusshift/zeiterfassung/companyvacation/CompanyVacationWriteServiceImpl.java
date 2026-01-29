package de.focusshift.zeiterfassung.companyvacation;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class CompanyVacationWriteServiceImpl implements CompanyVacationWriteService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final CompanyVacationRepository repository;

    CompanyVacationWriteServiceImpl(CompanyVacationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void addOrUpdateCompanyVacation(CompanyVacationWrite companyVacation) {
        final Optional<CompanyVacationEntity> existing = findEntity(companyVacation);

        if (existing.isEmpty()) {
            final CompanyVacationEntity entity = new CompanyVacationEntity();
            setEntityFields(entity, companyVacation);
            repository.save(entity);
            LOG.info("successfully added company vacation in database. sourceId={}", companyVacation.sourceId());
        } else {
            final CompanyVacationEntity entity = existing.get();
            final String sourceId = entity.getSourceId();
            setEntityFields(entity, companyVacation);
            repository.save(entity);
            LOG.info("successfully updated company vacation in database. sourceId={}", sourceId);
        }
    }

    @Override
    public void deleteCompanyVacation(Instant createdAt, String sourceId) {
        final boolean deleted = repository.deleteBySourceIdAndStartAndEndInSameYearAsCreatedAt(sourceId, createdAt) == 1;

        if (deleted) {
            LOG.info("successfully deleted company vacation. sourceId={}", sourceId);
        } else {
            LOG.info("did not delete company vacation. sourceId={}", sourceId);
        }
    }

    private Optional<CompanyVacationEntity> findEntity(CompanyVacationWrite companyVacation) {
        return repository.findBySourceId(companyVacation.sourceId());
    }

    private static void setEntityFields(CompanyVacationEntity entity, CompanyVacationWrite absence) {
        entity.setSourceId(absence.sourceId());
        entity.setStartDate(absence.startDate());
        entity.setEndDate(absence.endDate());
        entity.setDayLength(absence.dayLength());
    }
}
