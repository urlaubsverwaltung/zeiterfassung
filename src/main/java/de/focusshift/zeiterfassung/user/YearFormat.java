package de.focusshift.zeiterfassung.user;

public enum YearFormat {

    NONE(""),
    SHORT("yy"),
    FULL("yyyy");

    private final String format;

    YearFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
