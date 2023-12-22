package de.focusshift.zeiterfassung.feedback;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Map;

import static de.focusshift.zeiterfassung.feedback.FeedbackViewController.FLASH_FEEDBACK_GIVEN;
import static java.lang.Boolean.TRUE;

@ControllerAdvice(basePackages = {"de.focusshift.zeiterfassung"})
class FeedbackGivenControllerAdvice {

    private final FeedbackConfigurationProperties feedbackProperties;

    FeedbackGivenControllerAdvice(FeedbackConfigurationProperties feedbackProperties) {
        this.feedbackProperties = feedbackProperties;
    }

    @ModelAttribute
    void userFeedbackGiven(Model model, HttpServletRequest request) {

        final boolean enabled = feedbackProperties.isEnabled();
        model.addAttribute("feedbackEnabled", enabled);

        if (enabled) {
            final Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
            if (inputFlashMap != null && TRUE.equals(inputFlashMap.get(FLASH_FEEDBACK_GIVEN))) {
                model.addAttribute("showFeedbackKudo", true);
            }
        }
    }
}
