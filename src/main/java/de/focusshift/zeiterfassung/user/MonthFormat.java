package de.focusshift.zeiterfassung.user;

public enum MonthFormat {

    NONE(""),
    TWO_DIGIT("MM"),
    STRING("MMMM"),
    STRING_SHORT("MMM");

    private final String format;

    MonthFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
