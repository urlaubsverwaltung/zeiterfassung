package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.SPECIALLEAVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceWriteServiceImplTest {

    private AbsenceWriteServiceImpl sut;

    @Mock
    private AbsenceRepository repository;

    @BeforeEach
    void setUp() {
        sut = new AbsenceWriteServiceImpl(repository);
    }

    @Test
    void ensureAddAbsence() {

        final Instant startDate = Instant.now();
        final Instant endDate = Instant.now();

        when(repository.findBySourceId(42L))
            .thenReturn(Optional.empty());

        final AbsenceWrite absence = new AbsenceWrite(
            42L,
            new UserId("user-id"),
            startDate,
            endDate,
            DayLength.FULL,
            Duration.ZERO,
            SPECIALLEAVE
        );

        sut.addAbsence(absence);

        final ArgumentCaptor<AbsenceWriteEntity> captor = ArgumentCaptor.forClass(AbsenceWriteEntity.class);
        verify(repository).save(captor.capture());

        final AbsenceWriteEntity actualPersistedEntity = captor.getValue();
        assertThat(actualPersistedEntity.getTenantId()).isNull();
        assertThat(actualPersistedEntity.getId()).isNull();
        assertThat(actualPersistedEntity.getUserId()).isEqualTo("user-id");
        assertThat(actualPersistedEntity.getStartDate()).isEqualTo(startDate);
        assertThat(actualPersistedEntity.getEndDate()).isEqualTo(endDate);
        assertThat(actualPersistedEntity.getDayLength()).isEqualTo(DayLength.FULL);
        assertThat(actualPersistedEntity.getType().getCategory()).isEqualTo(SPECIALLEAVE);
    }

    @Test
    void ensureAddAbsenceDoesNothingWhenItExistsAlready() {

        final Instant startDate = Instant.now();
        final Instant endDate = Instant.now();

        when(repository.findBySourceId(42L))
            .thenReturn(Optional.of(new AbsenceWriteEntity()));

        final AbsenceWrite absence = new AbsenceWrite(
            42L,
            new UserId("user-id"),
            startDate,
            endDate,
            DayLength.FULL,
            Duration.ZERO,
            SPECIALLEAVE
        );

        sut.addAbsence(absence);

        verifyNoMoreInteractions(repository);
    }
}
