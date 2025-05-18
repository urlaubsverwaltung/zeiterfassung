package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.feedback.events.FeedbackGivenEvent;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {
    private FeedbackService sut;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;


    @BeforeEach
    void setUp() {
        sut = new FeedbackService(applicationEventPublisher);
    }

    @Test
    void ensureFeedbackGivenEventIsPublished() {

        sut.sendFeedback(new EMailAddress("user@example.org"), "feedback message");

        verify(applicationEventPublisher).publishEvent(new FeedbackGivenEvent(new EMailAddress("user@example.org"), "feedback message"));
    }

}
