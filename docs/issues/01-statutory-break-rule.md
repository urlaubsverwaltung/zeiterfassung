# Issue 01: StatutoryBreakRule – ArbZG §4 Kernlogik

**Typ:** AFK  
**Blocked by:** –  
**User stories:** Fundament für alle weiteren Slices

## What to build

Eine reine, zustandslose Funktion `StatutoryBreakRule`, die aus einer gegebenen Brutto-Arbeitszeit die gesetzliche Pflichtpause nach ArbZG §4 berechnet. Das Modul hat kein eigenes Persistenz- oder UI-Layer – es ist die testbare Kernlogik, auf der alle anderen Slices aufbauen.

Staffel:
| Brutto-Arbeitszeit | Pflichtpause |
|--------------------|--------------|
| ≤ 6h               | 0 min        |
| > 6h bis ≤ 9h      | 30 min       |
| > 9h               | 45 min       |

## Acceptance criteria

- [ ] `StatutoryBreakRule` berechnet für Brutto-Arbeitszeit ≤ 6h → 0 min
- [ ] `StatutoryBreakRule` berechnet für Brutto-Arbeitszeit > 6h und ≤ 9h → 30 min
- [ ] `StatutoryBreakRule` berechnet für Brutto-Arbeitszeit > 9h → 45 min
- [ ] Grenzwerte (genau 6h, genau 9h) sind korrekt behandelt
- [ ] Unit-Tests decken alle Staffelstufen und Grenzwerte ab
- [ ] Keine Abhängigkeiten zu Persistenz, UI oder anderen Feature-Modulen

## Blocked by

None – can start immediately.