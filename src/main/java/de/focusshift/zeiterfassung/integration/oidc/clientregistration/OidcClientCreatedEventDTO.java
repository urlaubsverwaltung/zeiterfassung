package de.focusshift.zeiterfassung.integration.oidc.clientregistration;

public record OidcClientCreatedEventDTO(String tenantId, String clientId, String clientSecret) {
}
