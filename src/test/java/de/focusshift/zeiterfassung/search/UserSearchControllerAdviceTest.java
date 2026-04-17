package de.focusshift.zeiterfassung.search;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.ui.ConcurrentModel;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;

class UserSearchControllerAdviceTest implements ControllerTest {

    private UserSearchControllerAdvice userSearchControllerAdvice;

    @BeforeEach
    void setUp() {
        userSearchControllerAdvice = new UserSearchControllerAdvice();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, mode = INCLUDE, names = {
        "ZEITERFASSUNG_VIEW_REPORT_ALL",
        "ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL",
        "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL",
        "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL",
        "ZEITERFASSUNG_PERMISSIONS_EDIT_ALL",
    })
    void ensureUserIsAllowedToSearch(SecurityRole role) {

        final List<SecurityRole> roles = List.of(ZEITERFASSUNG_USER, role);
        final CurrentOidcUser currentUser = currentOidcUser(anyUserIdComposite(new UserId("uuid")), roles);

        final ConcurrentModel model = new ConcurrentModel();
        userSearchControllerAdvice.addAttributes("", model, currentUser);

        assertThat(model.getAttribute("userSearchEnabled")).isEqualTo(true);
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, mode = EXCLUDE, names = {
        "ZEITERFASSUNG_VIEW_REPORT_ALL",
        "ZEITERFASSUNG_TIME_ENTRY_EDIT_ALL",
        "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL",
        "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL",
        "ZEITERFASSUNG_PERMISSIONS_EDIT_ALL",
    })
    void ensureUserIsNotAllowedToSearch(SecurityRole role) {

        final List<SecurityRole> roles = List.of(ZEITERFASSUNG_USER, role);
        final CurrentOidcUser currentUser = currentOidcUser(anyUserIdComposite(new UserId("uuid")), roles);

        final ConcurrentModel model = new ConcurrentModel();
        userSearchControllerAdvice.addAttributes("", model, currentUser);

        assertThat(model.getAttribute("userSearchEnabled")).isEqualTo(false);
    }
}
