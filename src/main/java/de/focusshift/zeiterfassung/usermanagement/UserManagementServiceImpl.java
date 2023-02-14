package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.stereotype.Service;

import java.util.Collection;
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
    public List<User> findAllUsersByIds(List<UserId> userIds) {
        return mapToUser(tenantUserService.findAllUsersById(userIds));
    }

    @Override
    public List<User> findAllUsersByLocalIds(List<UserLocalId> localIds) {
        return mapToUser(tenantUserService.findAllUsersByLocalId(localIds));
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
        return new User(new UserId(tenantUser.id()), new UserLocalId(tenantUser.localId()), tenantUser.givenName(), tenantUser.familyName(), tenantUser.eMail(), tenantUser.authorities());
    }
}
