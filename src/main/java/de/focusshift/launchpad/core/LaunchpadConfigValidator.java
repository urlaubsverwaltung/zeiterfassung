package de.focusshift.launchpad.core;

import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Locale;

/**
 * Checks whether the launchpad apps messages key are defined in {@linkplain MessageSource} or not.
 * Validation fails when keys cannot be found.
 */
class LaunchpadConfigValidator implements Validator {

    private static final Locale DEFAULT_MESSAGE_LOCALE = Locale.GERMAN;

    private final ApplicationContext applicationContext;

    LaunchpadConfigValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return LaunchpadConfigProperties.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

        final MessageSource messageSource = applicationContext.getBean(MessageSource.class);

        ((LaunchpadConfigProperties) target).getApps().forEach(app -> {
            try {
                messageSource.getMessage(app.messageKey(), new Object[]{}, DEFAULT_MESSAGE_LOCALE);
            } catch(NoSuchMessageException e) {
                errors.rejectValue(null, "", "could not find messages key='%s'".formatted(app.messageKey()));
            }
        });
    }
}
