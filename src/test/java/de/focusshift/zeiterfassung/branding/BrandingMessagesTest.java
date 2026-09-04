package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class BrandingMessagesTest {

    private final ResourceBundleMessageSource messageSource = createMessageSource();

    @Test
    void ensureSettingsTeaserTextUsesApplicationName() {

        final Object[] args = {"Custom Name"};

        assertThat(messageSource.getMessage("settings.teaser-text", args, Locale.GERMAN))
            .isEqualTo("Globale Einstellungen für Custom Name.");
        assertThat(messageSource.getMessage("settings.teaser-text", args, Locale.ENGLISH))
            .isEqualTo("Global settings for Custom Name.");
    }

    @Test
    void ensureUsermanagementTeaserTextUsesApplicationName() {

        final Object[] args = {"Custom Name"};

        assertThat(messageSource.getMessage("usermanagement.teaser-text", args, Locale.GERMAN))
            .isEqualTo("Information: Es werden hier nur Personen angezeigt, welche sich mindestens einmal bei Custom Name angemeldet haben.");
        assertThat(messageSource.getMessage("usermanagement.teaser-text", args, Locale.ENGLISH))
            .isEqualTo("Information: Only persons who have logged in to Custom Name at least once are displayed here.");
    }

    private static ResourceBundleMessageSource createMessageSource() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
