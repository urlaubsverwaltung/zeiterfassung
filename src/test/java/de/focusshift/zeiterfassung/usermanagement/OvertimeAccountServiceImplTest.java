package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

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
    @Mock
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        sut = new OvertimeAccountServiceImpl(repository, userManagementService);
    }

    @Test
    void ensureGetOvertimeAccount() {

        final UserId userId = new UserId("uuid-1");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user_1 = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user_1));

        final OvertimeAccountEntity entity = new OvertimeAccountEntity();
        entity.setUserId(userLocalId.value());
        entity.setAllowed(true);
        entity.setMaxAllowedOvertime("PT1337H");

        when(repository.findById(userLocalId.value())).thenReturn(Optional.of(entity));

        final OvertimeAccount overtimeAccount = sut.getOvertimeAccount(userLocalId);

        assertThat(overtimeAccount).isNotNull();
        assertThat(overtimeAccount.getUserIdComposite()).isEqualTo(userIdComposite);
        assertThat(overtimeAccount.isAllowed()).isTrue();
        assertThat(overtimeAccount.getMaxAllowedOvertime()).hasValue(Duration.ofHours(1337));
    }

    @Test
    void ensureGetOvertimeAccountReturnsDefaultWhenNothingExistsInDatabase() {

        final UserId userId = new UserId("uuid-1");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user_1 = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user_1));

        when(repository.findById(userLocalId.value())).thenReturn(Optional.empty());

        final OvertimeAccount overtimeAccount = sut.getOvertimeAccount(userLocalId);

        assertThat(overtimeAccount).isNotNull();
        assertThat(overtimeAccount.getUserIdComposite()).isEqualTo(userIdComposite);
        assertThat(overtimeAccount.isAllowed()).isTrue();
        assertThat(overtimeAccount.getMaxAllowedOvertime()).isEmpty();
    }

    @Test
    void ensureUpdateOvertimeAccount() {

        final UserId userId = new UserId("uuid-1");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user_1 = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user_1));

        final OvertimeAccountEntity existingEntity = new OvertimeAccountEntity();
        existingEntity.setUserId(userLocalId.value());
        existingEntity.setAllowed(true);
        existingEntity.setMaxAllowedOvertime("PT1337H");

        when(repository.findById(userLocalId.value())).thenReturn(Optional.of(existingEntity));
        when(repository.save(any())).thenAnswer(returnsFirstArg());

        final OvertimeAccount actualUpdatedOvertimeAccount = sut.updateOvertimeAccount(userLocalId, false, Duration.ofHours(10));

        assertThat(actualUpdatedOvertimeAccount.getUserIdComposite()).isEqualTo(userIdComposite);

        final ArgumentCaptor<OvertimeAccountEntity> captor = ArgumentCaptor.forClass(OvertimeAccountEntity.class);
        verify(repository).save(captor.capture());

        final OvertimeAccountEntity actualPersisted = captor.getValue();
        assertThat(actualPersisted).isNotNull();
        assertThat(actualPersisted.getUserId()).isEqualTo(1L);
        assertThat(actualPersisted.isAllowed()).isFalse();
        assertThat(actualPersisted.getMaxAllowedOvertime()).isEqualTo("PT10H");
    }

    @Test
    void ensureUpdateOvertimeAccountWithoutMaxAllowedOvertime() {

        final UserId userId = new UserId("uuid-1");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user_1 = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user_1));

        final OvertimeAccountEntity existingEntity = new OvertimeAccountEntity();
        existingEntity.setMaxAllowedOvertime("PT1337H");

        when(repository.findById(userLocalId.value())).thenReturn(Optional.of(existingEntity));
        when(repository.save(any())).thenAnswer(returnsFirstArg());

        final OvertimeAccount actualUpdatedOvertimeAccount = sut.updateOvertimeAccount(userLocalId, false, null);

        assertThat(actualUpdatedOvertimeAccount.getUserIdComposite()).isEqualTo(userIdComposite);

        final ArgumentCaptor<OvertimeAccountEntity> captor = ArgumentCaptor.forClass(OvertimeAccountEntity.class);
        verify(repository).save(captor.capture());

        final OvertimeAccountEntity actualPersisted = captor.getValue();
        assertThat(actualPersisted.getMaxAllowedOvertime()).isNull();
    }

    @Test
    void ensureUpdateOvertimeAccountWhenNothingExistsInDatabaseYet() {

        final UserId userId = new UserId("uuid-1");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user_1 = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user_1));

        when(repository.findById(userLocalId.value())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(returnsFirstArg());

        final OvertimeAccount actualUpdatedOvertime = sut.updateOvertimeAccount(userLocalId, false, Duration.ofHours(10));

        assertThat(actualUpdatedOvertime.getUserIdComposite()).isEqualTo(userIdComposite);

        final ArgumentCaptor<OvertimeAccountEntity> captor = ArgumentCaptor.forClass(OvertimeAccountEntity.class);
        verify(repository).save(captor.capture());

        final OvertimeAccountEntity actualPersisted = captor.getValue();
        assertThat(actualPersisted).isNotNull();
        assertThat(actualPersisted.getUserId()).isEqualTo(1L);
        assertThat(actualPersisted.isAllowed()).isFalse();
        assertThat(actualPersisted.getMaxAllowedOvertime()).isEqualTo("PT10H");
    }
}
