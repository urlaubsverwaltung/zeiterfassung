package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import com.zaxxer.hikari.HikariDataSource;
import de.focusshift.zeiterfassung.Application;
import de.focusshift.zeiterfassung.tenancy.configuration.multi.AdminAwareDatabaseConfiguration.AdminAwareRepositories;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.hibernate.SpringBeanContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

import static de.focusshift.zeiterfassung.tenancy.configuration.multi.AdminAwareDatabaseConfiguration.jpaTypes;
import static java.util.Objects.requireNonNull;
import static org.hibernate.cfg.AvailableSettings.BEAN_CONTAINER;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableEnversRepositories(
    basePackageClasses = Application.class,
    // the admin repositories belong to AdminAwareDatabaseConfiguration
    excludeFilters = @Filter(type = FilterType.CUSTOM, classes = AdminAwareRepositories.class),
    entityManagerFactoryRef = "tenantAwareEntityManagerFactory",
    transactionManagerRef = "tenantAwareTransactionManager"
)
@ConditionalOnMultiTenantMode
class TenantAwareDatabaseConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties tenantAwareDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    DataSource tenantAwareDataSource(
        DataSourceProperties tenantAwareDataSourceProperties,
        TenantContextHolder tenantContextHolder
    ) {
        final HikariDataSource dataSource = tenantAwareDataSourceProperties
            .initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
        dataSource.setPoolName("tenantAwareDataSource");
        return new TenantAwareDataSource(dataSource, tenantContextHolder);
    }

    @Bean
    @Primary
    LocalContainerEntityManagerFactoryBean tenantAwareEntityManagerFactory(
        EntityManagerFactoryBuilder builder,
        ConfigurableListableBeanFactory beanFactory,
        ResourceLoader resourceLoader,
        DataSource tenantAwareDataSource
    ) {

        return builder
            .dataSource(tenantAwareDataSource)
            .managedTypes(jpaTypes(resourceLoader, false))
            .persistenceUnit("tenantAware")
            // enable hibernate to access spring beans and inject them into jpa entity lifecycle events
            .properties(Map.of(BEAN_CONTAINER, new SpringBeanContainer(beanFactory)))
            .build();
    }

    @Bean
    @Primary
    PlatformTransactionManager tenantAwareTransactionManager(
        LocalContainerEntityManagerFactoryBean tenantAwareEntityManagerFactory
    ) {
        return new JpaTransactionManager(requireNonNull(tenantAwareEntityManagerFactory.getObject()));
    }
}
