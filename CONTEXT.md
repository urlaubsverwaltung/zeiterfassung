# Ubiquitous Language – zeiterfassung

## Rechtliche Rahmenbedingungen & Disclaimer

### Mitbestimmungspflicht (BetrVG §87)
Der automatische Pausenabzug ist bei Betriebsratspflichtigen Unternehmen mitbestimmungspflichtig (BetrVG §87 Abs. 1 Nr. 2 und Nr. 3). Die Aktivierung durch den Mandanten ersetzt keine Betriebsvereinbarung. Das Settings-UI muss einen entsprechenden Hinweis enthalten.

### Retroaktiver Abzug vs. „Im voraus feststehende Ruhepausen"
ArbZG §4 Satz 1 verlangt Pausen, die im Voraus feststehen. Ein automatischer Nachtrags-Abzug ist nur rechtlich sauber, wenn der Arbeitgeber vorab (z.B. im Arbeitsvertrag oder per Betriebsvereinbarung) festlegt, dass Pausen zu nehmen sind und bei Nicht-Erfassung abgezogen werden.

### Vergütungsrechtliches Restrisiko
Der automatische Abzug reduziert die Netto-Arbeitszeit im System, beseitigt aber nicht den schuldrechtlichen Vergütungsanspruch des Arbeitnehmers, wenn die Pause tatsächlich nicht genommen wurde. Das Feature verbessert die Compliance-Dokumentation, schützt den Arbeitgeber aber nicht vor Lohnnachforderungen.

## Arbeitszeit-Begriffe

### Brutto-Arbeitszeit
Die Summe aller Zeiteinträge mit `isBreak=false` eines Tages – d.h. die reine Arbeitsleistungszeit vor einem automatischen Pausenabzug. Entspricht dem Begriff „Arbeitszeit" in §2 Abs. 1 ArbZG (Zeit vom Beginn bis zum Ende der Arbeit ohne die Ruhepausen).

### Netto-Arbeitszeit
Die Brutto-Arbeitszeit abzüglich des automatisch abgezogenen Anteils der Pflichtpause.

Formel: `Netto-Arbeitszeit = Brutto-Arbeitszeit − max(0, Pflichtpause − erfasste Pausenzeit)`

### Pflichtpause (`StatutoryBreakRule`)
Die gesetzlich vorgeschriebene Mindestpause gemäß ArbZG §4, abhängig von der Brutto-Arbeitszeit. Im Code als `StatutoryBreakRule` implementiert:

| Brutto-Arbeitszeit | Pflichtpause |
|--------------------|--------------|
| ≤ 6h               | 0 min        |
| > 6h bis ≤ 9h      | 30 min       |
| > 9h               | 45 min       |

### Aktivierungsdatum (des automatischen Pausenabzugs)
Das Datum, ab dem der automatische Pausenabzug für einen Mandanten wirksam ist. Zeiteinträge vor diesem Datum werden nicht rückwirkend angepasst. Wird analog zum bestehenden `enabledTimestamp` in `SubtractBreakFromTimeEntrySettings` gespeichert.

### Automatischer Pausenabzug (nach ArbZG)
Ein mandantenweit konfigurierbares Feature: Das System zieht automatisch den fehlenden Teil der Pflichtpause von der Brutto-Arbeitszeit ab, wenn die erfassten Pausenzeiten die Pflichtpause nicht erreichen. Bereits erfasste Pausen (isBreak=true-Einträge) werden angerechnet – es wird nur die Differenz abgezogen.

Abzugrenzen von `SubtractBreakFromTimeEntrySettings`, das ausschließlich zeitlich überlappende Pausen- und Arbeitseinträge behandelt.

### CSV-Export (im Kontext des automatischen Pausenabzugs)
Die bestehende Spalte `workedHours` wird zu `grossWorkingHours` umbenannt. Wenn der automatische Pausenabzug aktiv ist, wird zusätzlich die Spalte `netWorkingHours` ergänzt. Dies ist ein Breaking Change im CSV-Format.

### Pausenpflicht-Hinweis
Ein systemseitiger Hinweis pro Tageszeile, der bei Verletzung einer der beiden folgenden ArbZG-Regeln erscheint. Wird sowohl in der Zeiteinträgeansicht als auch in der Berichtsansicht (Woche/Monat) angezeigt. Die Prüfung erfolgt unabhängig davon, ob der automatische Pausenabzug aktiviert ist.

**Tagesregel (ArbZG §4 Satz 1):** `erfasste Pausenzeit < Pflichtpause`

**Kontinuitätsregel (ArbZG §4 Satz 3):** Ein ununterbrochener Arbeitsblock (nur durch explizite `isBreak=true`-Einträge mit ≥ 15 min Dauer unterbrochen) überschreitet 6h. Unerfasste Lücken zwischen Zeiteinträgen zählen nicht als Unterbrechung, da sie rechtlich keine nachgewiesene Ruhepause darstellen (ArbZG §4 Satz 1: „im voraus feststehende Ruhepausen").

### Überstunden (im Kontext des automatischen Pausenabzugs)
Wenn der automatische Pausenabzug aktiv ist, werden Überstunden auf Basis der Netto-Arbeitszeit berechnet:
`Überstunden = Netto-Arbeitszeit − ShouldWorkingHours`