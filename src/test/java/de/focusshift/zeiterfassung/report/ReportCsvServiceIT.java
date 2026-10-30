package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.PrintWriter;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReportCsvServiceIT extends SingleTenantTestContainersBase {

    @Autowired
    private ReportCsvService sut;

    @MockitoBean
    private UserManagementService userManagementService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({"de,Datum;Vorname;Nachname;Von;Bis;Erfasste Stunden;Sollarbeitszeit;Kommentar;Pause", "en,Date;Given name;Family name;From;To;Worked hours;Should working hours;Comment;Break"})
    void ensureI18nHeader(String languageTag, String expectedHeader) {
        final PrintWriter printWriter = mock(PrintWriter.class);

        final UserId userId = new UserId("user");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        sut.writeMonthReportCsv(YearMonth.of(2022, 9), Locale.forLanguageTag(languageTag), userId, printWriter);

        verify(printWriter).println(expectedHeader);
    }

    @ParameterizedTest
    @CsvSource({"de,Monat;Von;Bis;Vorname;Nachname;Erfasste Stunden;Sollarbeitszeit", "en,Month;From;To;Given name;Family name;Worked hours;Should working hours"})
    void ensureI18nHeaderAggregated(String languageTag, String expectedHeader) {
        final PrintWriter printWriter = mock(PrintWriter.class);

        final UserId userId = new UserId("user");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        when(userManagementService.findUserById(userId)).thenReturn(Optional.of(user));

        sut.writeMonthReportCsvAggregated(YearMonth.of(2022, 9), Locale.forLanguageTag(languageTag), userId, printWriter);

        verify(printWriter).println(expectedHeader);
    }

    @ParameterizedTest
    @CsvSource({"de,KW;Von;Bis;Vorname;Nachname;Erfasste Stunden;Sollarbeitszeit", "en,CW;From;To;Given name;Family name;Worked hours;Should working hours"})
    void ensureI18nHeaderWeekAggregated(String languageTag, String expectedHeader) {
        final PrintWriter printWriter = mock(PrintWriter.class);

        final UserId userId = new UserId("user");
        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
        final User user = new User(userIdComposite, "Bruce", "Wayne", new EMailAddress(""), Set.of());

        when(userManagementService.findUserByLocalId(userLocalId)).thenReturn(Optional.of(user));
        when(userManagementService.findAllUsersByLocalIds(List.of(userLocalId))).thenReturn(List.of(user));

        // permission check for the week report requires an authenticated current user
        final OidcIdToken idToken = OidcIdToken.withTokenValue("token-value").claim("sub", userId.value()).build();
        final OidcUserInfo userInfo = OidcUserInfo.builder().subject(userId.value()).name("Bruce Wayne").build();
        final CurrentOidcUser currentOidcUser = new CurrentOidcUser(new DefaultOidcUser(List.of(), idToken, userInfo), List.of(), List.of(), userLocalId);
        final Authentication authentication = new OAuth2AuthenticationToken(currentOidcUser, List.of(), "registrationId");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        sut.writeWeekReportCsvAggregated(Year.of(2022), 36, Locale.forLanguageTag(languageTag), userLocalId, printWriter);

        verify(printWriter).println(expectedHeader);
    }
}
