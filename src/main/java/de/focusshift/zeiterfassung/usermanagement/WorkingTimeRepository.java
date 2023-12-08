package de.focusshift.zeiterfassung.usermanagement;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WorkingTimeRepository extends CrudRepository<WorkingTimeEntity, UUID> {

    Optional<WorkingTimeEntity> findByUserId(Long userId);

    Optional<WorkingTimeEntity> findByUserIdAndValidFrom(Long userId, LocalDate validFrom);

    List<WorkingTimeEntity> findAllByUserId(Long userId);

    List<WorkingTimeEntity> findAllByUserIdIsIn(Collection<Long> userIds);

    @Query(
        "SELECT x FROM working_time x WHERE x.userId = ?1 "
            + "AND x.validFrom = (SELECT MAX(w.validFrom) from working_time w WHERE w.userId = ?1 AND w.validFrom <= ?2)"
    )
    WorkingTimeEntity findByPersonAndValidityDateEqualsOrMinorDate(Long userId, LocalDate date);
}
