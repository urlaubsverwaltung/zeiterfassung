# Configurable Branding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the application name configurable via `zeiterfassung.branding.name` so self-hosted operators can rebrand the header, page titles, prose text, e-mails and web manifest.

**Architecture:** A validated `@ConfigurationProperties` record holds the name. A `DataProviderInterceptor` subclass puts it in every view model as `applicationName`. Templates read that attribute; a shared `fragments/page-title.html` formats every browser tab title as `<page> – <Name>`. Non-view consumers (feedback mail, web manifest) inject the properties record directly.

**Tech Stack:** Java 25, Spring Boot (Spring MVC + Thymeleaf), Maven, JUnit 5 + AssertJ + Mockito, prettier (HTML formatting via lint-staged).

## Global Constraints

- Property name: `zeiterfassung.branding.name`, type String, default `Zeiterfassung`, validated `@NotEmpty`.
- Model attribute name: `applicationName`.
- Title format: `<page> – <applicationName>` — page name first, en dash `–` (U+2013), application name last. Matches urlaubsverwaltung PR #6506.
- Thymeleaf fragment name: `page-title`, file `templates/fragments/page-title.html`.
- New Java code lives in package `de.focusshift.zeiterfassung.branding`.
- `src/test/resources/application.yaml` **shadows** `src/main/resources/application.yaml` on the test classpath — any new property with a default in main must be repeated there or `@SpringBootTest` will fail validation.
- HTML/CSS/JS files are prettier-formatted by lint-staged on commit. Run `npx prettier --write <files>` before `git add` for any touched `.html`.
- Never add a `Co-Authored-By` line to commit messages.
- Run tests with `./mvnw test -Dtest=<ClassName>`; full unit suite `./mvnw test`.

---

### Task 1: Branding properties, data provider, header logo

**Files:**
- Create: `src/main/java/de/focusshift/zeiterfassung/branding/BrandingConfigProperties.java`
- Create: `src/main/java/de/focusshift/zeiterfassung/branding/BrandingDataProvider.java`
- Create: `src/main/java/de/focusshift/zeiterfassung/branding/BrandingConfiguration.java`
- Create: `src/test/java/de/focusshift/zeiterfassung/branding/BrandingConfigurationTest.java`
- Create: `src/test/java/de/focusshift/zeiterfassung/branding/BrandingDataProviderTest.java`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/test/resources/application.yaml`
- Modify: `src/main/resources/templates/_layout.html:186`
- Modify: `src/main/resources/messages.properties:259`, `src/main/resources/messages_en.properties:257`
- Modify: `README.md` (after the Info-Banner section table)

**Interfaces:**
- Produces: `public record BrandingConfigProperties(String name)` in package `de.focusshift.zeiterfassung.branding` — accessor `name()`. Tasks 5 and 6 construct it directly as `new BrandingConfigProperties("...")` and inject it as a Spring bean.
- Produces: model attribute `applicationName` (String) on every non-redirect/non-forward view. Tasks 2–4 read it in templates.

- [ ] **Step 1: Write the failing configuration test**

`src/test/java/de/focusshift/zeiterfassung/branding/BrandingConfigurationTest.java`

```java
package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BrandingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(BrandingConfiguration.class);

    @Test
    void ensureBrandingDataProviderExists() {
        contextRunner
            .withPropertyValues("zeiterfassung.branding.name=Custom Name")
            .run(context -> assertThat(context).hasSingleBean(BrandingDataProvider.class));
    }

    @Test
    void ensureContextFailsWhenNameIsEmpty() {
        contextRunner
            .withPropertyValues("zeiterfassung.branding.name=")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void ensureContextFailsWhenNameIsMissing() {
        contextRunner
            .run(context -> assertThat(context).hasFailed());
    }
}
```

- [ ] **Step 2: Write the failing data provider test**

`src/test/java/de/focusshift/zeiterfassung/branding/BrandingDataProviderTest.java`

```java
package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class BrandingDataProviderTest {

    @Controller
    static class DummyController {
        @GetMapping("/test-endpoint")
        public String handleRequest() {
            return "dummy-view";
        }

        @GetMapping("/redirect-endpoint")
        public String handleRedirect() {
            return "redirect:/test-endpoint";
        }
    }

    private BrandingDataProvider sut;

    @BeforeEach
    void setUp() {
        sut = new BrandingDataProvider(new BrandingConfigProperties("Custom Name"));
    }

    @Test
    void ensureApplicationNameIsAddedToModel() throws Exception {
        perform(get("/test-endpoint"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("applicationName", "Custom Name"));
    }

    @Test
    void ensureApplicationNameIsNotAddedForRedirectView() throws Exception {
        perform(get("/redirect-endpoint"))
            .andExpect(status().is3xxRedirection())
            .andExpect(model().attributeDoesNotExist("applicationName"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(new DummyController())
            .addInterceptors(sut)
            .build()
            .perform(builder);
    }
}
```

- [ ] **Step 3: Run both tests to verify they fail**

Run: `./mvnw test -Dtest='Branding*Test'`
Expected: FAIL — compilation error, `BrandingConfiguration`/`BrandingDataProvider`/`BrandingConfigProperties` do not exist.

- [ ] **Step 4: Create the properties record**

`src/main/java/de/focusshift/zeiterfassung/branding/BrandingConfigProperties.java`

```java
package de.focusshift.zeiterfassung.branding;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "zeiterfassung.branding")
public record BrandingConfigProperties(@NotEmpty String name) {
}
```

It is `public` (unlike `InfoBannerConfigProperties`) because `de.focusshift.zeiterfassung.feedback` consumes it in Task 5.

- [ ] **Step 5: Create the data provider**

`src/main/java/de/focusshift/zeiterfassung/branding/BrandingDataProvider.java`

```java
package de.focusshift.zeiterfassung.branding;

import de.focusshift.zeiterfassung.web.DataProviderInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides the configured application name to every rendered view.
 */
class BrandingDataProvider extends DataProviderInterceptor {

    private final BrandingConfigProperties brandingConfigProperties;

    BrandingDataProvider(BrandingConfigProperties brandingConfigProperties) {
        this.brandingConfigProperties = brandingConfigProperties;
    }

    @Override
    protected void addData(@NonNull ModelAndView modelAndView, @NonNull HttpServletRequest request) {
        modelAndView.getModelMap().addAttribute("applicationName", brandingConfigProperties.name());
    }
}
```

- [ ] **Step 6: Create the configuration**

`src/main/java/de/focusshift/zeiterfassung/branding/BrandingConfiguration.java`

```java
package de.focusshift.zeiterfassung.branding;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(BrandingConfigProperties.class)
class BrandingConfiguration {

    @Bean
    BrandingDataProvider brandingDataProvider(BrandingConfigProperties brandingConfigProperties) {
        return new BrandingDataProvider(brandingConfigProperties);
    }

    @Bean
    WebMvcConfigurer brandingDataProviderWebMvcConfigurer(BrandingDataProvider brandingDataProvider) {
        return new BrandingWebMvcConfigurer(brandingDataProvider);
    }

    static class BrandingWebMvcConfigurer implements WebMvcConfigurer {

        private final BrandingDataProvider brandingDataProvider;

        BrandingWebMvcConfigurer(BrandingDataProvider brandingDataProvider) {
            this.brandingDataProvider = brandingDataProvider;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(brandingDataProvider);
        }
    }
}
```

There is deliberately no `@ConditionalOnProperty` — branding is always active, including on error pages.

- [ ] **Step 7: Run both tests to verify they pass**

Run: `./mvnw test -Dtest='Branding*Test'`
Expected: PASS (5 tests).

- [ ] **Step 8: Add the property default to main and test config**

`src/main/resources/application.yaml` — add as the first key under `zeiterfassung:`:

```yaml
zeiterfassung:
  branding:
    name: Zeiterfassung
  mail:
    from: zeiterfassung@localhost
```

`src/test/resources/application.yaml` — add the identical block as the first key under `zeiterfassung:` (this file shadows the main one on the test classpath):

```yaml
zeiterfassung:
  branding:
    name: Zeiterfassung
  mail:
    from: zeiterfassung@localhost
```

- [ ] **Step 9: Use the model attribute for the header logo**

`src/main/resources/templates/_layout.html` line 186 — replace

```html
        th:text="#{navigation.zeiterfassung}"
```

with

```html
        th:text="${applicationName}"
```

- [ ] **Step 10: Delete the now-unused message key**

Delete line 259 of `src/main/resources/messages.properties` and line 257 of `src/main/resources/messages_en.properties`:

```
navigation.zeiterfassung=Zeiterfassung
```

- [ ] **Step 11: Document the property in the README**

In `README.md`, directly after the Info-Banner property table (the row ending `Text of the info banner for the German Locale.`) and before `#### Launchpad`, insert:

```markdown
#### Branding

The application name can be changed, e.g. to match your own product name. It is shown in the header, in browser
tab titles, in the web manifest and as the display name of outgoing e-mails.

```yaml
zeiterfassung:
  branding:
    name: Zeiterfassung
```

| Property                    | Type   | Description                                                    |
|-----------------------------|--------|----------------------------------------------------------------|
| zeiterfassung.branding.name | String | (default) `Zeiterfassung`. Must not be empty.                  |

```

- [ ] **Step 12: Run the full unit test suite**

Run: `./mvnw test`
Expected: PASS. (Verifies the shadowed test `application.yaml` change keeps every `@SpringBootTest` booting.)

- [ ] **Step 13: Format and commit**

```bash
npx prettier --write src/main/resources/templates/_layout.html
git add src/main/java/de/focusshift/zeiterfassung/branding src/test/java/de/focusshift/zeiterfassung/branding \
  src/main/resources/application.yaml src/test/resources/application.yaml \
  src/main/resources/templates/_layout.html \
  src/main/resources/messages.properties src/main/resources/messages_en.properties README.md
git commit -m "Add configurable branding name"
```

---

### Task 2: Page title fragment

**Files:**
- Create: `src/main/resources/templates/fragments/page-title.html`
- Create: `src/test/java/de/focusshift/zeiterfassung/branding/PageTitleFragmentTest.java`

**Interfaces:**
- Consumes: model attribute `applicationName` from Task 1.
- Produces: Thymeleaf fragment `~{fragments/page-title::page-title(pageTitle)}` rendering a `<title>` element. Task 3 uses it on all ten pages.

- [ ] **Step 1: Write the failing test**

`src/test/java/de/focusshift/zeiterfassung/branding/PageTitleFragmentTest.java`

```java
package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PageTitleFragmentTest {

    private final TemplateEngine templateEngine = createTemplateEngine();

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

    private static TemplateEngine createTemplateEngine() {

        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");

        final TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw test -Dtest=PageTitleFragmentTest`
Expected: FAIL — template `fragments/page-title` cannot be resolved.

- [ ] **Step 3: Create the fragment**

`src/main/resources/templates/fragments/page-title.html`

```html
<!doctype html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
  <head>
    <meta charset="UTF-8" />
    <title
      th:fragment="page-title(pageTitle)"
      th:text="${applicationName == null} ? ${pageTitle} : |${pageTitle} – ${applicationName}|"
    >
      Page Title – Zeiterfassung
    </title>
  </head>
  <body></body>
</html>
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw test -Dtest=PageTitleFragmentTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Format and commit**

```bash
npx prettier --write src/main/resources/templates/fragments/page-title.html
git add src/main/resources/templates/fragments/page-title.html \
  src/test/java/de/focusshift/zeiterfassung/branding/PageTitleFragmentTest.java
git commit -m "Add page title fragment with configurable application name"
```

---

### Task 3: Apply the page title fragment

**Files:**
- Modify: `src/main/resources/templates/error/403.html:10`, `error/404.html:10`, `error/5xx.html:10`
- Modify: `src/main/resources/templates/reports/user-report.html:12`, `reports/user-report-edit-time-entry.html:12`
- Modify: `src/main/resources/templates/settings/settings.html:12`
- Modify: `src/main/resources/templates/timeclock/timeclock-edit.html:10`
- Modify: `src/main/resources/templates/timeentries/index.html:12-23`
- Modify: `src/main/resources/templates/usermanagement/users.html:10`
- Modify: `src/main/resources/templates/user/user-settings.html:12`
- Modify: `src/main/resources/messages.properties`, `src/main/resources/messages_en.properties`
- Modify (placeholder tidy): `src/main/resources/templates/usermanagement/user/permission-settings.html:10`, `usermanagement/user/overtime-account-settings.html:10`, `usermanagement/user/working-time-settings.html:10`, `usermanagement/user/working-time-edit.html:10`, `reports/user-report-month.html:10`, `reports/user-report-week.html:10`, `reports/_user-select.html:10`

**Interfaces:**
- Consumes: fragment `~{fragments/page-title::page-title(...)}` from Task 2.

- [ ] **Step 1: Change the message values that carried the brand name**

`src/main/resources/messages.properties`:

| Line | From | To |
|---|---|---|
| 340 | `report.page.meta.title=Zeiterfassung - Berichte` | `report.page.meta.title=Berichte` |
| 342 | `timeentries.page.meta.title=Zeiterfassung` | `timeentries.page.meta.title=Zeiteinträge` |
| 343 | `timeentries.page.meta.title.person=Zeiterfassung - Zeiteinträge {0}` | `timeentries.page.meta.title.person=Zeiteinträge {0}` |

`src/main/resources/messages_en.properties`:

| Line | From | To |
|---|---|---|
| 338 | `timeentries.page.meta.title=Zeiterfassung` | `timeentries.page.meta.title=Time Entries` |
| 339 | `timeentries.page.meta.title.person=Zeiterfassung - Time Entries {0}` | `timeentries.page.meta.title.person=Time Entries {0}` |
| 342 | `report.page.meta.title=Zeiterfassung - Reports` | `report.page.meta.title=Reports` |

- [ ] **Step 2: Add the three new page title keys**

`src/main/resources/messages.properties` — add `timeclock.page.meta.title=Stoppuhr` immediately above `timeclock.start=Start`; add `usermanagement.page.meta.title=Personen` immediately above `usermanagement.heading=Personen`; add `settings.page.meta.title=Einstellungen` immediately above `settings.heading=Einstellungen`.

`src/main/resources/messages_en.properties` — add `timeclock.page.meta.title=Stopwatch` immediately above `timeclock.start=Start`; add `usermanagement.page.meta.title=Persons` immediately above `usermanagement.heading=Persons`; add `settings.page.meta.title=Settings` immediately above `settings.heading=Settings`.

- [ ] **Step 3: Delete the two dead keys**

Delete `logout.page.meta.title=Zeiterfassung` (line 8) and `time-entry.head.title=Zeiterfassung` from **both** `messages.properties` and `messages_en.properties`. Neither key is referenced anywhere under `src/`.

Verify with: `grep -rn "logout.page.meta.title\|time-entry.head.title" src/` → no hits.

- [ ] **Step 4: Point the ten page titles at the fragment**

Replace each `<title>` element as follows.

`error/403.html:10`, `error/404.html:10`, `error/5xx.html:10` →

```html
    <title th:replace="~{fragments/page-title::page-title(#{error-page.common.meta.title})}"></title>
```

`reports/user-report.html:12` and `reports/user-report-edit-time-entry.html:12` →

```html
    <title th:replace="~{fragments/page-title::page-title(#{report.page.meta.title})}"></title>
```

`settings/settings.html:12` →

```html
    <title th:replace="~{fragments/page-title::page-title(#{settings.page.meta.title})}"></title>
```

`timeclock/timeclock-edit.html:10` →

```html
    <title th:replace="~{fragments/page-title::page-title(#{timeclock.page.meta.title})}"></title>
```

`usermanagement/users.html:10` →

```html
    <title th:replace="~{fragments/page-title::page-title(#{usermanagement.page.meta.title})}"></title>
```

`user/user-settings.html:12` →

```html
    <title th:replace="~{fragments/page-title::page-title(#{user-settings.header.title})}"></title>
```

`timeentries/index.html` lines 12–23 — both conditional titles, replacing the whole block:

```html
    <title
      th:if="${viewedUser == null}"
      th:replace="~{fragments/page-title::page-title(#{timeentries.page.meta.title})}"
    ></title>
    <title
      th:if="${viewedUser != null}"
      th:replace="~{fragments/page-title::page-title(#{timeentries.page.meta.title.person(${viewedUser.fullName})})}"
    ></title>
```

- [ ] **Step 5: Neutralise the dead `<title>` placeholders in fragment-only templates**

In each of `usermanagement/user/permission-settings.html`, `usermanagement/user/overtime-account-settings.html`, `usermanagement/user/working-time-settings.html`, `usermanagement/user/working-time-edit.html`, `reports/user-report-month.html`, `reports/user-report-week.html`, `reports/_user-select.html`, replace the `<title>Zeiterfassung - Personen</title>` / `<title>Zeiterfassung - Bericht</title>` line with:

```html
    <title>Title</title>
```

These templates are only ever included as fragments; the tag is never rendered. This matches the convention in `templates/icons/*`.

- [ ] **Step 6: Verify no brand literal remains in templates**

Run: `grep -rn "Zeiterfassung" src/main/resources/templates/`
Expected: only `fragments/page-title.html` (the static placeholder text inside the fragment).

- [ ] **Step 7: Run the full unit test suite**

Run: `./mvnw test`
Expected: PASS.

- [ ] **Step 8: Format and commit**

```bash
npx prettier --write "src/main/resources/templates/**/*.html"
git add src/main/resources/templates src/main/resources/messages.properties src/main/resources/messages_en.properties
git commit -m "Use configurable application name in page titles"
```

---

### Task 4: Configurable prose text

**Files:**
- Modify: `src/main/resources/messages.properties` (`settings.teaser-text`, `usermanagement.teaser-text`)
- Modify: `src/main/resources/messages_en.properties` (same two keys)
- Modify: `src/main/resources/templates/settings/settings.html:33`
- Modify: `src/main/resources/templates/usermanagement/users.html:34`
- Create: `src/test/java/de/focusshift/zeiterfassung/branding/BrandingMessagesTest.java`

**Interfaces:**
- Consumes: model attribute `applicationName` from Task 1.

- [ ] **Step 1: Write the failing test**

`src/test/java/de/focusshift/zeiterfassung/branding/BrandingMessagesTest.java`

```java
package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class BrandingMessagesTest {

    private final ResourceBundleMessageSource messageSource = createMessageSource();

    @Test
    void ensureSettingsTeaserTextUsesApplicationName() {

        final Object[] args = {"Custom Name"};

        assertThat(messageSource.getMessage("settings.teaser-text", args, Locale.GERMAN))
            .isEqualTo("Globale Einstellungen für Custom Name.");
        assertThat(messageSource.getMessage("settings.teaser-text", args, Locale.ENGLISH))
            .isEqualTo("Global settings for Custom Name.");
    }

    @Test
    void ensureUsermanagementTeaserTextUsesApplicationName() {

        final Object[] args = {"Custom Name"};

        assertThat(messageSource.getMessage("usermanagement.teaser-text", args, Locale.GERMAN))
            .isEqualTo("Information: Es werden hier nur Personen angezeigt, welche sich mindestens einmal bei Custom Name angemeldet haben.");
        assertThat(messageSource.getMessage("usermanagement.teaser-text", args, Locale.ENGLISH))
            .isEqualTo("Information: Only persons who have logged in to Custom Name at least once are displayed here.");
    }

    private static ResourceBundleMessageSource createMessageSource() {
        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw test -Dtest=BrandingMessagesTest`
Expected: FAIL — the messages still contain the literal "Zeiterfassung" and no `{0}`.

- [ ] **Step 3: Change the four message values**

`src/main/resources/messages.properties`:

```
usermanagement.teaser-text=Information: Es werden hier nur Personen angezeigt, welche sich mindestens einmal bei {0} angemeldet haben.
settings.teaser-text=Globale Einstellungen für {0}.
```

`src/main/resources/messages_en.properties`:

```
usermanagement.teaser-text=Information: Only persons who have logged in to {0} at least once are displayed here.
settings.teaser-text=Global settings for {0}.
```

The German is reworded from "in **der** Zeiterfassung" / "für **die** Zeiterfassung" — the definite article does not survive an arbitrary configured name.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw test -Dtest=BrandingMessagesTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Pass the application name from the templates**

`src/main/resources/templates/settings/settings.html` line 33 — replace

```html
            <p class="mt-2 text-dimmed" th:text="#{settings.teaser-text}">
```

with

```html
            <p
              class="mt-2 text-dimmed"
              th:text="#{settings.teaser-text(${applicationName})}"
            >
```

`src/main/resources/templates/usermanagement/users.html` line 34 — replace

```html
            <p class="mt-2 text-dimmed" th:text="#{usermanagement.teaser-text}">
```

with

```html
            <p
              class="mt-2 text-dimmed"
              th:text="#{usermanagement.teaser-text(${applicationName})}"
            >
```

- [ ] **Step 6: Run the full unit test suite**

Run: `./mvnw test`
Expected: PASS.

- [ ] **Step 7: Format and commit**

```bash
npx prettier --write src/main/resources/templates/settings/settings.html src/main/resources/templates/usermanagement/users.html
git add src/main/resources/templates src/main/resources/messages.properties src/main/resources/messages_en.properties \
  src/test/java/de/focusshift/zeiterfassung/branding/BrandingMessagesTest.java
git commit -m "Use configurable application name in teaser texts"
```

---

### Task 5: E-mail branding

**Files:**
- Modify: `src/main/resources/application.yaml` (mail display names)
- Modify: `src/main/java/de/focusshift/zeiterfassung/feedback/FeedbackGivenListenerEmail.java`
- Modify: `src/main/java/de/focusshift/zeiterfassung/feedback/FeedbackConfiguration.java`
- Modify: `src/main/resources/mail/text/user-feedback.txt`
- Modify: `src/test/java/de/focusshift/zeiterfassung/feedback/FeedbackGivenListenerEmailTest.java`

**Interfaces:**
- Consumes: `BrandingConfigProperties` (public record, accessor `name()`) from Task 1.

- [ ] **Step 1: Update the failing test**

In `src/test/java/de/focusshift/zeiterfassung/feedback/FeedbackGivenListenerEmailTest.java`:

Add `de.focusshift.zeiterfassung.branding.BrandingConfigProperties` to the `@SpringBootTest` classes and set the property:

```java
@SpringBootTest(
    classes = {FeedbackGivenListenerEmail.class, FeedbackConfigurationProperties.class, BrandingConfigProperties.class},
    properties = {
        "zeiterfassung.feedback.enabled=true",
        "zeiterfassung.feedback.email.to=zeiterfassung@example.org",
        "zeiterfassung.branding.name=Custom Name"
    }
)
```

with the import `import de.focusshift.zeiterfassung.branding.BrandingConfigProperties;`.

Change the expected subject in `ensureHandledFeedbackGivenEventSendsAnEmail`:

```java
            verify(eMailService).sendMail("feedback@example.org", "Custom Name - Nutzer Feedback", "rendered text email", "");
```

And add the new context variable assertion in `ensureHandledFeedbackGivenEventSendsAnEmailWithCorrectModel`, after the `message` assertion:

```java
            assertThat(model.getVariable("applicationName")).isEqualTo("Custom Name");
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw test -Dtest=FeedbackGivenListenerEmailTest`
Expected: FAIL — subject is `Zeiterfassung - Nutzer Feedback` and `applicationName` is null.

- [ ] **Step 3: Inject the branding properties into the listener**

In `src/main/java/de/focusshift/zeiterfassung/feedback/FeedbackGivenListenerEmail.java`, add the import
`import de.focusshift.zeiterfassung.branding.BrandingConfigProperties;`, add the field and constructor parameter,
and use it for the subject and the template context:

```java
    private final EMailService eMailService;
    private final ITemplateEngine mailTemplateEngine;
    private final FeedbackConfigurationProperties feedbackConfigurationProperties;
    private final BrandingConfigProperties brandingConfigProperties;

    FeedbackGivenListenerEmail(EMailService eMailService, ITemplateEngine emailTemplateEngine,
                               FeedbackConfigurationProperties feedbackConfigurationProperties,
                               BrandingConfigProperties brandingConfigProperties) {
        this.eMailService = eMailService;
        this.mailTemplateEngine = emailTemplateEngine;
        this.feedbackConfigurationProperties = feedbackConfigurationProperties;
        this.brandingConfigProperties = brandingConfigProperties;
    }
```

In `handleFeedbackGiven`:

```java
        final String applicationName = brandingConfigProperties.name();
        final String subject = "%s - Nutzer Feedback".formatted(applicationName);
```

and after `context.setVariable("message", message);`:

```java
        context.setVariable("applicationName", applicationName);
```

- [ ] **Step 4: Update the bean definition**

In `src/main/java/de/focusshift/zeiterfassung/feedback/FeedbackConfiguration.java`, add
`BrandingConfigProperties` (import `de.focusshift.zeiterfassung.branding.BrandingConfigProperties`) as a parameter of
the `FeedbackGivenListenerEmail` `@Bean` method and pass it through as the fourth constructor argument.

- [ ] **Step 5: Update the mail template**

`src/main/resources/mail/text/user-feedback.txt` — replace the first line

```
Wir haben Feedback zur Zeiterfassung bekommen :party:
```

with

```
Wir haben Feedback zu [(${applicationName})] bekommen :party:
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `./mvnw test -Dtest=FeedbackGivenListenerEmailTest`
Expected: PASS (2 tests).

- [ ] **Step 7: Derive the mail display names from the branding name**

`src/main/resources/application.yaml`:

```yaml
zeiterfassung:
  branding:
    name: Zeiterfassung
  mail:
    from: zeiterfassung@localhost
    fromDisplayName: ${zeiterfassung.branding.name}
    replyTo: replyto@localhost
    replyToDisplayName: ${zeiterfassung.branding.name}
```

Leave `src/test/resources/application.yaml` with its explicit literal display names — tests assert on them.

- [ ] **Step 8: Run the full unit test suite**

Run: `./mvnw test`
Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/de/focusshift/zeiterfassung/feedback src/main/resources/mail/text/user-feedback.txt \
  src/main/resources/application.yaml src/test/java/de/focusshift/zeiterfassung/feedback
git commit -m "Use configurable application name for e-mails"
```

---

### Task 6: Dynamic web manifest

**Files:**
- Delete: `src/main/resources/static/site.webmanifest`
- Create: `src/main/java/de/focusshift/zeiterfassung/branding/WebManifestController.java`
- Create: `src/test/java/de/focusshift/zeiterfassung/branding/WebManifestControllerTest.java`

**Interfaces:**
- Consumes: `BrandingConfigProperties` from Task 1.
- Produces: `GET /site.webmanifest` returning `application/manifest+json`.

- [ ] **Step 1: Write the failing test**

`src/test/java/de/focusshift/zeiterfassung/branding/WebManifestControllerTest.java`

```java
package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WebManifestControllerTest {

    private WebManifestController sut;

    @BeforeEach
    void setUp() {
        sut = new WebManifestController(new BrandingConfigProperties("Custom Name"));
    }

    @Test
    void ensureWebManifestUsesConfiguredApplicationName() throws Exception {

        standaloneSetup(sut).build()
            .perform(get("/site.webmanifest"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/manifest+json"))
            .andExpect(jsonPath("$.name").value("Custom Name"))
            .andExpect(jsonPath("$.short_name").value("Custom Name"))
            .andExpect(jsonPath("$.start_url").value("/"))
            .andExpect(jsonPath("$.display").value("standalone"))
            .andExpect(jsonPath("$.icons.length()").value(2))
            .andExpect(jsonPath("$.icons[0].src").value("/favicons/android-chrome-192x192.png"))
            .andExpect(jsonPath("$.icons[1].src").value("/favicons/android-chrome-512x512.png"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw test -Dtest=WebManifestControllerTest`
Expected: FAIL — `WebManifestController` does not exist.

- [ ] **Step 3: Create the controller**

`src/main/java/de/focusshift/zeiterfassung/branding/WebManifestController.java`

```java
package de.focusshift.zeiterfassung.branding;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Serves the web app manifest with the configured application name.
 */
@RestController
class WebManifestController {

    private static final List<Icon> ICONS = List.of(
        new Icon("/favicons/android-chrome-192x192.png", "192x192", "image/png"),
        new Icon("/favicons/android-chrome-512x512.png", "512x512", "image/png")
    );

    private final BrandingConfigProperties brandingConfigProperties;

    WebManifestController(BrandingConfigProperties brandingConfigProperties) {
        this.brandingConfigProperties = brandingConfigProperties;
    }

    @GetMapping(value = "/site.webmanifest", produces = "application/manifest+json")
    WebManifest webManifest() {
        final String name = brandingConfigProperties.name();
        return new WebManifest(name, name, "/", ICONS, "#ffffff", "#ffffff", "standalone");
    }

    record WebManifest(
        String name,
        @JsonProperty("short_name") String shortName,
        @JsonProperty("start_url") String startUrl,
        List<Icon> icons,
        @JsonProperty("theme_color") String themeColor,
        @JsonProperty("background_color") String backgroundColor,
        String display
    ) {
    }

    record Icon(String src, String sizes, String type) {
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw test -Dtest=WebManifestControllerTest`
Expected: PASS.

- [ ] **Step 5: Delete the static manifest**

```bash
git rm src/main/resources/static/site.webmanifest
```

The old file named `urlaubsverwaltung.cloud` / `uv.cloud` and linked the Urlaubsverwaltung Play Store app —
`related_applications` and the absolute `start_url` are intentionally dropped.

`/site.webmanifest` is already `permitAll()` in `SecurityWebConfiguration:64`, so no security change is needed.

- [ ] **Step 6: Verify the layout link still resolves**

`src/main/resources/templates/_layout.html:137` links the manifest with `th:href="@{/site.webmanifest}"` — leave
this line unchanged.

Spring Boot registers `ResourceUrlEncodingFilter` because `spring.web.resources.chain.enabled=true`, and Thymeleaf's
`@{...}` routes through `response.encodeURL()`. That filter only rewrites a path that `ResourceUrlProvider` resolves
to an actual static resource. Once `static/site.webmanifest` is deleted the lookup returns `null` and the URL is
emitted verbatim, so the link keeps pointing at the new controller endpoint.

Verify the static file is gone and the link is untouched:

```bash
test ! -f src/main/resources/static/site.webmanifest && echo "static manifest removed"
grep -n "site.webmanifest" src/main/resources/templates/_layout.html
```

Expected: `static manifest removed`, and line 137 still reading `<link rel="manifest" th:href="@{/site.webmanifest}" />`.

- [ ] **Step 7: Run the full unit test suite**

Run: `./mvnw test`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/de/focusshift/zeiterfassung/branding/WebManifestController.java \
  src/test/java/de/focusshift/zeiterfassung/branding/WebManifestControllerTest.java
git commit -m "Serve web manifest with configurable application name"
```

---

## Self-Review Notes

Spec coverage check:

| Spec section | Task |
|---|---|
| Configuration (`BrandingConfigProperties`, default, `@NotEmpty`) | 1 |
| Model plumbing (`BrandingDataProvider`, `BrandingConfiguration`) | 1 |
| Header logo + dead `navigation.zeiterfassung` key | 1 |
| Page title fragment | 2 |
| Page title application, message keys, dead keys, placeholder tidy | 3 |
| Prose text (`settings.teaser-text`, `usermanagement.teaser-text`) | 4 |
| Mail display names | 5 |
| Feedback mail subject + body | 5 |
| Web manifest | 6 |
| README documentation | 1 |
