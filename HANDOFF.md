# Handoff: Automatischer Pausenabzug (Issue #94)

**Datum:** 2026-05-23  
**Branch:** `94-automatic-break-deduction`  
**Repo:** `/Users/honnel/dev/zeiterfassung`

---

## Was in dieser Session passiert ist

Alle Planungsartefakte für Issue #94 wurden erstellt. Es gibt **keine Implementierung** auf dem Branch – nur Dokumentation:

| Artefakt | Pfad |
|---|---|
| Ubiquitous Language | `CONTEXT.md` (inkl. `StatutoryBreakRule`-Mapping) |
| PRD | `docs/prd-94-automatischer-pausenabzug.md` |
| Issues (8 Slices) | `docs/issues/01-statutory-break-rule.md` bis `08-csv-export-breaking-change.md` |

---

## Nächste Schritte: Implementierung

Die Issues sind priorisiert und mit Abhängigkeiten versehen. Empfohlener Einstieg:

**Parallel startbar (keine Blocker):**
- `docs/issues/01-statutory-break-rule.md` – `StatutoryBreakRule` (reine Funktion, ideal für TDD)
- `docs/issues/02-automatic-break-deduction-settings.md` – Mandantenkonfiguration + Settings-UI

**Danach (jeweils abhängig):**
- Issue 03 (BreakViolationChecker) ← blocked by 01
- Issue 05 (MandatoryBreakDeductionCalculator) ← blocked by 01 + 02
- Issues 04, 06, 07, 08 folgen darauf

Vollständige Abhängigkeitsgrafik steht im PRD und in den einzelnen Issue-Dateien.

---

## Wichtige Entscheidungen dieser Session

- `PflichtpauseRule` heißt im Code **`StatutoryBreakRule`** (CONTEXT.md und alle Issues aktualisiert)
- `WorkDurationCalculator`-Strategy-Pattern bleibt erhalten; neuer `MandatoryBreakDeductionCalculator` ist eine weitere Implementierung
- Überstunden-Berechnung braucht **keine eigene Anpassung** – korrekte Netto-Arbeitszeit kommt implizit vom Calculator
- CSV-Export ist ein **Breaking Change** (`workedHours` → `grossWorkingHours`)
- BetrVG §87-Hinweis ist Pflicht im Settings-UI

---

## Suggested Skills

- **`/tdd`** – Ideal für `StatutoryBreakRule` und `BreakViolationChecker` (reine Funktionen mit klaren Eingabe-/Ausgabe-Erwartungen)
- **`/grill-with-docs`** – Falls beim Implementieren Unklarheiten entstehen, CONTEXT.md und PRD als Anker nutzen
- **`/simplify`** – Nach der Implementierung eines Slices, um den Code zu reviewen

---

## Kontext für den nächsten Agenten

- Kommunikationssprache: **Deutsch**
- Codestruktur bereits erkundet: Strategy-Pattern mit `WorkDurationCalculator`, `SubtractBreakFromTimeEntrySettings` als Analogie für neue Settings, `TimeEntryDayServiceImpl` als Orchestrierungspunkt
- Vor Implementierungsbeginn: Issue-Datei des gewählten Slices lesen, dann direkt starten