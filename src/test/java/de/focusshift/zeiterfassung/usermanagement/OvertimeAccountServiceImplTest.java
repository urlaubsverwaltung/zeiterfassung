package de.focusshift.zeiterfassung.usermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimeAccountServiceImplTest {

    private OvertimeAccountServiceImpl sut;

    @Mock
    private OvertimeAccountRepository repository;

    @BeforeEach
    void setUp() {
        sut = new OvertimeAccountServiceImpl(repository);
    }

    @Test
    void ensureGetOvertimeAccount() {

        final OvertimeAccountEntity entity = new OvertimeAccountEntity();
        entity.setUserId(42L);
        entity.setAllowed(true);
        entity.setMaxAllowedOvertime("PT1337H");

        when(repository.findById(42L)).thenReturn(Optional.of(entity));

        final OvertimeAccount overtimeAccount = sut.getOvertimeAccount(new UserLocalId(42L));

        assertThat(overtimeAccount).isNotNull();
        assertThat(overtimeAccount.getUserLocalId()).isEqualTo(new UserLocalId(42L));
        assertThat(overtimeAccount.isAllowed()).isTrue();
        assertThat(overtimeAccount.getMaxAllowedOvertime()).hasValue(Duration.ofHours(1337));
    }

    @Test
    void ensureGetOvertimeAccountReturnsDefaultWhenNothingExistsInDatabase() {

        when(repository.findById(42L)).thenReturn(Optional.empty());

        final OvertimeAccount overtimeAccount = sut.getOvertimeAccount(new UserLocalId(42L));

        assertThat(overtimeAccount).isNotNull();
        assertThat(overtimeAccount.getUserLocalId()).isEqualTo(new UserLocalId(42L));
        assertThat(overtimeAccount.isAllowed()).isTrue();
        assertThat(overtimeAccount.getMaxAllowedOvertime()).isEmpty();
    }

    @Test
    void ensureUpdateOvertimeAccount() {

        final OvertimeAccountEntity existingEntity = new OvertimeAccountEntity();
        existingEntity.setUserId(42L);
        existingEntity.setAllowed(true);
        existingEntity.setMaxAllowedOvertime("PT1337H");

        when(repository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(repository.save(any())).thenAnswer(returnsFirstArg());

        sut.updateOvertimeAccount(new UserLocalId(42L), false, Duration.ofHours(10));

        final ArgumentCaptor<OvertimeAccountEntity> captor = ArgumentCaptor.forClass(OvertimeAccountEntity.class);
        verify(repository).save(captor.capture());

        final OvertimeAccountEntity actualPersisted = captor.getValue();
        assertThat(actualPersisted).isNotNull();
        assertThat(actualPersisted.getUserId()).isEqualTo(42L);
        assertThat(actualPersisted.isAllowed()).isFalse();
        assertThat(actualPersisted.getMaxAllowedOvertime()).isEqualTo("PT10H");
    }

    @Test
    void ensureUpdateOvertimeAccountWithoutMaxAllowedOvertime() {

        final OvertimeAccountEntity existingEntity = new OvertimeAccountEntity();
        existingEntity.setMaxAllowedOvertime("PT1337H");

        when(repository.findById(42L)).thenReturn(Optional.of(existingEntity));
        when(repository.save(any())).thenAnswer(returnsFirstArg());

        sut.updateOvertimeAccount(new UserLocalId(42L), false, null);

        final ArgumentCaptor<OvertimeAccountEntity> captor = ArgumentCaptor.forClass(OvertimeAccountEntity.class);
        verify(repository).save(captor.capture());

        final OvertimeAccountEntity actualPersisted = captor.getValue();
        assertThat(actualPersisted.getMaxAllowedOvertime()).isNull();
    }

    @Test
    void ensureUpdateOvertimeAccountWhenNothingExistsInDatabaseYet() {

        when(repository.findById(42L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(returnsFirstArg());

        sut.updateOvertimeAccount(new UserLocalId(42L), false, Duration.ofHours(10));

        final ArgumentCaptor<OvertimeAccountEntity> captor = ArgumentCaptor.forClass(OvertimeAccountEntity.class);
        verify(repository).save(captor.capture());

        final OvertimeAccountEntity actualPersisted = captor.getValue();
        assertThat(actualPersisted).isNotNull();
        assertThat(actualPersisted.getUserId()).isEqualTo(42L);
        assertThat(actualPersisted.isAllowed()).isFalse();
        assertThat(actualPersisted.getMaxAllowedOvertime()).isEqualTo("PT10H");
    }
}
