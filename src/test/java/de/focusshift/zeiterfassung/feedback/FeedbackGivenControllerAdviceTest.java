package de.focusshift.zeiterfassung.feedback;


import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE;

class FeedbackGivenControllerAdviceTest {

    @Test
    void ensureSetAttributeIfUserFeedbackWasGiven() {

        final FeedbackConfigurationProperties feedbackProperties = enabledFeedbackConfigurationProperties();

        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Map.of("user-feedback-given", true));

        final Model model = new ConcurrentModel();

        final FeedbackGivenControllerAdvice sut = new FeedbackGivenControllerAdvice(feedbackProperties);
        sut.userFeedbackGiven(model, mockHttpServletRequest);
        assertThat(model.getAttribute("showFeedbackKudo")).isEqualTo(true);
    }

    @Test
    void ensureNotSetAttributeIfUserFeedbackWasNotGiven() {

        final FeedbackConfigurationProperties feedbackProperties = enabledFeedbackConfigurationProperties();

        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Map.of());

        final Model model = new ConcurrentModel();

        final FeedbackGivenControllerAdvice sut = new FeedbackGivenControllerAdvice(feedbackProperties);
        sut.userFeedbackGiven(model, mockHttpServletRequest);
        assertThat(model.getAttribute("showFeedbackKudo")).isNull();
    }

    @Test
    void ensureNotSetAttributeIfUserFeedbackWasFalse() {

        final FeedbackConfigurationProperties feedbackProperties = enabledFeedbackConfigurationProperties();

        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Map.of("user-feedback-given", false));

        final Model model = new ConcurrentModel();

        final FeedbackGivenControllerAdvice sut = new FeedbackGivenControllerAdvice(feedbackProperties);
        sut.userFeedbackGiven(model, mockHttpServletRequest);
        assertThat(model.getAttribute("showFeedbackKudo")).isNull();
    }

    private FeedbackConfigurationProperties enabledFeedbackConfigurationProperties() {

        final FeedbackConfigurationProperties.Email email = new FeedbackConfigurationProperties.Email();
        email.setTo("to@example.org");

        final FeedbackConfigurationProperties feedbackConfigurationProperties = new FeedbackConfigurationProperties();
        feedbackConfigurationProperties.setEnabled(true);
        feedbackConfigurationProperties.setEmail(email);

        return feedbackConfigurationProperties;
    }
}
