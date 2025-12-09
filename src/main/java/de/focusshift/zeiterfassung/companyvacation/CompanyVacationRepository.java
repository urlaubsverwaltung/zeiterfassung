package de.focusshift.zeiterfassung.companyvacation;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface CompanyVacationRepository extends CrudRepository<CompanyVacationEntity, String> {

    Optional<CompanyVacationEntity> findBySourceId(String sourceId);

    /**
     * Finds all absences of intersection with interval from and toExclusive-1
     */
    List<CompanyVacationEntity> findAllByStartDateLessThanAndEndDateGreaterThanEqual(
        Instant toExclusive, Instant from
    );

    @Modifying
    @Transactional
    long deleteBySourceId(String sourceId);
}
