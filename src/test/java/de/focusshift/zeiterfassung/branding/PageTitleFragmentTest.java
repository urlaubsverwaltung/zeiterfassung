package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PageTitleFragmentTest {

    private final SpringTemplateEngine templateEngine = createTemplateEngine();

    @Test
    void ensureTitleIsPageTitleFollowedByApplicationName() {

        final Context context = new Context(Locale.GERMAN);
        context.setVariable("applicationName", "Custom Name");
        context.setVariable("pageTitle", "Berichte");

        final String html = templateEngine.process("fragments/page-title", Set.of("page-title"), context);

        assertThat(html).contains("Berichte – Custom Name");
    }

    @Test
    void ensureTitleIsPageTitleOnlyWhenApplicationNameIsMissing() {

        final Context context = new Context(Locale.GERMAN);
        context.setVariable("pageTitle", "Berichte");

        final String html = templateEngine.process("fragments/page-title", Set.of("page-title"), context);

        assertThat(html).contains(">Berichte<");
        assertThat(html).doesNotContain("–");
    }

    /**
     * {@code th:replace} has a higher precedence than {@code th:if}, so a conditionally rendered title element would
     * still evaluate its fragment parameters. Guards against re-introducing two conditional title elements, which
     * broke rendering of the whole page whenever {@code viewedUser} was null.
     */
    @Test
    void ensureTimeEntriesTitleWithoutViewedUser() {

        final Context context = new Context(Locale.GERMAN, Map.of("applicationName", "Custom Name"));

        final String html = templateEngine.process("timeentries/index", Set.of("title"), context);

        assertThat(html).contains("Zeiteinträge – Custom Name");
    }

    @Test
    void ensureTimeEntriesTitleWithViewedUser() {

        final Context context = new Context(Locale.GERMAN, Map.of(
            "applicationName", "Custom Name",
            "viewedUser", new ViewedUser("Bruce Wayne")
        ));

        final String html = templateEngine.process("timeentries/index", Set.of("title"), context);

        assertThat(html).contains("Zeiteinträge Bruce Wayne – Custom Name");
    }

    record ViewedUser(String fullName) {
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
