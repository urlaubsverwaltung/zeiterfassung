package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserId;

import java.util.List;
import java.util.Optional;

public interface UserManagementService {

    Optional<User> findUserById(UserId userId);

    List<User> findAllUsers();

    List<User> findAllUsersByIds(List<UserId> ids);

    List<User> findAllUsersByLocalIds(List<UserLocalId> localIds);
}
