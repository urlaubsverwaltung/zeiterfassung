# Issue 02: AutomaticBreakDeductionSettings – Mandantenkonfiguration & Settings-UI

**Typ:** AFK  
**Blocked by:** –  
**User stories:** 1, 2, 3, 4

## What to build

Persistierbare Mandantenkonfiguration für den automatischen Pausenabzug, analog zur bestehenden `SubtractBreakFromTimeEntrySettings`. Dazu ein erweitertes Settings-UI mit BetrVG-Pflichthinweis.

End-to-End: DB-Schema → Entity → Repository → Service → Settings-Controller → Settings-View.

Das Feature kann aktiviert/deaktiviert werden. Beim Aktivieren wird ein Aktivierungsdatum gesetzt, ab dem der Abzug wirksam ist. Zeiteinträge vor diesem Datum bleiben unverändert.

Das Settings-UI zeigt beim Aktivieren einen gut sichtbaren Hinweis: Der automatische Pausenabzug ist bei betriebsratspflichtigen Unternehmen mitbestimmungspflichtig (BetrVG §87 Abs. 1 Nr. 2 und Nr. 3). Die Aktivierung ersetzt keine Betriebsvereinbarung.

## Acceptance criteria

- [ ] Neue DB-Tabelle (Tenant-aware) persistiert `active` (boolean) und `enabled_timestamp` (Instant, nullable)
- [ ] Service-Interface mit `getSettings()` und `updateSettings(boolean, Instant)` vorhanden
- [ ] Settings-UI zeigt Toggle für Aktivierung und Datumsfeld für Aktivierungsdatum
- [ ] Settings-UI zeigt BetrVG §87-Hinweis prominent (nicht nur als Tooltip)
- [ ] Aktivierungsdatum kann nicht in der Vergangenheit vor dem heutigen Tag liegen (serverseitige Validierung)
- [ ] Deaktivierung ist jederzeit möglich
- [ ] Integrationstests für Persistenz und Controller (analog zu bestehenden Settings-Tests)

## Blocked by

None – can start immediately.