package de.focusshift.zeiterfassung.companyvacation;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface CompanyVacationRepository extends CrudRepository<CompanyVacationEntity, String> {

    @Query("""
        SELECT e
        FROM CompanyVacationEntity e
        WHERE e.sourceId = :sourceId
          AND EXTRACT(YEAR FROM e.startDate) = EXTRACT(YEAR FROM CAST(:createdAt AS timestamp))
          AND EXTRACT(YEAR FROM e.endDate) = EXTRACT(YEAR FROM CAST(:createdAt AS timestamp))
    """)
    Optional<CompanyVacationEntity> findBySourceIdAndStartAndEndInSameYearAsCreatedAt(String sourceId, Instant createdAt);

    /**
     * Finds all absences of intersection with interval from and toExclusive-1
     */
    List<CompanyVacationEntity> findAllByStartDateLessThanAndEndDateGreaterThanEqual(
        Instant toExclusive, Instant from
    );

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM CompanyVacationEntity e
        WHERE e.sourceId = :sourceId
          AND EXTRACT(YEAR FROM e.startDate) = EXTRACT(YEAR FROM CAST(:createdAt AS timestamp))
          AND EXTRACT(YEAR FROM e.endDate) = EXTRACT(YEAR FROM CAST(:createdAt AS timestamp))
    """)
    long deleteBySourceIdAndStartAndEndInSameYearAsCreatedAt(String sourceId, Instant createdAt);
}
