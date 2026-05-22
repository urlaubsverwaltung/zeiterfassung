# Handoff: Automatischer Pausenabzug (Issue #94)

**Datum:** 2026-05-23  
**Branch:** `94-automatic-break-deduction`

---

## Aktueller Stand

### Abgeschlossen

| Issue | Was | Commit |
|---|---|---|
| 01 | `StatutoryBreakRule` – ArbZG §4 Staffellogik + Tests | `5b8b65ed` |
| 02 (30%) | `AutomaticBreakDeductionSettings` – Record, Entity, Repository, Service, Liquibase-Migration | `c61f4755` |

### Issue 02 – Offen (~70%)

Noch fehlend für `docs/issues/02-automatic-break-deduction-settings.md`:

1. **`SettingsDto`** – zwei neue Felder: `Boolean automaticBreakDeductionIsActive`, `LocalDate automaticBreakDeductionActiveDate`
2. **`SettingsController`** – GET: Settings ins Model laden; POST: `updateAutomaticBreakDeductionSettings()` aufrufen
3. **Validierung** – Aktivierungsdatum darf nicht in der Vergangenheit liegen (serverseitige Bean Validation oder manuelle Prüfung im Controller, analog zu `subtractBreakFromTimeEntryActiveDate`)
4. **View** (`settings/settings.html`) – Toggle + Datumsfeld + BetrVG §87-Hinweis
5. **Controller-Tests** – TDD-Zyklen 6–8 (GET mit Settings im Model, POST speichert, POST mit Datum in Vergangenheit schlägt fehl)

---

## Nächste Schritte (TDD-Zyklen 6–8)

**Behavior 6 – Controller GET:** `AutomaticBreakDeductionSettings` erscheint im Model  
**Behavior 7 – Controller POST:** Settings werden korrekt gespeichert  
**Behavior 8 – Controller POST:** Validierungsfehler bei Datum in der Vergangenheit

Danach: Issue 03 (`BreakViolationChecker`) – ist unabhängig und reine Logik, ideal für TDD.

---

## Wichtige Referenzen

| Was | Wo |
|---|---|
| PRD | `docs/prd-94-automatischer-pausenabzug.md` |
| Issue 02 | `docs/issues/02-automatic-break-deduction-settings.md` |
| Vorlage Controller | `SettingsController.java` – `SubtractBreakFromTimeEntry`-Abschnitt |
| Vorlage View | `settings/settings.html` – `subtractBreakFromTimeEntry`-Block |
| Vorlage Controller-Tests | `SettingsControllerTest.java` |
| Neue Service-Methode | `SettingsService.updateAutomaticBreakDeductionSettings(boolean, Instant)` |

---

## Suggested Skills

- **`/tdd`** – Weiter mit Behaviors 6–8 (Controller-Tests), danach Issue 03
- **`/simplify`** – Nach Abschluss von Issue 02 den Controller-Code reviewen

---

## Kontext für den nächsten Agenten

- Kommunikationssprache: **Deutsch**
- Handoff-Dokumente ins Repo-Root, nicht ins Temp-Verzeichnis
- Das `SettingsController`-Pattern (GET/POST mit SettingsDto) ist gut etabliert – neue Settings analog zum `subtractBreakFromTimeEntry`-Block einbauen
- BetrVG §87-Hinweis muss **prominent** sein (nicht nur Tooltip) – das ist eine explizite Anforderung aus Issue 02
