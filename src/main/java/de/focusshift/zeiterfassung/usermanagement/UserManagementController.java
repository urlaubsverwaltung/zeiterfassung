package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;
import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRoles.ZEITERFASSUNG_USER;
import static de.focusshift.zeiterfassung.security.SecurityRoles.ZEITERFASSUNG_VIEW_REPORT_ALL;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ZEITERFASSUNG_USER')")
class UserManagementController {

    private final UserManagementService userManagementService;

    UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    String users(Model model) {

        List<UserDto> users = userManagementService.findAllUsers()
            .stream()
            .map(UserManagementController::userToDto)
            .toList();

        model.addAttribute("users", users);

        return "usermanagement/users";
    }

    static UserDto userToDto(User user) {
        final UserAccountAuthoritiesDto authorities = toUserAccountAuthoritiesDto(user.authorities());
        return new UserDto(user.localId().value(), user.givenName(), user.familyName(), fullName(user), user.email().value(), authorities);
    }

    static UserAccountAuthoritiesDto toUserAccountAuthoritiesDto(Collection<SecurityRoles> authorities) {
        return UserAccountAuthoritiesDto.builder()
            .user(authorities.contains(ZEITERFASSUNG_USER))
            .viewReportAll(authorities.contains(ZEITERFASSUNG_VIEW_REPORT_ALL))
            .build();
    }

    static String fullName(User user) {
        return user.givenName() + " " + user.familyName();
    }
}
