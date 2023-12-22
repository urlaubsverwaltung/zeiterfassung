package de.focusshift.zeiterfassung.feedback;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withPropertyValues(
            "zeiterfassung.email.from=zeiterfassung@example.org",
            "zeiterfassung.email.replyTo=no-reply@example.org"
        )
        .withUserConfiguration(FeedbackConfigurationProperties.class)
        .withBean(FeedbackService.class)
        .withBean(FeedbackViewController.class)
        .withBean(FeedbackGivenControllerAdvice.class);

    @Test
    void ensureNoServiceAndNoControllerWhenNothingIsConfigured() {
        applicationContextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(FeedbackService.class);
                assertThat(context).doesNotHaveBean(FeedbackViewController.class);
                assertThat(context).hasSingleBean(FeedbackGivenControllerAdvice.class);
            });
    }

    @Test
    void ensureNoServiceAndNoControllerWhenFeedbackIsDisabled() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.feedback.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(FeedbackService.class);
                assertThat(context).doesNotHaveBean(FeedbackViewController.class);
                assertThat(context).hasSingleBean(FeedbackGivenControllerAdvice.class);
            });
    }

    @Test
    void ensureServiceAndControllerWhenFeedbackIsEnabled() {
        applicationContextRunner
            .withPropertyValues("zeiterfassung.feedback.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(FeedbackService.class);
                assertThat(context).hasSingleBean(FeedbackViewController.class);
                assertThat(context).hasSingleBean(FeedbackGivenControllerAdvice.class);
            });
    }
}
