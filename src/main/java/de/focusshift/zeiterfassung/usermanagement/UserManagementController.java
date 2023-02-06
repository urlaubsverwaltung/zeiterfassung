package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ZEITERFASSUNG_USER')")
class UserManagementController implements HasTimeClock, HasLaunchpad {

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
        model.addAttribute("selectedUser", null);

        return "usermanagement/users";
    }

    @GetMapping("/{id}")
    String user(@PathVariable("id") Long id) {
        return "forward:/users/%s/working-time".formatted(id);
    }

    static UserDto userToDto(User user) {
        return new UserDto(user.localId().value(), user.givenName(), user.familyName(), user.givenName() + " " + user.familyName(), user.email().value());
    }
}
