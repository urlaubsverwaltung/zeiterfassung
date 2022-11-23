package de.focusshift.zeiterfassung.usermanagement;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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
        return new UserDto(user.givenName(), user.familyName(), user.givenName() + " " + user.familyName(), user.email().value());
    }
}
