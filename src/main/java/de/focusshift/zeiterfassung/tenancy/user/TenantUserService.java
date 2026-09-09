package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TenantUserService {

    TenantUser createNewUser(String uuid, String givenName, String familyName, EMailAddress eMailAddress, Collection<SecurityRole> authorities);

    TenantUser updateUser(TenantUser user);

    List<TenantUser> findAllUsers();

    List<TenantUser> findAllUsers(String query);

    List<TenantUser> findAllUsersById(Collection<UserId> userIds);

    List<TenantUser> findAllUsersByLocalId(Collection<UserLocalId> userLocalIds);

    Optional<TenantUser> findById(UserId userId);

    Optional<TenantUser> findByLocalId(UserLocalId localId);

    /**
     * Irreversibly removes the user and everything the database knows about this person. All related data
     * (time entries, time clocks, working times, overtime account, user settings, absences and the time entry
     * history) is removed by {@code ON DELETE CASCADE}. Revisions the person authored on time entries of other
     * people are kept, but lose the reference to the person.
     *
     * @param id local id of the user to delete
     * @throws IllegalArgumentException when there is no user with the given id
     */
    void deleteUserPermanently(Long id);

    void activateUser(Long id);

    void deactivateUser(Long id);

    long countUsers();

}
