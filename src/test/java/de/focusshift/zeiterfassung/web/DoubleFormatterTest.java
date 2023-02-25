package de.focusshift.zeiterfassung.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;

class DoubleFormatterTest {

    private DoubleFormatter sut;

    @BeforeEach
    void setUp() {
        sut = new DoubleFormatter();
    }

    @ParameterizedTest
    @CsvSource(value = { "de_13,37", "us_13.37"}, delimiterString = "_")
    void ensurePrintUsesLocale(String languageTag, String expected) {
        final String actual = sut.print(13.37, Locale.forLanguageTag(languageTag));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void ensurePrintDoesNotUserGrouping() {
        final String actual = sut.print(1000.0, GERMAN);
        assertThat(actual).isEqualTo("1000");
    }
}
