package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.search.SearchContext;
import de.focusshift.zeiterfassung.search.UserSuggestionUrlStrategy;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.usermanagement.User;
import org.springframework.stereotype.Component;

@Component
public class TimeEntryUserSuggestionUrlStrategy implements UserSuggestionUrlStrategy {

    @Override
    public String buildSuggestionMainLink(User suggestion, SearchContext context) {

        final CurrentOidcUser user = context.getUser();
        final boolean isLoggedInUser = user.getUserIdComposite().equals(suggestion.userIdComposite());

        if (isLoggedInUser) {
            return "/timeentries";
        } else {
            return "/timeentries/users/%s".formatted(suggestion.userLocalId().value());
        }
    }
}
