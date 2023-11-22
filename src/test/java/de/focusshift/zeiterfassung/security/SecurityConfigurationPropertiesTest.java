package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.security.oidc.OidcSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
        final OidcSecurityProperties securityConfigurationProperties = new OidcSecurityProperties();
        securityConfigurationProperties.setPostLogoutRedirectUri(null);
        securityConfigurationProperties.setLoginFormUrl(null);
        final Set<ConstraintViolation<OidcSecurityProperties>> violations = validator.validate(securityConfigurationProperties);

        assertThat(violations).hasSize(2);
    }
}
