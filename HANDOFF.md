# Handoff: Automatischer Pausenabzug (Issue #94)

**Datum:** 2026-06-08
**Branch:** `94-automatic-break-deduction`

---

## Aktueller Stand

### Abgeschlossen

| Issue | Was | Letzte Commits |
|---|---|---|
| 01 | `StatutoryBreakRule` – ArbZG §4 Staffellogik + Tests | `5b8b65ed` |
| 02 | `AutomaticBreakDeductionSettings` – vollständig (DB → Service → Controller → View) | `f7fc7bb4`–`6c15ebf1` |

### Issue 02 – vollständig abgeschlossen

Alle Acceptance Criteria aus `docs/issues/02-automatic-break-deduction-settings.md` erfüllt:

- **Behavior 6:** `SettingsDto` um `automaticBreakDeductionIsActive` + `automaticBreakDeductionActiveDate` erweitert; Controller GET lädt Settings via `getAutomaticBreakDeductionSettings()` ins Model
- **Behavior 7:** Controller POST delegiert an `updateAutomaticBreakDeductionSettings()`, analog zum `subtractBreakFromTimeEntry`-Block
- **Behavior 8:** `SettingsDtoValidator` erhält `Clock` per Konstruktor und prüft, dass das Aktivierungsdatum nicht vor heute liegt (Error-Key: `settings.automatic-break-deduction.date.validation.past`)
- **View:** `settings/settings.html` – Toggle + Datumsfeld + **prominenter BetrVG §87-Hinweis** als gelbes Info-Banner
- **i18n:** alle Keys in `messages.properties` + `messages_en.properties`

---

## Nächste Schritte

**Issue 03 – `BreakViolationChecker`** (`docs/issues/03-break-violation-checker.md`)

Reine, zustandslose Logik – kein Persistence- oder UI-Layer. Ideal für TDD-Einstieg.

Zwei Regeln:
- **Tagesregel (ArbZG §4 Satz 1):** Erfasste Pausenzeit < gesetzliche Pflichtpause laut `StatutoryBreakRule`
- **Kontinuitätsregel (ArbZG §4 Satz 3):** Ununterbrochener Arbeitsblock > 6h (Unterbrechung = `isBreak=true`-Eintrag ≥ 15 min; zeitliche Lücken zählen nicht)

Interface: `List<BreakViolation> check(List<TimeEntry> entriesForDay)`
`BreakViolation` = Value Object mit Typ-Feld `DAILY` / `CONTINUITY`

Danach in Reihenfolge: Issue 04 (Views), 05 (Calculator), 06 (Datenmodell), 07 (Views), 08 (CSV).

---

## Wichtige Referenzen

| Was | Wo |
|---|---|
| PRD | `docs/prd-94-automatischer-pausenabzug.md` |
| Issue 03 | `docs/issues/03-break-violation-checker.md` |
| `StatutoryBreakRule` | Vorhanden, vollständig getestet (Issue 01) |
| Neuer Controller-Block | `SettingsController.java` – `automaticBreakDeduction`-Abschnitt |
| Neuer Validator | `SettingsDtoValidator.java` – `validateAutomaticBreakDeduction()` |
| Neue View-Sektion | `settings/settings.html` – `automaticBreakDeduction`-Block |

---

## Hinweise für den nächsten Agenten

- **Kommunikationssprache:** Deutsch
- **Commit-Messages:** immer auf Englisch (Betreffzeile und Body)
- **Handoff-Dokument:** als `HANDOFF.md` ins Repo-Root, nicht ins Temp-Verzeichnis
- `SettingsService` implementiert `AutomaticBreakDeductionSettingsService` – kein separater Service-Mock nötig
- `SettingsDtoValidator` hat jetzt einen `Clock`-Konstruktorparameter – `Clock` ist als Spring-Bean bereits vorhanden

---

## Suggested Skills

- **`/tdd`** – Issue 03 starten: `BreakViolationChecker` mit Tages- und Kontinuitätsregel (reine Logik, kein UI/Persistence)
- **`/simplify`** – Nach Abschluss von Issue 03 den neuen Code reviewen