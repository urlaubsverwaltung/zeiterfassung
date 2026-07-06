package de.focusshift.zeiterfassung.config;

import de.focusshift.zeiterfassung.timeclock.TimeClockDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class, ValidationAutoConfiguration.class))
        .withUserConfiguration(ClockConfig.class);

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    /**
     * Guards against regressing to a hand-rolled {@code LocalValidatorFactoryBean} that would replace Spring Boot's
     * auto-configured validator and therefore lose the {@code MessageSource}-backed message interpolation. Without it
     * a {@code {message.key}} defined in {@code messages.properties} would render as the literal {@code {key}} instead
     * of the localized text. Customizing via {@code ValidationConfigurationCustomizer} keeps that interpolation intact.
     */
    @Test
    void ensureValidationMessagesAreInterpolatedFromMessageSource() {

        // pin the locale so the assertion is independent of the machine default (messages_en.properties)
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        contextRunner.run(context -> {
            final Validator validator = context.getBean(Validator.class);

            final TimeClockDto dto = new TimeClockDto();
            dto.setZoneId(ZoneId.of("Europe/Berlin"));
            dto.setDate(LocalDate.now().plusDays(1));
            dto.setTime(LocalTime.NOON);

            final Set<ConstraintViolation<TimeClockDto>> violations = validator.validate(dto);

            assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getMessageTemplate()).isEqualTo("{timeclock.edit.startAt.error.past-or-present}");
                    // the interpolated message must be resolved from messages_en.properties, not a literal {key}
                    assertThat(violation.getMessage()).isEqualTo("Start must be in the past.");
                });
        });
    }
}
