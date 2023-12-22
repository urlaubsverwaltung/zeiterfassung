package de.focusshift.zeiterfassung.feedback;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FeedbackConfigurationProperties.class)
class FeedbackConfiguration {

}
