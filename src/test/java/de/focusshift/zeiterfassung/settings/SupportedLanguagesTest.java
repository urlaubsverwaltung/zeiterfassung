package de.focusshift.zeiterfassung.settings;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SupportedLanguagesTest {

    @ParameterizedTest
    @ValueSource(strings = {"de-AT", "el"})
    void ensureValueOfLocaleReturnsEmptyOptionalForUnsupported(String languageTag) {
        final Locale locale = Locale.forLanguageTag(languageTag);
        assertThat(SupportedLanguages.valueOfLocale(locale)).isEmpty();
    }

    static Stream<Arguments> supportedLanguages() {
        return Stream.of(
            Arguments.of("de", SupportedLanguages.GERMAN),
            Arguments.of("en", SupportedLanguages.ENGLISH)
        );
    }

    @ParameterizedTest
    @MethodSource("supportedLanguages")
    void ensureValueOfLocale(String languageTag, SupportedLanguages supportedLanguage) {

        final Locale locale = Locale.forLanguageTag(languageTag);

        final Optional<SupportedLanguages> actual = SupportedLanguages.valueOfLocale(locale);
        assertThat(actual).hasValue(supportedLanguage);
        assertThat(actual.map(SupportedLanguages::getLocale)).hasValue(locale);
    }
}
