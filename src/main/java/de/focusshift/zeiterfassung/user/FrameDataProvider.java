package de.focusshift.zeiterfassung.user;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Provides all necessary information for the frame (navigation, footer, ...)
 */
@ControllerAdvice
class FrameDataProvider {

    @ModelAttribute
    public void addLocale(Model model, @AuthenticationPrincipal OidcUser oidcUser, HttpServletRequest httpServletRequest) {

        model.addAttribute("lang", LocaleContextHolder.getLocale().toLanguageTag());

        model.addAttribute("currentRequestURI", httpServletRequest.getRequestURI());
        model.addAttribute("header_referer", httpServletRequest.getHeader("Referer"));

        if (oidcUser != null) {
            model.addAttribute("signedInUser", oidcUserToSignedInUserDto(oidcUser));
        }
    }

    private static SignedInUserDto oidcUserToSignedInUserDto(OidcUser oidcUser) {
        final String fullName = oidcUser.getUserInfo().getFullName();

        return new SignedInUserDto(fullName);
    }
}
