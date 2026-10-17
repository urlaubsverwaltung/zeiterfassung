package de.focusshift.zeiterfassung.tenancy.configuration.multi;


import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderMultiTenantTest {

    private final TenantContextHolderMultiTenant sut = new TenantContextHolderMultiTenant();

    @BeforeEach
    void setup() {
        sut.clear();
    }

    @AfterEach
    void tearDown() {
        sut.clear();
    }

    @Test
    void hasTenantId() {
        sut.setTenantId(new TenantId("a154bc4e"));
        assertThat(sut.getCurrentTenantId()).hasValue(new TenantId("a154bc4e"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void hasDetectsInvalidTenantId(String value) {
        TenantId tenantId = new TenantId(value);
        assertThatThrownBy(() -> {
            sut.setTenantId(tenantId);
        })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid tenantId passed!");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a154bc4e", "multitenant", "default", "DEFAULT", "uvcloud", "3f2c1a4e-9b7d-4c1e-8f2a-6d5b4c3a2e1f", "tenant_1.a"})
    void acceptsWellFormedTenantId(String value) {
        sut.setTenantId(new TenantId(value));
        assertThat(sut.getCurrentTenantId()).hasValue(new TenantId(value));
    }

    @Test
    void rejectsTooLongTenantId() {
        TenantId tenantId = new TenantId("a".repeat(256));
        assertThatThrownBy(() -> sut.setTenantId(tenantId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid tenantId passed!");
    }

    @Test
    void clearsTenantId() {
        sut.setTenantId(new TenantId("a154bc4e"));
        assertThat(sut.getCurrentTenantId()).hasValue(new TenantId("a154bc4e"));

        sut.clear();
        assertThat(sut.getCurrentTenantId()).isEmpty();
    }

}
