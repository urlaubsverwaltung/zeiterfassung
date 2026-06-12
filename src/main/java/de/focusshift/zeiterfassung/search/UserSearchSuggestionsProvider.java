package de.focusshift.zeiterfassung.search;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
import static org.springframework.util.StringUtils.hasText;

@Component
class UserSearchSuggestionsProvider {

    public static final String USER_SEARCH_QUERY_PARAM = "query";

    private static final int USER_RESULT_LIMIT = 6;

    private final UserManagementService userManagementService;

    UserSearchSuggestionsProvider(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * Builds the user search suggestions for the given query, scoped to what the logged-in user is allowed to see.
     *
     * <p>
     * Note that this method does not check authorization!
     *
     * @param oidcUser the currently logged-in user
     * @param query search query
     * @param mainLinkBuilder builds the main link of a suggestion
     * @return the (possibly empty) list of suggestions, limited to {@value #USER_RESULT_LIMIT}
     */
    List<UserSuggestion> userSuggestions(CurrentOidcUser oidcUser, String query, Function<User, String> mainLinkBuilder) {

        final List<User> users = userManagementService.findAllUsers(query);

        return users.stream()
            .limit(USER_RESULT_LIMIT)
            .map(user -> toUserSearchSuggestion(oidcUser, user, query, mainLinkBuilder))
            .toList();
    }

    private static UserSuggestion toUserSearchSuggestion(CurrentOidcUser currentUser, User user, String query, Function<User, String> mainLinkBuilder) {

        final Long userLocalId = user.userLocalId().value();
        final boolean isLoggedInUser = currentUser.getUserIdComposite().equals(user.userIdComposite());

        final List<UserSuggestionLink> links = new ArrayList<>();
        if (isLoggedInUser || currentUser.hasRole(ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL)) {
            final String href = isLoggedInUser ? "/timeentries" : "/timeentries/users/" + userLocalId;
            links.add(new UserSuggestionLink(withQuery(href, query), "user-search.suggestion.link.time", UserSuggestionLink.Icon.TIME));
        }
        if (isLoggedInUser || currentUser.hasRole(ZEITERFASSUNG_VIEW_REPORT_ALL)) {
            final String href = isLoggedInUser ? "/report" : "/report?user=" + userLocalId;
            links.add(new UserSuggestionLink(withQuery(href, query), "user-search.suggestion.link.reports", UserSuggestionLink.Icon.REPORTS));
        }
        if (currentUser.hasAnyRole(ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL, ZEITERFASSUNG_PERMISSIONS_EDIT_ALL, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL)) {
            final String href = "/users/" + userLocalId;
            links.add(new UserSuggestionLink(withQuery(href, query), "user-search.suggestion.link.settings", UserSuggestionLink.Icon.SETTINGS));
        }

        final String mainHref = withQuery(mainLinkBuilder.apply(user), query);

        return new UserSuggestion(userLocalId, user.fullName(), user.initials(), user.email().value(), mainHref, links);
    }

    private static String withQuery(String url, String query) {
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(url);
        if (hasText(query)) {
            uriComponentsBuilder.queryParam(USER_SEARCH_QUERY_PARAM, query);
        }
        return uriComponentsBuilder.build().toUriString();
    }
}
