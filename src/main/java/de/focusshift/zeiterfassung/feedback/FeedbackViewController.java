package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ConditionalOnBean(FeedbackService.class)
@Controller
@RequestMapping("/feedback")
class FeedbackViewController {

    public static final String FLASH_FEEDBACK_GIVEN = "user-feedback-given";

    private final FeedbackService feedbackService;

    FeedbackViewController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public String feedback(FeedbackDto feedbackDto, @CurrentUser CurrentOidcUser currentUser, HttpServletRequest request, RedirectAttributes redirectAttributes) {

        feedbackService.sendFeedback(new EMailAddress(currentUser.getEmail()), feedbackDto.body());

        redirectAttributes.addFlashAttribute(FLASH_FEEDBACK_GIVEN, true);

        final String referer = request.getHeader("referer");
        return "redirect:" + referer;
    }
}
