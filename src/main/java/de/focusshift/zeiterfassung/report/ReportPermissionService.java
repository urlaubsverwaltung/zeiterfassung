package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.user.CurrentUserProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRoles.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class ReportPermissionService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final CurrentUserProvider currentUserProvider;
    private final UserManagementService userManagementService;

    ReportPermissionService(CurrentUserProvider currentUserProvider, UserManagementService userManagementService) {
        this.currentUserProvider = currentUserProvider;
        this.userManagementService = userManagementService;
    }

    boolean currentUserHasPermissionForAllUsers() {
        final Authentication authentication = currentUserProvider.getCurrentAuthentication();
        final boolean contains = authentication.getAuthorities().contains(ZEITERFASSUNG_VIEW_REPORT_ALL.authority());

        if (LOG.isDebugEnabled()) {
            LOG.debug("user={} has permission={} for all users", authentication.getName(), contains);
        }

        return contains;
    }

    List<UserLocalId> filterUserLocalIdsByCurrentUserHasPermissionFor(List<UserLocalId> userLocalIds) {
        if (currentUserHasPermissionForAllUsers()) {
            return userLocalIds;
        }

        final UserLocalId currentUserLocaleId = currentUserProvider.getCurrentUser().localId();
        if (userLocalIds.contains(currentUserLocaleId)) {
            return List.of(currentUserLocaleId);
        }

        return List.of();
    }

    List<UserLocalId> findAllPermittedUserLocalIdsForCurrentUser() {
        if (currentUserHasPermissionForAllUsers()) {
            return userManagementService.findAllUsers().stream().map(User::localId).toList();
        }

        final UserLocalId currentUserLocaleId = currentUserProvider.getCurrentUser().localId();
        return List.of(currentUserLocaleId);
    }

    /**
     * This method returns a list of all {@link User}s the logged-in user is permitted to see reports for.
     * Note that the logged-in user is part of the list. So the returned list has at least one element.
     *
     * @return a list of all valid {@link User}s.
     */
    List<User> findAllPermittedUsersForCurrentUser() {
        if (currentUserHasPermissionForAllUsers()) {
            return userManagementService.findAllUsers();
        }

        final User currentUser = currentUserProvider.getCurrentUser();
        return List.of(currentUser);
    }
}
