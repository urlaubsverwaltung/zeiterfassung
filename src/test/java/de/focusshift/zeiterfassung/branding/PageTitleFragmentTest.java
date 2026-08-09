package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Locale;
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

    private static SpringTemplateEngine createTemplateEngine() {

        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");

        final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}
