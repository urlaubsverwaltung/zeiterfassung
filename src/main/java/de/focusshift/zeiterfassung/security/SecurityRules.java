package de.focusshift.zeiterfassung.security;

public final class SecurityRules {

    public static final String ALLOW_EDIT_WORKING_TIME_ALL = "hasAuthority('ROLE_ZEITERFASSUNG_WORKING_TIME_EDIT_ALL')";
    public static final String ALLOW_OVERTIME_ACCOUNT_EDIT_ALL = "hasAuthority('ROLE_ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL')";

    private SecurityRules() {}
}
