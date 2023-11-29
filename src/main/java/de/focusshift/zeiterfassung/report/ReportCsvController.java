package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.threeten.extra.YearWeek;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.DateTimeException;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.invoke.MethodHandles.lookup;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Controller
@PreAuthorize("hasAuthority('ZEITERFASSUNG_USER')")
class ReportCsvController {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private final ReportCsvService reportCsvService;
    private final MessageSource messageSource;

    ReportCsvController(ReportCsvService reportCsvService, MessageSource messageSource) {
        this.reportCsvService = reportCsvService;
        this.messageSource = messageSource;
    }

    @GetMapping(value = "/report/year/{year}/week/{week}", params = {"csv"})
    public void weeklyUserReportCsv(
        @PathVariable("year") Integer year,
        @PathVariable("week") Integer week,
        @RequestParam(value = "user", required = false) Optional<List<Long>> optionalUserIds,
        @AuthenticationPrincipal DefaultOidcUser principal,
        Locale locale,
        HttpServletResponse response
    ) {

        final YearWeek reportYearWeek = getYearWeek(year, week)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid week."));

        final UserId userId = principalToUserId(principal);
        final List<UserLocalId> userLocalIds = optionalUserIds.orElse(List.of()).stream().map(UserLocalId::new).toList();
        final String fileName = messageSource.getMessage("report.weekly.csv.filename", new Object[]{year, week}, locale);

        final Consumer<PrintWriter> csvWriteConsumer = userLocalIds.isEmpty()
            ? writer -> reportCsvService.writeWeekReportCsv(Year.of(reportYearWeek.getYear()), reportYearWeek.getWeek(), locale, userId, writer)
            : writer -> reportCsvService.writeWeekReportCsvForUserLocalIds(Year.of(reportYearWeek.getYear()), reportYearWeek.getWeek(), locale, userLocalIds, writer);

        writeCsv(fileName, response, csvWriteConsumer);
    }

    @GetMapping(value = "/report/year/{year}/month/{month}", params = {"csv"})
    public void monthlyUserReportCsv(
        @PathVariable("year") Integer year,
        @PathVariable("month") Integer month,
        @RequestParam(value = "user", required = false) Optional<List<Long>> optionalUserIds,
        @AuthenticationPrincipal DefaultOidcUser principal,
        Locale locale,
        HttpServletResponse response
    ) {

        final YearMonth yearMonth = yearMonth(year, month)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Invalid month."));

        final UserId userId = principalToUserId(principal);
        final List<UserLocalId> userIds = optionalUserIds.orElse(List.of()).stream().map(UserLocalId::new).toList();
        final String fileName = messageSource.getMessage("report.monthly.csv.filename", new Object[]{year, month}, locale);

        final Consumer<PrintWriter> csvWriteConsumer = userIds.isEmpty()
            ? writer -> reportCsvService.writeMonthReportCsv(yearMonth, locale, userId, writer)
            : writer -> reportCsvService.writeMonthReportCsvForUserLocalIds(yearMonth, locale, userIds, writer);

        writeCsv(fileName, response, csvWriteConsumer);
    }

    private void writeCsv(String filename, HttpServletResponse response, Consumer<PrintWriter> csvConsumer) {
        response.setContentType("text/csv");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + filename);

        try (PrintWriter writer = response.getWriter()) {
            csvConsumer.accept(writer);
        } catch (IOException exception) {
            LOG.error("error while writing report csv", exception);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Could not write report csv.");
        }
    }

    private static Optional<YearWeek> getYearWeek(int year, int week) {
        try {
            return Optional.of(YearWeek.of(Year.of(year), week));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearWeek with year={} week={}", year, week, exception);
            return Optional.empty();
        }
    }

    private static Optional<YearMonth> yearMonth(int year, int month) {
        try {
            return Optional.of(YearMonth.of(year, month));
        } catch (DateTimeException exception) {
            LOG.error("could not create YearMonth with year={} month={}", year, month, exception);
            return Optional.empty();
        }
    }

    private static UserId principalToUserId(DefaultOidcUser principal) {
        return new UserId(principal.getUserInfo().getSubject());
    }
}
