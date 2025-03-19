package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.slf4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class TenantUserServiceImpl implements TenantUserService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final TenantUserRepository tenantUserRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    TenantUserServiceImpl(TenantUserRepository tenantUserRepository, ApplicationEventPublisher applicationEventPublisher, Clock clock) {
        this.tenantUserRepository = tenantUserRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Override
    public TenantUser createNewUser(String uuid, String givenName, String familyName, EMailAddress eMailAddress, Collection<SecurityRole> authorities) {

        final Instant now = clock.instant();

        final TenantUserEntity tenantUserEntity =
            new TenantUserEntity(null, uuid, now, now, givenName.strip(), familyName.strip(), eMailAddress.value(), distinct(authorities), now, now, null, null, UserStatus.ACTIVE);

        final TenantUserEntity persisted = tenantUserRepository.save(tenantUserEntity);

        final TenantUser tenantUser = entityToTenantUser(persisted);

        applicationEventPublisher.publishEvent(new TenantUserCreatedEvent(tenantUser));
        return tenantUser;
    }

    @Override
    public TenantUser updateUser(TenantUser user) {

        final Instant now = clock.instant();

        final TenantUserEntity current = getTenantUserOrThrow(user.localId());

        final String givenName = user.givenName().strip();
        final String familyName = user.familyName().strip();
        final String email = user.eMail().value();
        final Set<SecurityRole> authorities = distinct(user.authorities());

        final TenantUserEntity next =
            new TenantUserEntity(current.getId(), current.getUuid(), current.getFirstLoginAt(), now, givenName, familyName, email, authorities, current.getCreatedAt(), now, current.getDeactivatedAt(), current.getDeletedAt(), current.getStatus());

        final TenantUserEntity persisted = tenantUserRepository.save(next);

        return entityToTenantUser(persisted);
    }

    @Override
    public List<TenantUser> findAllUsers() {
        return mapToTenantUser(tenantUserRepository.findAllByOrderByGivenNameAscFamilyNameAsc());
    }

    @Override
    public List<TenantUser> findAllUsers(String query) {
        return mapToTenantUser(tenantUserRepository.findAllByNiceNameContainingIgnoreCaseOrderByGivenNameAscFamilyNameAsc(query));
    }

    @Override
    public List<TenantUser> findAllUsersById(Collection<UserId> userIds) {
        final List<String> idValues = userIds.stream().map(UserId::value).toList();
        return mapToTenantUser(tenantUserRepository.findAllByUuidIsInOrderByGivenNameAscFamilyNameAsc(idValues));
    }

    @Override
    public List<TenantUser> findAllUsersByLocalId(Collection<UserLocalId> userLocalIds) {
        final List<Long> idValues = userLocalIds.stream().map(UserLocalId::value).toList();
        return mapToTenantUser(tenantUserRepository.findAllByIdIsInOrderByGivenNameAscFamilyNameAsc(idValues));
    }

    @Override
    public Optional<TenantUser> findById(UserId userId) {
        return mapToTenantUser(tenantUserRepository.findByUuid(userId.value()));
    }

    @Override
    public Optional<TenantUser> findByLocalId(UserLocalId localId) {
        LOG.debug("search user by {}", localId);
        return mapToTenantUser(tenantUserRepository.findById(localId.value()));
    }

    @Override
    public void deleteUser(Long id) {

        final Instant now = clock.instant();

        final TenantUserEntity current = getTenantUserOrThrow(id);

        final TenantUserEntity next =
            new TenantUserEntity(current.getId(), current.getUuid(), current.getFirstLoginAt(), current.getLastLoginAt(), current.getGivenName(), current.getFamilyName(), current.getEmail(), current.getAuthorities(), current.getCreatedAt(), now, current.getDeactivatedAt(), now, UserStatus.DELETED);

        tenantUserRepository.save(next);
    }

    @Override
    public void activateUser(Long id) {

        final Instant now = clock.instant();

        final TenantUserEntity current = getTenantUserOrThrow(id);

        if (UserStatus.DEACTIVATED.equals(current.getStatus()) || UserStatus.DELETED.equals(current.getStatus())) {
            LOG.warn("Detected suspicious update for userId={}: status={} -> status=ACTIVE ...", id, current.getStatus());
        }

        final TenantUserEntity next =
            new TenantUserEntity(current.getId(), current.getUuid(), current.getFirstLoginAt(), current.getLastLoginAt(), current.getGivenName(), current.getFamilyName(), current.getEmail(), current.getAuthorities(), current.getCreatedAt(), now, current.getDeactivatedAt(), current.getDeletedAt(), UserStatus.ACTIVE);

        tenantUserRepository.save(next);
    }

    @Override
    public void deactivateUser(Long id) {

        final Instant now = clock.instant();

        final TenantUserEntity current = getTenantUserOrThrow(id);

        final TenantUserEntity next =
            new TenantUserEntity(current.getId(), current.getUuid(), current.getFirstLoginAt(), current.getLastLoginAt(), current.getGivenName(), current.getFamilyName(), current.getEmail(), current.getAuthorities(), current.getCreatedAt(), now, now, current.getDeletedAt(), UserStatus.DEACTIVATED);

        tenantUserRepository.save(next);
    }

    private Optional<TenantUser> mapToTenantUser(Optional<TenantUserEntity> optional) {
        return optional.map(TenantUserServiceImpl::entityToTenantUser);
    }

    private List<TenantUser> mapToTenantUser(Collection<TenantUserEntity> collection) {
        return collection.stream().map(TenantUserServiceImpl::entityToTenantUser).toList();
    }

    private TenantUserEntity getTenantUserOrThrow(Long id) {
        return tenantUserRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("could not find user with id=%s".formatted(id)));
    }

    private static TenantUser entityToTenantUser(TenantUserEntity tenantUserEntity) {

        final String uuid = tenantUserEntity.getUuid();
        final Long id = tenantUserEntity.getId();
        final String givenName = tenantUserEntity.getGivenName();
        final String familyName = tenantUserEntity.getFamilyName();
        final EMailAddress eMail = new EMailAddress(tenantUserEntity.getEmail());
        final Instant firstLoginAt = tenantUserEntity.getFirstLoginAt();
        final Set<SecurityRole> authorities = new HashSet<>(tenantUserEntity.getAuthorities());
        final Instant createdAt = tenantUserEntity.getCreatedAt();
        final Instant updatedAt = tenantUserEntity.getUpdatedAt();
        final Instant deactivatedAt = tenantUserEntity.getDeactivatedAt();
        final Instant deletedAt = tenantUserEntity.getDeletedAt();
        final UserStatus status = tenantUserEntity.getStatus();

        return new TenantUser(uuid, id, givenName, familyName, eMail, firstLoginAt, authorities, createdAt, updatedAt, deactivatedAt, deletedAt, status);
    }

    private static <T> Set<T> distinct(Collection<T> collection) {
        return new HashSet<>(collection);
    }
}
