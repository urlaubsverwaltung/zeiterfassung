package de.focusshift.zeiterfassung.security;

public class SecurityRules {

    public static final String ALLOW_EDIT_AUTHORITIES = "hasAuthority('ROLE_ZEITERFASSUNG_EDIT_AUTHORITIES')";

    private SecurityRules() {}
}
