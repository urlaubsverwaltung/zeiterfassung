package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantUserServiceImplTest {

    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

    private TenantUserServiceImpl sut;

    @Mock
    private TenantUserRepository repository;
    @Mock
    private ApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        sut = new TenantUserServiceImpl(repository, publisher, clock);
    }

    @Test
    void ensureFindAllUsersWithQueryReturnsEmpty() {

        when(repository.findAllByNiceNameContainingIgnoreCaseOrderByGivenNameAscFamilyNameAsc("batman"))
            .thenReturn(List.of());

        final List<TenantUser> actual = sut.findAllUsers("batman");
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllUsersWithQuery() {

        final Instant now = clock.instant();
        final TenantUserEntity entity = activeUserEntityOne(now);
        final TenantUser tenantUser = activeTenantUserOne(now);

        when(repository.findAllByNiceNameContainingIgnoreCaseOrderByGivenNameAscFamilyNameAsc("batman"))
            .thenReturn(List.of(entity));

        final List<TenantUser> actual = sut.findAllUsers("batman");
        assertThat(actual).containsExactly(tenantUser);
    }

    @Test
    void ensureFindAllUsersByIdReturnsEmpty() {

        when(repository.findAllByUuidIsInOrderByGivenNameAscFamilyNameAsc(List.of("uuid"))).thenReturn(List.of());

        final List<TenantUser> actual = sut.findAllUsersById(List.of(new UserId("uuid")));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllUsersById() {

        final Instant now = clock.instant();
        final TenantUserEntity entityOne = activeUserEntityOne(now);
        final TenantUser tenantUserOne = activeTenantUserOne(now);

        final TenantUserEntity entityTwo = activeUserEntityTwo(now);
        final TenantUser tenantUserTwo = activeTenantUserTwo(now);

        final String idOne = tenantUserOne.id();
        final String idTwo = tenantUserTwo.id();
        when(repository.findAllByUuidIsInOrderByGivenNameAscFamilyNameAsc(List.of(idOne, idTwo))).thenReturn(List.of(entityOne, entityTwo));

        final List<TenantUser> actual = sut.findAllUsersById(List.of(new UserId(idOne), new UserId(idTwo)));
        assertThat(actual).containsExactly(tenantUserOne, tenantUserTwo);
    }

    @Test
    void ensureFindAllUsersByLocalIdReturnsEmpty() {

        when(repository.findAllByIdIsInOrderByGivenNameAscFamilyNameAsc(List.of(42L))).thenReturn(List.of());

        final List<TenantUser> actual = sut.findAllUsersByLocalId(List.of(new UserLocalId(42L)));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindAllUsersByLocalId() {

        final Instant now = clock.instant();
        final TenantUserEntity entityOne = activeUserEntityOne(now);
        final TenantUser tenantUserOne = activeTenantUserOne(now);

        final TenantUserEntity entityTwo = activeUserEntityTwo(now);
        final TenantUser tenantUserTwo = activeTenantUserTwo(now);

        final Long localIdOne = tenantUserOne.localId();
        final Long localIdTwo = tenantUserTwo.localId();
        when(repository.findAllByIdIsInOrderByGivenNameAscFamilyNameAsc(List.of(localIdOne, localIdTwo))).thenReturn(List.of(entityOne, entityTwo));

        final List<TenantUser> actual = sut.findAllUsersByLocalId(List.of(new UserLocalId(localIdOne), new UserLocalId(localIdTwo)));
        assertThat(actual).containsExactly(tenantUserOne, tenantUserTwo);
    }

    @Test
    void ensureFindByIdReturnsEmpty() {

        when(repository.findByUuid("uuid")).thenReturn(Optional.empty());

        final Optional<TenantUser> actual = sut.findById(new UserId("uuid"));
        assertThat(actual).isEmpty();
    }

    @Test
    void ensureFindById() {

        final Instant now = clock.instant();
        final TenantUserEntity entity = activeUserEntityOne(now);
        final TenantUser tenantUser = activeTenantUserOne(now);

        final String externalId = tenantUser.id();

        when(repository.findByUuid(externalId)).thenReturn(Optional.of(entity));

        final Optional<TenantUser> actual = sut.findById(new UserId(externalId));

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

        final Instant now = clock.instant();
        final TenantUserEntity entity = activeUserEntityOne(now);
        final TenantUser tenantUser = activeTenantUserOne(now);

        when(repository.findById(tenantUser.localId())).thenReturn(Optional.of(entity));

        final Optional<TenantUser> actual = sut.findByLocalId(new UserLocalId(tenantUser.localId()));
        assertThat(actual)
            .isPresent()
            .hasValue(tenantUser);
    }

    @Nested
    class UserLifecycle {

        @Nested
        class CreateUser {

            @Test
            void newUserIsCreatedSuccessfully() {
                final String uuid = "my-external-id";
                final String givenName = "givenName";
                final String familyName = "familyName";
                final EMailAddress eMailAddress = new EMailAddress("email@example.com");
                final Set<SecurityRole> authorities = Set.of(SecurityRole.ZEITERFASSUNG_USER);
                final Instant now = clock.instant();

                when(repository.save(any(TenantUserEntity.class))).thenAnswer(returnsFirstArg());

                final TenantUser result = sut.createNewUser(uuid, givenName, familyName, eMailAddress, authorities);

                ArgumentCaptor<TenantUserEntity> entityArgumentCaptor = ArgumentCaptor.forClass(TenantUserEntity.class);

                verify(repository).save(entityArgumentCaptor.capture());

                assertThat(entityArgumentCaptor.getValue()).satisfies(entity -> {
                    assertThat(entity.getId()).isNull();
                    assertThat(entity.getUuid()).isEqualTo(uuid);
                    assertThat(entity.getGivenName()).isEqualTo(givenName);
                    assertThat(entity.getFamilyName()).isEqualTo(familyName);
                    assertThat(entity.getEmail()).isEqualTo(eMailAddress.value());
                    assertThat(entity.getAuthorities()).isEqualTo(authorities);
                    assertThat(entity.getCreatedAt()).isEqualTo(now);
                    assertThat(entity.getUpdatedAt()).isEqualTo(now);
                    assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
                });

                assertThat(result).satisfies(user -> {
                    assertThat(user.id()).isEqualTo(uuid);
                    assertThat(user.givenName()).isEqualTo(givenName);
                    assertThat(user.familyName()).isEqualTo(familyName);
                    assertThat(user.eMail()).isEqualTo(eMailAddress);
                    assertThat(user.firstLoginAt()).isEqualTo(now);
                    assertThat(user.authorities()).isEqualTo(authorities);
                    assertThat(user.createdAt()).isEqualTo(now);
                    assertThat(user.updatedAt()).isEqualTo(now);
                    assertThat(user.deactivatedAt()).isNull();
                    assertThat(user.deletedAt()).isNull();
                    assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
                });

                verify(publisher).publishEvent(any(TenantUserCreatedEvent.class));
            }

            @Test
            void ensureGivenNameAndFamilyNameIsStripped() {

                final String uuid = "my-external-id";
                final String givenName = " Hans Joachim  ";
                final String familyName = "  Sonnencreme ";
                final EMailAddress eMailAddress = new EMailAddress("hans@example.com");

                when(repository.save(any(TenantUserEntity.class))).thenAnswer(returnsFirstArg());

                final TenantUser result = sut.createNewUser(uuid, givenName, familyName, eMailAddress, Set.of());

                final ArgumentCaptor<TenantUserEntity> entityArgumentCaptor = ArgumentCaptor.forClass(TenantUserEntity.class);
                verify(repository).save(entityArgumentCaptor.capture());

                assertThat(entityArgumentCaptor.getValue()).satisfies(entity -> {
                    assertThat(entity.getGivenName()).isEqualTo("Hans Joachim");
                    assertThat(entity.getFamilyName()).isEqualTo("Sonnencreme");
                });

                assertThat(result.givenName()).isEqualTo("Hans Joachim");
                assertThat(result.familyName()).isEqualTo("Sonnencreme");
            }
        }

        @Nested
        class UpdateUser {

            private static TenantUser updateFromEntity(TenantUserEntity currentEntity, String givenName, String familyName, String mail) {
                return new TenantUser(currentEntity.getUuid(), currentEntity.getId(), givenName, familyName, new EMailAddress(mail), Instant.now(), currentEntity.getAuthorities(), Instant.now(), Instant.now(), currentEntity.getDeactivatedAt(), currentEntity.getDeletedAt(), currentEntity.getStatus());
            }

            @Test
            void updateUserSuccessfully() {
                final Instant now = clock.instant();

                final TenantUserEntity currentEntity = activeUserEntityOne(now);
                final TenantUser update = updateFromEntity(currentEntity, "updatedGivenName", "updatedFamilyName", "updatedEmail@example.com");

                when(repository.findById(any())).thenReturn(Optional.of(currentEntity));
                when(repository.save(any(TenantUserEntity.class))).thenAnswer(returnsFirstArg());

                TenantUser result = sut.updateUser(update);

                ArgumentCaptor<TenantUserEntity> entityArgumentCaptor = ArgumentCaptor.forClass(TenantUserEntity.class);

                verify(repository).save(entityArgumentCaptor.capture());

                assertThat(entityArgumentCaptor.getValue()).satisfies(entity -> {
                    assertThat(entity.getId()).isEqualTo(currentEntity.id);
                    assertThat(entity.getUuid()).isEqualTo(currentEntity.getUuid());
                    assertThat(entity.getGivenName()).isEqualTo(update.givenName());
                    assertThat(entity.getFamilyName()).isEqualTo(update.familyName());
                    assertThat(entity.getEmail()).isEqualTo(update.eMail().value());
                    assertThat(entity.getAuthorities()).isEqualTo(currentEntity.getAuthorities());
                    assertThat(entity.getCreatedAt()).isEqualTo(currentEntity.getCreatedAt());
                    assertThat(entity.getUpdatedAt()).isEqualTo(now);
                    assertThat(entity.getStatus()).isEqualTo(UserStatus.ACTIVE);
                });

                assertThat(result).satisfies(updatedUser -> {
                    assertThat(updatedUser.id()).isEqualTo(currentEntity.getUuid());
                    assertThat(updatedUser.localId()).isEqualTo(currentEntity.id);
                    assertThat(updatedUser.givenName()).isEqualTo(update.givenName());
                    assertThat(updatedUser.familyName()).isEqualTo(update.familyName());
                    assertThat(updatedUser.eMail()).isEqualTo(update.eMail());
                    assertThat(updatedUser.authorities()).isEqualTo(currentEntity.getAuthorities());
                    assertThat(updatedUser.createdAt()).isEqualTo(currentEntity.getCreatedAt());
                    assertThat(updatedUser.updatedAt()).isEqualTo(now);
                    assertThat(updatedUser.status()).isEqualTo(UserStatus.ACTIVE);
                });
            }

            @Test
            void ensureGivenNameAndFamilyNameIsStripped() {

                final Instant now = clock.instant();

                final TenantUserEntity currentEntity = activeUserEntityOne(now);
                final TenantUser update = updateFromEntity(currentEntity, "  Hans Joachim ", " Sonnencreme  ", "hans@example.com");

                when(repository.findById(any())).thenReturn(Optional.of(currentEntity));
                when(repository.save(any(TenantUserEntity.class))).thenAnswer(returnsFirstArg());

                final TenantUser result = sut.updateUser(update);

                final ArgumentCaptor<TenantUserEntity> entityArgumentCaptor = ArgumentCaptor.forClass(TenantUserEntity.class);
                verify(repository).save(entityArgumentCaptor.capture());

                assertThat(entityArgumentCaptor.getValue()).satisfies(entity -> {
                    assertThat(entity.getGivenName()).isEqualTo("Hans Joachim");
                    assertThat(entity.getFamilyName()).isEqualTo("Sonnencreme");
                });

                assertThat(result.givenName()).isEqualTo("Hans Joachim");
                assertThat(result.familyName()).isEqualTo("Sonnencreme");
            }

            @Test
            void updateUserThrowsExceptionWhenUserNotFound() {
                TenantUser user = activeTenantUserOne(Instant.now());

                when(repository.findById(any())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> sut.updateUser(user))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("could not find user with id=%s", user.localId());

                verify(repository).findById(user.localId());
            }
        }

        @Nested
        class DeleteUser {

            @Test
            void deleteUserSuccessfully() {

                final Instant now = clock.instant();
                final TenantUserEntity existing = activeUserEntityOne(now);

                when(repository.findById(any())).thenReturn(Optional.of(existing));

                sut.deleteUser(existing.getId());

                verify(repository).findById(existing.getId());

                final ArgumentCaptor<TenantUserEntity> entityArgumentCaptor = ArgumentCaptor.forClass(TenantUserEntity.class);
                verify(repository).save(entityArgumentCaptor.capture());
                assertThat(entityArgumentCaptor.getValue()).satisfies(entity -> {
                    assertThat(entity.getId()).isEqualTo(existing.id);
                    assertThat(entity.getUuid()).isEqualTo(existing.getUuid());
                    assertThat(entity.getGivenName()).isEqualTo(existing.getGivenName());
                    assertThat(entity.getFamilyName()).isEqualTo(existing.getFamilyName());
                    assertThat(entity.getEmail()).isEqualTo(existing.getEmail());
                    assertThat(entity.getAuthorities()).isEqualTo(existing.getAuthorities());
                    assertThat(entity.getCreatedAt()).isEqualTo(existing.getCreatedAt());
                    assertThat(entity.getUpdatedAt()).isEqualTo(now);
                    assertThat(entity.getDeactivatedAt()).isNull();
                    assertThat(entity.getDeletedAt()).isEqualTo(now);
                    assertThat(entity.getStatus()).isEqualTo(UserStatus.DELETED);
                });
            }

            @Test
            void deleteUserThrowsExceptionWhenUserNotFound() {
                final Long userId = 1L;

                when(repository.findById(any())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> sut.deleteUser(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("could not find user with id=%s", userId);

                verify(repository).findById(userId);
            }
        }

        @Nested
        class ActivateUser {
            @Test
            void activateUserSuccessfully() {
                final Long id = 1L;
                final Instant now = clock.instant();
                final TenantUserEntity currentEntity = new TenantUserEntity(id, "uuid", now, now, "givenName", "familyName", "email@example.com", Set.of(), now, now, null, null, UserStatus.UNKNOWN);
                final TenantUserEntity activatedEntity = new TenantUserEntity(id, "uuid", now, now, "givenName", "familyName", "email@example.com", Set.of(), now, now, null, null, UserStatus.ACTIVE);


                when(repository.findById(any())).thenReturn(Optional.of(currentEntity));
                when(repository.save(any(TenantUserEntity.class))).thenReturn(activatedEntity);

                sut.activateUser(id);

                verify(repository).findById(id);
                verify(repository).save(activatedEntity);
            }

            @Test
            void activateUserThrowsExceptionWhenUserNotFound() {
                final Long userId = 1L;

                when(repository.findById(any())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> sut.activateUser(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("could not find user with id=%s", userId);

                verify(repository).findById(userId);
            }

        }

        @Nested
        class DeactivateUser {
            @Test
            void deactivateUserSuccessfully() {
                final Long id = 1L;
                final Instant now = clock.instant();
                final TenantUserEntity currentEntity = new TenantUserEntity(id, "uuid", now, now, "givenName", "familyName", "email@example.com", Set.of(), now, now, null, null, UserStatus.ACTIVE);
                final TenantUserEntity deactivatedEntity = new TenantUserEntity(id, "uuid", now, now, "givenName", "familyName", "email@example.com", Set.of(), now, now, now, null, UserStatus.DEACTIVATED);


                when(repository.findById(any())).thenReturn(Optional.of(currentEntity));
                when(repository.save(any(TenantUserEntity.class))).thenReturn(deactivatedEntity);

                sut.deactivateUser(id);

                verify(repository).findById(id);
                verify(repository).save(deactivatedEntity);
            }

            @Test
            void deactivateUserThrowsExceptionWhenUserNotFound() {
                final Long userId = 1L;

                when(repository.findById(any())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> sut.deactivateUser(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("could not find user with id=%s", userId);

                verify(repository).findById(userId);
            }
        }
    }

    private static TenantUser activeTenantUserOne(Instant now) {
        return new TenantUser("my-external-id-1", 1L, "batman", "batman", new EMailAddress("batman@batman.com"), now, Set.of(), now, now, null, null, UserStatus.ACTIVE);
    }

    private static TenantUserEntity activeUserEntityOne(Instant now) {
        return new TenantUserEntity(1L, "my-external-id-1", now, now, "batman", "batman", "batman@batman.com", Set.of(), now, now, null, null, UserStatus.ACTIVE);
    }

    private static TenantUser activeTenantUserTwo(Instant now) {
        return new TenantUser("my-external-id-2", 2L, "petra", "panter", new EMailAddress("petra@panter.com"), now, Set.of(), now, now, null, null, UserStatus.ACTIVE);
    }

    private static TenantUserEntity activeUserEntityTwo(Instant now) {
        return new TenantUserEntity(2L, "my-external-id-2", now, now, "petra", "panter", "petra@panter.com", Set.of(), now, now, null, null, UserStatus.ACTIVE);
    }
}
