package de.focusshift.zeiterfassung.timeentry;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DayOfWeekBannerFragmentTest {

    private final SpringTemplateEngine templateEngine = createTemplateEngine();

    @Test
    void ensureBreakDeductionIsRenderedBelowTheBanner() {

        final String html = render(day("00:30", "break-violation.hint.daily"));

        assertThat(html).contains("inkl. −00:30 autom. Pausenabzug");
        assertThat(html).contains("data-testid=\"break-violation-hint\"");

        // the deduction must not be part of the banner row, the centered overtime would overlap it
        final int bannerRowEnd = html.indexOf("translate-y-1 h-1");
        assertThat(html.indexOf("data-testid=\"break-deduction\"")).isGreaterThan(bannerRowEnd);
    }

    @Test
    void ensureNoBreakDeductionAndNoHint() {

        final String html = render(day(null, null));

        assertThat(html)
            .doesNotContain("break-deduction")
            .doesNotContain("break-violation-hint");
    }

    private static TimeEntryDayDto day(String breakDeduction, String breakViolationMessageKey) {
        return TimeEntryDayDto.builder()
            .date("20. November 2026")
            .dayOfWeek(DayOfWeek.FRIDAY)
            .hoursWorked("05:31")
            .hoursWorkedShould("08:00")
            .hoursDelta("02:29")
            .hoursDeltaNegative(true)
            .hoursWorkedRatio(69.0)
            .timeEntries(List.of())
            .absenceEntries(List.of())
            .breakDeduction(breakDeduction)
            .breakViolationMessageKey(breakViolationMessageKey)
            .build();
    }

    private String render(TimeEntryDayDto day) {
        final Context context = new Context(Locale.GERMAN, Map.of("calendarWeek", 47, "day", day));
        return templateEngine.process("timeentries/index", Set.of("day-of-week-banner"), context);
    }

    private static SpringTemplateEngine createTemplateEngine() {

        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");

        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);

        final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.setTemplateEngineMessageSource(messageSource);
        return templateEngine;
    }
}
