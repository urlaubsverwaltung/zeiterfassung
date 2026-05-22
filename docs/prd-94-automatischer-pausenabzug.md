# PRD: Automatischer Pausenabzug (Issue #94)

**Branch:** `94-automatic-break-deduction`  
**Domänensprache:** siehe `CONTEXT.md`

---

## Problem Statement

Arbeitnehmer erfassen häufig keine expliziten Pausenzeiten, obwohl das ArbZG §4 Mindestpausen vorschreibt. Mandanten (Arbeitgeber) haben bisher keine Möglichkeit, diese Pflichtpausen automatisch von der Bruttoarbeitszeit abzuziehen. Ohne dieses Feature weist das System eine zu hohe Arbeitszeit aus, was bei Betriebsprüfungen und der Überstundenberechnung zu falschen Ergebnissen führt.

---

## Solution

Ein mandantenweit konfigurierbares Feature, das täglich automatisch den fehlenden Teil der Pflichtpause von der Brutto-Arbeitszeit abzieht und so die Netto-Arbeitszeit berechnet. Bereits erfasste Pausen (`isBreak=true`) werden angerechnet – es wird nur die Differenz abgezogen. Das Feature wirkt ab einem konfigurierbaren Aktivierungsdatum; ältere Zeiteinträge bleiben unverändert. Unabhängig davon zeigt das System Pausenpflicht-Hinweise an, wenn ArbZG §4 verletzt wird.

---

## User Stories

1. Als Mandantenadmin möchte ich den automatischen Pausenabzug aktivieren, damit die gesetzliche Pausenpflicht systemseitig durchgesetzt wird.
2. Als Mandantenadmin möchte ich ein Aktivierungsdatum setzen können, damit Zeiteinträge vor diesem Datum nicht rückwirkend verändert werden.
3. Als Mandantenadmin möchte ich beim Aktivieren einen Hinweis auf die Mitbestimmungspflicht (BetrVG §87) sehen, damit ich rechtliche Risiken kenne.
4. Als Mandantenadmin möchte ich den automatischen Pausenabzug wieder deaktivieren können, damit ich die Konfiguration korrigieren kann.
5. Als Arbeitnehmer möchte ich meine Netto-Arbeitszeit pro Tag sehen, damit ich nachvollziehen kann, wie viel Arbeitszeit nach Pausenabzug verbleibt.
6. Als Arbeitnehmer möchte ich meine Brutto-Arbeitszeit pro Tag sehen, damit ich die Basis des automatischen Abzugs nachvollziehen kann.
7. Als Arbeitnehmer möchte ich sehen, wie viel Pause automatisch abgezogen wurde, damit ich die Differenz zwischen Brutto- und Netto-Arbeitszeit verstehe.
8. Als Arbeitnehmer möchte ich, dass bereits erfasste Pausen angerechnet werden, damit ich nicht doppelt belastet werde.
9. Als Arbeitnehmer möchte ich einen Pausenpflicht-Hinweis sehen, wenn meine erfasste Pausenzeit die Pflichtpause unterschreitet (Tagesregel).
10. Als Arbeitnehmer möchte ich einen Pausenpflicht-Hinweis sehen, wenn ich einen ununterbrochenen Arbeitsblock von über 6 Stunden habe (Kontinuitätsregel).
11. Als Arbeitnehmer möchte ich, dass der Pausenpflicht-Hinweis auch dann erscheint, wenn der automatische Abzug deaktiviert ist, damit ich auf Verstöße aufmerksam werde.
12. Als Arbeitnehmer möchte ich Pausenpflicht-Hinweise in der Tagesansicht sehen, damit ich sie direkt im Kontext meiner Zeiteinträge wahrnehme.
13. Als Arbeitnehmer möchte ich Pausenpflicht-Hinweise in der Wochenberichtsansicht sehen, damit ich Verstöße auf Wochenebene erkennen kann.
14. Als Arbeitnehmer möchte ich Pausenpflicht-Hinweise in der Monatsberichtsansicht sehen, damit ich Verstöße auf Monatsebene erkennen kann.
15. Als Arbeitnehmer möchte ich, dass Überstunden auf Basis der Netto-Arbeitszeit berechnet werden (wenn der Abzug aktiv ist), damit die Überstunden korrekt ausgewiesen werden.
16. Als Arbeitnehmer möchte ich, dass Zeiteinträge vor dem Aktivierungsdatum nicht verändert werden, damit historische Daten korrekt bleiben.
17. Als Vorgesetzter möchte ich im Bericht die Brutto- und Netto-Arbeitszeit meiner Mitarbeitenden sehen, damit ich Compliance auf Teamebene prüfen kann.
18. Als Vorgesetzter möchte ich Pausenpflicht-Hinweise für meine Mitarbeitenden sehen, damit ich auf Verstöße reagieren kann.
19. Als Nutzer möchte ich beim CSV-Export die Brutto-Arbeitszeit als `grossWorkingHours` exportieren, damit bestehende Auswertungen weiterhin funktionieren (nach Migration).
20. Als Nutzer möchte ich beim CSV-Export zusätzlich die Netto-Arbeitszeit als `netWorkingHours` exportieren (wenn Feature aktiv), damit ich die abgezogene Pausenzeit in Exporten sehen kann.
21. Als Nutzer möchte ich im CSV-Export keine `netWorkingHours`-Spalte sehen, wenn der automatische Abzug deaktiviert ist, damit das CSV-Format stabil bleibt.

---

## Implementation Decisions

### Modul 1: `StatutoryBreakRule` (neues tiefes Modul)

Reine Funktion ohne Seiteneffekte, die aus der Brutto-Arbeitszeit die gesetzliche Pflichtpause nach ArbZG §4 berechnet:

| Brutto-Arbeitszeit | Pflichtpause |
|--------------------|--------------|
| ≤ 6h               | 0 min        |
| > 6h bis ≤ 9h      | 30 min       |
| > 9h               | 45 min       |

Interface: `Duration calculate(Duration bruttoArbeitszeit)`

Wird von `MandatoryBreakDeductionCalculator` und `BreakViolationChecker` genutzt.

---

### Modul 2: `MandatoryBreakDeductionCalculator` (neue `WorkDurationCalculator`-Implementierung)

Implementiert das bestehende `WorkDurationCalculator`-Interface.

Algorithmus:
1. Brutto-Arbeitszeit = Summe aller `!isBreak`-Einträge
2. Pflichtpause = `StatutoryBreakRule.calculate(bruttoArbeitszeit)`
3. Erfasste Pausenzeit = Summe aller `isBreak=true`-Einträge
4. Abzug = `max(0, Pflichtpause − erfasste Pausenzeit)`
5. Netto-Arbeitszeit = Brutto-Arbeitszeit − Abzug

Gibt eine `WorkDuration` zurück, die der Netto-Arbeitszeit entspricht.

---

### Modul 3: `AutomaticBreakDeductionSettings` (neue Mandantenkonfiguration)

Analog zu `SubtractBreakFromTimeEntrySettings`:
- Record-Felder: `boolean active`, `Optional<Instant> enabledTimestamp`
- JPA-Entity mit eigenem DB-Table (Tenant-aware)
- Eigene Repository- und Service-Interfaces
- `enabledTimestamp` entspricht dem **Aktivierungsdatum** aus `CONTEXT.md`

`WorkDurationCalculationService` wird erweitert: Für Einträge nach dem Aktivierungsdatum wird der `MandatoryBreakDeductionCalculator` genutzt; Einträge davor bleiben unverändert (analog zur bestehenden Logik für `SubtractBreakFromTimeEntrySettings`).

---

### Modul 4: `BreakViolationChecker` (neues tiefes Modul)

Prüft zwei ArbZG-Regeln unabhängig vom aktivierten Abzug:

**Tagesregel:** `erfasste Pausenzeit < StatutoryBreakRule.calculate(bruttoArbeitszeit)`

**Kontinuitätsregel:** Ein ununterbrochener Arbeitsblock (nur durch `isBreak=true`-Einträge mit ≥ 15 min Dauer als Unterbrechung) überschreitet 6 Stunden. Lücken zwischen Einträgen ohne expliziten `isBreak=true`-Eintrag zählen nicht.

Rückgabe: `List<BreakViolation>` (leere Liste = kein Verstoß). `BreakViolation` ist ein Value Object mit Typ (DAILY / CONTINUITY).

Interface: `List<BreakViolation> check(List<TimeEntry> entriesForDay)`

---

### Modul 5: Erweiterung `TimeEntryDay`

`TimeEntryDay` erhält zusätzlich `grossWorkingHours` (bisherige `workDuration`) und `netWorkingHours` (nach Abzug). Wenn der automatische Abzug nicht aktiv ist, sind beide gleich. Außerdem: `List<BreakViolation> breakViolations`.

---

### Modul 6: CSV-Export (Breaking Change)

`ReportCsvService`:
- Spalte `workedHours` wird zu `grossWorkingHours` umbenannt (immer vorhanden)
- Spalte `netWorkingHours` wird ergänzt, wenn der automatische Pausenabzug aktiv ist

Migration-Hinweis: Dies ist ein Breaking Change im CSV-Format, der in Release Notes dokumentiert werden muss.

---

### Modul 7: Überstunden-Berechnung

`HasWorkDurationByUser.overtimeByUser()` nutzt `workDuration` für die Berechnung. Wenn der automatische Abzug aktiv ist, muss `workDuration` bereits die Netto-Arbeitszeit enthalten (was durch den `MandatoryBreakDeductionCalculator` sichergestellt wird – keine Änderung am `OvertimeService` nötig).

---

### Modul 8: Settings-UI

Das bestehende Settings-Formular erhält ein neues Abschnitt für `AutomaticBreakDeductionSettings` mit:
- Aktivierungs-Toggle
- Datumsfeld für das Aktivierungsdatum
- Pflichthinweis auf Mitbestimmung nach BetrVG §87 Abs. 1 Nr. 2 und Nr. 3

---

### Modul 9: Pausenpflicht-Hinweis in Views

Die `BreakViolationChecker`-Ergebnisse werden an `TimeEntryDay` angehängt und in Zeiteinträge-, Wochen- und Monatsberichtsansicht als Hinweis pro Tageszeile gerendert. Unabhängig vom Aktivierungsstatus des automatischen Abzugs.

---

## Testing Decisions

**Was einen guten Test ausmacht:** Tests prüfen ausschließlich beobachtbares Verhalten über öffentliche Interfaces – keine internen Implementierungsdetails, keine Mocks für eigene Domain-Logik. Nur an Systemgrenzen (Datenbank, externe Services) werden Mocks eingesetzt.

**Zu testende Module:**

| Modul | Testart | Priorität |
|---|---|---|
| `StatutoryBreakRule` | Unit-Test (reine Funktion) | Hoch – alle 3 Schwellwerte + Grenzwerte |
| `MandatoryBreakDeductionCalculator` | Unit-Test | Hoch – Abzugsformel, Anrechnung erfasster Pausen |
| `BreakViolationChecker` | Unit-Test | Hoch – Tagesregel, Kontinuitätsregel, Grenzfälle (15-min-Schwelle) |
| `WorkDurationCalculationService` (erweitert) | Unit-Test | Mittel – Routing nach Aktivierungsdatum |
| `ReportCsvService` (erweitert) | Integrationstest | Mittel – CSV-Header und Spalteninhalte |
| `TimeEntryDayServiceImpl` (erweitert) | Integrationstest | Mittel – End-to-End für einen Tag mit Abzug |

**Prior Art:**
- `OverlappingBreakCalculatorTest` – Vorlage für Calculator-Tests
- `WorkDurationCalculationServiceTest` – Vorlage für Service-Routing-Tests
- `TimeEntryDayServiceImplTest` – Vorlage für Day-Integrationstests

---

## Out of Scope

- Benachrichtigungen oder E-Mails bei Pausenverstößen
- Konfigurierbare Pausenschwellen (die ArbZG-Staffel ist fest)
- Pausenabzug auf Stundenbasis (nur Tagesgranularität)
- Lohnnachforderungen oder juristische Automatismen – das Feature verbessert die Compliance-Dokumentation, schützt aber nicht vor Vergütungsansprüchen
- Rückwirkende Anpassung von Zeiteinträgen vor dem Aktivierungsdatum

---

## Further Notes

- Das Feature ist abzugrenzen von `SubtractBreakFromTimeEntrySettings`, das zeitlich überlappende Pausen- und Arbeitseinträge behandelt. Beide Features können unabhängig voneinander aktiviert sein.
- Der CSV-Breaking-Change muss in den Release Notes explizit kommuniziert werden, damit Nutzer mit Downstream-Auswertungen rechtzeitig migrieren können.
- Das Aktivierungsdatum (`enabledTimestamp`) verhindert, dass historische Auswertungen verfälscht werden – es ist kein optionales Detail, sondern ein Kernelement des Features.