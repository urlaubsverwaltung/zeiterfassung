package de.focusshift.zeiterfassung.security.oidc.clientregistration.messaging;

public record OidcClientCreatedEventDTO(String tenantId, String clientId, String clientSecret) {
}
