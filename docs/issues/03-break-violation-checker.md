# Issue 03: BreakViolationChecker – Pausenpflicht-Prüfung

**Typ:** AFK  
**Blocked by:** 01-pflichtpause-rule  
**User stories:** 9, 10, 11

## What to build

Ein zustandsloses Modul `BreakViolationChecker`, das für eine Liste von Zeiteinträgen eines Tages prüft, ob ArbZG §4 verletzt wird. Die Prüfung läuft **unabhängig** davon, ob der automatische Pausenabzug aktiviert ist.

Zwei Regeln werden geprüft:

**Tagesregel (ArbZG §4 Satz 1):** Erfasste Pausenzeit (Summe aller `isBreak=true`-Einträge) ist kleiner als die Pflichtpause laut `StatutoryBreakRule`.

**Kontinuitätsregel (ArbZG §4 Satz 3):** Ein ununterbrochener Arbeitsblock überschreitet 6 Stunden. Als Unterbrechung zählen ausschließlich explizite `isBreak=true`-Einträge mit einer Dauer von mindestens 15 Minuten. Zeitliche Lücken zwischen Einträgen ohne `isBreak=true`-Eintrag zählen nicht als Unterbrechung.

Interface: `List<BreakViolation> check(List<TimeEntry> entriesForDay)`  
`BreakViolation` ist ein Value Object mit einem Typ-Feld (DAILY / CONTINUITY).

## Acceptance criteria

- [ ] Tagesregel schlägt an, wenn erfasste Pausenzeit < Pflichtpause
- [ ] Tagesregel schlägt nicht an, wenn erfasste Pausenzeit ≥ Pflichtpause
- [ ] Kontinuitätsregel schlägt an bei ununterbrochenem Arbeitsblock > 6h
- [ ] Kontinuitätsregel schlägt nicht an bei explizitem `isBreak=true`-Eintrag ≥ 15 min als Unterbrechung
- [ ] Lücken zwischen Arbeitseinträgen ohne `isBreak=true` gelten nicht als Unterbrechung
- [ ] Beide Verstöße können gleichzeitig zurückgegeben werden
- [ ] Leere Liste wenn kein Verstoß vorliegt
- [ ] Unit-Tests decken alle Fälle inkl. 15-min-Grenzwert der Kontinuitätsregel ab

## Blocked by

- [Issue 01](01-statutory-break-rule.md)