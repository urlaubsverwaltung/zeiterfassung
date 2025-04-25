package de.focusshift.zeiterfassung.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;

public enum SecurityRole {

    ZEITERFASSUNG_OPERATOR, // used by fss employees only!
    /**
     * Allowed to use the application. Users without this role are not able to see anything!
     */
    ZEITERFASSUNG_USER,
    /**
     * Allowed to view {@link de.focusshift.zeiterfassung.report.ReportWeek} and
     * {@link de.focusshift.zeiterfassung.report.ReportMonth} of all persons.
     */
    ZEITERFASSUNG_VIEW_REPORT_ALL,
    /**
     * Allowed to edit (CRUD) {@link de.focusshift.zeiterfassung.timeentry.TimeEntry} of all persons.
     */
    ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL,
    /**
     * Allowed to edit {@link de.focusshift.zeiterfassung.workingtime.WorkingTime} settings of all persons.
     */
    ZEITERFASSUNG_WORKING_TIME_EDIT_ALL,
    /**
     * @deprecated for removal in 3.x. Use {@link SecurityRole#ZEITERFASSUNG_SETTINGS_GLOBAL} instead.
     */
    @Deprecated(forRemoval = true)
    ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL,
    /**
     * Allowed to edit global application settings like {@link de.focusshift.zeiterfassung.settings.FederalStateSettings}
     * or {@link de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings}.
     */
    ZEITERFASSUNG_SETTINGS_GLOBAL,
    /**
     * Allowed to edit {@link de.focusshift.zeiterfassung.usermanagement.OvertimeAccount} settings of all persons.
     */
    ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL,
    /**
     * Allowed to edit permissions of all persons.
     */
    ZEITERFASSUNG_PERMISSIONS_EDIT_ALL;

    private GrantedAuthority authority;

    public GrantedAuthority authority() {
        if (authority == null) {
            authority = new SimpleGrantedAuthority(this.name());
        }
        return authority;
    }

    public static Optional<SecurityRole> fromAuthority(GrantedAuthority authority) {
        try {
            return Optional.of(SecurityRole.valueOf(authority.getAuthority()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
