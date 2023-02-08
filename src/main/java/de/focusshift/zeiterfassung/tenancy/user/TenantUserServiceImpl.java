package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class TenantUserServiceImpl implements TenantUserService {

    private final TenantUserRepository tenantUserRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    TenantUserServiceImpl(TenantUserRepository tenantUserRepository, ApplicationEventPublisher applicationEventPublisher, Clock clock) {
        this.tenantUserRepository = tenantUserRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Override
    public Optional<TenantUser> getUserByUuid(UUID uuid) {
        return tenantUserRepository.findByUuid(uuid.toString())
            .map(TenantUserServiceImpl::entityToTenantUser);
    }

    @Override
    public TenantUser createNewUser(UUID uuid, String givenName, String familyName, EMailAddress eMailAddress) {

        final Instant now = clock.instant();

        final TenantUserEntity tenantUserEntity =
            new TenantUserEntity(null, uuid.toString(), now, now, givenName, familyName, eMailAddress.value());

        final TenantUserEntity persisted = tenantUserRepository.save(tenantUserEntity);

        final TenantUser tenantUser = entityToTenantUser(persisted);

        applicationEventPublisher.publishEvent(new TenantUserCreatedEvent(tenantUser));
        return tenantUser;
    }

    @Override
    public TenantUser updateUser(TenantUser user) {

        final Instant now = clock.instant();

        final TenantUserEntity current = tenantUserRepository.findById(user.localId())
            .orElseThrow(() -> new IllegalArgumentException(String.format("could not find user with id=%s", user.id())));

        final TenantUserEntity next =
            new TenantUserEntity(current.getId(), current.getUuid(), current.getFirstLoginAt(), now, user.givenName(), user.familyName(), user.eMail().value());

        final TenantUserEntity persisted = tenantUserRepository.save(next);

        return entityToTenantUser(persisted);
    }

    @Override
    public List<TenantUser> findAllUsers() {
        return tenantUserRepository.findAll().stream()
            .map(TenantUserServiceImpl::entityToTenantUser)
            .toList();
    }

    @Override
    public List<TenantUser> findAllUsers(String query) {
        final String queryLowercase = query.toLowerCase();
        return tenantUserRepository.findAll().stream()
            .map(TenantUserServiceImpl::entityToTenantUser)
            .filter(user -> user.givenName().toLowerCase().contains(queryLowercase) || user.familyName().toLowerCase().contains(queryLowercase))
            .toList();
    }

    @Override
    public List<TenantUser> findAllUsersById(Collection<UserId> userIds) {
        return findAllUsers().stream()
            .filter(user -> userIds.contains(new UserId(user.id())))
            .toList();
    }

    @Override
    public List<TenantUser> findAllUsersByLocalId(Collection<UserLocalId> userLocalIds) {
        return findAllUsers().stream()
            .filter(user -> userLocalIds.contains(new UserLocalId(user.localId())))
            .toList();
    }

    @Override
    public Optional<TenantUser> findById(UserId userId) {
        return tenantUserRepository.findByUuid(userId.value()).map(TenantUserServiceImpl::entityToTenantUser);
    }

    @Override
    public Optional<TenantUser> findByLocalId(UserLocalId localId) {
        return tenantUserRepository.findById(localId.value()).map(TenantUserServiceImpl::entityToTenantUser);
    }

    @Override
    public void deleteUser(Long id) {
        tenantUserRepository.deleteById(id);
    }

    private static TenantUser entityToTenantUser(TenantUserEntity tenantUserEntity) {
        final String uuid = tenantUserEntity.getUuid();
        final Long id = tenantUserEntity.getId();
        final String givenName = tenantUserEntity.getGivenName();
        final String familyName = tenantUserEntity.getFamilyName();
        final EMailAddress eMail = new EMailAddress(tenantUserEntity.getEmail());
        final Instant firstLoginAt = tenantUserEntity.getFirstLoginAt();

        return new TenantUser(uuid, id, givenName, familyName, eMail, firstLoginAt);
    }
}
