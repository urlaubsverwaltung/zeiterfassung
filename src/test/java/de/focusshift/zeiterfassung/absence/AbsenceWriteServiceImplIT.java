package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    @MockitoBean
    private TenantContextHolder tenantContextHolder;

    @Test
    void ensureUpdateAbsenceUpdatesChangedAbsenceType() {

        final Instant startDate = Instant.now();
        final Instant endDate = Instant.now();
        final UserId userId = new UserId("user-id");

        when(tenantContextHolder.getCurrentTenantId()).thenReturn(Optional.of(new TenantId("tenant")));

        sut.addAbsence(new AbsenceWrite(42L, userId, startDate, endDate, DayLength.FULL, null, HOLIDAY, new AbsenceTypeSourceId(1L)));
        sut.updateAbsence(new AbsenceWrite(42L, userId, startDate, endDate, DayLength.FULL, null, AbsenceTypeCategory.OTHER, new AbsenceTypeSourceId(2L)));

        final Optional<AbsenceWriteEntity> actual = repository.findBySourceId(42L);
        assertThat(actual).hasValueSatisfying(actualAbsence -> {
            assertThat(actualAbsence.getSourceId()).isEqualTo(42L);
            assertThat(actualAbsence.getUserId()).isEqualTo("user-id");
            assertThat(actualAbsence.getDayLength()).isEqualTo(DayLength.FULL);
            assertThat(actualAbsence.getStartDate()).isEqualTo(startDate);
            assertThat(actualAbsence.getEndDate()).isEqualTo(endDate);
            assertThat(actualAbsence.getType().getSourceId()).isEqualTo(2L);
            assertThat(actualAbsence.getType().getCategory()).isEqualTo(AbsenceTypeCategory.OTHER);
        });
    }

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
            Duration.ZERO,
            HOLIDAY
        );

        assertThat(repository.findAll()).hasSize(1);


        sut.deleteAbsence(absence);

        assertThat(repository.findAll()).isEmpty();
        verify(tenantContextHolder, times(2)).getCurrentTenantId();
    }

}
