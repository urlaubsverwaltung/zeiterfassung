package de.focusshift.zeiterfassung.integration.oidc.clientregistration;

public interface OidcClientRegistrationEventHandler {

    void handleEvent(OidcClientCreatedEventDTO event);

    void handleEvent(OidcClientDeletedEventDTO event);
}
