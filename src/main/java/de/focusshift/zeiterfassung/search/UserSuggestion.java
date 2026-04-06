package de.focusshift.zeiterfassung.search;

import java.util.List;

public record UserSuggestion(
    long userLocalId,
    String name,
    String initials,
    String email,
    String href,
    List<UserSuggestionLink> links
) {
}
