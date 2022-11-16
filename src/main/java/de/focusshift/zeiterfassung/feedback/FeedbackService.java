package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.tenantuser.EMailAddress;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
class FeedbackService {

    private final ApplicationEventPublisher applicationEventPublisher;

    FeedbackService(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    void sendFeedback(EMailAddress email, String message) {
        applicationEventPublisher.publishEvent(new FeedbackGivenEvent(email, message));
    }
}
