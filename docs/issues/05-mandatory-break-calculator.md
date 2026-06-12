# Issue 05: MandatoryBreakDeductionCalculator + WorkDurationCalculationService-Erweiterung

**Typ:** AFK  
**Blocked by:** 01-pflichtpause-rule, 02-automatic-break-deduction-settings  
**User stories:** 5, 6, 7, 8, 15, 16

## What to build

Eine neue `WorkDurationCalculator`-Implementierung `MandatoryBreakDeductionCalculator`, die aus einer Tages-Eintrags-Liste die Netto-Arbeitszeit berechnet. Dazu eine Erweiterung von `WorkDurationCalculationService`, die anhand der `AutomaticBreakDeductionSettings` zum neuen Calculator routet – nur für Einträge nach dem Aktivierungsdatum.

**Algorithmus `MandatoryBreakDeductionCalculator`:**
1. Brutto-Arbeitszeit = Summe aller `!isBreak`-Einträge
2. Pflichtpause = `StatutoryBreakRule.calculate(bruttoArbeitszeit)`
3. Erfasste Pausenzeit = Summe aller `isBreak=true`-Einträge
4. Abzug = `max(0, Pflichtpause − erfasste Pausenzeit)`
5. Netto-Arbeitszeit = Brutto-Arbeitszeit − Abzug

**Routing in `WorkDurationCalculationService`:**
- Feature inaktiv → `SimpleWorkDurationCalculator` (bisheriges Verhalten)
- Feature aktiv, Einträge vor Aktivierungsdatum → `SimpleWorkDurationCalculator`
- Feature aktiv, Einträge nach Aktivierungsdatum → `MandatoryBreakDeductionCalculator`
- Gemischte Tage → Einträge aufteilen, separat berechnen, addieren (analog zu bestehendem `SubtractBreakFromTimeEntrySettings`-Routing)

Da `WorkDurationCalculationService` die Netto-Arbeitszeit als `WorkDuration` zurückgibt, ergibt sich die korrekte Überstunden-Berechnung automatisch ohne Änderungen am `OvertimeService`.

## Acceptance criteria

- [ ] `MandatoryBreakDeductionCalculator` zieht bei Brutto > 6h die fehlende Pflichtpause ab
- [ ] Bereits erfasste Pausen werden angerechnet (kein doppelter Abzug)
- [ ] Kein Abzug bei Brutto ≤ 6h
- [ ] Kein negativer Abzug (wenn mehr Pause erfasst als Pflichtpause verlangt)
- [ ] Zeiteinträge vor dem Aktivierungsdatum werden weiterhin mit `SimpleWorkDurationCalculator` berechnet
- [ ] Überstunden basieren implizit auf Netto-Arbeitszeit (kein expliziter `OvertimeService`-Change nötig)
- [ ] Unit-Tests für `MandatoryBreakDeductionCalculator` (alle Staffelstufen, Anrechnung, Grenzfälle)
- [ ] Unit-Tests für das Routing in `WorkDurationCalculationService`

## Blocked by

- [Issue 01](01-statutory-break-rule.md)
- [Issue 02](02-automatic-break-deduction-settings.md)