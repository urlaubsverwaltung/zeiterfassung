package de.focusshift.zeiterfassung.search;

import de.focusshift.zeiterfassung.usermanagement.User;

@FunctionalInterface
public interface UserSuggestionUrlStrategy {

    String buildSuggestionMainLink(User suggestion, SearchContext context);
}
