package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.user.CurrentUserProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;

import static de.focusshift.zeiterfassung.security.SecurityRoles.ZEITERFASSUNG_EDIT_AUTHORITIES;
import static de.focusshift.zeiterfassung.security.SecurityRoles.ZEITERFASSUNG_VIEW_REPORT_ALL;

@ControllerAdvice(basePackages = {"de.focusshift.zeiterfassung"})
public class AuthorityControllerAdvice {

    private final CurrentUserProvider currentUserProvider;

    public AuthorityControllerAdvice(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @ModelAttribute
    void authorities(Model model, HttpServletRequest request) {

        final User currentUser = currentUserProvider.getCurrentUser();

        model.addAttribute("allowed_viewReportsAll", currentUser.hasAuthority(ZEITERFASSUNG_VIEW_REPORT_ALL));
        model.addAttribute("allowed_editAuthorities", currentUser.hasAuthority(ZEITERFASSUNG_EDIT_AUTHORITIES));
    }
}
