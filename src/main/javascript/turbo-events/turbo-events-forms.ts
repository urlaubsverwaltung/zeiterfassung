import type { TurboSubmitStartEvent } from "@hotwired/turbo";

export function onTurboSubmitStart(
  callback: (event: TurboSubmitStartEvent) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:submit-start", callback, options);
}
