package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(prefix = "zeiterfassung.feedback", name = "enabled", havingValue = "true")
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
