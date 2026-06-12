# Issue 04: Pausenpflicht-Hinweis in Views

**Typ:** AFK  
**Blocked by:** 03-break-violation-checker  
**User stories:** 9, 10, 11, 12, 13, 14

## What to build

Die vom `BreakViolationChecker` ermittelten Verstöße werden in der Zeiteinträgeansicht sowie in der Wochen- und Monatsberichtsansicht pro Tageszeile als Hinweis angezeigt. Der Hinweis erscheint unabhängig davon, ob der automatische Pausenabzug aktiviert ist.

End-to-End: `BreakViolationChecker` in `TimeEntryDayServiceImpl` verdrahten → `TimeEntryDay` erhält `List<BreakViolation>` → View-Templates rendern Hinweis-Badge/Icon pro Tageszeile.

## Acceptance criteria

- [ ] Hinweis erscheint in der Zeiteinträgeansicht wenn Tagesregel verletzt
- [ ] Hinweis erscheint in der Zeiteinträgeansicht wenn Kontinuitätsregel verletzt
- [ ] Hinweis erscheint in der Wochenberichtsansicht pro betroffener Tageszeile
- [ ] Hinweis erscheint in der Monatsberichtsansicht pro betroffener Tageszeile
- [ ] Hinweis erscheint auch wenn der automatische Pausenabzug deaktiviert ist
- [ ] Kein Hinweis wenn kein Verstoß vorliegt
- [ ] Tagesregel- und Kontinuitätsregel-Verstoß sind visuell unterscheidbar oder werden kombiniert angezeigt (Entscheidung im UI-Review)

## Blocked by

- [Issue 03](03-break-violation-checker.md)