# Issue 07: Brutto- und Netto-Arbeitszeit in Views

**Typ:** AFK  
**Blocked by:** 06-timeentry-day-gross-net  
**User stories:** 5, 6, 7, 17, 18

## What to build

Die in Issue 06 ergänzten `grossWorkingHours` und `netWorkingHours` in der Zeiteinträgeansicht sowie in der Wochen- und Monatsberichtsansicht darstellen – aber nur wenn der automatische Pausenabzug aktiviert ist. Wenn das Feature inaktiv ist, zeigt die UI das bisherige Verhalten (keine zusätzliche Spalte/Zeile).

Vorgesetzte sehen dieselben Werte für Mitarbeitende in der Berichtsansicht.

## Acceptance criteria

- [ ] Zeiteinträgeansicht zeigt Brutto- und Netto-Arbeitszeit pro Tag wenn Feature aktiv
- [ ] Wochenberichtsansicht zeigt Brutto- und Netto-Arbeitszeit pro Tageszeile wenn Feature aktiv
- [ ] Monatsberichtsansicht zeigt Brutto- und Netto-Arbeitszeit pro Tageszeile wenn Feature aktiv
- [ ] Wenn Feature inaktiv: nur bisherige Arbeitszeit-Anzeige, keine zusätzlichen Spalten
- [ ] Vorgesetzte sehen Brutto- und Netto-Werte für Mitarbeitende in der Berichtsansicht
- [ ] Automatisch abgezogene Pausenzeit ist nachvollziehbar dargestellt (z.B. als Differenz oder eigene Zeile)

## Blocked by

- [Issue 06](06-timeentry-day-gross-net.md)