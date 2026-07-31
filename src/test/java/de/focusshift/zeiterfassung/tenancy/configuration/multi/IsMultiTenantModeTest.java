package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IsMultiTenantModeTest {

    private static final String TENANT_MODE = "zeiterfassung.tenant.mode";

    private final IsMultiTenantMode sut = new IsMultiTenantMode();

    @Test
    void ensureDoesNotMatchWhenPropertyIsMissing() {
        assertThat(matches(new MockEnvironment())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"multi", "MULTI", "Multi"})
    void ensureMatchesForMultiValueIgnoringCase(String mode) {
        assertThat(matches(new MockEnvironment().withProperty(TENANT_MODE, mode))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"single", "SINGLE", "", " ", "unknown"})
    void ensureDoesNotMatchForNonMultiValue(String mode) {
        assertThat(matches(new MockEnvironment().withProperty(TENANT_MODE, mode))).isFalse();
    }

    private boolean matches(MockEnvironment environment) {
        final ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return sut.matches(context, mock(AnnotatedTypeMetadata.class));
    }
}
