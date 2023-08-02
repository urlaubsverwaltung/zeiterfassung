package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.MissingTenantException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbsenceTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"HOLIDAY", "SICK", "SPECIALLEAVE", "UNPAIDLEAVE", "OTHER"})
    void isValidVacationTypeCategory(String vacationType) {
        assertThat(AbsenceType.isValidVacationTypeCategory(vacationType)).isTrue();
    }

    @Test
    void isNotValidVacationTypeCategory() {
        assertThatThrownBy(() -> new AbsenceType("FOOBAR", null)).isInstanceOf(AbsenceTypeNotSupportedException.class);
    }
}
