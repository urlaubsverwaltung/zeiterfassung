package de.focusshift.zeiterfassung.security.oidc.clientregistration.messaging;

public record OidcClientDeletedEventDTO(String tenantId, String clientId) {
}
