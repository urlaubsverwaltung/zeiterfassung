package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.ClassUtils.resolveClassName;

class AdminAwareDatabaseConfigurationTest {

    @Test
    void ensureJpaTypesAreSplitByAdminAware() {
        final List<String> admin = AdminAwareDatabaseConfiguration.jpaTypes(new DefaultResourceLoader(), true).getManagedClassNames();
        final List<String> tenantAware = AdminAwareDatabaseConfiguration.jpaTypes(new DefaultResourceLoader(), false).getManagedClassNames();

        assertThat(admin)
            .contains("de.focusshift.zeiterfassung.tenancy.tenant.TenantEntity", "de.focusshift.zeiterfassung.security.oidc.clientregistration.OidcClientEntity")
            .allSatisfy(type -> assertThat(AdminAware.class.isAssignableFrom(resolveClassName(type, null))).isTrue());
        assertThat(tenantAware)
            .contains(
                "de.focusshift.zeiterfassung.timeentry.TimeEntryEntity",
                // lives next to TenantEntity, but belongs to the tenant aware database
                "de.focusshift.zeiterfassung.tenancy.tenant.TenantAwareRevisionEntity",
                // converters must be listed to be applied
                "de.focusshift.zeiterfassung.absence.LabelByLocaleConverter"
            )
            .doesNotContainAnyElementsOf(admin);
    }

    @ParameterizedTest
    @CsvSource({
        "de.focusshift.zeiterfassung.tenancy.tenant.TenantRepository, true",
        "de.focusshift.zeiterfassung.security.oidc.clientregistration.OidcClientEntityRepository, true",
        "de.focusshift.zeiterfassung.timeentry.TimeEntryRepository, false",
        "de.focusshift.zeiterfassung.tenancy.configuration.multi.AdminAware, false",
        "de.focusshift.zeiterfassung.tenancy.tenant.TenantEntity, false",
    })
    void ensureAdminAwareRepositoriesMatchOnlyRepositoriesOfAdminAwareEntities(String className, boolean expected) throws IOException {
        final MetadataReaderFactory factory = new SimpleMetadataReaderFactory();
        assertThat(new AdminAwareDatabaseConfiguration.AdminAwareRepositories().match(factory.getMetadataReader(className), factory))
            .isEqualTo(expected);
    }
}
