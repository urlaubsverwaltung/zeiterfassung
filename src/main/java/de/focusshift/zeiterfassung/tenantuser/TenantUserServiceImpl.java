package de.focusshift.zeiterfassung.tenantuser;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

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
    public TenantUser createNewUser(UUID uuid, String givenName, String familyName, EMailAddress eMailAddress, Collection<SecurityRoles> authorities) {

        final Instant now = clock.instant();

        final TenantUserEntity tenantUserEntity =
            new TenantUserEntity(null, uuid.toString(), now, now, givenName, familyName, eMailAddress.value(), distinct(authorities));

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
        return tenantUserRepository.findAll().stream()
            .map(TenantUserServiceImpl::entityToTenantUser)
            .toList();
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
        final HashSet<SecurityRoles> authorities = new HashSet<>(tenantUserEntity.getAuthorities());

        return new TenantUser(uuid, id, givenName, familyName, eMail, authorities);
    }

    private static <T> List<T> distinct(Collection<T> collection) {
        return collection.stream().distinct().toList();
    }
}
