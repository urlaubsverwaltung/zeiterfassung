# Configurable Branding

**Date:** 2026-08-09
**Status:** Approved, ready for implementation planning

Port of [urlaubsverwaltung#6505](https://github.com/urlaubsverwaltung/urlaubsverwaltung/issues/6505) /
[PR#6506](https://github.com/urlaubsverwaltung/urlaubsverwaltung/pull/6506) to zeiterfassung.

## Goal

Self-hosted operators can rebrand the application by configuring its name. A deployment configured with
`zeiterfassung.branding.name=Zeiterfassung` shows "Zeiterfassung" in the header, in browser tab titles, in the web
manifest, and as the sender display name of outgoing e-mails.

## Scope

**In scope:** the application *name*, as a single deployment-wide property.

**Out of scope:**

- Custom logo / favicon assets (larger surface: multiple icon sizes, `browserconfig.xml`, `safari-pinned-tab.svg`) —
  own issue.
- The `urlaubsverwaltung.cloud` link in `_footer.html`.
- Per-tenant branding (would need a DB column, a settings UI, and per-request resolution via `TenantContextHolder`).
  A single global property covers the self-hosted rebranding case; per-tenant branding can later swap the value
  source behind the same `applicationName` model attribute.
- A runtime settings UI.
- The hardcoded German `h1`/paragraph strings in `timeclock/timeclock-edit.html` — pre-existing, unrelated.

Two parts of the UV PR have no counterpart here: zeiterfassung has neither springdoc/Swagger nor iCal export.

## Configuration

New package `de.focusshift.zeiterfassung.branding`, shaped like the existing `infobanner` package
(record properties + `@Configuration` registering an interceptor).

```java
@Validated
@ConfigurationProperties(prefix = "zeiterfassung.branding")
record BrandingConfigProperties(@NotEmpty String name) {}
```

```yaml
zeiterfassung:
  branding:
    name: Zeiterfassung
```

`@NotEmpty` means an explicitly blank value fails startup rather than rendering an empty header.

## Model plumbing

`BrandingDataProvider extends DataProviderInterceptor` adds a single model attribute, `applicationName`, to every
non-redirect, non-forward view. It uses the same base class as `FrameDataProvider`.

`BrandingConfiguration` (`@EnableConfigurationProperties(BrandingConfigProperties.class)`) registers it through a
`WebMvcConfigurer`, mirroring `InfoBannerConfiguration` — but **without** `@ConditionalOnProperty`, since branding
is always active.

An interceptor rather than a `@ControllerAdvice`: it must also run for error pages, which render the layout
(and therefore the logo).

**Header logo:** `_layout.html:186` changes from `th:text="#{navigation.zeiterfassung}"` to
`th:text="${applicationName}"`. The then-unused `navigation.zeiterfassung` key is deleted from both message files.

## Page titles

Titles move from the current prefix form (`Zeiterfassung - Berichte`) to UV's suffix form (`Berichte – Zeiterfassung`),
built in one place.

New `templates/fragments/page-title.html`:

```html
<title th:fragment="title(pageTitle)" th:text="|${pageTitle} – ${applicationName}|">
  Title
</title>
```

Separator is an en dash (`–`), matching UV.

Pages keep their existing `_layout::head(title=~{::title}, …)` wiring; only the `<title>` element's body changes:

```html
<title th:replace="~{fragments/page-title::title(#{report.page.meta.title})}"></title>
```

Exactly ten templates pass a title to the layout (`grep -l "_layout::head"`). Message keys become page-name-only:

| Page | Today | New value (de / en) |
|---|---|---|
| `timeentries/index.html` | `timeentries.page.meta.title` = `Zeiterfassung` | `Zeiteinträge` / `Time Entries` |
| `timeentries/index.html` | `timeentries.page.meta.title.person` = `Zeiterfassung - Zeiteinträge {0}` | `Zeiteinträge {0}` / `Time Entries {0}` |
| `reports/user-report.html`, `reports/user-report-edit-time-entry.html` | `report.page.meta.title` = `Zeiterfassung - Berichte` | `Berichte` / `Reports` |
| `usermanagement/users.html` | literal `Zeiterfassung - Personen` | new key `usermanagement.page.meta.title` = `Personen` / `Persons` |
| `settings/settings.html` | literal `Zeiterfassung - Einstellungen` | new key `settings.page.meta.title` = `Einstellungen` / `Settings` |
| `timeclock/timeclock-edit.html` | literal `Zeiterfassung` | new key `timeclock.page.meta.title` = `Stoppuhr` / `Stopwatch` (matches the page's `h1`) |
| `error/404.html`, `error/5xx.html` | literal `Zeiterfassung` | reuse existing `error-page.common.meta.title` = `Fehlerseite` |
| `error/403.html` | `error-page.common.meta.title` | key unchanged |
| `user/user-settings.html` | `user-settings.header.title` = `Personalisierung` | key unchanged |

`timeentries/index.html` has two `<title>` elements guarded by `th:if` (`viewedUser` null or not); both use the
fragment.

Side effects, all intended:

- Five pages gain real, translatable titles they never had (they carried untranslated German literals).
- Two dead keys — `logout.page.meta.title` and `time-entry.head.title`, both literally `Zeiterfassung`, referenced
  nowhere in `src/` — are deleted.
- The `<title>Zeiterfassung - Personen</title>` / `<title>Zeiterfassung - Bericht</title>` placeholders in the
  fragment-only templates (`usermanagement/user/*`, `reports/user-report-month.html`,
  `reports/user-report-week.html`, `reports/_user-select.html`) become plain `<title>Title</title>`, matching the
  convention already used by `icons/*` and other fragments. These tags are never rendered.

## Prose text

Messages that name the product inside a sentence take `{0}`. The German is reworded: the current
"für **die** Zeiterfassung" / "in **der** Zeiterfassung" carry a definite article that an arbitrary configured name
will not fit.

| Key | German | English |
|---|---|---|
| `settings.teaser-text` | `Globale Einstellungen für {0}.` | `Global settings for {0}.` |
| `usermanagement.teaser-text` | `Information: Es werden hier nur Personen angezeigt, welche sich mindestens einmal bei {0} angemeldet haben.` | `Information: Only persons who have logged in to {0} at least once are displayed here.` |

Both templates pass `${applicationName}` as the message argument. The English `usermanagement.teaser-text` also
loses its awkward "in the time recording" phrasing.

## E-mail

**Display names.** `application.yaml` derives both display names from the branding name so operators configure it
once:

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

Both stay `@NotBlank` in `EMailConfigurationProperties` and remain independently overridable.

This changes two existing defaults: `fromDisplayName` `zeiterfassung` → `Zeiterfassung` (capitalized), and
`replyToDisplayName` `replyto` → `Zeiterfassung`. The second is a visible behaviour change for anyone who never
set it explicitly — **note it in the release notes**.

**Feedback mail.** `FeedbackGivenListenerEmail` takes `BrandingConfigProperties` and builds the subject as
`"%s - Nutzer Feedback".formatted(branding.name())`. The Thymeleaf body template `mail/text/user-feedback.txt`
uses `[(${applicationName})]` in place of the literal "Zeiterfassung", with the variable set on the `Context`
alongside `sender` and `message`.

Known consequence, accepted: this mail is sent to focus shift's own feedback inbox
(`zeiterfassung.feedback.email.to`), not to the operator's users. On a rebranded instance it arrives saying
"Wir haben Feedback zu &lt;CustomName&gt; bekommen" — which identifies the source, but does mean an internal mail
carries customer branding.

## Web manifest

`src/main/resources/static/site.webmanifest` is deleted and replaced by a `WebManifestController` serving the same
path. That path is already `permitAll()` (`SecurityWebConfiguration:64`), so no security change is needed.

Response: `application/manifest+json`, from a Jackson-serialized record.

```json
{
  "name": "<branding.name>",
  "short_name": "<branding.name>",
  "start_url": "/",
  "icons": [
    { "src": "/favicons/android-chrome-192x192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "/favicons/android-chrome-512x512.png", "sizes": "512x512", "type": "image/png" }
  ],
  "theme_color": "#ffffff",
  "background_color": "#ffffff",
  "display": "standalone"
}
```

This also fixes an existing bug: the static file says `"name": "urlaubsverwaltung.cloud"` / `"short_name": "uv.cloud"`.
Dropped: `related_applications` (the UV Play Store app, wrong for this project) and the absolute
`start_url: https://urlaubsverwaltung.cloud/`.

`browserconfig.xml` stays static — tile colors and an icon path only, no brand text.

**To verify during implementation:** `_layout.html:137` links the manifest via `@{/site.webmanifest}`, and
`spring.web.resources.chain.strategy.content` content-hashes static resource URLs. Once the manifest is a controller
endpoint rather than a static resource the URL should pass through unversioned, but the implementation must assert
on the *rendered link*, not only on the endpoint.

## Tests

Following the existing patterns in `web/` and `infobanner/`:

- `BrandingConfigPropertiesTest` — `@NotEmpty` rejects blank and missing values (cf. `InfoBannerConfigurationTest`).
- `BrandingDataProviderTest` — `applicationName` is added to the model; skipped for redirect/forward views
  (cf. `FrameDataProviderTest`).
- `WebManifestControllerTest` — MockMvc: content type, and `name`/`short_name` reflect a custom configured value.
- `FeedbackGivenListenerEmailTest` — exists already and asserts the subject; update for the configurable name.
- One page-title test asserting the `– <Name>` suffix renders with a custom configured name, rather than ten
  near-identical tests.

## Documentation

README gains a Branding row, in the settings-table style already used for feedback and info-banner:

| Setting | Type | Description |
|---|---|---|
| `zeiterfassung.branding.name` | String | (default) `Zeiterfassung`. Application name shown in the header, page titles, e-mails and web manifest. |
