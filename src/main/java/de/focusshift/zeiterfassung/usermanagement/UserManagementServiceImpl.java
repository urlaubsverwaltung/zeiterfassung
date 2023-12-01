package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
class UserManagementServiceImpl implements UserManagementService {

    private final TenantUserService tenantUserService;

    UserManagementServiceImpl(TenantUserService tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    @Override
    public Optional<User> findUserById(UserId userId) {
        return mapToUser(tenantUserService.findById(userId));
    }

    @Override
    public Optional<User> findUserByLocalId(UserLocalId userId) {
        return mapToUser(tenantUserService.findByLocalId(userId));
    }

    @Override
    public List<User> findAllUsers() {
        return mapToUser(tenantUserService.findAllUsers());
    }

    @Override
    public List<User> findAllUsers(String query) {
        return mapToUser(tenantUserService.findAllUsers(query));
    }

    @Override
    public List<User> findAllUsersByIds(Collection<UserId> userIds) {
        return mapToUser(tenantUserService.findAllUsersById(userIds));
    }

    @Override
    public List<User> findAllUsersByLocalIds(Collection<UserLocalId> localIds) {
        return mapToUser(tenantUserService.findAllUsersByLocalId(localIds));
    }

    @Override
    public User updateUserPermissions(UserLocalId userLocalId, Set<SecurityRole> permissions) throws UserNotFoundException {

        final TenantUser tenantUser = tenantUserService.findByLocalId(userLocalId)
            .orElseThrow(() -> new UserNotFoundException(userLocalId));

        final TenantUser tenantUserWithNewPermissions =
            new TenantUser(tenantUser.id(), tenantUser.localId(), tenantUser.givenName(), tenantUser.familyName(), tenantUser.eMail(), permissions);

        return tenantUserToUser(tenantUserService.updateUser(tenantUserWithNewPermissions));
    }

    private static Optional<User> mapToUser(Optional<TenantUser> optional) {
        return optional.map(UserManagementServiceImpl::tenantUserToUser);
    }

    private static List<User> mapToUser(Collection<TenantUser> collection) {
        return collection.stream()
            .map(UserManagementServiceImpl::tenantUserToUser)
            .toList();
    }

    private static User tenantUserToUser(TenantUser tenantUser) {

        final UserId userId = new UserId(tenantUser.id());
        final UserLocalId userLocalId = new UserLocalId(tenantUser.localId());
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        return new User(userIdComposite, tenantUser.givenName(), tenantUser.familyName(), tenantUser.eMail(), tenantUser.authorities());
    }
}
