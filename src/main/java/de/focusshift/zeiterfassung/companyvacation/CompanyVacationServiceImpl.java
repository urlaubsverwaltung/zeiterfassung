package de.focusshift.zeiterfassung.companyvacation;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CompanyVacationServiceImpl implements CompanyVacationService {

    private final CompanyVacationRepository companyVacationRepository;

    CompanyVacationServiceImpl(CompanyVacationRepository companyVacationRepository) {
        this.companyVacationRepository = companyVacationRepository;
    }

    @Override
    public List<CompanyVacation> getCompanyVacations(Instant from, Instant toExclusive) {

        final List<CompanyVacationEntity> entities =
            companyVacationRepository.findAllByStartDateLessThanAndEndDateGreaterThanEqual(toExclusive, from);

        return entities.stream()
            .map(CompanyVacationServiceImpl::toCompanyVacation)
            .toList();
    }

    private static CompanyVacation toCompanyVacation(CompanyVacationEntity entity) {
        return new CompanyVacation(
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getDayLength()
        );
    }
}
