package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.email.EMailConstants;
import de.focusshift.zeiterfassung.email.EMailService;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@ConditionalOnProperty(prefix = "zeiterfassung.feedback", name = "enabled", havingValue = "true")
@Component
class FeedbackGivenListenerEmail {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final EMailService eMailService;
    private final ITemplateEngine mailTemplateEngine;
    private final FeedbackConfigurationProperties feedbackConfigurationProperties;

    FeedbackGivenListenerEmail(EMailService eMailService, @Qualifier("emailTemplateEngine") ITemplateEngine mailTemplateEngine,
                               FeedbackConfigurationProperties feedbackConfigurationProperties) {
        this.eMailService = eMailService;
        this.mailTemplateEngine = mailTemplateEngine;
        this.feedbackConfigurationProperties = feedbackConfigurationProperties;
    }

    @Async
    @EventListener
    public void handleFeedbackGiven(FeedbackGivenEvent feedbackGivenEvent) {

        final EMailAddress sender = feedbackGivenEvent.sender();
        final String message = feedbackGivenEvent.message();
        final String subject = "Zeiterfassung - Nutzer Feedback";
        final String to = feedbackConfigurationProperties.getEmail().getTo();

        final Context context = new Context(EMailConstants.DEFAULT_LOCALE);
        context.setVariable("sender", sender.value());
        context.setVariable("message", message);

        final String plainTextEmail = mailTemplateEngine.process("text/user-feedback.txt", context);
        final String htmlEmail = "";

        try {
            eMailService.sendMail(to, subject, plainTextEmail, htmlEmail);
        } catch (MessagingException e) {
            LOG.error("could not send user feedback email to us.", e);
        }
    }
}
