package de.focusshift.zeiterfassung.web.html;

/**
 * A non-null aria-current state on an element indicates that this element represents the current item
 * within a container or set of related elements.
 *
 * <p>
 * Note: Don't use aria-current as a substitute for aria-selected in gridcell, option, row or tab.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Attributes/aria-current">MDN aria-current</a>
 */
public enum AriaCurrent {

    /**
     * Represents the current page within a set of pages such as the link to the current document in a breadcrumb.
     */
    PAGE("page"),

    /**
     * Represents the current step within a process such as the current step in an enumerated multi step checkout flow.
     */
    STEP("step"),

    /**
     * Represents the current location within an environment or context such as the image that is visually highlighted as the current component of a flow chart.
     */
    LOCATION("location"),

    /**
     * Represents the current date within a collection of dates such as the current date within a calendar.
     */
    DATE("date"),

    /**
     * Represents the current time within a set of times such as the current time within a timetable.
     */
    TIME("time"),

    /**
     * Represents the current item within a set.
     */
    TRUE("true"),

    /**
     * Does not represent the current item within a set.
     */
    FALSE("false");

    private final String value;

    AriaCurrent(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
