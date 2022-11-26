package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserManagementService {

    Optional<User> findUserById(UserId userId);

    List<User> findAllUsers();

    Page<User> findAllUsers(Pageable pageable);

    List<User> findAllUsersByIds(List<UserId> ids);

    List<User> findAllUsersByLocalIds(List<UserLocalId> localIds);

    User updateAuthorities(User user, Collection<SecurityRoles> toAuthorities);
}
