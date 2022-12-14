package de.focusshift.zeiterfassung.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void ensureAllValuesHasToBeSet() {
        final SecurityConfigurationProperties securityConfigurationProperties = new SecurityConfigurationProperties();
        securityConfigurationProperties.setPostLogoutRedirectUri(null);
        securityConfigurationProperties.setLoginFormUrl(null);
        securityConfigurationProperties.setClaimMapper(null);
        final Set<ConstraintViolation<SecurityConfigurationProperties>> violations = validator.validate(securityConfigurationProperties);

        assertThat(violations).hasSize(3);
    }
}
