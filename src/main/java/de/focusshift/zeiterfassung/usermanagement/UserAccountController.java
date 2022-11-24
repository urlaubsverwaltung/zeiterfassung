package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRoles.ZEITERFASSUNG_USER;
import static de.focusshift.zeiterfassung.security.SecurityRoles.ZEITERFASSUNG_VIEW_REPORT_ALL;

@Controller
@RequestMapping("/users/{id}/account")
class UserAccountController {

    private final UserManagementService userManagementService;

    UserAccountController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    String userAccount(@PathVariable("id") String id, Model model) {

        final User user = userById(id);

        model.addAttribute("userAccount", toUserAccountDto(user));

        return "usermanagement/user-account";
    }

    @PostMapping("/authorities")
    String userAuthorities(@PathVariable("id") String id, @ModelAttribute UserAccountAuthoritiesDto authoritiesDto, Model model) {

        final User user = userById(id);

        userManagementService.updateAuthorities(user, toAuthorities(authoritiesDto));

        return "redirect:/users/%s/account".formatted(id);
    }

    static UserAccountDto toUserAccountDto(User user) {
        final UserAccountAuthoritiesDto authorities = toUserAccountAuthoritiesDto(user.authorities());
        return new UserAccountDto(user.localId().value(), user.givenName(), user.familyName(), fullName(user), user.email().value(), authorities);
    }

    static Collection<SecurityRoles> toAuthorities(UserAccountAuthoritiesDto authoritiesDto) {
        final ArrayList<SecurityRoles> authorities = new ArrayList<>();
        authorities.add(ZEITERFASSUNG_USER);
        if (authoritiesDto.isViewReportAll()) {
            authorities.add(ZEITERFASSUNG_VIEW_REPORT_ALL);
        }
        return authorities;
    }

    static UserAccountAuthoritiesDto toUserAccountAuthoritiesDto(Collection<SecurityRoles> authorities) {
        return UserAccountAuthoritiesDto.builder()
            .user(authorities.contains(ZEITERFASSUNG_USER))
            .viewReportAll(authorities.contains(ZEITERFASSUNG_VIEW_REPORT_ALL))
            .build();
    }

    private User userById(String id) {
        return userManagementService.findAllUsersByLocalIds(List.of(UserLocalId.ofValue(id)))
            .stream()
            .findFirst()
            .orElseThrow();
    }

    static String fullName(User user) {
        return user.givenName() + " " + user.familyName();
    }
}
