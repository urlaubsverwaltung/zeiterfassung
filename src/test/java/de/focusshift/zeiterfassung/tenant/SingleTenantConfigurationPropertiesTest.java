package de.focusshift.zeiterfassung.tenant;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static javax.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;

class SingleTenantConfigurationPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @NullSource
    void ensureDefaultTenantIdIsSet(String input) {
        final SingleTenantConfigurationProperties singleTenantConfigurationProperties = new SingleTenantConfigurationProperties();
        singleTenantConfigurationProperties.setDefaultTenantId(input);
        final Set<ConstraintViolation<SingleTenantConfigurationProperties>> violations = validator.validate(singleTenantConfigurationProperties);

        assertThat(violations.size()).isOne();
    }
}
