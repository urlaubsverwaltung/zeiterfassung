package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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

        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final TenantUser tenantUser = new TenantUser(userId.value(), userLocalId.value(), "givenName", "familyName", email, Set.of());
        final User user = new User(userIdComposite, "givenName", "familyName", email, Set.of());

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

        final UserId userId = new UserId("user-id");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final TenantUser tenantUser = new TenantUser(userId.value(), userLocalId.value(), "givenName", "familyName", email, Set.of());
        final User user = new User(userIdComposite, "givenName", "familyName", email, Set.of());

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

        final UserId userId_1 = new UserId("user-id");
        final UserLocalId userLocalId_1 = new UserLocalId(42L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final TenantUser tenantUser = new TenantUser(userId_1.value(), userLocalId_1.value(), "givenName", "familyName", email, Set.of());
        final User user = new User(userIdComposite_1, "givenName", "familyName", email, Set.of());

        final UserId userId_2 = new UserId("user-id-2");
        final UserLocalId userLocalId_2 = new UserLocalId(1337L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final EMailAddress email2 = new EMailAddress("mail-2@example.org");

        final TenantUser tenantUser2 = new TenantUser(userId_2.value(), userLocalId_2.value(), "givenName-2", "familyName-2", email2, Set.of());
        final User user2 = new User(userIdComposite_2, "givenName-2", "familyName-2", email2, Set.of());

        when(tenantUserService.findAllUsers("batman")).thenReturn(List.of(tenantUser, tenantUser2));

        final List<User> actual = sut.findAllUsers("batman");
        assertThat(actual).containsExactly(user, user2);
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

        final UserId userId_1 = new UserId("user-id");
        final UserLocalId userLocalId_1 = new UserLocalId(42L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final TenantUser tenantUser = new TenantUser(userId_1.value(), userLocalId_1.value(), "givenName", "familyName", email, Set.of());
        final User user = new User(userIdComposite_1, "givenName", "familyName", email, Set.of());

        final UserId userId_2 = new UserId("user-id-2");
        final UserLocalId userLocalId_2 = new UserLocalId(1337L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final EMailAddress email2 = new EMailAddress("mail-2@example.org");

        final TenantUser tenantUser2 = new TenantUser(userId_2.value(), userLocalId_2.value(), "givenName-2", "familyName-2", email2, Set.of());
        final User user2 = new User(userIdComposite_2, "givenName-2", "familyName-2", email2, Set.of());

        when(tenantUserService.findAllUsersById(List.of(userId_1, userId_2))).thenReturn(List.of(tenantUser, tenantUser2));

        final List<User> actual = sut.findAllUsersByIds(List.of(userId_1, userId_2));
        assertThat(actual).containsExactly(user, user2);
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

        final UserId userId_1 = new UserId("user-id");
        final UserLocalId userLocalId_1 = new UserLocalId(42L);
        final UserIdComposite userIdComposite_1 = new UserIdComposite(userId_1, userLocalId_1);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final TenantUser tenantUser = new TenantUser(userId_1.value(), userLocalId_1.value(), "givenName", "familyName", email, Set.of());
        final User user = new User(userIdComposite_1, "givenName", "familyName", email, Set.of());

        final UserId userId_2 = new UserId("user-id-2");
        final UserLocalId userLocalId_2 = new UserLocalId(1337L);
        final UserIdComposite userIdComposite_2 = new UserIdComposite(userId_2, userLocalId_2);
        final EMailAddress email2 = new EMailAddress("mail-2@example.org");

        final TenantUser tenantUser2 = new TenantUser(userId_2.value(), userLocalId_2.value(), "givenName-2", "familyName-2", email2, Set.of());
        final User user2 = new User(userIdComposite_2, "givenName-2", "familyName-2", email2, Set.of());

        when(tenantUserService.findAllUsersByLocalId(List.of(userLocalId_1, userLocalId_2))).thenReturn(List.of(tenantUser, tenantUser2));

        final List<User> actual = sut.findAllUsersByLocalIds((List.of(userLocalId_1, userLocalId_2)));
        assertThat(actual).containsExactly(user, user2);
    }
}
