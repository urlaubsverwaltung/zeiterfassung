package de.focusshift.zeiterfassung.search;

import de.focusshift.zeiterfassung.search.UserSuggestionLink.Icon;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;

@Component
public class UserSearchViewHelper {

    /**
     * Name of the user search submit button to signal a user search.
     */
    public static final String USER_SEARCH_SIGNAL = "action-user-search";

    /**
     * Name of the user search query parameter.
     */
    public static final String USER_SEARCH_QUERY_PARAM = "query";

    /**
     * ID of the user suggestions turbo frame.
     */
    public static final String FRAME_USERS_SUGGESTION = "frame-users-suggestions";

    private static final int USER_RESULT_LIMIT = 6;

    private final UserManagementService userManagementService;

    UserSearchViewHelper(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * Prepares the model for user search suggestion list and returns the corresponding ModelAndView.
     *
     * <p>
     * Note that this method does not check authorization!
     *
     * @param userQuery search query
     * @param currentUser the currently logged-in user
     * @param model view model
     * @param suggestionMainLinkBuilder function to create the user suggestion main link
     * @return prepared ModelAndView
     */
    public ModelAndView getSuggestionFragment(String userQuery, CurrentOidcUser currentUser, Model model, Function<User, String> suggestionMainLinkBuilder) {

        if (isAllowedToSearch(currentUser)) {
            prepareUserSearchModel(model, currentUser, userQuery,
                suggestion -> new UserSearchViewHelper.UserSuggestionBlueprint(suggestionMainLinkBuilder.apply(suggestion))
            );
            return new ModelAndView("fragments/user-search::#" + FRAME_USERS_SUGGESTION);
        }

        return new ModelAndView("error/404");
    }

    /**
     * Checks whether the user is allowed to search for other users. This means user can do at least one
     * thing for another user (e.g. editing permissions).
     *
     * @return <code>true</code> if allowed, <code>false</code> otherwise
     */
    public static boolean isAllowedToSearch(CurrentOidcUser user) {
        return user.hasAnyRole(
            ZEITERFASSUNG_VIEW_REPORT_ALL,
            ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL,
            ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,
            ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,
            ZEITERFASSUNG_PERMISSIONS_EDIT_ALL
        );
    }

    private void prepareUserSearchModel(Model model, CurrentOidcUser currentUser, String query, UserSuggestionSupplier customizer) {

        final List<User> users = userManagementService.findAllUsers(query);
        final List<UserSuggestion> suggestions = users.stream()
            .limit(USER_RESULT_LIMIT)
            .map(user -> toUserSearchSuggestion(currentUser, user, query, customizer))
            .toList();

        model.addAttribute("userSearchSuggestions", suggestions);
    }

    public record UserSuggestionBlueprint(@NonNull String mainLink) {
    }

    @FunctionalInterface
    public interface UserSuggestionSupplier {
        UserSuggestionBlueprint get(User suggestion);
    }

    private static UserSuggestion toUserSearchSuggestion(CurrentOidcUser currentUser, User user, String query, UserSuggestionSupplier customizer) {

        final UserSuggestionBlueprint template = customizer.get(user);
        final Long userLocalId = user.userLocalId().value();
        final boolean isLoggedInUser = currentUser.getUserIdComposite().equals(user.userIdComposite());

        final List<UserSuggestionLink> links = new ArrayList<>();
        if (isLoggedInUser || currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL)) {
            final String href = isLoggedInUser ? "/timeentries" : "/timeentries/users/" + userLocalId;
            links.add(new UserSuggestionLink(withQuery(href, query), "user-search.suggestion.link.time", Icon.TIME));
        }
        if (isLoggedInUser || currentUser.hasRole(ZEITERFASSUNG_VIEW_REPORT_ALL)) {
            final String href = isLoggedInUser ? "/report" : "/report?user=" + userLocalId;
            links.add(new UserSuggestionLink(withQuery(href, query), "user-search.suggestion.link.reports", Icon.REPORTS));
        }
        if (currentUser.hasAnyRole(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, ZEITERFASSUNG_PERMISSIONS_EDIT_ALL, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL)) {
            final String href = "/users/" + userLocalId;
            links.add(new UserSuggestionLink(withQuery(href, query), "user-search.suggestion.link.settings", Icon.SETTINGS));
        }

        final String mainHref = withQuery(template.mainLink(), query);

        return new UserSuggestion(userLocalId, user.fullName(), user.initials(), user.email().value(), mainHref, links);
    }

    private static String withQuery(String url, String query) {
        return UriComponentsBuilder.fromUriString(url)
            .queryParam(USER_SEARCH_QUERY_PARAM, query)
            .build().toUriString();
    }
}
