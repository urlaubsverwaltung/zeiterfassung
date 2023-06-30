package de.focusshift.zeiterfassung.absence;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;

interface AbsenceRepository extends CrudRepository<AbsenceWriteEntity, Long> {

    List<AbsenceWriteEntity> findAllByTenantIdAndUserIdAndStartDateAndEndDateAndDayLengthAndType(
        String tenantId, String userId, Instant startDate, Instant endDate, DayLength dayLength, AbsenceType type
    );

    @Modifying
    void deleteAllByTenantIdAndUserIdAndStartDateAndEndDateAndDayLengthAndType(
        String tenantId, String userId, Instant startDate, Instant endDate, DayLength dayLength, AbsenceType type
    );
}
