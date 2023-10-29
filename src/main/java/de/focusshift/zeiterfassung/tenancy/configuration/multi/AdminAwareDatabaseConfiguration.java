package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import com.zaxxer.hikari.HikariDataSource;
import de.focusshift.zeiterfassung.absence.AbsenceTypeEntity;
import de.focusshift.zeiterfassung.absence.AbsenceWriteEntity;
import de.focusshift.zeiterfassung.security.oidc.clientregistration.OidcClientEntity;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;
import static java.util.Objects.requireNonNull;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackageClasses = {TenantEntity.class, OidcClientEntity.class, AbsenceWriteEntity.class},
    entityManagerFactoryRef = "adminEntityManagerFactory",
    transactionManagerRef = "adminTransactionManager"
)
@ConditionalOnProperty(value = "zeiterfassung.tenant.mode", havingValue = MULTI)
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
    LocalContainerEntityManagerFactoryBean adminEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("adminDataSource") DataSource adminDataSource) {
        return builder
            .dataSource(adminDataSource)
            // List all admin related entity packages here
            .packages(TenantEntity.class, OidcClientEntity.class, AbsenceWriteEntity.class, AbsenceTypeEntity.class)
            .persistenceUnit("admin")
            .build();
    }

    @Bean
    PlatformTransactionManager adminTransactionManager(@Qualifier("adminEntityManagerFactory") LocalContainerEntityManagerFactoryBean adminEntityManagerFactory) {
        return new JpaTransactionManager(requireNonNull(adminEntityManagerFactory.getObject()));
    }
}
