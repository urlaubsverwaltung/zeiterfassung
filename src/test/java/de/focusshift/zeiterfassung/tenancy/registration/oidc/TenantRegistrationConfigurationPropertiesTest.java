package de.focusshift.zeiterfassung.tenancy.registration.oidc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRegistrationConfigurationPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void ensureAllValuesHasToBeSet() {
        final TenantRegistrationConfigurationProperties securityConfigurationProperties = new TenantRegistrationConfigurationProperties();
        securityConfigurationProperties.setServerUrl(null);
        securityConfigurationProperties.setRedirectUriTemplate(null);
        final Set<ConstraintViolation<TenantRegistrationConfigurationProperties>> violations = validator.validate(securityConfigurationProperties);

        assertThat(violations).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "NotAUrl"})
    void ensuresServerUrlMustBeAUrlOrEmpty(String input) {
        final TenantRegistrationConfigurationProperties securityConfigurationProperties = new TenantRegistrationConfigurationProperties();
        securityConfigurationProperties.setServerUrl(input);
        securityConfigurationProperties.setRedirectUriTemplate("{baseScheme}://{baseHost}:${server.port}/login/oauth2/code/{registrationId}");
        final Set<ConstraintViolation<TenantRegistrationConfigurationProperties>> violations = validator.validate(securityConfigurationProperties);

        assertThat(violations).hasSize(1);
    }
}
