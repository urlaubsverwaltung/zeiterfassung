package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.PrintWriter;
import java.time.YearMonth;
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

    @MockBean
    private UserManagementService userManagementService;

    @ParameterizedTest
    @CsvSource({"de,Datum;Vorname;Nachname;Von;Bis;erfasste Stunden;Kommentar;Pause", "en,Date;Given name;Family name;From;To;Worked hours;Comment;Break"})
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
}
