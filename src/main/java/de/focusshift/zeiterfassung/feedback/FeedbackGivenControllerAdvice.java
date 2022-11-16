package de.focusshift.zeiterfassung.feedback;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static java.lang.Boolean.TRUE;

@ControllerAdvice(basePackages = {"de.focusshift.zeiterfassung"})
class FeedbackGivenControllerAdvice {

    @ModelAttribute
    void userFeedbackGiven(Model model, HttpServletRequest request) {
        final Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
        if (inputFlashMap != null && TRUE.equals(inputFlashMap.get("user-feedback-given"))) {
            model.addAttribute("showFeedbackKudo", true);
        }
    }
}
