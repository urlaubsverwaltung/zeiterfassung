package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class AbsenceWriteServiceImplIT extends SingleTenantTestContainersBase {

    @Autowired
    private AbsenceWriteServiceImpl sut;

    @Autowired
    private AbsenceRepository repository;

    @MockBean
    private TenantContextHolder tenantContextHolder;

    @Test
    void ensureDeleteAbsence() {

        final Instant startDate = Instant.now();
        final Instant endDate = Instant.now();

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        final AbsenceWriteEntity existingEntity = new AbsenceWriteEntity();
        existingEntity.setSourceId(42L);
        existingEntity.setUserId("user-id");
        existingEntity.setStartDate(startDate);
        existingEntity.setEndDate(endDate);
        existingEntity.setDayLength(DayLength.FULL);
        existingEntity.setType(new AbsenceTypeEntityEmbeddable(HOLIDAY, 1000L));
        repository.save(existingEntity);

        final AbsenceWrite absence = new AbsenceWrite(
            42L,
            new UserId("user-id"),
            startDate,
            endDate,
            DayLength.FULL,
            HOLIDAY
        );

        assertThat(repository.findAll()).hasSize(1);


        sut.deleteAbsence(absence);

        assertThat(repository.findAll()).isEmpty();
        verify(tenantContextHolder, times(2)).getCurrentTenantId();
    }

}
