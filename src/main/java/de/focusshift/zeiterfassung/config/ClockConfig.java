package de.focusshift.zeiterfassung.config;

import org.hibernate.validator.HibernateValidatorConfiguration;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Customizes the auto-configured bean-validation {@link jakarta.validation.Validator} to derive "now" from the
     * application {@link Clock} so constraints like {@code @PastOrPresent} are
     * controllable in tests. Using a customizer instead of a hand-rolled {@code LocalValidatorFactoryBean} keeps
     * Spring Boot's {@code MessageSource}-backed message interpolation, so validation messages defined in
     * {@code messages.properties} still resolve.
     */
    @Bean
    public ValidationConfigurationCustomizer clockValidationConfigurationCustomizer(Clock clock) {
        return configuration -> {
            if (configuration instanceof HibernateValidatorConfiguration hibernateConfiguration) {
                hibernateConfiguration.clockProvider(() -> clock);
            }
        };
    }
}
