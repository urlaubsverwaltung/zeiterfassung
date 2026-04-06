package de.focusshift.zeiterfassung.search;

public record UserSuggestionLink(String href, String messageKey, Icon icon) {

    public enum Icon {
        TIME,
        REPORTS,
        SETTINGS
    }
}
