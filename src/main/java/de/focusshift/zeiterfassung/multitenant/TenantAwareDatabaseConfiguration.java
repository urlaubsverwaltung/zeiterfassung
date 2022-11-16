package de.focusshift.zeiterfassung.multitenant;

import com.zaxxer.hikari.HikariDataSource;
import de.focusshift.zeiterfassung.tenantuser.TenantUserEntity;
import de.focusshift.zeiterfassung.timeclock.TimeClockEntity;
import de.focusshift.zeiterfassung.timeentry.TimeEntryEntity;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

import static de.focusshift.zeiterfassung.tenant.TenantConfigurationProperties.MULTI;
import static java.util.Objects.requireNonNull;
import static org.hibernate.cfg.AvailableSettings.BEAN_CONTAINER;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackageClasses = {TimeEntryEntity.class, TimeClockEntity.class, TenantUserEntity.class},
    entityManagerFactoryRef = "tenantAwareEntityManagerFactory",
    transactionManagerRef = "tenantAwareTransactionManager"
)
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
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
    DataSource tenantAwareDataSource(DataSourceProperties tenantAwareDataSourceProperties, TenantContextHolder tenantContextHolder) {
        final HikariDataSource dataSource = tenantAwareDataSourceProperties
            .initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
        dataSource.setPoolName("tenantAwareDataSource");
        return new TenantAwareDataSource(dataSource, tenantContextHolder);
    }

    @Bean
    @Primary
    LocalContainerEntityManagerFactoryBean tenantAwareEntityManagerFactory(EntityManagerFactoryBuilder builder,
                                                                           ConfigurableListableBeanFactory beanFactory,
                                                                           DataSource tenantAwareDataSource) {

        return builder
            .dataSource(tenantAwareDataSource)
            // List all tenant aware related entity packages here
            .packages(TimeEntryEntity.class, TimeClockEntity.class, TenantUserEntity.class)
            .persistenceUnit("tenantAware")
            // enable hibernate to access spring beans and inject them into jpa entity lifecycle events
            .properties(Map.of(BEAN_CONTAINER, new SpringBeanContainer(beanFactory)))
            .build();
    }

    @Bean
    @Primary
    PlatformTransactionManager tenantAwareTransactionManager(LocalContainerEntityManagerFactoryBean tenantAwareEntityManagerFactory) {
        return new JpaTransactionManager(requireNonNull(tenantAwareEntityManagerFactory.getObject()));
    }
}
