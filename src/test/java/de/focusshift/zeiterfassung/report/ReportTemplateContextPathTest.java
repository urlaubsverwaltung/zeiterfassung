package de.focusshift.zeiterfassung.report;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.GERMANY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the report navigation fragments with and without a servlet context path.
 *
 * <p>The controllers hand these URLs to the view as plain strings (see {@code ReportViewHelper#createUrl}), so the
 * templates have to route them through Thymeleaf's link expression - {@code th:href="@{__${someUrl}__}"} - for the
 * context path to be applied. A plain {@code th:href="${someUrl}"} renders verbatim and silently drops the prefix,
 * which is what broke every pagination and CSV link when deployed at a subpath (#2217).
 *
 * <p>The URLs carry a query string, and these are the only {@code @{__${...}__}} usages in the code base that do.
 * The assertions below therefore pin the full attribute value, not just the prefix, so that query parameters and
 * their escaping stay covered too.
 *
 * @see <a href="https://github.com/urlaubsverwaltung/zeiterfassung/issues/2258">#2258</a>
 */
class ReportTemplateContextPathTest {

    private static final String PREVIOUS_URL = "/report/year/2026/month/8?everyone=&user=1&user=2";
    private static final String TODAY_URL = "/report/month?everyone=&user=1&user=2";
    private static final String NEXT_URL = "/report/year/2026/month/10?everyone=&user=1&user=2";
    private static final String CSV_URL = "/report/year/2026/month/9?everyone=&user=1&user=2&csv";
    private static final String FILTER_URL = "/report/year/2026/month/9";

    @ParameterizedTest(name = "{0} with context path \"{1}\"")
    @CsvSource({
        "reports/user-report-month, ''",
        "reports/user-report-month, /zeiterfassung",
        "reports/user-report-week,  ''",
        "reports/user-report-week,  /zeiterfassung",
    })
    void ensureNavigationLinksRespectContextPath(String template, String contextPath) {

        final String html = render(template, "chart-navigation", contextPath);

        assertThat(html)
            .as("previous")
            .contains("href=\"%s/report/year/2026/month/8?everyone=&amp;user=1&amp;user=2\"".formatted(contextPath));
        assertThat(html)
            .as("today")
            .contains("href=\"%s/report/month?everyone=&amp;user=1&amp;user=2\"".formatted(contextPath));
        assertThat(html)
            .as("next")
            .contains("href=\"%s/report/year/2026/month/10?everyone=&amp;user=1&amp;user=2\"".formatted(contextPath));
        assertThat(html)
            .as("csv download")
            .contains("href=\"%s/report/year/2026/month/9?everyone=&amp;user=1&amp;user=2&amp;csv\"".formatted(contextPath));
        assertThat(html)
            .as("person filter form of reports/_user-select")
            .contains("action=\"%s/report/year/2026/month/9\"".formatted(contextPath));
    }

    private static String render(String template, String fragment, String contextPath) {

        final MockServletContext servletContext = new MockServletContext();
        final MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setContextPath(contextPath);

        final IServletWebExchange exchange = JakartaServletWebApplication
            .buildApplication(servletContext)
            .buildExchange(request, new MockHttpServletResponse());

        final WebContext context = new WebContext(exchange, GERMANY, model());

        return templateEngine().process(new TemplateSpec(template, Set.of(fragment), TemplateMode.HTML, null), context);
    }

    private static Map<String, Object> model() {
        return Map.of(
            "userReportPreviousSectionUrl", PREVIOUS_URL,
            "userReportTodaySectionUrl", TODAY_URL,
            "userReportNextSectionUrl", NEXT_URL,
            "userReportCsvDownloadUrl", CSV_URL,
            // reports/_user-select is pulled in by both chart-navigation fragments
            "userReportFilterUrl", FILTER_URL,
            "allUsersSelected", false,
            "selectedUserIds", List.of(1L, 2L),
            "users", List.of(user(1L, "Bruce Wayne", "BW"), user(2L, "Clark Kent", "CK")),
            "selectedUsers", List.of(user(1L, "Bruce Wayne", "BW"), user(2L, "Clark Kent", "CK"))
        );
    }

    private static Map<String, Object> user(long id, String fullName, String initials) {
        return Map.of("id", id, "fullName", fullName, "initials", initials, "selected", true);
    }

    private static SpringTemplateEngine templateEngine() {

        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(UTF_8.name());
        templateResolver.setCacheable(false);

        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding(UTF_8.name());

        final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.setTemplateEngineMessageSource(messageSource);

        return templateEngine;
    }
}
