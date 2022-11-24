package de.focusshift.zeiterfassung.usermanagement;

public record UserAccountDto(Long id, String firstName, String lastName, String fullName, String email, UserAccountAuthoritiesDto authorities) {
}
