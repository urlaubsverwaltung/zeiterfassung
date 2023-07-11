package de.focusshift.zeiterfassung.absence;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;

interface AbsenceRepository extends CrudRepository<AbsenceWriteEntity, Long> {

    List<AbsenceWriteEntity> findAllByTenantIdAndUserIdAndStartDateAndEndDateAndDayLengthAndType(
        String tenantId, String userId, Instant startDate, Instant endDate, DayLength dayLength, AbsenceType type
    );

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
    void deleteAllByTenantIdAndUserIdAndStartDateAndEndDateAndDayLengthAndType(
        String tenantId, String userId, Instant startDate, Instant endDate, DayLength dayLength, AbsenceType type
    );
}
