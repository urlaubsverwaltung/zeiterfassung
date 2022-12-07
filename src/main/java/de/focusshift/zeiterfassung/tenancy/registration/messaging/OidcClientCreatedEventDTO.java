package de.focusshift.zeiterfassung.tenancy.registration.messaging;

public record OidcClientCreatedEventDTO(String tenantId, String clientId, String clientSecret) {
}
