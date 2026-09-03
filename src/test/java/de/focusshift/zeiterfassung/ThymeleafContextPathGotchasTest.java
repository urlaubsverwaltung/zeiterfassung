package de.focusshift.zeiterfassung;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the templates against links that silently lose the servlet context path when the application is deployed
 * at a subpath (e.g. {@code server.servlet.context-path=/zeiterfassung} behind a reverse proxy).
 *
 * <p>Thymeleaf only applies the context path to <strong>link expressions</strong> {@code @{...}} - the
 * {@code StandardLinkBuilder} prepends {@code request.getApplicationPath()} there. Two shapes bypass that:
 *
 * <ul>
 *   <li>{@code th:href="${url}"} - a plain variable expression is written out verbatim. Route the value through the
 *       preprocessing idiom {@code th:href="@{__${url}__}"} instead.</li>
 *   <li>{@code href="/foo"} - a hardcoded absolute path without any {@code th:} attribute is not touched by Thymeleaf
 *       at all. Use {@code th:href="@{/foo}"} instead.</li>
 * </ul>
 *
 * <p>This has been fixed three times now (#2182, #2217, #2258) because nothing made it visible to the build.
 *
 * @see <a href="https://github.com/urlaubsverwaltung/zeiterfassung/issues/2258">#2258</a>
 */
class ThymeleafContextPathGotchasTest {

    private static final Path TEMPLATES = Path.of("src", "main", "resources", "templates");

    private static final Pattern THYMELEAF_URL_ATTRIBUTE =
        Pattern.compile("\\b(th:href|th:action|th:src)=\"([^\"]*)\"");

    private static final Pattern HARDCODED_ABSOLUTE_URL_ATTRIBUTE =
        Pattern.compile("(?<!:)\\b(href|action|src)=\"(/[^/\"][^\"]*)\"");

    /**
     * Values that are intentionally emitted without a link expression of their own, keyed by template and expression
     * so the entries survive reformatting. Every entry needs a reason - if you cannot give one, the link is a bug.
     */
    private static final Set<AllowedRawUrl> ALLOWED_RAW_URLS = Set.of(
        new AllowedRawUrl("error/403.html", "${previousPageLink}", "the Referer header, an already absolute URL"),
        new AllowedRawUrl("error/404.html", "${previousPageLink}", "the Referer header, an already absolute URL"),
        new AllowedRawUrl("error/5xx.html", "${previousPageLink}", "the Referer header, an already absolute URL"),
        new AllowedRawUrl("launchpad/launchpad.html", "${app.url}", "URL of another application, not ours"),
        new AllowedRawUrl("launchpad/launchpad.html", "${app.icon}", "icon of another application, not ours"),
        new AllowedRawUrl("_navigation.html", "${menuHelpUrl}", "externally configured help URL"),
        new AllowedRawUrl("_top.html", "${href}", "fragment parameter, callers pass an @{...} resolved value"),
        new AllowedRawUrl("timeentries/fragments/time-entry-form.html", "${formAction}", "fragment parameter, callers pass an @{...} resolved value"),
        new AllowedRawUrl("timeentries/index.html", "${href}", "resolved with @{...} in a th:with right above"),
        new AllowedRawUrl("timeentries/index.html", "${viewedUser == null ? myself : other}", "both branches are resolved with @{...} in a th:with right above")
    );

    @Test
    void ensureUrlAttributesUseLinkExpressions() {

        final List<String> violations = new ArrayList<>();

        forEachTemplateLine((template, lineNumber, line) -> {
            final Matcher matcher = THYMELEAF_URL_ATTRIBUTE.matcher(line);
            while (matcher.find()) {
                final String attribute = matcher.group(1);
                final String expression = matcher.group(2);
                if (expression.contains("@{")) {
                    // already a link expression, or a ternary between two of them
                    continue;
                }
                if (ALLOWED_RAW_URLS.contains(new AllowedRawUrl(template, expression, null))) {
                    continue;
                }
                violations.add("%s:%d %s=\"%s\"".formatted(template, lineNumber, attribute, expression));
            }
        });

        assertThat(violations)
            .as("""
                These attributes are written out verbatim, so the servlet context path is lost when the application \
                is deployed at a subpath. Wrap the value in Thymeleaf's link expression, e.g. \
                th:href="@{__${someUrl}__}". If the URL genuinely must not be rewritten (it points somewhere else, \
                or a caller already resolved it), add it to ALLOWED_RAW_URLS with a reason. See #2258.""")
            .isEmpty();
    }

    @Test
    void ensureNoHardcodedAbsoluteUrls() {

        final List<String> violations = new ArrayList<>();

        forEachTemplateLine((template, lineNumber, line) -> {
            final Matcher matcher = HARDCODED_ABSOLUTE_URL_ATTRIBUTE.matcher(line);
            while (matcher.find()) {
                violations.add("%s:%d %s=\"%s\"".formatted(template, lineNumber, matcher.group(1), matcher.group(2)));
            }
        });

        assertThat(violations)
            .as("""
                These attributes hardcode an absolute path, so Thymeleaf never sees them and the servlet context path \
                is lost when the application is deployed at a subpath. Use a link expression instead, e.g. \
                th:src="@{/images/sloth.svg}". See #2258.""")
            .isEmpty();
    }

    private static void forEachTemplateLine(TemplateLineVisitor visitor) {
        try (Stream<Path> templates = Files.walk(TEMPLATES)) {
            templates
                .filter(path -> path.toString().endsWith(".html"))
                .sorted()
                .forEach(path -> {
                    final String template = TEMPLATES.relativize(path).toString().replace('\\', '/');
                    final List<String> lines = readLines(path);
                    for (int i = 0; i < lines.size(); i++) {
                        visitor.visit(template, i + 1, lines.get(i));
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException("could not walk " + TEMPLATES.toAbsolutePath(), e);
        }
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + path, e);
        }
    }

    @FunctionalInterface
    private interface TemplateLineVisitor {
        void visit(String template, int lineNumber, String line);
    }

    /**
     * @param reason why this value must not be rewritten. Not part of equality, it only documents the entry.
     */
    private record AllowedRawUrl(String template, String expression, String reason) {

        @Override
        public boolean equals(Object o) {
            return o instanceof AllowedRawUrl other
                && template.equals(other.template)
                && expression.equals(other.expression);
        }

        @Override
        public int hashCode() {
            return template.hashCode() * 31 + expression.hashCode();
        }
    }
}
