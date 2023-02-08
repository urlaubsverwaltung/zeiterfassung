package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserService {

    Optional<TenantUser> getUserByUuid(UUID uuid);

    TenantUser createNewUser(UUID uuid, String givenName, String familyName, EMailAddress eMailAddress);

    TenantUser updateUser(TenantUser user);

    List<TenantUser> findAllUsers();

    List<TenantUser> findAllUsers(String query);

    List<TenantUser> findAllUsersById(Collection<UserId> userIds);

    List<TenantUser> findAllUsersByLocalId(Collection<UserLocalId> userLocalIds);

    Optional<TenantUser> findById(UserId userId);

    Optional<TenantUser> findByLocalId(UserLocalId localId);

    void deleteUser(Long id);
}
