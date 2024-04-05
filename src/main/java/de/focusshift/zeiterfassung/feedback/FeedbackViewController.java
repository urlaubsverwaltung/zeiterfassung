package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/feedback")
@PreAuthorize("hasRole('ZEITERFASSUNG_USER')")
class FeedbackViewController {

    public static final String FLASH_FEEDBACK_GIVEN = "user-feedback-given";

    private final FeedbackService feedbackService;

    FeedbackViewController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public String feedback(FeedbackDto feedbackDto, @AuthenticationPrincipal OidcUser principal, HttpServletRequest request, RedirectAttributes redirectAttributes) {

        feedbackService.sendFeedback(new EMailAddress(principal.getEmail()), feedbackDto.body());

        redirectAttributes.addFlashAttribute(FLASH_FEEDBACK_GIVEN, true);

        final String referer = request.getHeader("referer");
        return "redirect:" + referer;
    }
}
