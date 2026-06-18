package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchUiFragmentSupplier;
import de.focusshift.zeiterfassung.search.UserSuggestionUrlStrategy;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.slf4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasAnyAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_ALL', 'ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL', 'ZEITERFASSUNG_PERMISSIONS_EDIT_ALL')")
class UserManagementController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserManagementService userManagementService;
    private final UserManagementSearchUiFragmentSupplier searchUiFragmentSupplier;

    UserManagementController(
        UserManagementService userManagementService,
        UserManagementSearchUiFragmentSupplier searchUiFragmentSupplier
    ) {
        this.userManagementService = userManagementService;
        this.searchUiFragmentSupplier = searchUiFragmentSupplier;
    }

    @Override
    public UserSuggestionUrlStrategy userSuggestionUrlStrategy() {
        return (suggestion, context) -> "/users/%s".formatted(suggestion.userLocalId().value());
    }

    @Override
    public UserSearchUiFragmentSupplier userSearchUiFragmentSupplier() {
        return searchUiFragmentSupplier;
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

        if (StringUtils.hasText(turboFrame)) {
            return "usermanagement/users::#" + turboFrame;
        } else {
            return "usermanagement/users";
        }
    }

    @GetMapping("/{id}")
    ModelAndView user(@PathVariable Long id, @CurrentUser CurrentOidcUser currentUser) {
        return forward(currentUser, id);
    }

    private ModelAndView forward(CurrentOidcUser currentUser, Long selectedUserId) {
        final String slug;

        if (currentUser.hasRole(ZEITERFASSUNG_WORKING_TIME_EDIT_ALL)) {
            slug = "working-time";
        } else if (currentUser.hasRole(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL)) {
            slug = "overtime-account";
        } else if (currentUser.hasRole(ZEITERFASSUNG_PERMISSIONS_EDIT_ALL)) {
            slug = "permissions";
        } else {
            slug = null;
        }

        if (slug == null) {
            LOG.error("could not determine user settings slug for logged-in user.");
            return new ModelAndView("error/403");
        }

        final String uri = "forward:/users/%s/%s".formatted(selectedUserId, slug);
        return new ModelAndView(uri);
    }

    static UserDto userToDto(User user) {
        return new UserDto(user.userLocalId().value(), user.givenName(), user.familyName(), user.givenName() + " " + user.familyName(), user.initials(), user.email().value());
    }
}
