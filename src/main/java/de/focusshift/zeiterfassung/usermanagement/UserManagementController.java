package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    @GetMapping("/{id}/account")
    String userAccount(@PathVariable("id") String id, Model model) {

        final User user = userById(id);

        model.addAttribute("userAccount", toUserAccountDto(user));

        return "usermanagement/user-account";
    }

    @PostMapping("/{id}/account/authorities")
    String userAuthorities(@PathVariable("id") String id, @ModelAttribute UserAccountAuthoritiesDto authoritiesDto, Model model) {

        final User user = userById(id);

        userManagementService.updateAuthorities(user, toAuthorities(authoritiesDto));

        return "redirect:/users/%s/account".formatted(id);
    }

    private User userById(String id) {
        return userManagementService.findAllUsersByLocalIds(List.of(UserLocalId.ofValue(id)))
            .stream()
            .findFirst()
            .orElseThrow();
    }

    static UserDto userToDto(User user) {
        return new UserDto(user.localId().value(), user.givenName(), user.familyName(), fullName(user), user.email().value());
    }

    static UserAccountDto toUserAccountDto(User user) {
        final UserAccountAuthoritiesDto authorities = toUserAccountAuthoritiesDto(user.authorities());
        return new UserAccountDto(user.localId().value(), user.givenName(), user.familyName(), fullName(user), user.email().value(), authorities);
    }

    static UserAccountAuthoritiesDto toUserAccountAuthoritiesDto(Collection<SecurityRoles> authorities) {
        return new UserAccountAuthoritiesDto(authorities.contains(ZEITERFASSUNG_VIEW_REPORT_ALL));
    }

    static Collection<SecurityRoles> toAuthorities(UserAccountAuthoritiesDto authoritiesDto) {
        final ArrayList<SecurityRoles> authorities = new ArrayList<>();
        if (authoritiesDto.viewReportAll()) {
            authorities.add(ZEITERFASSUNG_VIEW_REPORT_ALL);
        }
        return authorities;
    }

    static String fullName(User user) {
        return user.givenName() + " " + user.familyName();
    }
}
