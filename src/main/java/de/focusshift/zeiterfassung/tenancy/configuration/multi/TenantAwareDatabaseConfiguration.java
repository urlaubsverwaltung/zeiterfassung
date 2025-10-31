package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import com.zaxxer.hikari.HikariDataSource;
import de.focusshift.zeiterfassung.absence.AbsenceTypeEntity;
import de.focusshift.zeiterfassung.absence.AbsenceWriteEntity;
import de.focusshift.zeiterfassung.settings.FederalStateSettingsEntity;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantAwareRevisionEntity;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserEntity;
import de.focusshift.zeiterfassung.timeclock.TimeClockEntity;
import de.focusshift.zeiterfassung.timeentry.TimeEntryEntity;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountEntity;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeEntity;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

import static de.focusshift.zeiterfassung.tenancy.TenantConfigurationProperties.MULTI;
import static java.util.Objects.requireNonNull;
import static org.hibernate.cfg.AvailableSettings.BEAN_CONTAINER;

@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EnableEnversRepositories(
    basePackageClasses = {
        AbsenceWriteEntity.class,
        AbsenceTypeEntity.class,
        TimeEntryEntity.class,
        TimeClockEntity.class,
        TenantUserEntity.class,
        WorkingTimeEntity.class,
        OvertimeAccountEntity.class,
        FederalStateSettingsEntity.class
        // SubtractBreakFromTimeEntrySettingsEntity.class, // disabled for now to prevent duplicated bean definitions
        // LockTimeEntriesSettingsEntity.class // disabled for now to prevent duplicated bean definitions
    },
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
        DataSource tenantAwareDataSource
    ) {

        return builder
            .dataSource(tenantAwareDataSource)
            // List all tenant aware related entity packages here
            .packages(
                // envers revinfo
                TenantAwareRevisionEntity.class,
                // domain
                AbsenceWriteEntity.class,
                AbsenceTypeEntity.class,
                TimeEntryEntity.class,
                TimeClockEntity.class,
                TenantUserEntity.class,
                WorkingTimeEntity.class,
                OvertimeAccountEntity.class,
                FederalStateSettingsEntity.class
                // SubtractBreakFromTimeEntrySettingsEntity.class, // disabled for now to prevent duplicated bean definitions
                // LockTimeEntriesSettingsEntity.class // disabled for now to prevent duplicated bean definitions
            )
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
