package de.focusshift.zeiterfassung.feedback;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Component
@Validated
@ConfigurationProperties(prefix = "zeiterfassung.feedback")
public class FeedbackConfigurationProperties {

    @Valid
    private Email email = new Email();

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public static class Email {

        @NotEmpty
        @jakarta.validation.constraints.Email
        private String to;

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }
    }
}
