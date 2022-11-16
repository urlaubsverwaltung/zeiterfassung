package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.TestContainersBase;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.PrintWriter;
import java.time.YearMonth;
import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ReportCsvServiceIT extends TestContainersBase {

    @Autowired
    private ReportCsvService sut;

    @ParameterizedTest
    @CsvSource({"de,Datum;Vorname;Nachname;erfasste Stunden;Kommentar", "en,Date;Given name;Family name;Worked hours;Comment"})
    void ensureI18nHeader(String languageTag, String expectedHeader) {
        final PrintWriter printWriter = mock(PrintWriter.class);

        sut.writeMonthReportCsv(YearMonth.of(2022, 9), Locale.forLanguageTag(languageTag), new UserId("user"), printWriter);

        verify(printWriter).println(expectedHeader);
    }
}
