package de.focusshift.zeiterfassung.tenancy.registration.messaging;

public record OidcClientDeletedEventDTO(String tenantId, String clientId) {
}
