package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantUserServiceImplTest {

    private TenantUserServiceImpl sut;

    @Mock
    private TenantUserRepository repository;

    @Mock
    private ApplicationEventPublisher publisher;

    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    @BeforeEach
    void setUp() {
        sut = new TenantUserServiceImpl(repository, publisher, clock);
    }

    @Test
    void ensureGetUserByUuidReturnsEmpty() {

        final UUID uuid = UUID.randomUUID();

        when(repository.findByUuid(uuid.toString())).thenReturn(Optional.empty());

        final Optional<TenantUser> actual = sut.getUserByUuid(uuid);
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureGetUserByUuid() {

        final UUID uuid = UUID.randomUUID();

        final UserId id = new UserId(uuid.toString());
        final UserLocalId localId = new UserLocalId(1337L);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final Instant firstLoginAt = clock.instant();
        final TenantUserEntity entity = new TenantUserEntity(localId.value(), id.value(), firstLoginAt, clock.instant(), "givenName", "familyName", email.value(), Set.of());
        final TenantUser tenantUser = new TenantUser(id.value(), localId.value(), "givenName", "familyName", email, firstLoginAt, Set.of());

        when(repository.findByUuid(uuid.toString())).thenReturn(Optional.of(entity));

        final Optional<TenantUser> actual = sut.getUserByUuid(uuid);

        assertThat(actual)
            .isPresent()
            .hasValue(tenantUser);
    }

    @Test
    void ensureFindAllUsersWithQueryReturnsEmpty() {

        when(repository.findAllByGivenNameContainingIgnoreCaseOrFamilyNameContainingIgnoreCase("batman", "batman"))
            .thenReturn(List.of());

        final List<TenantUser> actual = sut.findAllUsers("batman");
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllUsersWithQuery() {

        final UUID uuid = UUID.randomUUID();

        final UserId id = new UserId(uuid.toString());
        final UserLocalId localId = new UserLocalId(1337L);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final Instant firstLoginAt = clock.instant();
        final TenantUserEntity entity = new TenantUserEntity(localId.value(), id.value(), firstLoginAt, clock.instant(), "givenName", "familyName", email.value(), Set.of());
        final TenantUser tenantUser = new TenantUser(id.value(), localId.value(), "givenName", "familyName", email, firstLoginAt, Set.of());

        when(repository.findAllByGivenNameContainingIgnoreCaseOrFamilyNameContainingIgnoreCase("batman", "batman"))
            .thenReturn(List.of(entity));

        final List<TenantUser> actual = sut.findAllUsers("batman");
        assertThat(actual).containsExactly(tenantUser);
    }

    @Test
    void ensureFindAllUsersByIdReturnsEmpty() {

        when(repository.findAllByUuidIsIn(List.of("uuid"))).thenReturn(List.of());

        final List<TenantUser> actual = sut.findAllUsersById(List.of(new UserId("uuid")));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllUsersById() {

        final UUID uuid = UUID.randomUUID();

        final UserId id = new UserId(uuid.toString());
        final UserLocalId localId = new UserLocalId(1337L);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final Instant firstLoginAt = clock.instant();
        final TenantUserEntity entity = new TenantUserEntity(localId.value(), id.value(), firstLoginAt, clock.instant(), "givenName", "familyName", email.value(), Set.of());
        final TenantUser tenantUser = new TenantUser(id.value(), localId.value(), "givenName", "familyName", email, firstLoginAt, Set.of());

        final UUID uuid2 = UUID.randomUUID();

        final UserId id2 = new UserId(uuid2.toString());
        final UserLocalId localId2 = new UserLocalId(42L);
        final EMailAddress email2 = new EMailAddress("mail@example.org");

        final Instant firstLoginAt2 = clock.instant();
        final TenantUserEntity entity2 = new TenantUserEntity(localId2.value(), id2.value(), firstLoginAt2, clock.instant(), "givenName-2", "familyName-2", email2.value(), Set.of());
        final TenantUser tenantUser2 = new TenantUser(id2.value(), localId2.value(), "givenName-2", "familyName-2", email2, firstLoginAt2, Set.of());

        when(repository.findAllByUuidIsIn(List.of(uuid.toString(), uuid2.toString()))).thenReturn(List.of(entity, entity2));

        final List<TenantUser> actual = sut.findAllUsersById(List.of(id, id2));
        assertThat(actual).containsExactly(tenantUser, tenantUser2);
    }

    @Test
    void ensureFindAllUsersByLocalIdReturnsEmpty() {

        when(repository.findAllByIdIsIn(List.of(42L))).thenReturn(List.of());

        final List<TenantUser> actual = sut.findAllUsersByLocalId(List.of(new UserLocalId(42L)));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllUsersByLocalId() {

        final UUID uuid = UUID.randomUUID();

        final UserId id = new UserId(uuid.toString());
        final UserLocalId localId = new UserLocalId(1337L);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final Instant firstLoginAt = clock.instant();
        final TenantUserEntity entity = new TenantUserEntity(localId.value(), id.value(), firstLoginAt, clock.instant(), "givenName", "familyName", email.value(), Set.of());
        final TenantUser tenantUser = new TenantUser(id.value(), localId.value(), "givenName", "familyName", email, firstLoginAt, Set.of());

        final UUID uuid2 = UUID.randomUUID();

        final UserId id2 = new UserId(uuid2.toString());
        final UserLocalId localId2 = new UserLocalId(42L);
        final EMailAddress email2 = new EMailAddress("mail@example.org");

        final Instant firstLoginAt2 = clock.instant();
        final TenantUserEntity entity2 = new TenantUserEntity(localId2.value(), id2.value(), firstLoginAt2, clock.instant(), "givenName-2", "familyName-2", email2.value(), Set.of());
        final TenantUser tenantUser2 = new TenantUser(id2.value(), localId2.value(), "givenName-2", "familyName-2", email2, firstLoginAt2, Set.of());

        when(repository.findAllByIdIsIn(List.of(localId.value(), localId2.value()))).thenReturn(List.of(entity, entity2));

        final List<TenantUser> actual = sut.findAllUsersByLocalId(List.of(localId, localId2));
        assertThat(actual).containsExactly(tenantUser, tenantUser2);
    }

    @Test
    void ensureFindByIdReturnsEmpty() {

        when(repository.findByUuid("uuid")).thenReturn(Optional.empty());

        final Optional<TenantUser> actual = sut.findById(new UserId("uuid"));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindById() {

        final UUID uuid = UUID.randomUUID();

        final UserId id = new UserId(uuid.toString());
        final UserLocalId localId = new UserLocalId(1337L);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final Instant firstLoginAt = clock.instant();
        final TenantUserEntity entity = new TenantUserEntity(localId.value(), id.value(), firstLoginAt, clock.instant(), "givenName", "familyName", email.value(), Set.of());
        final TenantUser tenantUser = new TenantUser(id.value(), localId.value(), "givenName", "familyName", email, firstLoginAt, Set.of());

        when(repository.findByUuid(uuid.toString())).thenReturn(Optional.of(entity));

        final Optional<TenantUser> actual = sut.findById(id);

        assertThat(actual)
            .isPresent()
            .hasValue(tenantUser);
    }

    @Test
    void ensureFindByLocalIdReturnsEmpty() {

        when(repository.findById(42L)).thenReturn(Optional.empty());

        final Optional<TenantUser> actual = sut.findByLocalId(new UserLocalId(42L));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindByLocalId() {

        final UUID uuid = UUID.randomUUID();

        final UserId id = new UserId(uuid.toString());
        final UserLocalId localId = new UserLocalId(1337L);
        final EMailAddress email = new EMailAddress("mail@example.org");

        final Instant firstLoginAt = clock.instant();
        final TenantUserEntity entity = new TenantUserEntity(localId.value(), id.value(), firstLoginAt, clock.instant(), "givenName", "familyName", email.value(), Set.of());
        final TenantUser tenantUser = new TenantUser(id.value(), localId.value(), "givenName", "familyName", email, firstLoginAt, Set.of());

        when(repository.findById(localId.value())).thenReturn(Optional.of(entity));

        final Optional<TenantUser> actual = sut.findByLocalId(localId);
        assertThat(actual)
            .isPresent()
            .hasValue(tenantUser);
    }

    @Test
    void ensureDelete() {
        sut.deleteUser(42L);
        verify(repository).deleteById(42L);
    }
}
