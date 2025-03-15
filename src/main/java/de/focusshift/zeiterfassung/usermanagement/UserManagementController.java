package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
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
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasAnyAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_ALL', 'ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL', 'ZEITERFASSUNG_PERMISSIONS_EDIT_ALL')")
class UserManagementController implements HasTimeClock, HasLaunchpad {

    private final UserManagementService userManagementService;

    UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    String users(Model model,
                 @RequestParam(value = "query", required = false, defaultValue = "") String query,
                 @RequestHeader(name = TURBO_FRAME_HEADER, required = false) String turboFrame) {

        final List<UserDto> users = userManagementService.findAllUsers(query)
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
    String user(@PathVariable("id") Long id, @CurrentSecurityContext SecurityContext securityContext) {

        final String slug;

        if (hasAuthority(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, securityContext)) {
            slug = "working-time";
        } else if (hasAuthority(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, securityContext)) {
            slug = "overtime-account";
        } else if (hasAuthority(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL, securityContext)) {
            slug = "permissions";
        } else {
            slug = null;
        }

        if (slug == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }

        return "forward:/users/%s/%s".formatted(id, slug);
    }

    static UserDto userToDto(User user) {
        return new UserDto(user.userLocalId().value(), user.givenName(), user.familyName(), user.givenName() + " " + user.familyName(), user.email().value());
    }

    static boolean hasAuthority(SecurityRole securityRole, SecurityContext securityContext) {
        final Authentication authentication = securityContext.getAuthentication();
        return authentication.getAuthorities().contains(securityRole.authority());
    }
}
