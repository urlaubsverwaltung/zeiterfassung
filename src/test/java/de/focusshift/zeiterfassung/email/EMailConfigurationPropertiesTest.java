package de.focusshift.zeiterfassung.email;

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

class EMailConfigurationPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void ensureFromIsAnEmail() {
        final EMailConfigurationProperties eMailConfigurationProperties = new EMailConfigurationProperties();
        eMailConfigurationProperties.setFrom("email@example.org");
        eMailConfigurationProperties.setFromDisplayName("from");
        eMailConfigurationProperties.setReplyTo("reply@example.org");
        eMailConfigurationProperties.setReplyToDisplayName("to");
        final Set<ConstraintViolation<EMailConfigurationProperties>> violations = validator.validate(eMailConfigurationProperties);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "NotAnEmail"})
    @NullSource
    void ensureErrorOnFromNotAnEmail(String input) {
        final EMailConfigurationProperties eMailConfigurationProperties = new EMailConfigurationProperties();
        eMailConfigurationProperties.setFrom(input);
        eMailConfigurationProperties.setReplyTo("replyTo@example.org");
        eMailConfigurationProperties.setReplyToDisplayName("to");
        eMailConfigurationProperties.setFromDisplayName("from");
        final Set<ConstraintViolation<EMailConfigurationProperties>> violations = validator.validate(eMailConfigurationProperties);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void ensureReplyToIsAnEmail() {
        final EMailConfigurationProperties eMailConfigurationProperties = new EMailConfigurationProperties();
        eMailConfigurationProperties.setFrom("email@example.org");
        eMailConfigurationProperties.setFromDisplayName("from");
        eMailConfigurationProperties.setReplyTo("reply@example.org");
        eMailConfigurationProperties.setReplyToDisplayName("to");
        final Set<ConstraintViolation<EMailConfigurationProperties>> violations = validator.validate(eMailConfigurationProperties);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "NotAnEmail"})
    @NullSource
    void ensureErrorOnReplyToNotAnEmail(String input) {
        final EMailConfigurationProperties eMailConfigurationProperties = new EMailConfigurationProperties();
        eMailConfigurationProperties.setReplyTo(input);
        eMailConfigurationProperties.setFrom("from@example.org");
        eMailConfigurationProperties.setReplyToDisplayName("to");
        eMailConfigurationProperties.setFromDisplayName("from");
        final Set<ConstraintViolation<EMailConfigurationProperties>> violations = validator.validate(eMailConfigurationProperties);

        assertThat(violations).isNotEmpty();
    }
}
