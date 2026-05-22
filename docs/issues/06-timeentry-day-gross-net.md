# Issue 06: TimeEntryDay – Brutto- und Netto-Arbeitszeit im Datenmodell

**Typ:** AFK  
**Blocked by:** 05-mandatory-break-calculator  
**User stories:** 5, 6, 7, 15, 17

## What to build

`TimeEntryDay` um explizite `grossWorkingHours` (Brutto-Arbeitszeit) und `netWorkingHours` (Netto-Arbeitszeit nach automatischem Pausenabzug) erweitern. Wenn der Abzug nicht aktiv ist, sind beide Werte gleich.

Der `MandatoryBreakDeductionCalculator` gibt bisher nur die Netto-Arbeitszeit zurück. Damit `TimeEntryDay` auch die Brutto-Arbeitszeit kennt, muss der Calculator-Aufruf so angepasst werden, dass beide Werte verfügbar sind (z.B. als Return-Typ `WorkDurationResult` mit `gross` und `net`).

Dieses Slice stellt nur das Datenmodell bereit. Die Darstellung in Views ist Issue 07.

## Acceptance criteria

- [ ] `TimeEntryDay` enthält `grossWorkingHours` und `netWorkingHours`
- [ ] Wenn Feature inaktiv: `grossWorkingHours == netWorkingHours`
- [ ] Wenn Feature aktiv: `netWorkingHours = grossWorkingHours - Abzug`
- [ ] `TimeEntryDayServiceImpl` befüllt beide Felder korrekt
- [ ] Bestehende Überstunden-Berechnung nutzt weiterhin `netWorkingHours` (war schon implizit durch `WorkDuration`)
- [ ] Integrationstests in `TimeEntryDayServiceImplTest` für beide Felder

## Blocked by

- [Issue 05](05-mandatory-break-calculator.md)