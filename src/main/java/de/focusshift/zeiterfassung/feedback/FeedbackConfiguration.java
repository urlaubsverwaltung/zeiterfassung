package de.focusshift.zeiterfassung.feedback;

import de.focusshift.zeiterfassung.email.EMailService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.ITemplateEngine;

@Configuration
@EnableConfigurationProperties(FeedbackConfigurationProperties.class)
public class FeedbackConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "zeiterfassung.feedback", name = "enabled", havingValue = "true")
    static class FeedbackEnabledConfiguration {

        @Bean
        FeedbackService feedbackService(ApplicationEventPublisher applicationEventPublisher) {
            return new FeedbackService(applicationEventPublisher);
        }
        @Bean
        FeedbackViewController feedbackViewController(FeedbackService feedbackService) {
            return new FeedbackViewController(feedbackService);
        }

        @Bean
        FeedbackGivenListenerEmail feedbackGivenListenerEmail(EMailService eMailService, @Qualifier("emailTemplateEngine") ITemplateEngine emailTemplateEngine, FeedbackConfigurationProperties properties) {
            return new FeedbackGivenListenerEmail(eMailService, emailTemplateEngine, properties);
        }
    }
}
