package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.email.EMailConfigurationProperties;
import de.focusshift.zeiterfassung.email.EMailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withPropertyValues(
            "zeiterfassung.email.from=zeiterfassung@example.org",
            "zeiterfassung.email.replyTo=no-reply@example.org"
        )
        .withUserConfiguration(FeedbackConfiguration.class)
        .withBean(EMailService.class, new JavaMailSenderImpl(), new EMailConfigurationProperties())
        .withBean("emailTemplateEngine", SpringTemplateEngine.class)
        .withBean(FeedbackGivenControllerAdvice.class);

    @Test
    void ensureNoServiceAndNoControllerWhenNothingIsConfigured() {
        applicationContextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(FeedbackService.class);
                assertThat(context).hasSingleBean(FeedbackGivenControllerAdvice.class);
            });
    }

    @Test
    void ensureNoServiceAndNoControllerWhenFeedbackIsDisabled() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.feedback.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(FeedbackService.class);
                assertThat(context).hasSingleBean(FeedbackGivenControllerAdvice.class);
            });
    }

    @Test
    void ensureServiceAndControllerWhenFeedbackIsEnabled() {
        applicationContextRunner
            .withPropertyValues(
                "zeiterfassung.feedback.enabled=true",
                "zeiterfassung.feedback.email.to=feedback@example.org"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(FeedbackService.class);
                assertThat(context).hasSingleBean(FeedbackGivenControllerAdvice.class);
            });
    }
}
