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

        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Map.of("user-feedback-given", true));

        final Model model = new ConcurrentModel();

        final FeedbackGivenControllerAdvice sut = new FeedbackGivenControllerAdvice();
        sut.userFeedbackGiven(model, mockHttpServletRequest);
        assertThat(model.getAttribute("showFeedbackKudo")).isEqualTo(true);
    }

    @Test
    void ensureNotSetAttributeIfUserFeedbackWasNotGiven() {

        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Map.of());

        final Model model = new ConcurrentModel();

        final FeedbackGivenControllerAdvice sut = new FeedbackGivenControllerAdvice();
        sut.userFeedbackGiven(model, mockHttpServletRequest);
        assertThat(model.getAttribute("showFeedbackKudo")).isNull();
    }

    @Test
    void ensureNotSetAttributeIfUserFeedbackWasFalse() {

        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Map.of("user-feedback-given", false));

        final Model model = new ConcurrentModel();

        final FeedbackGivenControllerAdvice sut = new FeedbackGivenControllerAdvice();
        sut.userFeedbackGiven(model, mockHttpServletRequest);
        assertThat(model.getAttribute("showFeedbackKudo")).isNull();
    }
}
