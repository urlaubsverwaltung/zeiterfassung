import type { TurboClickEvent, TurboRenderEvent } from "@hotwired/turbo";

export function onTurboClick(
  callback: (event: TurboClickEvent) => void,
  options?: EventListenerOptions,
) {
  document.addEventListener("turbo:click", callback, options);
}

export function onTurboRender(
  callback: (event: TurboRenderEvent) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:render", callback, options);
}
