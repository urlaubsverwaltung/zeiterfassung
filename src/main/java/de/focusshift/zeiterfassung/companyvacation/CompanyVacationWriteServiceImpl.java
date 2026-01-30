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
    public void addOrUpdateCompanyVacation(Instant createdAt, CompanyVacationWrite companyVacation) {
        final Optional<CompanyVacationEntity> existing = repository.findBySourceIdAndStartAndEndInSameYearAsCreatedAt(companyVacation.sourceId(), createdAt);

        if (existing.isPresent()) {
            final CompanyVacationEntity entity = existing.get();
            entity.setDayLength(companyVacation.dayLength());

            final CompanyVacationEntity savedCompanyVacation = repository.save(entity);
            LOG.info("successfully updated company vacation in database. sourceId={}", savedCompanyVacation.getSourceId());
        } else {
            final CompanyVacationEntity entity = new CompanyVacationEntity();
            entity.setSourceId(companyVacation.sourceId());
            entity.setStartDate(companyVacation.startDate());
            entity.setEndDate(companyVacation.endDate());
            entity.setDayLength(companyVacation.dayLength());

            final CompanyVacationEntity savedCompanyVacation = repository.save(entity);
            LOG.info("successfully added company vacation in database. sourceId={}", savedCompanyVacation.getSourceId());
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
}
