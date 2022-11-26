package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRoles.*;
import static de.focusshift.zeiterfassung.security.SecurityRules.ALLOW_EDIT_AUTHORITIES;

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
    @PreAuthorize(ALLOW_EDIT_AUTHORITIES)
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
        if (authoritiesDto.isEditAuthorities()) {
            authorities.add(ZEITERFASSUNG_EDIT_AUTHORITIES);
        }
        return authorities;
    }

    static UserAccountAuthoritiesDto toUserAccountAuthoritiesDto(Collection<SecurityRoles> authorities) {
        return UserAccountAuthoritiesDto.builder()
            .user(authorities.contains(ZEITERFASSUNG_USER))
            .viewReportAll(authorities.contains(ZEITERFASSUNG_VIEW_REPORT_ALL))
            .editAuthorities(authorities.contains(ZEITERFASSUNG_EDIT_AUTHORITIES))
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
