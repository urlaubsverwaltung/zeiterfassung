package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    public TenantUser createNewUser(String uuid, String givenName, String familyName, EMailAddress eMailAddress, Collection<SecurityRole> authorities) {

        final Instant now = clock.instant();

        final TenantUserEntity tenantUserEntity =
            new TenantUserEntity(null, uuid, now, now, givenName, familyName, eMailAddress.value(), distinct(authorities));

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
            new TenantUserEntity(current.getId(), current.getUuid(), current.getFirstLoginAt(), now, user.givenName(), user.familyName(), user.eMail().value(), distinct(user.authorities()));

        final TenantUserEntity persisted = tenantUserRepository.save(next);

        return entityToTenantUser(persisted);
    }

    @Override
    public List<TenantUser> findAllUsers() {
        return mapToTenantUser(tenantUserRepository.findAll());
    }

    @Override
    public List<TenantUser> findAllUsers(String query) {
        return mapToTenantUser(tenantUserRepository.findAllByGivenNameContainingIgnoreCaseOrFamilyNameContainingIgnoreCase(query, query));
    }

    @Override
    public List<TenantUser> findAllUsersById(Collection<UserId> userIds) {
        final List<String> idValues = userIds.stream().map(UserId::value).toList();
        return mapToTenantUser(tenantUserRepository.findAllByUuidIsIn(idValues));
    }

    @Override
    public List<TenantUser> findAllUsersByLocalId(Collection<UserLocalId> userLocalIds) {
        final List<Long> idValues = userLocalIds.stream().map(UserLocalId::value).toList();
        return mapToTenantUser(tenantUserRepository.findAllByIdIsIn(idValues));
    }

    @Override
    public Optional<TenantUser> findById(UserId userId) {
        return mapToTenantUser(tenantUserRepository.findByUuid(userId.value()));
    }

    @Override
    public Optional<TenantUser> findByLocalId(UserLocalId localId) {
        return mapToTenantUser(tenantUserRepository.findById(localId.value()));
    }

    @Override
    public void deleteUser(Long id) {
        tenantUserRepository.deleteById(id);
    }

    private Optional<TenantUser> mapToTenantUser(Optional<TenantUserEntity> optional) {
        return optional.map(TenantUserServiceImpl::entityToTenantUser);
    }

    private List<TenantUser> mapToTenantUser(Collection<TenantUserEntity> collection) {
        return collection.stream().map(TenantUserServiceImpl::entityToTenantUser).toList();
    }

    private static TenantUser entityToTenantUser(TenantUserEntity tenantUserEntity) {
        final String uuid = tenantUserEntity.getUuid();
        final Long id = tenantUserEntity.getId();
        final String givenName = tenantUserEntity.getGivenName();
        final String familyName = tenantUserEntity.getFamilyName();
        final EMailAddress eMail = new EMailAddress(tenantUserEntity.getEmail());
        final Instant firstLoginAt = tenantUserEntity.getFirstLoginAt();
        final Set<SecurityRole> authorities = new HashSet<>(tenantUserEntity.getAuthorities());

        return new TenantUser(uuid, id, givenName, familyName, eMail, firstLoginAt, authorities);
    }

    private static <T> Set<T> distinct(Collection<T> collection) {
        return new HashSet<>(collection);
    }
}
