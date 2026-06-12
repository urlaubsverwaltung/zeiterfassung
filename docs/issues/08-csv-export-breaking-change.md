# Issue 08: CSV-Export – Breaking Change (grossWorkingHours / netWorkingHours)

**Typ:** AFK  
**Blocked by:** 05-mandatory-break-calculator  
**User stories:** 19, 20, 21

## What to build

Die bestehende CSV-Export-Spalte `workedHours` wird zu `grossWorkingHours` umbenannt. Wenn der automatische Pausenabzug aktiv ist, wird zusätzlich die Spalte `netWorkingHours` ergänzt.

Dies ist ein **Breaking Change** im CSV-Format und muss in den Release Notes dokumentiert werden.

End-to-End: `ReportCsvService` anpassen → Header und Datenzeilen → Integrationstests → Release-Notes-Eintrag.

## Acceptance criteria

- [ ] CSV-Header enthält `grossWorkingHours` statt `workedHours`
- [ ] CSV-Datenzeilen enthalten die Brutto-Arbeitszeit in `grossWorkingHours`
- [ ] CSV enthält `netWorkingHours`-Spalte wenn Feature aktiv
- [ ] CSV enthält keine `netWorkingHours`-Spalte wenn Feature inaktiv (kein leerer Wert, Spalte fehlt komplett)
- [ ] Integrationstests prüfen Header und Spaltenwerte für beide Zustände (Feature aktiv / inaktiv)
- [ ] Breaking Change ist in CHANGELOG / Release Notes dokumentiert

## Blocked by

- [Issue 05](05-mandatory-break-calculator.md)