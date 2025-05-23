package de.focusshift.zeiterfassung.absence;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface AbsenceRepository extends CrudRepository<AbsenceWriteEntity, Long> {

    Optional<AbsenceWriteEntity> findBySourceId(Long sourceId);

    /**
     * Finds all absences of userId of set and intersection with interval from and toExclusive-1
     */
    List<AbsenceWriteEntity> findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(
        List<String> userIds, Instant toExclusive, Instant from
    );

    /**
     * Finds all absences of intersection with interval from and toExclusive-1
     */
    List<AbsenceWriteEntity> findAllByStartDateLessThanAndEndDateGreaterThanEqual(
            Instant toExclusive, Instant from
    );

    @Modifying
    @Transactional
    int deleteBySourceIdAndType_Category(Long sourceId, AbsenceTypeCategory absenceTypeCategory);
}
