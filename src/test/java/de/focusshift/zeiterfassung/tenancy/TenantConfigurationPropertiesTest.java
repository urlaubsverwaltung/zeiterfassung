package de.focusshift.zeiterfassung.tenancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static javax.validation.Validation.buildDefaultValidatorFactory;
import static org.assertj.core.api.Assertions.assertThat;

class TenantConfigurationPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void ensureModeHasToBeSet() {
        final TenantConfigurationProperties tenantConfigurationProperties = new TenantConfigurationProperties();
        tenantConfigurationProperties.setMode(null);
        final Set<ConstraintViolation<TenantConfigurationProperties>> violations = validator.validate(tenantConfigurationProperties);

        assertThat(violations.size()).isOne();
    }
}
