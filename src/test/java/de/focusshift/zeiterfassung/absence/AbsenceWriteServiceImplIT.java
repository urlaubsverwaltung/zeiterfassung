package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AbsenceWriteServiceImplIT extends TestContainersBase {

    @Autowired
    private AbsenceWriteServiceImpl sut;

    @Autowired
    private AbsenceRepository repository;

    @AfterEach
    void tearDown() {
        // since we do not have a transaction we have to clean up after the test.
        // (no transaction to ensure @Transactional annotation in the sut)
        repository.deleteAll();
    }

    @Test
    void ensureDeleteAbsence() {

        final Instant startDate = Instant.now();
        final Instant endDate = Instant.now();

        final AbsenceWriteEntity existingEntity = new AbsenceWriteEntity();
        existingEntity.setTenantId("tenant");
        existingEntity.setUserId("user-id");
        existingEntity.setStartDate(startDate);
        existingEntity.setEndDate(endDate);
        existingEntity.setDayLength(DayLength.FULL);
        existingEntity.setType(AbsenceType.HOLIDAY);
        existingEntity.setColor(AbsenceColor.PINK);
        repository.save(existingEntity);

        final AbsenceWrite absence = new AbsenceWrite(
            new TenantId("tenant"),
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
        existingEntity.setUserId("user-id");
        existingEntity.setStartDate(startDate);
        existingEntity.setEndDate(endDate);
        existingEntity.setDayLength(DayLength.FULL);
        existingEntity.setType(AbsenceType.HOLIDAY);
        existingEntity.setColor(AbsenceColor.PINK);
        repository.save(existingEntity);

        final AbsenceWrite absence = new AbsenceWrite(
            new TenantId("tenant-2"),
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
