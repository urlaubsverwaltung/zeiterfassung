package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserEntity;
import de.focusshift.zeiterfassung.tenancy.user.UserStatus;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        assertThat(overtimeAccount.userIdComposite()).isEqualTo(userIdComposite);
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
        ensureIsDefaultOvertimeAccount(overtimeAccount, userIdComposite);
    }

    @Test
    void ensureGetAllOvertimeAccountsReturnsDefaultWhenNothingExistsInDatabase() {

        final UserId userId = new UserId("uuid-1");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());
        when(userManagementService.findAllUsers()).thenReturn(List.of(user));

        final Map<UserIdComposite, OvertimeAccount> actual = sut.getAllOvertimeAccounts();
        assertThat(actual.get(userIdComposite)).satisfies(overtimeAccount -> {
            ensureIsDefaultOvertimeAccount(overtimeAccount, userIdComposite);
        });
    }

    @Test
    void ensureGetAllOvertimeAccounts() {

        final UserId userId1 = new UserId("uuid-1");
        final UserLocalId userLocalId1 = new UserLocalId(1L);
        final UserIdComposite userIdComposite1 = new UserIdComposite(userId1, userLocalId1);
        final User user1 = new User(userIdComposite1, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        final UserId userId2 = new UserId("uuid-2");
        final UserLocalId userLocalId2 = new UserLocalId(2L);
        final UserIdComposite userIdComposite2 = new UserIdComposite(userId2, userLocalId2);
        final User user2 = new User(userIdComposite2, "Dick", "Johnson", new EMailAddress(""), Set.of());

        when(userManagementService.findAllUsers()).thenReturn(List.of(user1, user2));

        final OvertimeAccountEntity entity1 = new OvertimeAccountEntity();
        entity1.setUserId(userLocalId1.value());
        entity1.setUser(anyTenantUserEntity(1L, userIdComposite1));
        entity1.setAllowed(true);
        entity1.setMaxAllowedOvertime("PT1337H");

        final OvertimeAccountEntity entity2 = new OvertimeAccountEntity();
        entity2.setUserId(userLocalId2.value());
        entity2.setUser(anyTenantUserEntity(2L, userIdComposite2));
        entity2.setAllowed(false);

        when(repository.findAll()).thenReturn(List.of(entity1, entity2));

        final Map<UserIdComposite, OvertimeAccount> actual = sut.getAllOvertimeAccounts();
        assertThat(actual).hasSize(2);
        assertThat(actual.get(userIdComposite1)).satisfies(overtimeAccount1 -> {
            assertThat(overtimeAccount1).isNotNull();
            assertThat(overtimeAccount1.userIdComposite()).isEqualTo(userIdComposite1);
            assertThat(overtimeAccount1.isAllowed()).isTrue();
            assertThat(overtimeAccount1.getMaxAllowedOvertime()).hasValue(Duration.ofHours(1337));
        });
        assertThat(actual.get(userIdComposite2)).satisfies(overtimeAccount2 -> {
            assertThat(overtimeAccount2).isNotNull();
            assertThat(overtimeAccount2.userIdComposite()).isEqualTo(userIdComposite2);
            assertThat(overtimeAccount2.isAllowed()).isFalse();
            assertThat(overtimeAccount2.getMaxAllowedOvertime()).isEmpty();
        });
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

        assertThat(actualUpdatedOvertimeAccount.userIdComposite()).isEqualTo(userIdComposite);

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

        assertThat(actualUpdatedOvertimeAccount.userIdComposite()).isEqualTo(userIdComposite);

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

        assertThat(actualUpdatedOvertime.userIdComposite()).isEqualTo(userIdComposite);

        final ArgumentCaptor<OvertimeAccountEntity> captor = ArgumentCaptor.forClass(OvertimeAccountEntity.class);
        verify(repository).save(captor.capture());

        final OvertimeAccountEntity actualPersisted = captor.getValue();
        assertThat(actualPersisted).isNotNull();
        assertThat(actualPersisted.getUserId()).isEqualTo(1L);
        assertThat(actualPersisted.isAllowed()).isFalse();
        assertThat(actualPersisted.getMaxAllowedOvertime()).isEqualTo("PT10H");
    }

    void ensureIsDefaultOvertimeAccount(OvertimeAccount overtimeAccount, UserIdComposite userIdComposite) {
        assertThat(overtimeAccount).isNotNull();
        assertThat(overtimeAccount.userIdComposite()).isEqualTo(userIdComposite);
        assertThat(overtimeAccount.isAllowed()).isTrue();
        assertThat(overtimeAccount.getMaxAllowedOvertime()).isEmpty();
    }

    private static TenantUserEntity anyTenantUserEntity(Long id, UserIdComposite userIdComposite) {
        return new TenantUserEntity(id, userIdComposite.id().value(), Instant.MIN, Instant.now(), "", "", "", Set.of(), Instant.MIN, Instant.MIN, null, null, UserStatus.ACTIVE);
    }
}
