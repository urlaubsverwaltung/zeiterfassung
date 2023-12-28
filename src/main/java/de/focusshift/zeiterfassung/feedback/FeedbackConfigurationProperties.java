package de.focusshift.zeiterfassung.feedback;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.springframework.validation.ValidationUtils.rejectIfEmpty;

@Validated
@ConfigurationProperties("zeiterfassung.feedback")
public class FeedbackConfigurationProperties implements Validator {

    private boolean enabled = false;
    private Email email;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return FeedbackConfigurationProperties.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final var properties = (FeedbackConfigurationProperties) target;
        if (properties.isEnabled()) {
            rejectIfEmpty(errors, "email", "", "Email must not be null");
        }
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
