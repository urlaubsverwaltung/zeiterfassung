package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.feedback.events.FeedbackGivenEvent;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import org.springframework.context.ApplicationEventPublisher;

class FeedbackService {

    private final ApplicationEventPublisher applicationEventPublisher;

    FeedbackService(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    void sendFeedback(EMailAddress email, String message) {
        applicationEventPublisher.publishEvent(new FeedbackGivenEvent(email, message));
    }
}
