package de.focusshift.zeiterfassung.absence;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface AbsenceRepository extends CrudRepository<AbsenceWriteEntity, Long> {

    Optional<AbsenceWriteEntity> findByTenantIdAndSourceIdAndType(String tenantId, Long sourceId, AbsenceType absenceType);

    List<AbsenceWriteEntity> findAllByTenantIdAndUserIdAndStartDateGreaterThanEqualAndEndDateLessThan(
        String tenantId, String userId, Instant from, Instant toExclusive
    );

    List<AbsenceWriteEntity> findAllByTenantIdAndUserIdInAndStartDateGreaterThanEqualAndEndDateLessThan(
        String tenantId, List<String> userIds, Instant from, Instant toExclusive
    );

    List<AbsenceWriteEntity> findAllByTenantIdAndStartDateGreaterThanEqualAndEndDateLessThan(
        String tenantId, Instant from, Instant toExclusive
    );

    @Modifying
    @Transactional
    int deleteByTenantIdAndSourceIdAndType(String tenantId, Long sourceId, AbsenceType type);
}
