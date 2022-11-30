package de.focusshift.zeiterfassung.security.oidc.clientregistration.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OidcClientRegistrationRabbitmqConfigurationPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    @NullSource
    void ensureTopicCannotBeEmptyOrNull(String topic) {
        final OidcClientRegistrationRabbitmqConfigurationProperties sut = new OidcClientRegistrationRabbitmqConfigurationProperties();
        sut.setTopic(topic);
        final Set<ConstraintViolation<OidcClientRegistrationRabbitmqConfigurationProperties>> violations = validator.validate(sut);

        assertThat(violations.size()).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    @NullSource
    void ensureRoutingKeyCreatedTemplateCannotBeEmptyOrNull(String routingKeyCreatedTemplate) {
        final OidcClientRegistrationRabbitmqConfigurationProperties sut = new OidcClientRegistrationRabbitmqConfigurationProperties();
        sut.setRoutingKeyCreatedTemplate(routingKeyCreatedTemplate);
        final Set<ConstraintViolation<OidcClientRegistrationRabbitmqConfigurationProperties>> violations = validator.validate(sut);

        assertThat(violations.size()).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    @NullSource
    void ensureRoutingKeyDeletedTwoCannotBeEmptyOrNull(String routingKeyDeletedTwo) {
        final OidcClientRegistrationRabbitmqConfigurationProperties sut = new OidcClientRegistrationRabbitmqConfigurationProperties();
        sut.setRoutingKeyDeletedTwo(routingKeyDeletedTwo);
        final Set<ConstraintViolation<OidcClientRegistrationRabbitmqConfigurationProperties>> violations = validator.validate(sut);

        assertThat(violations.size()).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    @NullSource
    void ensureRoutingKeyDeletedTemplateCannotBeEmptyOrNull(String routingKeyDeletedTemplate) {
        final OidcClientRegistrationRabbitmqConfigurationProperties sut = new OidcClientRegistrationRabbitmqConfigurationProperties();
        sut.setRoutingKeyDeletedTemplate(routingKeyDeletedTemplate);
        final Set<ConstraintViolation<OidcClientRegistrationRabbitmqConfigurationProperties>> violations = validator.validate(sut);

        assertThat(violations.size()).isOne();
    }

    @Test
    void ensureDefaults() {
        final OidcClientRegistrationRabbitmqConfigurationProperties sut = new OidcClientRegistrationRabbitmqConfigurationProperties();
        assertThat(sut.getTopic()).isEqualTo("oidc_provider.topic");
        assertThat(sut.getRoutingKeyCreatedTemplate()).isEqualTo("OIDC_PROVIDER.%s.ZEITERFASSUNG.OIDC_CLIENT.CREATED");
        assertThat(sut.getRoutingKeyDeletedTemplate()).isEqualTo("OIDC_PROVIDER.%s.ZEITERFASSUNG.OIDC_CLIENT.DELETED");
    }
}
