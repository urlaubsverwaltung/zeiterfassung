import { Idiomorph } from "idiomorph";

/**
 * Morph without touching value of current <code>document.activeElement</code>.
 */
export function morphWithoutTouchingValueOfActiveElement(
  currentElement: HTMLElement,
  newElement: HTMLElement,
) {
  Idiomorph.morph(currentElement, newElement, {
    ignoreActive: false,
    ignoreActiveValue: true,
  });
}
