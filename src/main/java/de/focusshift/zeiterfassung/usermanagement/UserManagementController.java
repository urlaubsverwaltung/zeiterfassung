package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasAnyAuthority('ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL', 'ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL')")
class UserManagementController implements HasTimeClock, HasLaunchpad {

    private final UserManagementService userManagementService;

    UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    String users(Model model,
                 @RequestParam(value = "query", required = false, defaultValue = "") String query,
                 @RequestHeader(name = "Turbo-Frame", required = false) String turboFrame) {

        List<UserDto> users = userManagementService.findAllUsers(query)
            .stream()
            .map(UserManagementController::userToDto)
            .toList();

        model.addAttribute("query", query);
        model.addAttribute("slug", "");
        model.addAttribute("users", users);
        model.addAttribute("selectedUser", null);
        model.addAttribute("personSearchFormAction", "/users");

        if (StringUtils.hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    @GetMapping("/{id}")
    String user(@PathVariable("id") Long id, @AuthenticationPrincipal OidcUser principal) {

        final String slug;

        if (hasAuthority(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, principal)) {
            slug = "working-time";
        } else if (hasAuthority(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, principal)) {
            slug = "overtime-account";
        } else {
            slug = null;
        }

        if (slug == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }

        return "forward:/users/%s/%s".formatted(id, slug);
    }

    static UserDto userToDto(User user) {
        return new UserDto(user.localId().value(), user.givenName(), user.familyName(), user.givenName() + " " + user.familyName(), user.email().value());
    }

    static boolean hasAuthority(SecurityRole securityRole, OidcUser principal) {
        return principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_" + securityRole));
    }
}
