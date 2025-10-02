package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static de.focusshift.zeiterfassung.usermanagement.User.generateInitials;
import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @ParameterizedTest(name = "fullname({0}) should return {1}")
    @CsvSource({
        "'', '', ''",
        "' ', ' ', ''",
        "'Alice', '', 'Alice'",
        "'', 'bob', 'bob'",
        "'Alice', 'Smith', 'Alice Smith'",
        "'Alice', '   Johnson', 'Alice Johnson'",
        "'Alice -', 'Johnson', 'Alice - Johnson'",
        "' Alice ', '  Bob', 'Alice Bob'",
        "'Alice', 'Bob ', 'Alice Bob'",
        "' Alice', 'Bob', 'Alice Bob'",
        "'Alice  ', 'Bob', 'Alice Bob'"
    })
    void ensureToGenerateStrippedFullName(String givenName, String familyName, String expected) {
        assertThat(new User(new UserIdComposite(new UserId("id"), new UserLocalId(1L)), givenName, familyName,  new EMailAddress("mail@example.org"), Set.of()).fullName()).isEqualTo(expected);
    }

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
        "'Alice Bob', 'AB'",
        "' Alice  Bob ', 'AB'",
        "'Alice Bob ', 'AB'",
        "' Alice Bob', 'AB'"
    })
    void ensureToGenerateTwoLiteralInitials(String input, String expected) {
        assertThat(generateInitials(input)).isEqualTo(expected);
    }
}
