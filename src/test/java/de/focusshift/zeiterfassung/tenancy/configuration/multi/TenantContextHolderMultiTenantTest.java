package de.focusshift.zeiterfassung.tenancy.configuration.multi;


import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void clearsTenantId() {
        sut.setTenantId(new TenantId("a154bc4e"));
        assertThat(sut.getCurrentTenantId()).hasValue(new TenantId("a154bc4e"));

        sut.clear();
        assertThat(sut.getCurrentTenantId()).isEmpty();
    }

}
