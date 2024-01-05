package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.user.UserId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserManagementService {

    Optional<User> findUserById(UserId userId);

    Optional<User> findUserByLocalId(UserLocalId userId);

    List<User> findAllUsers();

    List<User> findAllUsers(String query);

    List<User> findAllUsersByIds(Collection<UserId> ids);

    List<User> findAllUsersByLocalIds(Collection<UserLocalId> localIds);

    /**
     * Updates the users local application permissions. Note that OIDC permissions are not handled here!
     *
     * @param userLocalId id of the user that should be updated
     * @param permissions new permissions of the user
     * @return the updated user
     * @throws UserNotFoundException when there is no user with the given id
     */
    User updateUserPermissions(UserLocalId userLocalId, Set<SecurityRole> permissions) throws UserNotFoundException;
}
