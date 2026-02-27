import type { TurboFetchRequestErrorEvent } from "@hotwired/turbo";

export function onTurboFetchRequestError(
  callback: (event: TurboFetchRequestErrorEvent) => void,
  options?: AddEventListenerOptions,
) {
  document.addEventListener("turbo:fetch-request-error", callback, options);
}
