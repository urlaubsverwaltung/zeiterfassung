package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.usermanagement.UserAuthoritiesUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
class SessionUpdater {

    private final SessionService sessionService;

    SessionUpdater(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Async
    @EventListener
    public void handleUserAuthoritiesUpdated(UserAuthoritiesUpdatedEvent event) {
        sessionService.markSessionToReloadAuthorities(event.user().id().value());
    }
}
