package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_VIEW_REPORT_ALL;
import static java.util.function.Predicate.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportSecurityIT extends SingleTenantTestContainersBase implements ControllerTest {

    private static final List<String> REPORT_URLS =  List.of(
        "/report",
        "/report/week",
        "/report/year/2026/week/42",
        "/report/month",
        "/report/year/2026/month/4"
    );

    private static final Set<SecurityRole> ALLOWED_ROLES = Set.of(ZEITERFASSUNG_VIEW_REPORT_ALL);

    @Autowired
    private MockMvc mockMvc;

    static Stream<Arguments> searchAllowedArguments() {
        return REPORT_URLS.stream().flatMap(url -> ALLOWED_ROLES.stream().map(role -> Arguments.of(url, role)));
    }

    @ParameterizedTest
    @MethodSource("searchAllowedArguments")
    void ensureUserSearchAllowed(String url, SecurityRole role) throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final CurrentOidcUser oidcUser = currentOidcUser(userIdComposite, List.of(ZEITERFASSUNG_USER, role));

        mockMvc.perform(get(url)
            .with(oidcLogin().oidcUser(oidcUser))
            .header("Turbo-Frame", "frame-users-suggestions")
            .param("query", "super")
        )
            .andExpect(status().isOk());
    }

    static Stream<Arguments> searchNotAllowedArguments() {
        final List<SecurityRole> allowedRoles = Arrays.stream(SecurityRole.values()).filter(not(ALLOWED_ROLES::contains)).toList();
        return REPORT_URLS.stream().flatMap(url -> allowedRoles.stream().map(role -> Arguments.of(url, role)));
    }

    @ParameterizedTest
    @MethodSource("searchNotAllowedArguments")
    void ensureUserSearchNotAllowed(String url, SecurityRole role) throws Exception {

        final UserId userId = new UserId("uuid");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final CurrentOidcUser oidcUser = currentOidcUser(userIdComposite, List.of(ZEITERFASSUNG_USER, role));

        mockMvc.perform(get(url)
            .with(oidcLogin().oidcUser(oidcUser))
            .header("Turbo-Frame", "frame-users-suggestions")
            .param("query", "super")
        )
            .andExpect(status().isForbidden());
    }
}
