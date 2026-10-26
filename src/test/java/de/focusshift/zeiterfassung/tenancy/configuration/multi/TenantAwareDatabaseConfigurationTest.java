package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.util.ClassUtils.getPackageName;

class TenantAwareDatabaseConfigurationTest {

    @Test
    void ensureEveryEntityIsScannedByExactlyOneDatabaseConfiguration() {

        final List<String> tenantAwarePackages = scannedPackages(TenantAwareDatabaseConfiguration.class);
        final List<String> adminPackages = scannedPackages(AdminAwareDatabaseConfiguration.class);

        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        final List<String> entities = scanner.findCandidateComponents("de.focusshift.zeiterfassung").stream()
            .map(BeanDefinition::getBeanClassName)
            .toList();

        assertThat(entities).isNotEmpty().allSatisfy(entity ->
            assertThat(isScannedBy(entity, tenantAwarePackages) ^ isScannedBy(entity, adminPackages))
                .as("%s must be in a package listed in the repository scan of exactly one of TenantAwareDatabaseConfiguration or AdminAwareDatabaseConfiguration", entity)
                .isTrue()
        );
    }

    private static List<String> scannedPackages(Class<?> configuration) {
        return Stream.of(findMergedAnnotation(configuration, EnableJpaRepositories.class).basePackageClasses())
            .map(Class::getPackageName)
            .toList();
    }

    private static boolean isScannedBy(String entity, List<String> scannedPackages) {
        final String entityPackage = getPackageName(entity);
        return scannedPackages.stream()
            .anyMatch(scanned -> entityPackage.equals(scanned) || entityPackage.startsWith(scanned + "."));
    }
}
