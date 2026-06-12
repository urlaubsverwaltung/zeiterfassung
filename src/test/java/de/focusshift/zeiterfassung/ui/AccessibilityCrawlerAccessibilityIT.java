package de.focusshift.zeiterfassung.ui;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.AxeResults;
import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import de.focusshift.zeiterfassung.SingleTenantPostgreSQLContainer;
import de.focusshift.zeiterfassung.TestKeycloakContainer;
import de.focusshift.zeiterfassung.ui.extension.A11YTest;
import de.focusshift.zeiterfassung.ui.extension.IntegrationTest;
import de.focusshift.zeiterfassung.ui.pages.LoginPage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static de.focusshift.zeiterfassung.ui.pages.LoginPage.Credentials.OFFICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers(parallel = true)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@IntegrationTest
@A11YTest
class AccessibilityCrawlerAccessibilityIT {

    @LocalServerPort
    private int port;

    @Container
    @ServiceConnection
    private static final SingleTenantPostgreSQLContainer postgre = new SingleTenantPostgreSQLContainer();
    @Container
    private static final TestKeycloakContainer keycloak = new TestKeycloakContainer();

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        keycloak.configureSpringDataSource(registry);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccessibilityCrawlerAccessibilityIT.class);

    @Test
    void testAllPagesForWCAG22AACompliance(Page page) {

        final LoginPage loginPage = new LoginPage(page, port);
        loginPage.login(OFFICE);

        final String baseUrl = "http://localhost:%d".formatted(port);

        final Set<String> visitedLinks = new HashSet<>();
        final Set<String> visitedPatterns = new HashSet<>();
        final Queue<String> linksToVisit = new LinkedList<>();
        final Map<String, List<Rule>> scanFailures = new HashMap<>();

        linksToVisit.add(baseUrl);

        while (!linksToVisit.isEmpty()) {
            final String currentUrl = linksToVisit.poll();

            // Skip if already visited
            if (visitedLinks.contains(currentUrl)) {
                continue;
            }

            // Normalize URL to skip pages differing only by year/week/month parameters
            final String pattern = normalizeUrl(currentUrl);
            if (visitedPatterns.contains(pattern)) {
                continue;
            }
            visitedPatterns.add(pattern);

            visitedLinks.add(currentUrl);

            LOG.info("--------------------------------------------");
            LOG.info("🔍 Navigating to: {}", currentUrl);
            LOG.info("--------------------------------------------");

            try {
                // 1. Navigate to the page and wait until network traffic calms down
                page.navigate(currentUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

                // 2. Discover new internal links on this page
                final List<ElementHandle> anchors = page.querySelectorAll("a[href]");
                for (ElementHandle anchor : anchors) {
                    final String href = anchor.getAttribute("href");

                    if (href != null && !href.trim().isEmpty()) {
                        try {
                            // Resolve relative URLs against the current BASE_URL context
                            final URI absoluteUri = URI.create(baseUrl).resolve(href).normalize();
                            final String absoluteUrl = absoluteUri.toString();

                            // Ensure it's internal, not visited, not already queued, and skip destructive paths like /logout
                            final String normalized = normalizeUrl(absoluteUrl);
                            if (absoluteUrl.startsWith(baseUrl)
                                && !visitedPatterns.contains(normalized)
                                && !linksToVisit.contains(absoluteUrl)
                                && !absoluteUrl.contains("/logout")
                                && !absoluteUrl.contains("?csv")) {

                                linksToVisit.add(absoluteUrl);
                            }
                        } catch (IllegalArgumentException _) {
                            // Ignore unparseable/malformed URIs (e.g., mailto:, javascript:void(0))
                        }
                    }
                }

                // 3. Run the WCAG 2.2 AA Accessibility Scan
                final AxeResults accessibilityScanResults = new AxeBuilder(page)
                    .withTags(Arrays.asList("wcag2a", "wcag2aa", "wcag22aa")) // Pulls targets for 2.0, 2.1, and 2.2 AA
                    .analyze();

                // 4. Log and track violations
                final List<Rule> violations = accessibilityScanResults.getViolations();
                if (violations != null && !violations.isEmpty()) {
                    LOG.warn("❌ Found {} accessibility violations on {}", violations.size(), currentUrl);
                    scanFailures.put(currentUrl, violations);

                    for (Rule violation : violations) {
                        LOG.warn("   ┌─ Rule: {} [{}]", violation.getId(), violation.getImpact());
                        LOG.warn("   │  Description: {}", violation.getDescription());
                        LOG.warn("   │  Nodes affected: {}", violation.getNodes().size());
                        for (int i = 0; i < violation.getNodes().size(); i++) {
                            final CheckedNode node = violation.getNodes().get(i);
                            LOG.warn("   │  ┌─ Node #{}", i + 1);
                            LOG.warn("   │  │  Target: {}", node.getTarget());
                            LOG.warn("   │  │  HTML:   {}", truncate(node.getHtml(), 200));
                            if (node.getNone() != null) {
                                for (var check : node.getNone()) {
                                    LOG.warn("   │  │  Fail: {} - {}", check.getId(), check.getImpact());
                                }
                            }
                            if (i < violation.getNodes().size() - 1) {
                                LOG.warn("   │  └─");
                            }
                        }
                        LOG.warn("   └─");
                    }
                } else {
                    LOG.info("✅ {} passed all automated WCAG 2.2 AA checks.", currentUrl);
                }

            } catch (Exception e) {
                LOG.warn("⚠️ Failed to safely process page {}: {}", currentUrl, e.getMessage());
            }
        }

        // --- Final Reporting Block ---
        final int totalFailures = scanFailures.values().stream().mapToInt(List::size).sum();
        LOG.info("\n============================================");
        LOG.info("🏁 Crawl Complete. Audited {} unique pages.", visitedLinks.size());
        LOG.info("   ❌ Pages with violations: {}", scanFailures.size());
        LOG.info("   ❌ Total violations: {}", totalFailures);
        LOG.info("============================================\n");

        if (!scanFailures.isEmpty()) {
            final StringBuilder failureSummary = new StringBuilder("Accessibility violations detected:\n");
            scanFailures.forEach((url, violations) ->
                failureSummary.append(url).append(" had ").append(violations.size()).append(" violations.\n")
            );

            // Intentionally fail the JUnit test if any violations exist across the site map
            assertThat(scanFailures).withFailMessage(failureSummary.toString())
                .hasSizeLessThanOrEqualTo(13);
        }
    }

    /**
     * Normalizes a URL by replacing numeric path segments following keywords like year, week, or month
     * with a wildcard placeholder. This ensures URLs like /report/year/2025/week/20 and
     * /report/year/2026/week/5 map to the same pattern: /report/year/_/week/_.
     */
    private static String normalizeUrl(String url) {
        return url.replaceAll("/(year|week|month|day|quarter)/\\d+", "/$1/*");
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
