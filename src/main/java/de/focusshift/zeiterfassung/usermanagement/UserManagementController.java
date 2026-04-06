package de.focusshift.zeiterfassung.usermanagement;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.FRAME_USERS_SUGGESTION;
import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasAnyAuthority('ZEITERFASSUNG_WORKING_TIME_EDIT_ALL', 'ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL', 'ZEITERFASSUNG_PERMISSIONS_EDIT_ALL')")
class UserManagementController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserManagementService userManagementService;
    private final UserSearchViewHelper userSearchViewHelper;

    UserManagementController(UserManagementService userManagementService, UserSearchViewHelper userSearchViewHelper) {
        this.userManagementService = userManagementService;
        this.userSearchViewHelper = userSearchViewHelper;
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

    @GetMapping(value = {"","/{userId}"}, params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(@RequestParam(USER_SEARCH_QUERY_PARAM) String query,
                                    @PathVariable(required = false) Long userId,
                                    @RequestHeader(TURBO_FRAME_HEADER) String turboFrame,
                                    @CurrentUser CurrentOidcUser currentUser, Model model) {

        if (FRAME_USERS_SUGGESTION.equals(turboFrame)) {
            return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
                suggestion -> "/users/%s".formatted(suggestion.userLocalId().value())
            );
        } else if ("person-frame".equals(turboFrame) && userId != null) {
            return forward(currentUser, userId);
        } else {
            LOG.error("unknown turbo-frame requested or person-frame but without userId");
            return new ModelAndView("error/404", UNPROCESSABLE_CONTENT);
        }
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
