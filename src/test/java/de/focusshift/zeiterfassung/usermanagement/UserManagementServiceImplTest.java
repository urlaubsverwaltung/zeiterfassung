package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.tenancy.user.UserStatus;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    private UserManagementServiceImpl sut;

    @Mock
    private TenantUserService tenantUserService;

    @BeforeEach
    void setUp() {
        sut = new UserManagementServiceImpl(tenantUserService);
    }

    @Test
    void ensureFindUserByIdReturnsEmpty() {

        when(tenantUserService.findById(new UserId("user-id"))).thenReturn(Optional.empty());

        final Optional<User> actual = sut.findUserById(new UserId("user-id"));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindUserById() {
        final Instant now = Instant.now();
        final TenantUser tenantUser = activeTenantUserOne(now);
        final User user = activeUserOne(now);
        final UserId userId = user.userId();

        when(tenantUserService.findById(userId)).thenReturn(Optional.of(tenantUser));

        final Optional<User> actual = sut.findUserById(userId);

        assertThat(actual)
            .isPresent()
            .hasValue(user);
    }

    @Test
    void ensureFindUserByLocalIdReturnsEmpty() {

        when(tenantUserService.findByLocalId(new UserLocalId(42L))).thenReturn(Optional.empty());

        final Optional<User> actual = sut.findUserByLocalId(new UserLocalId(42L));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindUserByLocalId() {

        final Instant now = Instant.now();
        final TenantUser tenantUser = activeTenantUserOne(now);
        final User user = activeUserOne(now);
        final UserLocalId userLocalId = user.userLocalId();

        when(tenantUserService.findByLocalId(userLocalId)).thenReturn(Optional.of(tenantUser));

        final Optional<User> actual = sut.findUserByLocalId(userLocalId);

        assertThat(actual)
            .isPresent()
            .hasValue(user);
    }

    @Test
    void ensureFindAllUsersWithQueryEmptyList() {

        when(tenantUserService.findAllUsers("batman")).thenReturn(List.of());

        final List<User> actual = sut.findAllUsers("batman");
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllUsersWithQuery() {

        Instant now = Instant.now();
        final TenantUser tenantUserOne = activeTenantUserOne(now);
        final User userOne = activeUserOne(now);

        final TenantUser tenantUserTwo = activeTenantUserTwo(now);
        final User userTwo = activeUserTwo(now);

        when(tenantUserService.findAllUsers("batman")).thenReturn(List.of(tenantUserOne, tenantUserTwo));

        final List<User> actual = sut.findAllUsers("batman");
        assertThat(actual).containsExactly(userOne, userTwo);
    }

    @Test
    void ensureFindAllByIdsReturnsEmpty() {

        final UserId id = new UserId("user-id");

        when(tenantUserService.findAllUsersById(List.of(id))).thenReturn(List.of());

        final List<User> actual = sut.findAllUsersByIds(List.of(id));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllByIds() {

        final Instant now = Instant.now();
        final TenantUser tenantUserOne = activeTenantUserOne(now);
        final User userOne = activeUserOne(now);
        final TenantUser tenantUserTwo = activeTenantUserOne(now);
        final User userTwo = activeUserOne(now);

        when(tenantUserService.findAllUsersById(List.of(userOne.userId(), userTwo.userId()))).thenReturn(List.of(tenantUserOne, tenantUserTwo));

        final List<User> actual = sut.findAllUsersByIds(List.of(userOne.userId(), userTwo.userId()));
        assertThat(actual).containsExactly(userOne, userTwo);
    }

    @Test
    void ensureFindAllByLocalIdsReturnsEmpty() {

        final UserLocalId localId = new UserLocalId(42L);

        when(tenantUserService.findAllUsersByLocalId(List.of(localId))).thenReturn(List.of());

        final List<User> actual = sut.findAllUsersByLocalIds(List.of(localId));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllByLocalIds() {

        final Instant now = Instant.now();
        final TenantUser tenantUserOne = activeTenantUserOne(now);
        final User userOne = activeUserOne(now);
        final TenantUser tenantUserTwo = activeTenantUserOne(now);
        final User userTwo = activeUserOne(now);

        when(tenantUserService.findAllUsersByLocalId(List.of(userOne.userLocalId(), userTwo.userLocalId()))).thenReturn(List.of(tenantUserOne, tenantUserTwo));

        final List<User> actual = sut.findAllUsersByLocalIds(List.of(userOne.userLocalId(), userTwo.userLocalId()));
        assertThat(actual).containsExactly(userOne, userTwo);
    }

    @Test
    void ensureUpdateUserPermissionsThrowsUserNotFoundException() {

        final UserLocalId userLocalId = new UserLocalId(42L);

        when(tenantUserService.findByLocalId(userLocalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateUserPermissions(userLocalId, Set.of()))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void ensureUpdateUserPermissions() throws Exception {

        TenantUser existing = activeTenantUserOne(Instant.now());
        UserLocalId localId = new UserLocalId(existing.localId());
        when(tenantUserService.findByLocalId(localId)).thenReturn(Optional.of(existing));

        final TenantUser expectedUpdatedTenant = new TenantUser(
            existing.id(),
            existing.localId(),
            existing.givenName(),
            existing.familyName(),
            existing.eMail(),
            existing.firstLoginAt(),
            Set.of(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL),
            existing.createdAt(),
            existing.updatedAt(),
            existing.deactivatedAt(),
            existing.deletedAt(),
            existing.status()
        );

        when(tenantUserService.updateUser(expectedUpdatedTenant)).thenAnswer(returnsFirstArg());

        final User actualUpdatedUser = sut.updateUserPermissions(localId, Set.of(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL));
        assertThat(actualUpdatedUser.authorities()).isEqualTo(Set.of(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL));
    }

    private static TenantUser activeTenantUserOne(Instant now) {
        return new TenantUser("my-external-id-1", 1L, "batman", "batman", new EMailAddress("batman@batman.com"), now, Set.of(), now, now, null, null, UserStatus.ACTIVE);
    }

    private static User activeUserOne(Instant now) {
        return fromTenantUser(activeTenantUserOne(now));
    }

    private static TenantUser activeTenantUserTwo(Instant now) {
        return new TenantUser("my-external-id-2", 2L, "petra", "panter", new EMailAddress("petra@panter.com"), now, Set.of(), now, now, null, null, UserStatus.ACTIVE);
    }

    private static User activeUserTwo(Instant now) {
        return fromTenantUser(activeTenantUserTwo(now));
    }

    private static User fromTenantUser(TenantUser input) {
        return new User(new UserIdComposite(new UserId(input.id()), new UserLocalId(input.localId())), input.givenName(), input.familyName(), input.eMail(), input.authorities());
    }

}
