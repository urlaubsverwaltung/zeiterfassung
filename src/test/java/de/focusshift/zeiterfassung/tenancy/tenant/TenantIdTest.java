package de.focusshift.zeiterfassung.tenancy.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIdTest {

    @Test
    void ensureTrueForUUIDBasedTenantId() {
        assertThat(new TenantId(UUID.randomUUID().toString().substring(0, 8)).valid()).isTrue();
    }

    @Test
    void ensureTrueForAnyStringTenantId() {
        assertThat(new TenantId("Zeiterfassung").valid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    @NullSource
    void ensureFalseForValidTenantId(String brokenTenant) {
        assertThat(new TenantId(brokenTenant).valid()).isFalse();
    }

    @Test
    void ensureFalseForTooLongTenantId() {
        assertThat(new TenantId("x".repeat(256)).valid()).isFalse();
    }
}
