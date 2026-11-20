package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import com.zaxxer.hikari.HikariDataSource;
import de.focusshift.zeiterfassung.Application;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.Repository;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.ClassUtils.resolveClassName;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackageClasses = Application.class,
    includeFilters = @Filter(type = FilterType.CUSTOM, classes = AdminAwareDatabaseConfiguration.AdminAwareRepositories.class),
    entityManagerFactoryRef = "adminEntityManagerFactory",
    transactionManagerRef = "adminTransactionManager"
)
@ConditionalOnMultiTenantMode
class AdminAwareDatabaseConfiguration {

    @Bean
    @ConfigurationProperties("admin.datasource")
    DataSourceProperties adminDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @LiquibaseDataSource
    @ConfigurationProperties("admin.datasource.hikari")
    DataSource adminDataSource(@Qualifier("adminDataSourceProperties") DataSourceProperties adminDataSourceProperties) {
        final HikariDataSource dataSource = adminDataSourceProperties
            .initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
        dataSource.setPoolName("adminDataSource");
        return dataSource;
    }

    @Bean
    LocalContainerEntityManagerFactoryBean adminEntityManagerFactory(EntityManagerFactoryBuilder builder, ResourceLoader resourceLoader, @Qualifier("adminDataSource") DataSource adminDataSource) {
        return builder
            .dataSource(adminDataSource)
            .managedTypes(jpaTypes(resourceLoader, true))
            .persistenceUnit("admin")
            .build();
    }

    @Bean
    PlatformTransactionManager adminTransactionManager(@Qualifier("adminEntityManagerFactory") LocalContainerEntityManagerFactoryBean adminEntityManagerFactory) {
        return new JpaTransactionManager(requireNonNull(adminEntityManagerFactory.getObject()));
    }

    /**
     * Entities implementing {@link AdminAware} belong to the admin database, all other jpa types to the tenant aware one.
     */
    static PersistenceManagedTypes jpaTypes(ResourceLoader resourceLoader, boolean adminAware) {
        return new PersistenceManagedTypesScanner(resourceLoader, type -> isAdminAware(resolveClassName(type, resourceLoader.getClassLoader())) == adminAware)
            .scan(Application.class.getPackageName());
    }

    /**
     * Matches the repositories of {@link AdminAware} entities.
     */
    static class AdminAwareRepositories extends AbstractClassTestingTypeFilter {

        @Override
        protected boolean match(ClassMetadata metadata) {
            // runs for every class of the application, so only interfaces are loaded
            if (!metadata.isInterface()) {
                return false;
            }
            final Class<?> repository = resolveClassName(metadata.getClassName(), null);
            return isAdminAware(ResolvableType.forClass(repository).as(Repository.class).resolveGeneric(0));
        }
    }

    private static boolean isAdminAware(@Nullable Class<?> type) {
        return type != null && AdminAware.class.isAssignableFrom(type);
    }
}
