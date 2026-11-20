package de.focusshift.zeiterfassung.workduration;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BreakRulesFragmentTest {

    private final SpringTemplateEngine templateEngine = createTemplateEngine();

    @Test
    void ensureViolationIconWithHint() {

        final String messageKey = BreakViolationHint.messageKey(List.of(new BreakViolation(BreakViolationType.CONTINUITY))).orElseThrow();
        final String html = render("violation-icon", Map.of("messageKey", messageKey));

        assertThat(html)
            .contains("data-testid=\"break-violation-hint\"")
            .contains("title=\"Pausenregel verletzt: mehr als 6 Stunden Arbeit ohne Pause von mindestens 15 Minuten (ArbZG §4).\"");
    }

    @Test
    void ensureNoViolationIconWithoutHint() {

        final Map<String, Object> variables = new HashMap<>();
        variables.put("messageKey", null);

        assertThat(render("violation-icon", variables)).doesNotContain("break-violation-hint");
    }

    @Test
    void ensureDeduction() {
        assertThat(render("deduction", Map.of("deduction", "00:30"))).contains("inkl. −00:30 autom. Pausenabzug");
    }

    @Test
    void ensureNoDeductionWithoutValue() {

        final Map<String, Object> variables = new HashMap<>();
        variables.put("deduction", null);

        assertThat(render("deduction", variables)).doesNotContain("break-deduction");
    }

    @Test
    void ensureAllHintMessagesExist() {

        final List<List<BreakViolation>> combinations = List.of(
            List.of(new BreakViolation(BreakViolationType.DAILY)),
            List.of(new BreakViolation(BreakViolationType.CONTINUITY)),
            List.of(new BreakViolation(BreakViolationType.DAILY), new BreakViolation(BreakViolationType.CONTINUITY))
        );

        for (List<BreakViolation> violations : combinations) {
            final String messageKey = BreakViolationHint.messageKey(violations).orElseThrow();
            for (Locale locale : List.of(Locale.GERMAN, Locale.ENGLISH)) {
                final Context context = new Context(locale, Map.of("messageKey", messageKey));
                final String html = templateEngine.process("fragments/break-rules", Set.of("violation-icon"), context);
                assertThat(html).doesNotContain("??");
            }
        }
    }

    private String render(String fragment, Map<String, Object> variables) {
        final Context context = new Context(Locale.GERMAN, variables);
        return templateEngine.process("fragments/break-rules", Set.of(fragment), context);
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
