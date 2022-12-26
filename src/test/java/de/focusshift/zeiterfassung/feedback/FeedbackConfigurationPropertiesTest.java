package de.focusshift.zeiterfassung.feedback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackConfigurationPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void organizerIsAnEmail() {
        final FeedbackConfigurationProperties feedbackConfigurationProperties = new FeedbackConfigurationProperties();
        feedbackConfigurationProperties.getEmail().setTo("email@example.org");
        final Set<ConstraintViolation<FeedbackConfigurationProperties>> violations = validator.validate(feedbackConfigurationProperties);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "NotAnEmail"})
    @NullSource
    void organizerIsWrong(String input) {
        final FeedbackConfigurationProperties feedbackConfigurationProperties = new FeedbackConfigurationProperties();
        feedbackConfigurationProperties.getEmail().setTo(input);
        final Set<ConstraintViolation<FeedbackConfigurationProperties>> violations = validator.validate(feedbackConfigurationProperties);

        assertThat(violations.size()).isOne();
    }
}
