package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AbsenceWriteServiceImplIT extends TestContainersBase {

    @Autowired
    private AbsenceWriteServiceImpl sut;

    @Autowired
    private AbsenceRepository repository;

    @Test
    void ensureDeleteAbsence() {

        final Instant startDate = Instant.now();
        final Instant endDate = Instant.now();

        final AbsenceWriteEntity existingEntity = new AbsenceWriteEntity();
        existingEntity.setTenantId("tenant");
        existingEntity.setSourceId(42L);
        existingEntity.setUserId("user-id");
        existingEntity.setStartDate(startDate);
        existingEntity.setEndDate(endDate);
        existingEntity.setDayLength(DayLength.FULL);
        existingEntity.setType(new AbsenceTypeEntityEmbeddable(AbsenceType.HOLIDAY.category(), AbsenceType.HOLIDAY.sourceId()));
        existingEntity.setColor(AbsenceColor.PINK);
        repository.save(existingEntity);

        final AbsenceWrite absence = new AbsenceWrite(
            new TenantId("tenant"),
            42L,
            new UserId("user-id"),
            startDate,
            endDate,
            DayLength.FULL,
            AbsenceType.HOLIDAY,
            AbsenceColor.BLUE
        );

        assertThat(repository.findAll()).hasSize(1);

        sut.deleteAbsence(absence);

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void ensureDeleteAbsenceDoesNotDeleteWhenTenantIdIsDifferent() {

        final Instant startDate = Instant.now();
        final Instant endDate = Instant.now();

        final AbsenceWriteEntity existingEntity = new AbsenceWriteEntity();
        existingEntity.setTenantId("tenant");
        existingEntity.setSourceId(42L);
        existingEntity.setUserId("user-id");
        existingEntity.setStartDate(startDate);
        existingEntity.setEndDate(endDate);
        existingEntity.setDayLength(DayLength.FULL);
        existingEntity.setType(new AbsenceTypeEntityEmbeddable(AbsenceType.HOLIDAY.category(), AbsenceType.HOLIDAY.sourceId()));
        existingEntity.setColor(AbsenceColor.PINK);
        repository.save(existingEntity);

        final AbsenceWrite absence = new AbsenceWrite(
            new TenantId("tenant-2"),
            42L,
            new UserId("user-id"),
            startDate,
            endDate,
            DayLength.FULL,
            AbsenceType.HOLIDAY,
            AbsenceColor.BLUE
        );

        sut.deleteAbsence(absence);

        assertThat(repository.findAll()).hasSize(1);
    }
}
