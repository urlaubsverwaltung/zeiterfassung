package de.focusshift.zeiterfassung.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MenuPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void helpUrlDefault() {
        final MenuProperties menuProperties = new MenuProperties();
        assertThat(menuProperties.getHelp().getUrl()).isEqualTo("https://urlaubsverwaltung.cloud/hilfe/?utm_source=zeiterfassung-open-source#dokumentation-zeiterfassung");
    }

    @Test
    void helpUrlIsGiven() {
        final MenuProperties menuProperties = new MenuProperties();
        menuProperties.getHelp().setUrl("https://urlaubsverwaltung.cloud/hilfe/?utm_source=zeiterfassung-open-source#dokumentation-zeiterfassung");
        final Set<ConstraintViolation<MenuProperties>> violations = validator.validate(menuProperties);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "NotAnUrl"})
    @NullSource
    void helpUrlIsWrong(String input) {
        final MenuProperties menuProperties = new MenuProperties();
        menuProperties.getHelp().setUrl(input);
        final Set<ConstraintViolation<MenuProperties>> violations = validator.validate(menuProperties);

        assertThat(violations.size()).isOne();
    }

}
