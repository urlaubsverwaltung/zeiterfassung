package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RolesFromClaimMappersPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void ensureThatAllDefaultValuesDoesNotTriggerAViolation() {
        final RolesFromClaimMappersProperties properties = new RolesFromClaimMappersProperties();
        final Set<ConstraintViolation<RolesFromClaimMappersProperties>> violations = validator.validate(properties);
        assertThat(violations).isEmpty();
    }

    @Test
    void ensureDefaultOfResourceAccessClaimMapper() {
        final RolesFromClaimMappersProperties properties = new RolesFromClaimMappersProperties();
        final RolesFromClaimMappersProperties.ResourceAccessClaimMapperProperties resourceAccessClaim = properties.getResourceAccessClaim();

        assertThat(resourceAccessClaim.isEnabled()).isFalse();
        assertThat(resourceAccessClaim.getResourceApp()).isEqualTo("zeiterfassung");
    }

    @Test
    void ensureDefaultOfGroupClaimMapper() {
        final RolesFromClaimMappersProperties properties = new RolesFromClaimMappersProperties();
        final RolesFromClaimMappersProperties.GroupClaimMapperProperties groupClaim = properties.getGroupClaim();

        assertThat(groupClaim.isEnabled()).isFalse();
        assertThat(groupClaim.getClaimName()).isEqualTo("groups");
    }

}
