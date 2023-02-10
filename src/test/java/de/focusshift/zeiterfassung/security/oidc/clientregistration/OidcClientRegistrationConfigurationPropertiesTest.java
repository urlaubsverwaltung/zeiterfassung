package de.focusshift.zeiterfassung.security.oidc.clientregistration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OidcClientRegistrationConfigurationPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void ensureAllValuesHasToBeSet() {
        final OidcClientRegistrationConfigurationProperties securityConfigurationProperties = new OidcClientRegistrationConfigurationProperties();
        securityConfigurationProperties.setServerUrl(null);
        securityConfigurationProperties.setRedirectUriTemplate(null);
        final Set<ConstraintViolation<OidcClientRegistrationConfigurationProperties>> violations = validator.validate(securityConfigurationProperties);

        assertThat(violations).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "NotAUrl"})
    void ensuresServerUrlMustBeAUrlOrEmpty(String input) {
        final OidcClientRegistrationConfigurationProperties securityConfigurationProperties = new OidcClientRegistrationConfigurationProperties();
        securityConfigurationProperties.setServerUrl(input);
        securityConfigurationProperties.setRedirectUriTemplate("{baseScheme}://{baseHost}:${server.port}/login/oauth2/code/{registrationId}");
        final Set<ConstraintViolation<OidcClientRegistrationConfigurationProperties>> violations = validator.validate(securityConfigurationProperties);

        assertThat(violations).hasSize(1);
    }
}
