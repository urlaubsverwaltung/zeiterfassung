package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.branding.BrandingConfigProperties;
import de.focusshift.zeiterfassung.email.EMailService;
import de.focusshift.zeiterfassung.feedback.events.FeedbackGivenEvent;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.IContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = {FeedbackGivenListenerEmail.class, FeedbackConfigurationProperties.class, FeedbackGivenListenerEmailTest.BrandingTestConfiguration.class},
    properties = {
        "zeiterfassung.feedback.enabled=true",
        "zeiterfassung.feedback.email.to=zeiterfassung@example.org",
        "zeiterfassung.branding.name=Custom Name"
    }
)
class FeedbackGivenListenerEmailTest {

    @Configuration
    @EnableConfigurationProperties(BrandingConfigProperties.class)
    static class BrandingTestConfiguration {
    }

    @Autowired
    private FeedbackGivenListenerEmail sut;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @MockitoBean
    private EMailService eMailService;

    @MockitoBean(name = "emailTemplateEngine")
    private ITemplateEngine emailTemplateEngine;

    @MockitoBean
    private FeedbackConfigurationProperties feedbackConfigurationProperties;

    @Test
    void ensureHandledFeedbackGivenEventSendsAnEmail() {

        final EMailAddress sender = new EMailAddress("user@example.org");
        final FeedbackGivenEvent event = new FeedbackGivenEvent(sender, "awesome feedback message");

        mockFeedbackEmailConfiguration("feedback@example.org");
        when(emailTemplateEngine.process(eq("text/user-feedback.txt"), any(IContext.class))).thenReturn("rendered text email");

        applicationEventPublisher.publishEvent(event);

        await().untilAsserted(() -> {
            verify(eMailService).sendMail("feedback@example.org", "Custom Name - Nutzer Feedback", "rendered text email", "");
        });
    }

    @Test
    void ensureHandledFeedbackGivenEventSendsAnEmailWithCorrectModel() {

        final EMailAddress sender = new EMailAddress("user@example.org");
        final FeedbackGivenEvent event = new FeedbackGivenEvent(sender, "awesome feedback message");

        mockFeedbackEmailConfiguration("feedback@example.org");
        when(emailTemplateEngine.process(eq("text/user-feedback.txt"), any(IContext.class))).thenReturn("rendered text email");

        applicationEventPublisher.publishEvent(event);

        await().untilAsserted(() -> {
            final ArgumentCaptor<IContext> captor = ArgumentCaptor.forClass(IContext.class);
            verify(emailTemplateEngine).process(eq("text/user-feedback.txt"), captor.capture());

            final IContext model = captor.getValue();
            assertThat(model.getVariable("sender")).isEqualTo("user@example.org");
            assertThat(model.getVariable("message")).isEqualTo("awesome feedback message");
            assertThat(model.getVariable("applicationName")).isEqualTo("Custom Name");
        });
    }

    private void mockFeedbackEmailConfiguration(String to) {
        final FeedbackConfigurationProperties.Email emailConfigurationProperties = new FeedbackConfigurationProperties.Email();
        emailConfigurationProperties.setTo(to);

        when(feedbackConfigurationProperties.isEnabled()).thenReturn(true);
        when(feedbackConfigurationProperties.getEmail()).thenReturn(emailConfigurationProperties);
    }
}
