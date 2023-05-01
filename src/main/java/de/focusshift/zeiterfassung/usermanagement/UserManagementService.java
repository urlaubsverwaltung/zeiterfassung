package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserManagementService {

    Optional<User> findUserById(UserId userId);

    Optional<User> findUserByLocalId(UserLocalId userId);

    List<User> findAllUsers();

    List<User> findAllUsers(String query);

    List<User> findAllUsersByIds(Collection<UserId> ids);

    List<User> findAllUsersByLocalIds(List<UserLocalId> localIds);
}
