package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import de.focusshift.zeiterfassung.tenantuser.TenantUser;
import de.focusshift.zeiterfassung.tenantuser.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
class UserManagementServiceImpl implements UserManagementService {

    private final TenantUserService tenantUserService;

    UserManagementServiceImpl(TenantUserService tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    @Override
    public Optional<User> findUserById(UserId userId) {
        return findAllUsers().stream().filter(user -> user.id().equals(userId)).findFirst();
    }

    @Override
    public List<User> findAllUsers() {
        return tenantUserService.findAllUsers()
            .stream()
            .map(UserManagementServiceImpl::tenantUserToUser)
            .toList();
    }

    @Override
    public List<User> findAllUsersByIds(List<UserId> userIds) {
        return findAllUsers().stream().filter(user -> userIds.contains(user.id())).toList();
    }

    @Override
    public List<User> findAllUsersByLocalIds(List<UserLocalId> localIds) {
        return findAllUsers().stream().filter(user -> localIds.contains(user.localId())).toList();
    }

    @Override
    public User updateAuthorities(User user, Collection<SecurityRoles> authorities) {

        final TenantUser tenantUser = new TenantUser(user.id().value(), user.localId().value(), user.givenName(),
            user.familyName(), user.email(), new HashSet<>(authorities));

        final TenantUser updatedTenantUser = tenantUserService.updateUser(tenantUser);

        // TODO propagate new info to keycloak

        return tenantUserToUser(updatedTenantUser);
    }

    private static User tenantUserToUser(TenantUser tenantUser) {
        return new User(new UserId(tenantUser.id()), new UserLocalId(tenantUser.localId()), tenantUser.givenName(),
            tenantUser.familyName(), tenantUser.eMail(), tenantUser.authorities());
    }
}
