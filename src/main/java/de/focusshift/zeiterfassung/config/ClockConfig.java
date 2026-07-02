package de.focusshift.zeiterfassung.config;

import org.hibernate.validator.HibernateValidatorConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;

@Configuration
class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public LocalValidatorFactoryBean defaultValidator(Clock clock) {
        final LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setConfigurationInitializer(configuration -> {
            if (configuration instanceof HibernateValidatorConfiguration hibernateConfiguration) {
                hibernateConfiguration.clockProvider(() -> clock);
            }
        });
        return bean;
    }
}
