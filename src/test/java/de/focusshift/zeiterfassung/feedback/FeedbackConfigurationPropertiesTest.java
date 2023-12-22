package de.focusshift.zeiterfassung.feedback;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;

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
    void emailMustNotBeNull() {

        final FeedbackConfigurationProperties properties = new FeedbackConfigurationProperties();
        properties.setEnabled(true);

        final BeanPropertyBindingResult errors = new BeanPropertyBindingResult(properties, "feebackConfigurationProperties");
        properties.validate(properties, errors);
        assertThat(errors.hasErrors()).isTrue();
    }

    @Test
    void organizerIsAnEmail() {

        final FeedbackConfigurationProperties.Email email = new FeedbackConfigurationProperties.Email();
        email.setTo("email@example.org");

        final Set<ConstraintViolation<FeedbackConfigurationProperties.Email>> violations = validator.validate(email);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "NotAnEmail"})
    @NullSource
    void organizerIsWrong(String input) {

        final FeedbackConfigurationProperties.Email email = new FeedbackConfigurationProperties.Email();
        email.setTo(input);

        final Set<ConstraintViolation<FeedbackConfigurationProperties.Email>> violations = validator.validate(email);
        assertThat(violations.size()).isOne();
    }
}
