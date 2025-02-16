package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.security.AuthenticationService;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;

@Service
class ReportPermissionService {

    private final AuthenticationService authenticationService;
    private final UserManagementService userManagementService;

    ReportPermissionService(AuthenticationService authenticationService,
                            UserManagementService userManagementService) {
        this.authenticationService = authenticationService;
        this.userManagementService = userManagementService;
    }

    boolean currentUserHasPermissionForAllUsers() {
        return authenticationService.hasSecurityRole(ZEITERFASSUNG_VIEW_REPORT_ALL);
    }

    List<UserLocalId> filterUserLocalIdsByCurrentUserHasPermissionFor(List<UserLocalId> userLocalIds) {
        if (currentUserHasPermissionForAllUsers()) {
            return userLocalIds;
        }

        final UserLocalId currentUserLocaleId = authenticationService.getCurrentUserIdComposite().localId();
        if (userLocalIds.contains(currentUserLocaleId)) {
            return List.of(currentUserLocaleId);
        }

        return List.of();
    }

    List<UserLocalId> findAllPermittedUserLocalIdsForCurrentUser() {
        if (currentUserHasPermissionForAllUsers()) {
            return userManagementService.findAllUsers().stream().map(User::userLocalId).toList();
        }

        final UserLocalId currentUserLocaleId = authenticationService.getCurrentUserIdComposite().localId();
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

        final UserId userId = authenticationService.getCurrentUserIdComposite().id();

        final User user = userManagementService.findUserById(userId)
            .orElseThrow(() -> new IllegalStateException("could not find user for currently logged in user " + userId));

        return List.of(user);
    }
}
