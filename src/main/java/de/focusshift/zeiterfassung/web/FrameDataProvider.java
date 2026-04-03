package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.web.html.AriaCurrent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_SETTINGS_GLOBAL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL;
import static de.focusshift.zeiterfassung.usermanagement.User.generateInitials;

/**
 * Provides all necessary information for the frame (navigation, footer, ...)
 */
@ControllerAdvice
class FrameDataProvider {

    private final MenuProperties menuProperties;

    FrameDataProvider(MenuProperties menuProperties) {
        this.menuProperties = menuProperties;
    }

    @ModelAttribute
    public void addLocale(Model model, @CurrentUser CurrentOidcUser user, HttpServletRequest httpServletRequest) {

        model.addAttribute("lang", LocaleContextHolder.getLocale().toLanguageTag());

        model.addAttribute("menuHelpUrl", menuProperties.getHelp().getUrl());
        model.addAttribute("navigation", createNavigation(httpServletRequest, user));

        model.addAttribute("currentRequestURI", httpServletRequest.getRequestURI());
        model.addAttribute("header_referer", httpServletRequest.getHeader("Referer"));

        model.addAttribute("signedInUser", oidcUserToSignedInUserDto(user));
    }

    private NavigationDto createNavigation(HttpServletRequest request, CurrentOidcUser currentUser) {

        final String url = request.getRequestURI();

        final String timeentries = "/timeentries";
        final boolean timeentriesActive = url.startsWith(timeentries);
        final AriaCurrent timeentriesCurrent = (timeentriesActive && !url.startsWith("/timeentries/users")) ? AriaCurrent.PAGE : AriaCurrent.FALSE;

        final String report = "/report";
        final boolean reportActive = url.startsWith(report);
        final AriaCurrent reportCurrent = reportActive ? AriaCurrent.PAGE : AriaCurrent.FALSE;

        final String users = "/users";
        final boolean usersActive = url.startsWith(users);
        final AriaCurrent usersCurrent = usersActive ? AriaCurrent.PAGE : AriaCurrent.FALSE;

        final String settings = "/settings";
        final boolean settingsActive = url.startsWith(settings);
        final AriaCurrent settingsCurrent = settingsActive ? AriaCurrent.PAGE : AriaCurrent.FALSE;

        final List<NavigationItemDto> items = new ArrayList<>();
        items.add(new NavigationItemDto("main-navigation-link-timeentries", timeentries, "navigation.main.timetrack", timeentriesActive, timeentriesCurrent));
        items.add(new NavigationItemDto("main-navigation-link-reports", report, "navigation.main.reports", reportActive, reportCurrent, "navigation-link-reports"));

        if (currentUser.hasAnyRole(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL, ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, ZEITERFASSUNG_PERMISSIONS_EDIT_ALL)) {
            items.add(new NavigationItemDto("main-navigation-link-users", users, "navigation.main.users", usersActive, usersCurrent, "navigation-link-users"));
        }

        if (currentUser.hasAnyRole(ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL, ZEITERFASSUNG_SETTINGS_GLOBAL)) {
            items.add(new NavigationItemDto("main-navigation-link-settings", settings, "navigation.main.settings", settingsActive, settingsCurrent, "navigation-link-settings"));
        }

        return new NavigationDto(items);
    }

    private static SignedInUserDto oidcUserToSignedInUserDto(OidcUser oidcUser) {
        final String fullName = oidcUser.getUserInfo().getFullName();
        return new SignedInUserDto(fullName, generateInitials(fullName));
    }
}
