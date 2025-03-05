package de.focusshift.zeiterfassung.usermanagement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static de.focusshift.zeiterfassung.usermanagement.User.generateInitials;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @ParameterizedTest(name = "generateInitials({0}) should return {1}")
    @CsvSource({
        "'', '??'",
        "'   ', '??'",
        "'Alice', 'A'",
        "'bob', 'B'",
        "'Alice Smith', 'AS'",
        "'Bob Jones', 'BJ'",
        "'bob miller', 'BM'",
        "'Alice   Johnson', 'AJ'",
        "'Alice -', 'A-'",
        "'Alice Éclair', 'AÉ'",
        "'Alice Bob Doe', 'AD'",
        "'Alice Bob', 'AB'"
    })
    void ensureToGenerateTwoLiteralInitials(String input, String expected) {
        assertThat(generateInitials(input)).isEqualTo(expected);
    }
}
